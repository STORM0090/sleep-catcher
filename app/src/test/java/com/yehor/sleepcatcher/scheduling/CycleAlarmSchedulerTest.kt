package com.yehor.sleepcatcher.scheduling

import com.yehor.sleepcatcher.core.WakeMode
import com.yehor.sleepcatcher.testing.FakePlanEngine
import com.yehor.sleepcatcher.testing.FixedClock
import com.yehor.sleepcatcher.testing.InMemorySettingsStore
import com.yehor.sleepcatcher.testing.RecordingAlarmGateway
import com.yehor.sleepcatcher.testing.RecordingAlarmGateway.Call
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
    private val clock = FixedClock(at(1))

    // In UTC the test helper at(9) really is 09:00, which keeps the numbers readable.
    private val scheduler = CycleAlarmScheduler(engine, gateway, settings, clock, ZoneId.of("UTC"))

    @Test
    fun aRefreshedPlanBecomesOneLoudAlarm() {
        scheduler.scheduleFromOnset(at(1))
        assertEquals(listOf(Call.Loud(0, at(8, 45))), gateway.calls)
    }

    @Test
    fun aCatchDreamPlanBecomesANudgeAndAnAlarm() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))

        scheduler.scheduleFromOnset(at(1))

        assertEquals(listOf(Call.Soft(0, at(7, 15)), Call.Loud(1, at(9))), gateway.calls)
    }

    @Test
    fun theSettingsDescribeTheNightInProgress() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))

        scheduler.scheduleFromOnset(at(1))

        assertEquals(at(1), settings.lastOnsetMillis)
        assertEquals(2, settings.scheduledSignalCount)
        assertEquals(at(9), settings.plannedFinalWakeMillis)
    }

    @Test
    fun theWantedTimesAreReadFromTheSettings() {
        settings.mode = WakeMode.CATCH_DREAM
        settings.targetHour = 7
        settings.targetMinute = 30

        scheduler.scheduleFromOnset(at(1))

        val request = requireNotNull(engine.lastRequest)
        assertEquals(at(1), request.onsetMillis)
        assertEquals(at(7, 30), request.targetMillis)
        assertEquals(WakeMode.CATCH_DREAM, request.mode)
        assertEquals(at(CycleAlarmScheduler.NATURAL_WAKE_HOUR), request.naturalWakeMillis)
    }

    @Test
    fun aSecondOnsetClearsTheAlarmsItReplaces() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        scheduler.scheduleFromOnset(at(1))
        gateway.calls.clear()

        engine.plan = planOf(loud(at(9)))
        scheduler.scheduleFromOnset(at(2))

        assertEquals(listOf(Call.Cancel(0), Call.Cancel(1), Call.Loud(0, at(9))), gateway.calls)
        assertEquals(1, settings.scheduledSignalCount)
    }

    @Test
    fun anOnsetTooCloseToTheTargetLeavesTheAlarmsAlone() {
        scheduler.scheduleFromOnset(at(1))
        gateway.calls.clear()

        clock.now = at(8)
        val result = scheduler.scheduleFromOnset(at(8))

        assertEquals(ScheduleResult.KeptExisting, result)
        assertTrue(gateway.calls.isEmpty())
        assertEquals(at(1), settings.lastOnsetMillis)
    }

    @Test
    fun cancellingForgetsTheWholeNight() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        scheduler.scheduleFromOnset(at(1))
        gateway.calls.clear()

        scheduler.cancelScheduled()

        assertEquals(listOf(Call.Cancel(0), Call.Cancel(1)), gateway.calls)
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun aLateRebuildSkipsTheNudgeThatAlreadyPassed() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        clock.now = at(8)

        val result = scheduler.scheduleFromOnset(at(1))

        assertEquals(listOf(Call.Loud(0, at(9))), gateway.calls)
        assertEquals(listOf(loud(at(9))), (result as ScheduleResult.Scheduled).scheduled)
        assertEquals(1, settings.scheduledSignalCount)
        assertEquals(at(9), settings.plannedFinalWakeMillis)
    }

    @Test
    fun aRebuildAfterTheAlarmSchedulesNothing() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        clock.now = at(10)

        val result = scheduler.scheduleFromOnset(at(1))

        assertEquals(ScheduleResult.NothingInFuture, result)
        assertTrue(gateway.calls.isEmpty())
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun withoutExactAlarmsNothingIsScheduled() {
        gateway.exactAllowed = false

        val result = scheduler.scheduleFromOnset(at(1))

        assertEquals(ScheduleResult.ExactAlarmsNotAllowed, result)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun losingExactAlarmsAlsoForgetsTheOldPlan() {
        scheduler.scheduleFromOnset(at(1))
        gateway.calls.clear()
        gateway.exactAllowed = false

        val result = scheduler.scheduleFromOnset(at(2))

        assertEquals(ScheduleResult.ExactAlarmsNotAllowed, result)
        assertEquals(listOf(Call.Cancel(0)), gateway.calls)
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun aRefusalHalfwayTakesBackWhatWasAlreadySet() {
        engine.plan = planOf(soft(at(7, 15)), loud(at(9)))
        gateway.loudFailure = SecurityException("exact alarms revoked")

        val result = scheduler.scheduleFromOnset(at(1))

        assertEquals(ScheduleResult.ExactAlarmsNotAllowed, result)
        assertEquals(listOf(Call.Soft(0, at(7, 15)), Call.Cancel(0), Call.Cancel(1)), gateway.calls)
        assertEquals(0, settings.scheduledSignalCount)
        assertEquals(0L, settings.plannedFinalWakeMillis)
    }

    @Test
    fun previewingDoesNotTouchTheAlarms() {
        val plan = scheduler.previewPlan(at(1))

        assertEquals(at(8, 45), plan.finalWakeMillis)
        assertTrue(gateway.calls.isEmpty())
        assertEquals(0, settings.scheduledSignalCount)
    }
}
