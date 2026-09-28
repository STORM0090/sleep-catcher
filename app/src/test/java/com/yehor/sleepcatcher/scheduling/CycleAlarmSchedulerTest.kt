package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.testing.FakePlanEngine
import com.yehor.sleepcatcher.testing.InMemorySettingsStore
import com.yehor.sleepcatcher.testing.RecordingAlarmGateway
import com.yehor.sleepcatcher.testing.at
import com.yehor.sleepcatcher.testing.loud
import com.yehor.sleepcatcher.testing.planOf
import com.yehor.sleepcatcher.testing.soft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class CycleAlarmSchedulerTest {

    private val engine = FakePlanEngine(planOf(loud(at(8, 45))))
    private val gateway = RecordingAlarmGateway()
    private val settings = InMemorySettingsStore()

    // In UTC at(9) really is 09:00, which keeps the numbers readable.
    private val scheduler = CycleAlarmScheduler(engine, gateway, settings, ZoneId.of("UTC"))

    @Test
    fun refreshedPlanBecomesOneLoudAlarm() {
        val result = scheduler.scheduleFromOnset(at(1), at(1))

        assertEquals(ScheduleResult.SCHEDULED, result)
        assertEquals(listOf("loud 0 at 08:45"), gateway.calls)
    }

    @Test
    fun catchDreamPlanBecomesNudgeAndAlarm() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))

        scheduler.scheduleFromOnset(at(1), at(1))

        assertEquals(listOf("soft 0 at 07:15", "loud 1 at 09:00"), gateway.calls)
    }

    @Test
    fun settingsRememberTheNight() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))

        scheduler.scheduleFromOnset(at(1), at(1))

        assertEquals(at(1), settings.lastOnsetMillis)
        assertEquals(2, settings.scheduledSignalCount)
        assertEquals(at(9), settings.plannedFinalWakeMillis)
    }

    @Test
    fun requestIsBuiltFromTheSettings() {
        settings.mode = WakeMode.CATCH_DREAM
        settings.targetHour = 7
        settings.targetMinute = 30

        scheduler.scheduleFromOnset(at(1), at(1))

        val request = engine.lastRequest!!
        assertEquals(at(1), request.onsetMillis)
        assertEquals(at(7, 30), request.targetMillis)
        assertEquals(WakeMode.CATCH_DREAM, request.mode)
        assertEquals(at(11), request.naturalWakeMillis)
    }

    @Test
    fun secondOnsetReplacesTheOldAlarms() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        scheduler.scheduleFromOnset(at(1), at(1))
        gateway.calls.clear()

        engine.plan = planOf(loud(at(9)))
        scheduler.scheduleFromOnset(at(2), at(2))

        assertEquals(listOf("cancel 0", "cancel 1", "loud 0 at 09:00"), gateway.calls)
        assertEquals(1, settings.scheduledSignalCount)
    }

    @Test
    fun firstOnsetIsAlwaysPlannedEvenIfLate() {
        val result = scheduler.scheduleFromOnset(at(8, 30), at(8, 30))
        assertEquals(ScheduleResult.SCHEDULED, result)
    }

    @Test
    fun onsetLessThanOneCycleBeforeTargetKeepsThePlan() {
        scheduler.scheduleFromOnset(at(1), at(1))
        gateway.calls.clear()

        // Sleep would start at 8:15, only 45 minutes before the 9:00 target.
        val result = scheduler.scheduleFromOnset(at(8), at(8))

        assertEquals(ScheduleResult.KEPT_EXISTING, result)
        assertTrue(gateway.calls.isEmpty())
        assertEquals(at(1), settings.lastOnsetMillis)
    }

    @Test
    fun exactlyOneCycleLeftIsStillPlanned() {
        scheduler.scheduleFromOnset(at(1), at(1))

        // Sleep would start at 7:30, exactly 90 minutes before 9:00.
        val result = scheduler.scheduleFromOnset(at(7, 15), at(7, 15))

        assertEquals(ScheduleResult.SCHEDULED, result)
    }

    @Test
    fun planThatAlreadyRangDoesNotCount() {
        scheduler.scheduleFromOnset(at(1), at(1))
        engine.plan = planOf(loud(at(9)))

        // 8:50: the 8:45 alarm has rung, so a new onset gets a new plan.
        val result = scheduler.scheduleFromOnset(at(8, 50), at(8, 50))

        assertEquals(ScheduleResult.SCHEDULED, result)
    }

    @Test
    fun lateRebuildSkipsSignalsThatAlreadyPassed() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))

        // Rebuilt at 8:00, for example after a reboot.
        val result = scheduler.scheduleFromOnset(at(1), at(8))

        assertEquals(ScheduleResult.SCHEDULED, result)
        assertEquals(listOf("loud 0 at 09:00"), gateway.calls)
        assertEquals(1, settings.scheduledSignalCount)
        assertEquals(at(9), settings.plannedFinalWakeMillis)
    }

    @Test
    fun rebuildAfterTheAlarmSetsNothing() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))

        val result = scheduler.scheduleFromOnset(at(1), at(10))

        assertEquals(ScheduleResult.NOTHING_IN_FUTURE, result)
        assertTrue(gateway.calls.isEmpty())
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun withoutExactAlarmsNothingIsSet() {
        gateway.exactAllowed = false

        val result = scheduler.scheduleFromOnset(at(1), at(1))

        assertEquals(ScheduleResult.NO_EXACT_ALARMS, result)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun losingExactAlarmsAlsoForgetsTheOldPlan() {
        scheduler.scheduleFromOnset(at(1), at(1))
        gateway.calls.clear()
        gateway.exactAllowed = false

        val result = scheduler.scheduleFromOnset(at(2), at(2))

        assertEquals(ScheduleResult.NO_EXACT_ALARMS, result)
        assertEquals(listOf("cancel 0"), gateway.calls)
        assertEquals(0, settings.scheduledSignalCount)
    }

    @Test
    fun refusalHalfwayTakesBackWhatWasAlreadySet() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        gateway.failOnLoud = true

        val result = scheduler.scheduleFromOnset(at(1), at(1))

        assertEquals(ScheduleResult.NO_EXACT_ALARMS, result)
        assertEquals(listOf("soft 0 at 07:15", "cancel 0", "cancel 1"), gateway.calls)
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun cancellingForgetsTheWholeNight() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        scheduler.scheduleFromOnset(at(1), at(1))
        gateway.calls.clear()

        scheduler.cancelScheduled()

        assertEquals(listOf("cancel 0", "cancel 1"), gateway.calls)
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun previewDoesNotTouchTheAlarms() {
        val plan = scheduler.previewPlan(at(1))

        assertEquals(at(8, 45), plan.finalWakeMillis)
        assertTrue(gateway.calls.isEmpty())
    }
}
