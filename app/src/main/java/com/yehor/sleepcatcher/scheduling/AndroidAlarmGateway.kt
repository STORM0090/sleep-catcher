package com.yehor.sleepcatcher.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yehor.sleepcatcher.alarm.AlarmReceiver
import com.yehor.sleepcatcher.core.SignalKind

class AndroidAlarmGateway(private val context: Context) : AlarmGateway {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    // setAlarmClock is the strongest promise Android makes: it survives Doze
    // and the system shows the pending alarm in the status bar.
    override fun scheduleLoud(index: Int, atMillis: Long) {
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(atMillis, null),
            operation(index, SignalKind.LOUD_ALARM),
        )
    }

    override fun scheduleSoft(index: Int, atMillis: Long) {
        val operation = operation(index, SignalKind.SOFT_PULSE)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
        }
    }

    override fun cancel(index: Int) {
        val existing = PendingIntent.getBroadcast(
            context,
            index,
            fireIntent(kind = null),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    // Alarms are told apart by their request code; the extras are not part of
    // the identity, which is why cancelling can pass a null kind.
    private fun operation(index: Int, kind: SignalKind): PendingIntent = PendingIntent.getBroadcast(
        context,
        index,
        fireIntent(kind),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun fireIntent(kind: SignalKind?): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            if (kind != null) putExtra(AlarmReceiver.EXTRA_KIND, kind.nativeId)
        }
}
