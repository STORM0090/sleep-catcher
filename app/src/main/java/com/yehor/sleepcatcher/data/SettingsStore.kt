package com.yehor.sleepcatcher.data

import com.yehor.sleepcatcher.core.WakeMode

// An interface so the tests can keep the settings in memory.
interface SettingsStore {
    var mode: WakeMode
    var targetHour: Int
    var targetMinute: Int
    var armed: Boolean

    // The current night: when sleep started, how many alarms are set, when the last one rings.
    var lastOnsetMillis: Long
    var scheduledSignalCount: Int
    var plannedFinalWakeMillis: Long
}
