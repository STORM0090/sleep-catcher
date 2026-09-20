package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.data.SettingsDefaults
import com.yehor.sleepcatcher.data.SettingsStore

// Same contract as the stored settings, but nothing leaves the test.
class InMemorySettingsStore : SettingsStore {
    override var mode: WakeMode = SettingsDefaults.MODE
    override var targetHour: Int = SettingsDefaults.TARGET_HOUR
    override var targetMinute: Int = SettingsDefaults.TARGET_MINUTE
    override var armed: Boolean = false
    override var lastOnsetMillis: Long = 0L
    override var scheduledSignalCount: Int = 0
    override var plannedFinalWakeMillis: Long = 0L
}
