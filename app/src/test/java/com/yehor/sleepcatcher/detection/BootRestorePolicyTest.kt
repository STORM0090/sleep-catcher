package com.yehor.sleepcatcher.detection

import com.yehor.sleepcatcher.testing.at
import org.junit.Assert.assertEquals
import org.junit.Test

class BootRestorePolicyTest {

    @Test
    fun aDisarmedAppStaysQuiet() {
        val action = BootRestorePolicy.decide(
            armed = false,
            lastOnsetMillis = at(1),
            plannedFinalWakeMillis = at(9),
            nowMillis = at(3),
        )
        assertEquals(BootAction.DoNothing, action)
    }

    @Test
    fun beforeFallingAsleepOnlyTheWatchComesBack() {
        val action = BootRestorePolicy.decide(
            armed = true,
            lastOnsetMillis = 0L,
            plannedFinalWakeMillis = 0L,
            nowMillis = at(23),
        )
        assertEquals(BootAction.StartWatch, action)
    }

    @Test
    fun aRebootInTheMiddleOfTheNightBringsThePlanBack() {
        val action = BootRestorePolicy.decide(
            armed = true,
            lastOnsetMillis = at(1),
            plannedFinalWakeMillis = at(9),
            nowMillis = at(3),
        )
        assertEquals(BootAction.StartWatchAndRestore(at(1)), action)
    }

    @Test
    fun aRebootAfterTheAlarmRangDoesNotRingItAgain() {
        val action = BootRestorePolicy.decide(
            armed = true,
            lastOnsetMillis = at(1),
            plannedFinalWakeMillis = at(9),
            nowMillis = at(10),
        )
        assertEquals(BootAction.StartWatch, action)
    }

    @Test
    fun aRebootAtTheExactAlarmTimeCountsAsTooLate() {
        val action = BootRestorePolicy.decide(
            armed = true,
            lastOnsetMillis = at(1),
            plannedFinalWakeMillis = at(9),
            nowMillis = at(9),
        )
        assertEquals(BootAction.StartWatch, action)
    }
}
