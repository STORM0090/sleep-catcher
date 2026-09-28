package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.scheduling.AlarmGateway

// Writes every call down as text, so a test can compare it with a list of strings.
class RecordingAlarmGateway : AlarmGateway {

    val calls = mutableListOf<String>()
    var exactAllowed = true

    // Pretends the exact alarm permission disappears before the loud alarm is set.
    var failOnLoud = false

    override fun canScheduleExact(): Boolean = exactAllowed

    override fun scheduleLoud(index: Int, atMillis: Long) {
        if (failOnLoud) throw SecurityException("exact alarms revoked")
        calls.add("loud $index at ${hhmm(atMillis)}")
    }

    override fun scheduleSoft(index: Int, atMillis: Long) {
        calls.add("soft $index at ${hhmm(atMillis)}")
    }

    override fun cancel(index: Int) {
        calls.add("cancel $index")
    }
}
