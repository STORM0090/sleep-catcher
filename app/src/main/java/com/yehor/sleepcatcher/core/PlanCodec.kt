package com.yehor.sleepcatcher.core

// Unpacks the flat array the native core returns:
// [warning flag, time 0, kind 0, time 1, kind 1, ...]
object PlanCodec {

    fun decode(raw: LongArray): AlarmPlan {
        require(raw.size >= 3) { "A plan needs at least one signal, got ${raw.size} values" }
        require(raw.size % 2 == 1) { "Expected a flag plus pairs of values, got ${raw.size}" }

        val signals = (1 until raw.size step 2).map { index ->
            val kind = SignalKind.fromNativeId(raw[index + 1].toInt())
                ?: error("Unknown signal kind ${raw[index + 1]}")
            AlarmSignal(atMillis = raw[index], kind = kind)
        }
        return AlarmPlan(signals, naturalWakeWarning = raw[0] != 0L)
    }
}
