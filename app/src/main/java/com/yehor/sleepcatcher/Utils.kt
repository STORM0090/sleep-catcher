package com.yehor.sleepcatcher

import android.app.PendingIntent
import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// One tag for the whole app, so `adb logcat -s SleepCatcher` shows the whole night.
const val LOG_TAG = "SleepCatcher"

private val timeFormat = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH)

// For example "Thu 08:45" in the phone's time zone.
fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

// Opens the main screen. Used by the notification and by the alarm icon in the status bar.
fun openAppIntent(context: Context): PendingIntent? {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getActivity(context, 0, launch, flags)
}
