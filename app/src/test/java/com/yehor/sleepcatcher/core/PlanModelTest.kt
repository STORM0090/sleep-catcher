package com.yehor.sleepcatcher.core

import com.yehor.sleepcatcher.testing.at
import com.yehor.sleepcatcher.testing.loud
import com.yehor.sleepcatcher.testing.planOf
import com.yehor.sleepcatcher.testing.soft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanModelTest {

    @Test
    fun theFinalWakeUpIsTheLastSignal() {
        val plan = planOf(soft(at(7, 15)), loud(at(9)))
        assertEquals(at(9), plan.finalWakeMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aPlanNeedsAtLeastOneSignal() {
        planOf()
    }

    @Test
    fun modeIdsMatchTheNativeEnums() {
        assertEquals(WakeMode.REFRESHED, WakeMode.fromNativeId(0))
        assertEquals(WakeMode.CATCH_DREAM, WakeMode.fromNativeId(1))
        assertNull(WakeMode.fromNativeId(2))

        assertEquals(SignalKind.SOFT_PULSE, SignalKind.fromNativeId(0))
        assertEquals(SignalKind.LOUD_ALARM, SignalKind.fromNativeId(1))
        assertNull(SignalKind.fromNativeId(-1))
    }

    @Test
    fun tuningExposesItsValuesInMilliseconds() {
        val tuning = PlanTuning()
        assertEquals(90 * 60_000L, tuning.cycleMillis)
        assertEquals(15 * 60_000L, tuning.fallAsleepMillis)
    }
}
