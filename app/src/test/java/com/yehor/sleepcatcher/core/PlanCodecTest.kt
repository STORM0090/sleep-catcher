package com.yehor.sleepcatcher.core

import com.yehor.sleepcatcher.testing.at
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanCodecTest {

    @Test
    fun readsTheWarningFlagAndEverySignal() {
        val plan = PlanCodec.decode(longArrayOf(1, at(7, 15), 0, at(9), 1))

        assertTrue(plan.naturalWakeWarning)
        assertEquals(2, plan.signals.size)
        assertEquals(AlarmSignal(at(7, 15), SignalKind.SOFT_PULSE), plan.signals[0])
        assertEquals(AlarmSignal(at(9), SignalKind.LOUD_ALARM), plan.signals[1])
    }

    @Test
    fun readsZeroAsNoWarning() {
        assertFalse(PlanCodec.decode(longArrayOf(0, at(9), 1)).naturalWakeWarning)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAPlanWithoutSignals() {
        PlanCodec.decode(longArrayOf(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAHalfFinishedPair() {
        PlanCodec.decode(longArrayOf(0, at(9)))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsAnUnknownSignalKind() {
        PlanCodec.decode(longArrayOf(0, at(9), 7))
    }
}
