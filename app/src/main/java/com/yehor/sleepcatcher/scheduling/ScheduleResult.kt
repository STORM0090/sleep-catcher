package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.AlarmSignal

sealed interface ScheduleResult {

    data class Scheduled(val plan: AlarmPlan, val scheduled: List<AlarmSignal>) : ScheduleResult

    data object KeptExisting : ScheduleResult
}
