package com.yehor.sleepcatcher.scheduling

import android.content.Context
import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.AlarmSignal
import com.yehor.sleepcatcher.core.NativePlanEngine
import com.yehor.sleepcatcher.core.PlanEngine
import com.yehor.sleepcatcher.core.PlanRequest
import com.yehor.sleepcatcher.core.PlanTuning
import com.yehor.sleepcatcher.core.SignalKind
import com.yehor.sleepcatcher.data.PrefsSettingsStore
import com.yehor.sleepcatcher.data.SettingsStore
import com.yehor.sleepcatcher.time.Clock
import com.yehor.sleepcatcher.time.TargetTime
import com.yehor.sleepcatcher.time.WallClock
import java.time.ZoneId

// Turns a moment of falling asleep into real alarms. Everything it needs is
// passed in, so the whole class can be exercised without a phone.
class CycleAlarmScheduler(
    private val engine: PlanEngine,
    private val gateway: AlarmGateway,
    private val settings: SettingsStore,
    private val clock: Clock,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val tuning: PlanTuning = PlanTuning(),
) {

    fun previewPlan(onsetMillis: Long): AlarmPlan = engine.buildPlan(requestFor(onsetMillis))

    fun scheduleFromOnset(onsetMillis: Long): ScheduleResult {
        if (!gateway.canScheduleExact()) {
            // Android drops every exact alarm of an app when this permission
            // is taken away, so there is no old plan left to keep either.
            cancelScheduled()
            return ScheduleResult.ExactAlarmsNotAllowed
        }

        val now = clock.nowMillis()
        val decision = OnsetPolicy.decide(
            onsetMillis = onsetMillis,
            targetMillis = targetFor(onsetMillis),
            plannedFinalWakeMillis = settings.plannedFinalWakeMillis,
            nowMillis = now,
            tuning = tuning,
        )
        when (decision) {
            OnsetDecision.KeepExisting -> return ScheduleResult.KeptExisting
            OnsetDecision.Reschedule -> Unit
        }

        val plan = engine.buildPlan(requestFor(onsetMillis))
        val future = PlanFilter.futureSignals(plan, now)

        // Old alarms go first: they use the same request codes as the new ones.
        cancelScheduled()
        settings.lastOnsetMillis = onsetMillis
        if (future.isEmpty()) return ScheduleResult.NothingInFuture

        try {
            future.forEachIndexed(::schedule)
        } catch (e: SecurityException) {
            // The permission can disappear between the check and this call.
            // Half a plan is worse than none, so take back what did get set.
            future.indices.forEach(gateway::cancel)
            return ScheduleResult.ExactAlarmsNotAllowed
        }
        settings.scheduledSignalCount = future.size
        settings.plannedFinalWakeMillis = future.last().atMillis
        return ScheduleResult.Scheduled(plan, future)
    }

    fun cancelScheduled() {
        for (index in 0 until settings.scheduledSignalCount) {
            gateway.cancel(index)
        }
        settings.scheduledSignalCount = 0
        settings.plannedFinalWakeMillis = 0L
    }

    private fun schedule(index: Int, signal: AlarmSignal) = when (signal.kind) {
        SignalKind.SOFT_PULSE -> gateway.scheduleSoft(index, signal.atMillis)
        SignalKind.LOUD_ALARM -> gateway.scheduleLoud(index, signal.atMillis)
    }

    private fun requestFor(onsetMillis: Long) = PlanRequest(
        onsetMillis = onsetMillis,
        targetMillis = targetFor(onsetMillis),
        mode = settings.mode,
        naturalWakeMillis = TargetTime.nextOccurrence(onsetMillis, NATURAL_WAKE_HOUR, 0, zone),
        tuning = tuning,
    )

    private fun targetFor(onsetMillis: Long): Long =
        TargetTime.nextOccurrence(onsetMillis, settings.targetHour, settings.targetMinute, zone)

    companion object {

        // Past this hour I wake up by myself whatever the alarm says.
        const val NATURAL_WAKE_HOUR = 11

        fun create(context: Context): CycleAlarmScheduler = CycleAlarmScheduler(
            engine = NativePlanEngine(),
            gateway = AndroidAlarmGateway(context),
            settings = PrefsSettingsStore(context),
            clock = WallClock,
        )
    }
}
