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

// Registered by SleepWatchService. Android never sends SCREEN_OFF to receivers from the manifest.
class SleepOnsetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_OFF) return

        val settings = PrefsSettingsStore(context)
        if (!settings.armed) return

        val detector = ForegroundAppDetector(context)
        if (!detector.hasUsageAccess()) {
            Log.w(LOG_TAG, "Screen off, but usage access is missing")
            Notifications.showProblem(context, R.string.problem_usage_access)
            return
        }

        val now = System.currentTimeMillis()
        val app = detector.lastForegroundPackage(now)
        if (!detector.isTikTok(app)) {
            Log.i(LOG_TAG, "Screen off after $app, not TikTok")
            return
        }

        val result = CycleAlarmScheduler.create(context).scheduleFromOnset(now, now)
        Log.i(LOG_TAG, "Fell asleep at $now: $result")
        if (result == ScheduleResult.NO_EXACT_ALARMS) {
            Notifications.showProblem(context, R.string.problem_exact_alarms)
        }
    }
}
