package com.yehor.sleepcatcher.ui

import com.yehor.sleepcatcher.testing.at
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class StatusLinesTest {

    private val utc = ZoneId.of("UTC")
    private val labels = StatusLabels(
        armed = "Armed",
        notArmed = "Not armed",
        usageAccess = "Usage access",
        exactAlarms = "Exact alarms",
        fullScreenAlarm = "Full-screen alarm",
        batteryExemption = "Battery exemption",
        allowed = "allowed",
        missing = "missing",
        fellAsleep = "Fell asleep",
        finalWake = "Final wake-up",
    )

    @Test
    fun aFreshInstallShowsWhatIsMissing() {
        val status = StatusSnapshot(
            armed = false,
            usageAccess = false,
            exactAlarms = true,
            fullScreenAlarm = true,
            batteryExempt = false,
            lastOnsetMillis = 0L,
            plannedFinalWakeMillis = 0L,
        )

        val expected = listOf(
            "Not armed",
            "Usage access: missing",
            "Exact alarms: allowed",
            "Full-screen alarm: allowed",
            "Battery exemption: missing",
        )
        assertEquals(expected, StatusLines.build(status, labels, utc))
    }

    @Test
    fun aNightInProgressShowsItsTimes() {
        val status = StatusSnapshot(
            armed = true,
            usageAccess = true,
            exactAlarms = true,
            fullScreenAlarm = true,
            batteryExempt = true,
            lastOnsetMillis = at(1),
            plannedFinalWakeMillis = at(8, 45),
        )

        val lines = StatusLines.build(status, labels, utc)

        assertEquals("Armed", lines.first())
        assertEquals("Fell asleep: Thu 01:00", lines[5])
        assertEquals("Final wake-up: Thu 08:45", lines[6])
    }

    @Test
    fun aFinishedNightOnlyShowsWhenSleepStarted() {
        val status = StatusSnapshot(
            armed = true,
            usageAccess = true,
            exactAlarms = true,
            fullScreenAlarm = true,
            batteryExempt = true,
            lastOnsetMillis = at(1),
            plannedFinalWakeMillis = 0L,
        )

        val lines = StatusLines.build(status, labels, utc)

        assertEquals(6, lines.size)
        assertEquals("Fell asleep: Thu 01:00", lines.last())
    }
}
