package com.yehor.sleepcatcher.core

import com.yehor.sleepcatcher.testing.at
import com.yehor.sleepcatcher.testing.loud
import com.yehor.sleepcatcher.testing.soft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodePlanTest {

    @Test
    fun readsTheWarningAndEverySignal() {
        val plan = decodePlan(longArrayOf(1, at(7, 15), 0, at(9), 1))

        assertTrue(plan.naturalWakeWarning)
        assertEquals(listOf(soft(at(7, 15)), loud(at(9))), plan.signals)
    }

    @Test
    fun zeroMeansNoWarning() {
        assertFalse(decodePlan(longArrayOf(0, at(9), 1)).naturalWakeWarning)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnArrayWithoutSignals() {
        decodePlan(longArrayOf(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHalfASignal() {
        decodePlan(longArrayOf(0, at(9)))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsAnUnknownSignalKind() {
        decodePlan(longArrayOf(0, at(9), 7))
    }
}
