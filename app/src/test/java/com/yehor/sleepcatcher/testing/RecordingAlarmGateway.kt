package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.scheduling.AlarmGateway

// Writes down what the scheduler asked for, in the order it asked.
class RecordingAlarmGateway(var exactAllowed: Boolean = true) : AlarmGateway {

    sealed interface Call {
        data class Loud(val index: Int, val atMillis: Long) : Call
        data class Soft(val index: Int, val atMillis: Long) : Call
        data class Cancel(val index: Int) : Call
    }

    val calls = mutableListOf<Call>()

    // Set to make the next loud alarm fail the way Android does when the
    // exact alarm permission is revoked halfway through.
    var loudFailure: SecurityException? = null

    override fun canScheduleExact(): Boolean = exactAllowed

    override fun scheduleLoud(index: Int, atMillis: Long) {
        loudFailure?.let { throw it }
        calls += Call.Loud(index, atMillis)
    }

    override fun scheduleSoft(index: Int, atMillis: Long) {
        calls += Call.Soft(index, atMillis)
    }

    override fun cancel(index: Int) {
        calls += Call.Cancel(index)
    }
}
