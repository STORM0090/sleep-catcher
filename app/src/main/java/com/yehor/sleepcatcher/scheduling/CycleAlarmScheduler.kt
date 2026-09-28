package com.yehor.sleepcatcher.scheduling

import android.content.Context
import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.NativePlanEngine
import com.yehor.sleepcatcher.core.PlanEngine
import com.yehor.sleepcatcher.core.PlanRequest
import com.yehor.sleepcatcher.core.PlanTuning
import com.yehor.sleepcatcher.core.SignalKind
import com.yehor.sleepcatcher.data.PrefsSettingsStore
import com.yehor.sleepcatcher.data.SettingsStore
import com.yehor.sleepcatcher.time.TargetTime
import java.time.ZoneId

enum class ScheduleResult {
    SCHEDULED,
    KEPT_EXISTING,
    NOTHING_IN_FUTURE,
    NO_EXACT_ALARMS
}

// Turns the moment of falling asleep into real alarms.
class CycleAlarmScheduler(
    private val engine: PlanEngine,
    private val gateway: AlarmGateway,
    private val settings: SettingsStore,
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    private val tuning = PlanTuning()

    fun previewPlan(onsetMillis: Long): AlarmPlan = engine.buildPlan(requestFor(onsetMillis))

    fun scheduleFromOnset(onsetMillis: Long, nowMillis: Long): ScheduleResult {
        if (!gateway.canScheduleExact()) {
            // Android deletes all exact alarms of an app when this permission is taken away.
            cancelScheduled()
            return ScheduleResult.NO_EXACT_ALARMS
        }
        if (shouldKeepCurrentPlan(onsetMillis, nowMillis)) {
            return ScheduleResult.KEPT_EXISTING
        }

        val plan = engine.buildPlan(requestFor(onsetMillis))
        // After a late rebuild, for example after a reboot, some signals are already
        // in the past and Android would fire them right away.
        val future = plan.signals.filter { it.atMillis > nowMillis }

        // Cancel first, because the new alarms reuse the same numbers.
        cancelScheduled()
        settings.lastOnsetMillis = onsetMillis
        if (future.isEmpty()) {
            return ScheduleResult.NOTHING_IN_FUTURE
        }

        try {
            for (i in future.indices) {
                val signal = future[i]
                if (signal.kind == SignalKind.LOUD_ALARM) {
                    gateway.scheduleLoud(i, signal.atMillis)
                } else {
                    gateway.scheduleSoft(i, signal.atMillis)
                }
            }
        } catch (e: SecurityException) {
            // The permission was taken away halfway. Half a plan is worse than none.
            for (i in future.indices) {
                gateway.cancel(i)
            }
            return ScheduleResult.NO_EXACT_ALARMS
        }

        settings.scheduledSignalCount = future.size
        settings.plannedFinalWakeMillis = future.last().atMillis
        return ScheduleResult.SCHEDULED
    }

    fun cancelScheduled() {
        for (i in 0 until settings.scheduledSignalCount) {
            gateway.cancel(i)
        }
        settings.scheduledSignalCount = 0
        settings.plannedFinalWakeMillis = 0L
    }

    // Locking the phone again usually means I fell asleep later, so the plan is rebuilt.
    // But not in the morning, when less than one cycle is left before the target.
    private fun shouldKeepCurrentPlan(onsetMillis: Long, nowMillis: Long): Boolean {
        val planIsRunning = settings.plannedFinalWakeMillis > nowMillis
        val sleepStart = onsetMillis + tuning.fallAsleepMinutes * 60_000L
        val sleepLeft = targetFor(onsetMillis) - sleepStart
        return planIsRunning && sleepLeft < tuning.cycleMinutes * 60_000L
    }

    private fun requestFor(onsetMillis: Long): PlanRequest {
        val naturalWake = TargetTime.nextOccurrence(onsetMillis, NATURAL_WAKE_HOUR, 0, zone)
        return PlanRequest(onsetMillis, targetFor(onsetMillis), settings.mode, naturalWake, tuning)
    }

    private fun targetFor(onsetMillis: Long): Long =
        TargetTime.nextOccurrence(onsetMillis, settings.targetHour, settings.targetMinute, zone)

    companion object {
        // After this hour I wake up by myself anyway.
        const val NATURAL_WAKE_HOUR = 11

        fun create(context: Context): CycleAlarmScheduler =
            CycleAlarmScheduler(NativePlanEngine(), AndroidAlarmGateway(context), PrefsSettingsStore(context))
    }
}
