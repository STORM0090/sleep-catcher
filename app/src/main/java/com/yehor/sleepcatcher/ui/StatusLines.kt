package com.yehor.sleepcatcher.ui

import java.time.ZoneId

// Everything the status panel shows, collected by the screen beforehand.
data class StatusSnapshot(
    val armed: Boolean,
    val usageAccess: Boolean,
    val exactAlarms: Boolean,
    val fullScreenAlarm: Boolean,
    val batteryExempt: Boolean,
    val lastOnsetMillis: Long,
    val plannedFinalWakeMillis: Long,
)

data class StatusLabels(
    val armed: String,
    val notArmed: String,
    val usageAccess: String,
    val exactAlarms: String,
    val fullScreenAlarm: String,
    val batteryExemption: String,
    val allowed: String,
    val missing: String,
    val fellAsleep: String,
    val finalWake: String,
)

object StatusLines {

    fun build(status: StatusSnapshot, labels: StatusLabels, zone: ZoneId): List<String> = buildList {
        add(if (status.armed) labels.armed else labels.notArmed)
        add("${labels.usageAccess}: ${state(status.usageAccess, labels)}")
        add("${labels.exactAlarms}: ${state(status.exactAlarms, labels)}")
        add("${labels.fullScreenAlarm}: ${state(status.fullScreenAlarm, labels)}")
        add("${labels.batteryExemption}: ${state(status.batteryExempt, labels)}")
        if (status.lastOnsetMillis > 0) {
            add("${labels.fellAsleep}: ${PlanFormatter.formatTime(status.lastOnsetMillis, zone)}")
        }
        if (status.plannedFinalWakeMillis > 0) {
            add("${labels.finalWake}: ${PlanFormatter.formatTime(status.plannedFinalWakeMillis, zone)}")
        }
    }

    private fun state(ok: Boolean, labels: StatusLabels): String = if (ok) labels.allowed else labels.missing
}
