package com.yehor.sleepcatcher.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.yehor.sleepcatcher.Notifications
import com.yehor.sleepcatcher.R
import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.data.PrefsSettingsStore
import com.yehor.sleepcatcher.data.SettingsStore
import com.yehor.sleepcatcher.detection.ForegroundAppDetector
import com.yehor.sleepcatcher.detection.SleepWatchService
import com.yehor.sleepcatcher.scheduling.AndroidAlarmGateway
import com.yehor.sleepcatcher.scheduling.CycleAlarmScheduler
import com.yehor.sleepcatcher.scheduling.ScheduleResult
import com.yehor.sleepcatcher.time.WallClock
import java.time.ZoneId

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsStore
    private lateinit var scheduler: CycleAlarmScheduler
    private lateinit var gateway: AndroidAlarmGateway
    private lateinit var detector: ForegroundAppDetector

    private lateinit var statusText: TextView
    private lateinit var planText: TextView
    private lateinit var modeGroup: RadioGroup
    private lateinit var targetTime: TimePicker
    private lateinit var armButton: Button
    private lateinit var fullScreenButton: Button

    // Read each time, so the screen follows the phone when you travel.
    private val zone: ZoneId get() = ZoneId.systemDefault()

    // What to do once the notification permission dialog has been answered.
    private var afterPermission: (() -> Unit)? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val next = afterPermission
            afterPermission = null
            if (granted) next?.invoke() else toast(R.string.notifications_needed)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        settings = PrefsSettingsStore(this)
        scheduler = CycleAlarmScheduler.create(this)
        gateway = AndroidAlarmGateway(this)
        detector = ForegroundAppDetector(this)

        statusText = findViewById(R.id.status_text)
        planText = findViewById(R.id.plan_text)
        modeGroup = findViewById(R.id.mode_group)
        targetTime = findViewById(R.id.target_time)
        armButton = findViewById(R.id.arm_button)
        fullScreenButton = findViewById(R.id.full_screen_button)

        targetTime.setIs24HourView(true)
        targetTime.hour = settings.targetHour
        targetTime.minute = settings.targetMinute
        modeGroup.check(
            when (settings.mode) {
                WakeMode.REFRESHED -> R.id.mode_refreshed
                WakeMode.CATCH_DREAM -> R.id.mode_catch_dream
            },
        )

        armButton.setOnClickListener { if (settings.armed) disarm() else withPermissions(::arm) }
        findViewById<Button>(R.id.preview_button).setOnClickListener { preview() }
        findViewById<Button>(R.id.simulate_button).setOnClickListener { withPermissions(::simulateOnset) }
        findViewById<Button>(R.id.usage_access_button).setOnClickListener {
            openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        findViewById<Button>(R.id.battery_button).setOnClickListener {
            openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        fullScreenButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:$packageName".toUri()))
            }
        }
    }

    // Permissions can change while the app is in the background, so the
    // status is rebuilt every time the screen comes back.
    override fun onResume() {
        super.onResume()
        refresh()
    }

    // Arming needs usage access to see TikTok, and notifications because the
    // watch and the alarm run as foreground services.
    private fun withPermissions(action: () -> Unit) {
        if (!detector.hasUsageAccess()) {
            toast(R.string.usage_access_needed)
            return
        }
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            afterPermission = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        action()
    }

    private fun arm() {
        saveInputs()
        settings.armed = true
        SleepWatchService.start(this)
        refresh()
    }

    private fun disarm() {
        settings.armed = false
        scheduler.cancelScheduled()
        SleepWatchService.stop(this)
        planText.text = ""
        refresh()
    }

    private fun preview() {
        saveInputs()
        showPlan(scheduler.previewPlan(WallClock.nowMillis()))
    }

    // Runs the whole chain during the day instead of waiting for a real night.
    private fun simulateOnset() {
        saveInputs()
        if (!settings.armed) {
            settings.armed = true
            SleepWatchService.start(this)
        }
        when (val result = scheduler.scheduleFromOnset(WallClock.nowMillis())) {
            is ScheduleResult.Scheduled -> showPlan(result.plan)
            ScheduleResult.KeptExisting -> toast(R.string.kept_existing_plan)
            ScheduleResult.NothingInFuture -> toast(R.string.nothing_in_future)
        }
        refresh()
    }

    private fun saveInputs() {
        settings.mode = if (modeGroup.checkedRadioButtonId == R.id.mode_catch_dream) {
            WakeMode.CATCH_DREAM
        } else {
            WakeMode.REFRESHED
        }
        settings.targetHour = targetTime.hour
        settings.targetMinute = targetTime.minute
    }

    private fun showPlan(plan: AlarmPlan) {
        planText.text = PlanFormatter.lines(plan, signalLabels(), zone).joinToString("\n")
    }

    private fun refresh() {
        val status = StatusSnapshot(
            armed = settings.armed,
            usageAccess = detector.hasUsageAccess(),
            exactAlarms = gateway.canScheduleExact(),
            fullScreenAlarm = Notifications.canUseFullScreenIntent(this),
            batteryExempt = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName),
            lastOnsetMillis = settings.lastOnsetMillis,
            plannedFinalWakeMillis = settings.plannedFinalWakeMillis,
        )
        statusText.text = StatusLines.build(status, statusLabels(), zone).joinToString("\n")
        armButton.setText(if (status.armed) R.string.disarm else R.string.arm)
        fullScreenButton.isVisible = !status.fullScreenAlarm
    }

    // Some phones lack one settings page or another; that must not crash the app.
    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.settings_not_found)
        }
    }

    private fun toast(@StringRes text: Int) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun signalLabels() = SignalLabels(
        softPulse = getString(R.string.plan_soft_pulse),
        loudAlarm = getString(R.string.plan_loud_alarm),
        naturalWakeWarning = getString(R.string.plan_natural_wake_warning),
    )

    private fun statusLabels() = StatusLabels(
        armed = getString(R.string.status_armed),
        notArmed = getString(R.string.status_not_armed),
        usageAccess = getString(R.string.status_usage_access),
        exactAlarms = getString(R.string.status_exact_alarms),
        fullScreenAlarm = getString(R.string.status_full_screen_alarm),
        batteryExemption = getString(R.string.status_battery_exemption),
        allowed = getString(R.string.status_allowed),
        missing = getString(R.string.status_missing),
        fellAsleep = getString(R.string.status_fell_asleep),
        finalWake = getString(R.string.status_final_wake),
    )
}
