package com.yehor.sleepcatcher.core

// Calls the C++ planner in src/main/cpp.
class NativePlanEngine : PlanEngine {

    override fun buildPlan(request: PlanRequest): AlarmPlan {
        val t = request.tuning
        val raw = nativeBuildPlan(
            request.onsetMillis, request.targetMillis, request.mode.nativeId, request.naturalWakeMillis,
            t.cycleMinutes, t.fallAsleepMinutes, t.minCycles, t.softGapMinutes
        )
        return decodePlan(raw)
    }

    private external fun nativeBuildPlan(
        onsetMillis: Long, targetMillis: Long, mode: Int, naturalWakeMillis: Long,
        cycleMinutes: Int, fallAsleepMinutes: Int, minCycles: Int, softGapMinutes: Int
    ): LongArray

    companion object {
        init {
            System.loadLibrary("sleepcore")
        }
    }
}

// The C++ side returns [warning, time0, kind0, time1, kind1, ...].
// This is outside the class so the tests can call it without loading the library.
fun decodePlan(raw: LongArray): AlarmPlan {
    require(raw.size >= 3 && raw.size % 2 == 1) { "Bad plan array of size ${raw.size}" }

    val signals = mutableListOf<AlarmSignal>()
    for (i in 1 until raw.size step 2) {
        val kind = SignalKind.fromNativeId(raw[i + 1].toInt()) ?: error("Unknown signal kind ${raw[i + 1]}")
        signals.add(AlarmSignal(raw[i], kind))
    }
    return AlarmPlan(signals, raw[0] != 0L)
}
