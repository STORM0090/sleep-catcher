package com.yehor.sleepcatcher

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yehor.sleepcatcher.alarm.AlarmActivity
import com.yehor.sleepcatcher.alarm.AlarmRingService

object Notifications {

    const val WATCH_ID = 1
    const val ALARM_ID = 2
    private const val PROBLEM_ID = 3

    private const val WATCH_CHANNEL = "sleep_watch"
    private const val ALARM_CHANNEL = "alarm"
    private const val PROBLEM_CHANNEL = "problems"

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun ensureChannels(context: Context) {
        val watch = NotificationChannel(
            WATCH_CHANNEL, context.getString(R.string.notification_channel_watch), NotificationManager.IMPORTANCE_LOW
        )
        val alarm = NotificationChannel(
            ALARM_CHANNEL, context.getString(R.string.notification_channel_alarm), NotificationManager.IMPORTANCE_HIGH
        )
        // The alarm service plays the sound itself.
        alarm.setSound(null, null)
        val problems = NotificationChannel(
            PROBLEM_CHANNEL,
            context.getString(R.string.notification_channel_problems),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(watch)
        manager.createNotificationChannel(alarm)
        manager.createNotificationChannel(problems)
    }

    // Since Android 14 the user can turn off full-screen alarms for an app.
    fun canUseFullScreenIntent(context: Context): Boolean =
        NotificationManagerCompat.from(context).canUseFullScreenIntent()

    fun watchNotification(context: Context): Notification =
        NotificationCompat.Builder(context, WATCH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.watch_title))
            .setContentText(context.getString(R.string.watch_text))
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun alarmNotification(context: Context): Notification {
        val stop = PendingIntent.getService(context, 0, AlarmRingService.stopIntent(context), FLAGS)
        val alarmScreen = Intent(context, AlarmActivity::class.java)
        alarmScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val showAlarm = PendingIntent.getActivity(context, 0, alarmScreen, FLAGS)

        return NotificationCompat.Builder(context, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(showAlarm)
            // On a locked phone this opens the alarm screen right away.
            .setFullScreenIntent(showAlarm, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.alarm_stop), stop)
            .setOngoing(true)
            .build()
    }

    // Tells the user why the app could not do its job. Same id, so a new one replaces the old one.
    fun showProblem(context: Context, @StringRes text: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)

        val message = context.getString(text)
        val notification = NotificationCompat.Builder(context, PROBLEM_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        NotificationManagerCompat.from(context).notify(PROBLEM_ID, notification)
    }
}
