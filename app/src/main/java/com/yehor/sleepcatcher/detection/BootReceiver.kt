package com.yehor.sleepcatcher.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yehor.sleepcatcher.LOG_TAG
import com.yehor.sleepcatcher.data.PrefsSettingsStore
import com.yehor.sleepcatcher.scheduling.CycleAlarmScheduler
import com.yehor.sleepcatcher.time.WallClock

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val settings = PrefsSettingsStore(context)
        val action = BootRestorePolicy.decide(
            armed = settings.armed,
            lastOnsetMillis = settings.lastOnsetMillis,
            plannedFinalWakeMillis = settings.plannedFinalWakeMillis,
            nowMillis = WallClock.nowMillis(),
        )

        when (action) {
            BootAction.DoNothing -> Unit
            BootAction.StartWatch -> SleepWatchService.start(context)
            is BootAction.StartWatchAndRestore -> {
                SleepWatchService.start(context)
                // The alarms died with the reboot. Forget them, otherwise the
                // scheduler would think the night is covered and keep a plan
                // that no longer exists.
                settings.scheduledSignalCount = 0
                settings.plannedFinalWakeMillis = 0L
                val result = CycleAlarmScheduler.create(context).scheduleFromOnset(action.onsetMillis)
                Log.i(LOG_TAG, "Restored the night after a reboot: $result")
            }
        }
    }
}
