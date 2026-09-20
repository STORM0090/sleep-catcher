package com.yehor.sleepcatcher.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TargetTimeTest {

    private val vienna: ZoneId = ZoneId.of("Europe/Vienna")

    private fun at(text: String, zone: ZoneId = vienna): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun aTargetLaterTheSameDayIsUsedAsIs() {
        val onset = at("2026-03-10T01:30")
        assertEquals(at("2026-03-10T09:00"), TargetTime.nextOccurrence(onset, 9, 0, vienna))
    }

    @Test
    fun aTargetEarlierThanTheOnsetRollsOverToTomorrow() {
        val onset = at("2026-03-10T23:40")
        assertEquals(at("2026-03-11T08:30"), TargetTime.nextOccurrence(onset, 8, 30, vienna))
    }

    @Test
    fun aTargetEqualToTheOnsetRollsOverToTomorrow() {
        val onset = at("2026-03-10T09:00")
        assertEquals(at("2026-03-11T09:00"), TargetTime.nextOccurrence(onset, 9, 0, vienna))
    }

    @Test
    fun theSummerTimeJumpKeepsTheWallClockTime() {
        // Vienna skips 02:00 to 03:00 on this night, so the night is an hour shorter.
        val onset = at("2026-03-29T00:30")
        val target = TargetTime.nextOccurrence(onset, 9, 0, vienna)

        assertEquals(at("2026-03-29T09:00"), target)
        assertEquals(7 * 60 + 30, (target - onset) / 60_000)
    }

    @Test
    fun aTargetInsideTheSkippedHourMovesForward() {
        // 02:30 does not exist on that night; the next real moment is 03:30.
        val onset = at("2026-03-29T00:30")
        assertEquals(at("2026-03-29T03:30"), TargetTime.nextOccurrence(onset, 2, 30, vienna))
    }

    @Test
    fun theTimeZoneDecidesTheResult() {
        val utc = ZoneId.of("UTC")
        val onset = at("2026-03-10T01:30", utc)
        assertEquals(at("2026-03-10T09:00", utc), TargetTime.nextOccurrence(onset, 9, 0, utc))
    }
}
