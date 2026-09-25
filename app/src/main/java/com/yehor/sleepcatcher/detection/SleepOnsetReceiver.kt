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
import com.yehor.sleepcatcher.time.WallClock

// Registered at runtime by SleepWatchService: Android never delivers
// SCREEN_OFF to receivers declared in the manifest.
class SleepOnsetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_OFF) return

        val now = WallClock.nowMillis()
        val detector = ForegroundAppDetector(context)
        val handler = SleepOnsetHandler(PrefsSettingsStore(context), CycleAlarmScheduler.create(context))

        when (val outcome = handler.onScreenOff(now, detector.hasUsageAccess()) { detector.lastForegroundPackage(now) }) {
            OnsetOutcome.NotArmed ->
                Log.i(LOG_TAG, "Screen off, but the alarm is not armed")
            OnsetOutcome.NoUsageAccess -> {
                Log.w(LOG_TAG, "Screen off, but usage access is missing")
                Notifications.showProblem(context, R.string.problem_usage_access)
            }
            is OnsetOutcome.NotTriggerApp ->
                Log.i(LOG_TAG, "Screen off after ${outcome.packageName}, not a trigger app")
            is OnsetOutcome.Planned -> when (val result = outcome.result) {
                is ScheduleResult.Scheduled ->
                    Log.i(LOG_TAG, "Fell asleep at $now, planned ${result.scheduled}")
                ScheduleResult.KeptExisting ->
                    Log.i(LOG_TAG, "Fell asleep again at $now, too close to the target to move the alarm")
                ScheduleResult.NothingInFuture ->
                    Log.i(LOG_TAG, "Fell asleep at $now, but every planned signal is already in the past")
            }
        }
    }
}
