package com.yehor.sleepcatcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
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

    fun alarmNotification(context: Context): Notification {
        val stop = PendingIntent.getService(
            context,
            0,
            AlarmRingService.stopIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.notification_alarm_title))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_alarm_stop),
                stop,
            )
            .setOngoing(true)
            .build()
    }
}
