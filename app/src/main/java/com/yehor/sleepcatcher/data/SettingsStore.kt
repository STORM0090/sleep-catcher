package com.yehor.sleepcatcher.data

import com.yehor.sleepcatcher.core.WakeMode

// What the app remembers between launches. An interface, so tests can keep
// the same state in memory instead of talking to Android.
interface SettingsStore {

    var mode: WakeMode
    var targetHour: Int
    var targetMinute: Int

    // Set while the app is listening for the screen to go off.
    var armed: Boolean

    // The night in progress: when sleep started, how many alarms are pending
    // and when the last of them will ring.
    var lastOnsetMillis: Long
    var scheduledSignalCount: Int
    var plannedFinalWakeMillis: Long
}

object SettingsDefaults {
    val MODE = WakeMode.REFRESHED
    const val TARGET_HOUR = 9
    const val TARGET_MINUTE = 0
}
