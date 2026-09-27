package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.AlarmSignal

sealed interface ScheduleResult {

    // plan is what the core decided, scheduled is what actually went to Android.
    data class Scheduled(val plan: AlarmPlan, val scheduled: List<AlarmSignal>) : ScheduleResult

    data object KeptExisting : ScheduleResult

    data object NothingInFuture : ScheduleResult

    data object ExactAlarmsNotAllowed : ScheduleResult
}
