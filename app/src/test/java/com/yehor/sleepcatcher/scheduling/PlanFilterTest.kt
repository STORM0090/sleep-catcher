package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.testing.at
import com.yehor.sleepcatcher.testing.loud
import com.yehor.sleepcatcher.testing.planOf
import com.yehor.sleepcatcher.testing.soft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanFilterTest {

    private val plan = planOf(soft(at(7, 15)), loud(at(9)))

    @Test
    fun earlyInTheNightEverythingIsKept() {
        assertEquals(plan.signals, PlanFilter.futureSignals(plan, at(3)))
    }

    @Test
    fun aNudgeThatAlreadyPassedIsDropped() {
        assertEquals(listOf(loud(at(9))), PlanFilter.futureSignals(plan, at(8)))
    }

    @Test
    fun aSignalDueRightNowCountsAsPassed() {
        assertEquals(listOf(loud(at(9))), PlanFilter.futureSignals(plan, at(7, 15)))
    }

    @Test
    fun afterTheAlarmNothingIsLeft() {
        assertTrue(PlanFilter.futureSignals(plan, at(10)).isEmpty())
    }
}
