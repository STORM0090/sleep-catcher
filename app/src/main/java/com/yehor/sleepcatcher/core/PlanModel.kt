package com.yehor.sleepcatcher.core

// The ids must stay in step with the enums in cpp/sleepcore/cycle_planner.h.
enum class WakeMode(val nativeId: Int) {
    REFRESHED(0),
    CATCH_DREAM(1);

    companion object {
        fun fromNativeId(id: Int): WakeMode? = entries.firstOrNull { it.nativeId == id }
    }
}

enum class SignalKind(val nativeId: Int) {
    SOFT_PULSE(0),
    LOUD_ALARM(1);

    companion object {
        fun fromNativeId(id: Int): SignalKind? = entries.firstOrNull { it.nativeId == id }
    }
}

data class AlarmSignal(val atMillis: Long, val kind: SignalKind)

data class AlarmPlan(val signals: List<AlarmSignal>, val naturalWakeWarning: Boolean) {

    init {
        require(signals.isNotEmpty()) { "A plan without signals would wake nobody" }
    }

    val finalWakeMillis: Long get() = signals.last().atMillis
}

data class PlanTuning(
    val cycleMinutes: Int = 90,
    val fallAsleepMinutes: Int = 15,
    val minCycles: Int = 3,
    val softGapMinutes: Int = 20,
) {
    val cycleMillis: Long get() = cycleMinutes * 60_000L
    val fallAsleepMillis: Long get() = fallAsleepMinutes * 60_000L
}

data class PlanRequest(
    val onsetMillis: Long,
    val targetMillis: Long,
    val mode: WakeMode,
    val naturalWakeMillis: Long,
    val tuning: PlanTuning = PlanTuning(),
)
