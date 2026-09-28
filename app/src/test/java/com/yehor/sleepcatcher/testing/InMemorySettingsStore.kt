package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.data.SettingsStore

// Same as the real settings, but nothing is saved to a file.
class InMemorySettingsStore : SettingsStore {
    override var mode = WakeMode.REFRESHED
    override var targetHour = 9
    override var targetMinute = 0
    override var armed = false
    override var lastOnsetMillis = 0L
    override var scheduledSignalCount = 0
    override var plannedFinalWakeMillis = 0L
}
