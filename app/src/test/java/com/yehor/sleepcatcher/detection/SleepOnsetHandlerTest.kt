package com.yehor.sleepcatcher.detection

import com.yehor.sleepcatcher.scheduling.CycleAlarmScheduler
import com.yehor.sleepcatcher.scheduling.ScheduleResult
import com.yehor.sleepcatcher.testing.FakePlanEngine
import com.yehor.sleepcatcher.testing.FixedClock
import com.yehor.sleepcatcher.testing.InMemorySettingsStore
import com.yehor.sleepcatcher.testing.RecordingAlarmGateway
import com.yehor.sleepcatcher.testing.RecordingAlarmGateway.Call
import com.yehor.sleepcatcher.testing.at
import com.yehor.sleepcatcher.testing.loud
import com.yehor.sleepcatcher.testing.planOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SleepOnsetHandlerTest {

    private val settings = InMemorySettingsStore().apply { armed = true }
    private val gateway = RecordingAlarmGateway()
    private val clock = FixedClock(at(1))
    private val scheduler = CycleAlarmScheduler(
        FakePlanEngine(planOf(loud(at(8, 45)))),
        gateway,
        settings,
        clock,
        ZoneId.of("UTC"),
    )
    private val handler = SleepOnsetHandler(settings, scheduler)

    private var lookups = 0

    private fun lastApp(packageName: String?): () -> String? = {
        lookups++
        packageName
    }

    @Test
    fun lockingThePhoneAfterTikTokPlansTheNight() {
        val outcome = handler.onScreenOff(at(1), hasUsageAccess = true, lastApp("com.zhiliaoapp.musically"))

        assertTrue(outcome is OnsetOutcome.Planned)
        assertTrue((outcome as OnsetOutcome.Planned).result is ScheduleResult.Scheduled)
        assertEquals(listOf(Call.Loud(0, at(8, 45))), gateway.calls)
    }

    @Test
    fun lockingThePhoneAfterAnotherAppChangesNothing() {
        val outcome = handler.onScreenOff(at(1), hasUsageAccess = true, lastApp("com.whatsapp"))

        assertEquals(OnsetOutcome.NotTriggerApp("com.whatsapp"), outcome)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun nothingHappensWhileTheAlarmIsNotArmed() {
        settings.armed = false

        val outcome = handler.onScreenOff(at(1), hasUsageAccess = true, lastApp("com.zhiliaoapp.musically"))

        assertEquals(OnsetOutcome.NotArmed, outcome)
        assertEquals(0, lookups)
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun withoutUsageAccessTheAppIsNotEvenAskedFor() {
        val outcome = handler.onScreenOff(at(1), hasUsageAccess = false, lastApp("com.zhiliaoapp.musically"))

        assertEquals(OnsetOutcome.NoUsageAccess, outcome)
        assertEquals(0, lookups)
    }

    @Test
    fun anUnknownAppIsReportedAsSuch() {
        val outcome = handler.onScreenOff(at(1), hasUsageAccess = true, lastApp(null))
        assertEquals(OnsetOutcome.NotTriggerApp(null), outcome)
    }

    @Test
    fun aMorningCheckOfThePhoneKeepsThePlan() {
        handler.onScreenOff(at(1), hasUsageAccess = true, lastApp("com.zhiliaoapp.musically"))
        gateway.calls.clear()

        clock.now = at(8)
        val outcome = handler.onScreenOff(at(8), hasUsageAccess = true, lastApp("com.zhiliaoapp.musically"))

        assertEquals(OnsetOutcome.Planned(ScheduleResult.KeptExisting), outcome)
        assertTrue(gateway.calls.isEmpty())
    }
}
