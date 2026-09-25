package com.yehor.sleepcatcher.ui

import com.yehor.sleepcatcher.testing.at
import com.yehor.sleepcatcher.testing.loud
import com.yehor.sleepcatcher.testing.planOf
import com.yehor.sleepcatcher.testing.soft
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class PlanFormatterTest {

    private val utc = ZoneId.of("UTC")
    private val labels = SignalLabels(
        softPulse = "soft pulse",
        loudAlarm = "loud alarm",
        naturalWakeWarning = "You will probably wake up on your own first",
    )

    @Test
    fun timesShowTheWeekdayAndTheClock() {
        // The test clock starts on 1 January 1970, which was a Thursday.
        assertEquals("Thu 09:00", PlanFormatter.formatTime(at(9), utc))
    }

    @Test
    fun theTimeZoneDecidesWhatTheClockShows() {
        assertEquals("Thu 10:00", PlanFormatter.formatTime(at(9), ZoneId.of("Europe/Vienna")))
    }

    @Test
    fun everySignalGetsItsOwnLine() {
        val lines = PlanFormatter.lines(planOf(soft(at(7, 15)), loud(at(9))), labels, utc)
        assertEquals(listOf("Thu 07:15  soft pulse", "Thu 09:00  loud alarm"), lines)
    }

    @Test
    fun theWarningComesLast() {
        val lines = PlanFormatter.lines(planOf(loud(at(11, 45)), warning = true), labels, utc)
        assertEquals(listOf("Thu 11:45  loud alarm", "You will probably wake up on your own first"), lines)
    }
}
