package com.yehor.sleepcatcher.time

import java.time.Instant
import java.time.ZoneId

object TargetTime {

    // The first moment after afterMillis when the clock shows hour:minute.
    fun nextOccurrence(afterMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long {
        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        var target = after.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(after)) {
            // plusDays keeps the clock time, even on the night the clocks change.
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }
}
