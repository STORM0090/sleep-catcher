package com.yehor.sleepcatcher.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yehor.sleepcatcher.alarm.AlarmReceiver
import com.yehor.sleepcatcher.core.SignalKind
import com.yehor.sleepcatcher.openAppIntent

class AndroidAlarmGateway(private val context: Context) : AlarmGateway {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canScheduleExact(): Boolean {
        // Before Android 12 every app was allowed to set exact alarms.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return alarmManager.canScheduleExactAlarms()
    }

    // setAlarmClock rings on time even in deep sleep mode and shows the alarm icon in the status bar.
    override fun scheduleLoud(index: Int, atMillis: Long) {
        val info = AlarmManager.AlarmClockInfo(atMillis, openAppIntent(context))
        alarmManager.setAlarmClock(info, alarmIntent(index, SignalKind.LOUD_ALARM))
    }

    override fun scheduleSoft(index: Int, atMillis: Long) {
        val intent = alarmIntent(index, SignalKind.SOFT_PULSE)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        }
    }

    override fun cancel(index: Int) {
        // FLAG_NO_CREATE gives null if there is no such alarm.
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val existing = PendingIntent.getBroadcast(context, index, fireIntent(null), flags)
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    // Alarms are told apart by the index (the request code). Extras do not count.
    private fun alarmIntent(index: Int, kind: SignalKind): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, index, fireIntent(kind), flags)
    }

    private fun fireIntent(kind: SignalKind?): Intent {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.action = AlarmReceiver.ACTION_FIRE
        if (kind != null) intent.putExtra(AlarmReceiver.EXTRA_KIND, kind.nativeId)
        return intent
    }
}
