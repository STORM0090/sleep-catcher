package com.yehor.sleepcatcher.scheduling

// Everything the scheduler needs from AlarmManager. The tests use a fake that only writes the calls down.
interface AlarmGateway {
    fun canScheduleExact(): Boolean
    fun scheduleLoud(index: Int, atMillis: Long)
    fun scheduleSoft(index: Int, atMillis: Long)
    fun cancel(index: Int)
}
