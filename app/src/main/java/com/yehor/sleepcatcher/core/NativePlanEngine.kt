package com.yehor.sleepcatcher.core

class NativePlanEngine : PlanEngine {

    override fun buildPlan(request: PlanRequest): AlarmPlan = PlanCodec.decode(
        nativeBuildPlan(
            request.onsetMillis,
            request.targetMillis,
            request.mode.nativeId,
            request.naturalWakeMillis,
            request.tuning.cycleMinutes,
            request.tuning.fallAsleepMinutes,
            request.tuning.minCycles,
            request.tuning.softGapMinutes,
        ),
    )

    private external fun nativeBuildPlan(
        onsetMillis: Long,
        targetMillis: Long,
        mode: Int,
        naturalWakeMillis: Long,
        cycleMinutes: Int,
        fallAsleepMinutes: Int,
        minCycles: Int,
        softGapMinutes: Int,
    ): LongArray

    private companion object {
        init {
            System.loadLibrary("sleepcore")
        }
    }
}
