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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.yehor.sleepcatcher.Notifications
import com.yehor.sleepcatcher.R
import com.yehor.sleepcatcher.alarm.AlarmRingService
import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.SignalKind
import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.data.PrefsSettingsStore
import com.yehor.sleepcatcher.detection.ForegroundAppDetector
import com.yehor.sleepcatcher.detection.SleepWatchService
import com.yehor.sleepcatcher.formatTime
import com.yehor.sleepcatcher.scheduling.AndroidAlarmGateway
import com.yehor.sleepcatcher.scheduling.CycleAlarmScheduler
import com.yehor.sleepcatcher.scheduling.ScheduleResult

class MainActivity : AppCompatActivity() {

    private lateinit var settings: PrefsSettingsStore
    private lateinit var scheduler: CycleAlarmScheduler
    private lateinit var alarms: AndroidAlarmGateway
    private lateinit var detector: ForegroundAppDetector

    private lateinit var statusText: TextView
    private lateinit var planText: TextView
    private lateinit var modeGroup: RadioGroup
    private lateinit var timePicker: TimePicker
    private lateinit var armButton: Button
    private lateinit var exactAlarmButton: Button
    private lateinit var fullScreenButton: Button

    // After the answer the user presses the button again.
    private val askForNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) toast(R.string.notifications_needed)
            refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = PrefsSettingsStore(this)
        scheduler = CycleAlarmScheduler.create(this)
        alarms = AndroidAlarmGateway(this)
        detector = ForegroundAppDetector(this)

        statusText = findViewById(R.id.status_text)
        planText = findViewById(R.id.plan_text)
        modeGroup = findViewById(R.id.mode_group)
        timePicker = findViewById(R.id.target_time)
        armButton = findViewById(R.id.arm_button)
        exactAlarmButton = findViewById(R.id.exact_alarm_button)
        fullScreenButton = findViewById(R.id.full_screen_button)

        timePicker.setIs24HourView(true)
        timePicker.hour = settings.targetHour
        timePicker.minute = settings.targetMinute
        if (settings.mode == WakeMode.CATCH_DREAM) {
            modeGroup.check(R.id.mode_catch_dream)
        } else {
            modeGroup.check(R.id.mode_refreshed)
        }

        armButton.setOnClickListener {
            if (settings.armed) {
                disarm()
            } else if (permissionsReady()) {
                arm()
            }
        }
        findViewById<Button>(R.id.preview_button).setOnClickListener {
            saveInputs()
            showPlan(scheduler.previewPlan(System.currentTimeMillis()))
        }
        findViewById<Button>(R.id.simulate_button).setOnClickListener {
            if (permissionsReady()) simulateFallingAsleep()
        }
        findViewById<Button>(R.id.usage_access_button).setOnClickListener {
            openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        exactAlarmButton.setOnClickListener {
            // Only Android 12 lets the user take exact alarms away from an alarm app.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri()))
            }
        }
        fullScreenButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, "package:$packageName".toUri()))
            }
        }
        findViewById<Button>(R.id.battery_button).setOnClickListener {
            openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        // Samsung likes to stop background services, so start the watch again if it should run.
        if (settings.armed) SleepWatchService.start(this)
        refresh()
    }

    // Returns true if everything is allowed, otherwise tells the user what is missing.
    private fun permissionsReady(): Boolean {
        if (!detector.hasUsageAccess()) {
            toast(R.string.usage_access_needed)
            return false
        }
        if (!alarms.canScheduleExact()) {
            toast(R.string.exact_alarms_needed)
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return false
        }
        return true
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
        AlarmRingService.stop(this)
        planText.text = ""
        refresh()
    }

    // Does the same as locking the phone after TikTok, so it can be tried during the day.
    private fun simulateFallingAsleep() {
        saveInputs()
        if (!settings.armed) {
            settings.armed = true
            SleepWatchService.start(this)
        }
        val now = System.currentTimeMillis()
        when (scheduler.scheduleFromOnset(now, now)) {
            ScheduleResult.SCHEDULED -> showPlan(scheduler.previewPlan(now))
            ScheduleResult.KEPT_EXISTING -> toast(R.string.kept_existing_plan)
            ScheduleResult.NOTHING_IN_FUTURE -> toast(R.string.nothing_in_future)
            ScheduleResult.NO_EXACT_ALARMS -> toast(R.string.exact_alarms_needed)
        }
        refresh()
    }

    private fun saveInputs() {
        if (modeGroup.checkedRadioButtonId == R.id.mode_catch_dream) {
            settings.mode = WakeMode.CATCH_DREAM
        } else {
            settings.mode = WakeMode.REFRESHED
        }
        settings.targetHour = timePicker.hour
        settings.targetMinute = timePicker.minute
    }

    private fun showPlan(plan: AlarmPlan) {
        val lines = mutableListOf<String>()
        for (signal in plan.signals) {
            if (signal.kind == SignalKind.SOFT_PULSE) {
                lines.add(getString(R.string.plan_soft_pulse, formatTime(signal.atMillis)))
            } else {
                lines.add(getString(R.string.plan_loud_alarm, formatTime(signal.atMillis)))
            }
        }
        if (plan.naturalWakeWarning) {
            lines.add(getString(R.string.plan_natural_wake_warning))
        }
        planText.text = lines.joinToString("\n")
    }

    private fun refresh() {
        val usageAccess = detector.hasUsageAccess()
        val exactAlarms = alarms.canScheduleExact()
        val fullScreen = Notifications.canUseFullScreenIntent(this)
        val batteryFree = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

        val lines = mutableListOf<String>()
        lines.add(getString(if (settings.armed) R.string.status_armed else R.string.status_not_armed))
        lines.add(getString(R.string.status_usage_access, allowedText(usageAccess)))
        lines.add(getString(R.string.status_exact_alarms, allowedText(exactAlarms)))
        lines.add(getString(R.string.status_full_screen_alarm, allowedText(fullScreen)))
        lines.add(getString(R.string.status_battery_exemption, allowedText(batteryFree)))
        if (settings.lastOnsetMillis > 0) {
            lines.add(getString(R.string.status_fell_asleep, formatTime(settings.lastOnsetMillis)))
        }
        if (settings.plannedFinalWakeMillis > 0) {
            lines.add(getString(R.string.status_final_wake, formatTime(settings.plannedFinalWakeMillis)))
        }
        statusText.text = lines.joinToString("\n")

        armButton.setText(if (settings.armed) R.string.disarm else R.string.arm)
        exactAlarmButton.isVisible = !exactAlarms
        fullScreenButton.isVisible = !fullScreen
    }

    private fun allowedText(allowed: Boolean): String =
        getString(if (allowed) R.string.status_allowed else R.string.status_missing)

    // Some phones do not have every settings page.
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
}
