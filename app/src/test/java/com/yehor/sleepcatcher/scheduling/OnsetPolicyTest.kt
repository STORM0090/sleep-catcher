package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.testing.at
import org.junit.Assert.assertEquals
import org.junit.Test

class OnsetPolicyTest {

    @Test
    fun theFirstOnsetOfTheNightIsAlwaysPlanned() {
        // Nothing scheduled yet, so even a late bedtime gets a plan.
        val decision = OnsetPolicy.decide(
            onsetMillis = at(8, 30),
            targetMillis = at(9),
            plannedFinalWakeMillis = 0L,
            nowMillis = at(8, 30),
        )
        assertEquals(OnsetDecision.Reschedule, decision)
    }

    @Test
    fun lockingThePhoneAgainEarlyInTheNightRebuildsThePlan() {
        val decision = OnsetPolicy.decide(
            onsetMillis = at(1),
            targetMillis = at(9),
            plannedFinalWakeMillis = at(8, 45),
            nowMillis = at(1),
        )
        assertEquals(OnsetDecision.Reschedule, decision)
    }

    @Test
    fun lockingThePhoneLessThanACycleBeforeTheTargetKeepsThePlan() {
        // Sleep would start at 7:55, only 65 minutes before the target.
        val decision = OnsetPolicy.decide(
            onsetMillis = at(7, 40),
            targetMillis = at(9),
            plannedFinalWakeMillis = at(8, 45),
            nowMillis = at(7, 40),
        )
        assertEquals(OnsetDecision.KeepExisting, decision)
    }

    @Test
    fun exactlyOneCycleIsStillWorthPlanning() {
        // Sleep would start at 7:30, exactly 90 minutes before the target.
        val decision = OnsetPolicy.decide(
            onsetMillis = at(7, 15),
            targetMillis = at(9),
            plannedFinalWakeMillis = at(8, 45),
            nowMillis = at(7, 15),
        )
        assertEquals(OnsetDecision.Reschedule, decision)
    }

    @Test
    fun aPlanThatHasAlreadyRungDoesNotCount() {
        val decision = OnsetPolicy.decide(
            onsetMillis = at(7, 40),
            targetMillis = at(9),
            plannedFinalWakeMillis = at(6),
            nowMillis = at(7, 40),
        )
        assertEquals(OnsetDecision.Reschedule, decision)
    }
}
