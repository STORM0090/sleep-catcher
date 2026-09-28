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
    fun finalWakeUpIsTheLastSignal() {
        val plan = planOf(soft(at(7, 15)), loud(at(9)))
        assertEquals(at(9), plan.finalWakeMillis)
    }

    @Test(expected = IllegalArgumentException::class)
    fun planNeedsAtLeastOneSignal() {
        planOf()
    }

    @Test
    fun idsMatchTheCppEnums() {
        assertEquals(WakeMode.REFRESHED, WakeMode.fromNativeId(0))
        assertEquals(WakeMode.CATCH_DREAM, WakeMode.fromNativeId(1))
        assertNull(WakeMode.fromNativeId(2))

        assertEquals(SignalKind.SOFT_PULSE, SignalKind.fromNativeId(0))
        assertEquals(SignalKind.LOUD_ALARM, SignalKind.fromNativeId(1))
        assertNull(SignalKind.fromNativeId(-1))
    }
}
