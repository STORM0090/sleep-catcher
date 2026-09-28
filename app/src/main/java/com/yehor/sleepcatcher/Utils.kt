package com.yehor.sleepcatcher

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
