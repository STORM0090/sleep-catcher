package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.AlarmSignal

// The planner says when the night's signals should ring; this says which of
// them still can. The two differ when a plan is rebuilt late, and Android
// fires an alarm set in the past the moment it is scheduled.
object PlanFilter {

    fun futureSignals(plan: AlarmPlan, nowMillis: Long): List<AlarmSignal> =
        plan.signals.filter { it.atMillis > nowMillis }
}
