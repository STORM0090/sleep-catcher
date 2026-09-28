package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.AlarmSignal
import com.yehor.sleepcatcher.core.SignalKind

// Readable times for tests: at(9, 30) is 09:30 after an arbitrary midnight.
fun at(hour: Int, minute: Int = 0): Long = (hour * 60L + minute) * 60_000L

// The other way round: at(7, 15) becomes "07:15".
fun hhmm(millis: Long): String {
    val minutes = millis / 60_000
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}

fun soft(atMillis: Long): AlarmSignal = AlarmSignal(atMillis, SignalKind.SOFT_PULSE)

fun loud(atMillis: Long): AlarmSignal = AlarmSignal(atMillis, SignalKind.LOUD_ALARM)

fun planOf(vararg signals: AlarmSignal, warning: Boolean = false): AlarmPlan =
    AlarmPlan(signals.toList(), warning)
