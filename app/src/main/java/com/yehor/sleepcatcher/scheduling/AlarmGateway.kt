package com.yehor.sleepcatcher.scheduling

// The only door to AlarmManager. Tests record the calls instead of making them.
interface AlarmGateway {

    fun canScheduleExact(): Boolean

    fun scheduleLoud(index: Int, atMillis: Long)

    fun scheduleSoft(index: Int, atMillis: Long)

    fun cancel(index: Int)
}
