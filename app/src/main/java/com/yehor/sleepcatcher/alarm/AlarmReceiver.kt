package com.yehor.sleepcatcher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yehor.sleepcatcher.LOG_TAG
import com.yehor.sleepcatcher.core.SignalKind

// Woken by AlarmManager at the time the planner picked.
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val kind = SignalKind.fromNativeId(intent.getIntExtra(EXTRA_KIND, UNKNOWN_KIND)) ?: return
        Log.i(LOG_TAG, "Signal fired: $kind")

        when (kind) {
            SignalKind.SOFT_PULSE -> SignalPlayer.softPulse(context)
            SignalKind.LOUD_ALARM -> AlarmRingService.start(context)
        }
    }

    companion object {
        const val ACTION_FIRE = "com.yehor.sleepcatcher.action.FIRE_SIGNAL"
        const val EXTRA_KIND = "signal_kind"
        private const val UNKNOWN_KIND = -1
    }
}
