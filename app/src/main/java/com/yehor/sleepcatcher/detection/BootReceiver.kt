package com.yehor.sleepcatcher.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yehor.sleepcatcher.LOG_TAG
import com.yehor.sleepcatcher.Notifications
import com.yehor.sleepcatcher.R
import com.yehor.sleepcatcher.data.PrefsSettingsStore
import com.yehor.sleepcatcher.scheduling.CycleAlarmScheduler
import com.yehor.sleepcatcher.scheduling.ScheduleResult

// A reboot removes all alarms and stops all services, so bring them back.
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val settings = PrefsSettingsStore(context)
        if (!settings.armed) return
        SleepWatchService.start(context)

        val now = System.currentTimeMillis()
        val nightIsRunning = settings.lastOnsetMillis > 0 && settings.plannedFinalWakeMillis > now
        if (!nightIsRunning) return

        // The settings still say the alarms are set, but the reboot deleted them.
        // Clear that first, otherwise the scheduler would keep a plan that no longer exists.
        settings.scheduledSignalCount = 0
        settings.plannedFinalWakeMillis = 0L
        val result = CycleAlarmScheduler.create(context).scheduleFromOnset(settings.lastOnsetMillis, now)
        Log.i(LOG_TAG, "Restored the night after a reboot: $result")
        if (result == ScheduleResult.NO_EXACT_ALARMS) {
            Notifications.showProblem(context, R.string.problem_exact_alarms)
        }
    }
}
