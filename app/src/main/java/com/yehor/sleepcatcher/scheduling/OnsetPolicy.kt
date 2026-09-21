package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.core.PlanTuning

sealed interface OnsetDecision {
    data object Reschedule : OnsetDecision
    data object KeepExisting : OnsetDecision
}

// A night can start several times: you lock the phone, pick it up again,
// scroll for a while and lock it again. The later moment is the better guess
// at when sleep actually began, so the plan is normally rebuilt. The
// exception is the morning, when less than one cycle is left before the
// target: moving the alarm then would only make the night worse.
object OnsetPolicy {

    fun decide(
        onsetMillis: Long,
        targetMillis: Long,
        plannedFinalWakeMillis: Long,
        nowMillis: Long,
        tuning: PlanTuning = PlanTuning(),
    ): OnsetDecision {
        val planIsStillRunning = plannedFinalWakeMillis > nowMillis
        val sleepLeft = targetMillis - (onsetMillis + tuning.fallAsleepMillis)
        return if (planIsStillRunning && sleepLeft < tuning.cycleMillis) {
            OnsetDecision.KeepExisting
        } else {
            OnsetDecision.Reschedule
        }
    }
}
