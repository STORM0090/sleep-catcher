package com.yehor.sleepcatcher.time

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

// The planning core only knows epoch milliseconds. This is the one place
// that turns a wall-clock time such as "09:00" into such a number.
object TargetTime {

    // First moment strictly after afterMillis when the clock shows hour:minute.
    fun nextOccurrence(afterMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long {
        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        var candidate: ZonedDateTime = after.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(after)) candidate = candidate.plusDays(1)
        return candidate.toInstant().toEpochMilli()
    }
}
