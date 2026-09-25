package com.yehor.sleepcatcher.ui

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.SignalKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// The words come from string resources, passed in by the screen, so this
// stays plain Kotlin that a unit test can call.
data class SignalLabels(
    val softPulse: String,
    val loudAlarm: String,
    val naturalWakeWarning: String,
)

object PlanFormatter {

    // English weekdays to match the rest of the app, whatever the phone's language.
    private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH)

    fun formatTime(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(TIME)

    fun lines(plan: AlarmPlan, labels: SignalLabels, zone: ZoneId): List<String> {
        val lines = plan.signals.map { signal ->
            val label = when (signal.kind) {
                SignalKind.SOFT_PULSE -> labels.softPulse
                SignalKind.LOUD_ALARM -> labels.loudAlarm
            }
            "${formatTime(signal.atMillis, zone)}  $label"
        }
        return if (plan.naturalWakeWarning) lines + labels.naturalWakeWarning else lines
    }
}
