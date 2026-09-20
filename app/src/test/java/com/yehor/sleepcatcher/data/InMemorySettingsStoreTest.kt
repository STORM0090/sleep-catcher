package com.yehor.sleepcatcher.data

import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.testing.InMemorySettingsStore
import com.yehor.sleepcatcher.testing.at
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InMemorySettingsStoreTest {

    private val settings = InMemorySettingsStore()

    @Test
    fun startsOnTheSharedDefaults() {
        assertEquals(SettingsDefaults.MODE, settings.mode)
        assertEquals(SettingsDefaults.TARGET_HOUR, settings.targetHour)
        assertEquals(SettingsDefaults.TARGET_MINUTE, settings.targetMinute)
    }

    @Test
    fun startsWithNoNightInProgress() {
        assertFalse(settings.armed)
        assertEquals(0L, settings.lastOnsetMillis)
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun keepsWhateverIsWrittenToIt() {
        settings.mode = WakeMode.CATCH_DREAM
        settings.targetHour = 7
        settings.targetMinute = 45
        settings.armed = true
        settings.lastOnsetMillis = at(1)
        settings.scheduledSignalCount = 2
        settings.plannedFinalWakeMillis = at(9)

        assertEquals(WakeMode.CATCH_DREAM, settings.mode)
        assertEquals(7, settings.targetHour)
        assertEquals(45, settings.targetMinute)
        assertEquals(true, settings.armed)
        assertEquals(at(1), settings.lastOnsetMillis)
        assertEquals(2, settings.scheduledSignalCount)
        assertEquals(at(9), settings.plannedFinalWakeMillis)
    }
}
