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

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                WATCH_CHANNEL,
                context.getString(R.string.notification_channel_watch),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
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
        manager.createNotificationChannel(
            NotificationChannel(
                PROBLEM_CHANNEL,
                context.getString(R.string.notification_channel_problems),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    // Android 14 lets the user switch off full-screen alarms for an app.
    // Without them the alarm is only a heads-up notification on a locked phone.
    fun canUseFullScreenIntent(context: Context): Boolean =
        NotificationManagerCompat.from(context).canUseFullScreenIntent()

    fun watchNotification(context: Context): Notification {
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(context, WATCH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.watch_title))
            .setContentText(context.getString(R.string.watch_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

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

    // Something stops the app from working at night. One id, so repeated
    // problems update the same notification instead of piling up.
    fun showProblem(context: Context, @StringRes text: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, PROBLEM_CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(text)))
            .build()
        NotificationManagerCompat.from(context).notify(PROBLEM_ID, notification)
    }
}
