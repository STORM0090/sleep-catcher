package com.yehor.sleepcatcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yehor.sleepcatcher.alarm.AlarmActivity
import com.yehor.sleepcatcher.alarm.AlarmRingService

object Notifications {

    const val ALARM_ID = 2

    private const val ALARM_CHANNEL = "alarm"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ALARM_CHANNEL,
                context.getString(R.string.notification_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                // The service plays the sound itself, so the channel stays quiet.
                setSound(null, null)
            },
        )
    }

    // Android 14 lets the user switch off full-screen alarms for an app.
    // Without them the alarm is only a heads-up notification on a locked phone.
    fun canUseFullScreenIntent(context: Context): Boolean =
        NotificationManagerCompat.from(context).canUseFullScreenIntent()

    fun alarmNotification(context: Context): Notification {
        val stop = PendingIntent.getService(
            context,
            0,
            AlarmRingService.stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showAlarm = PendingIntent.getActivity(
            context,
            0,
            Intent(context, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(showAlarm)
            .setFullScreenIntent(showAlarm, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.alarm_stop),
                stop,
            )
            .setOngoing(true)
            .build()
    }
}
