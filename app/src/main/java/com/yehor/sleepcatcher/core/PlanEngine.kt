package com.yehor.sleepcatcher.core

// The tests use a fake instead of the native library.
interface PlanEngine {
    fun buildPlan(request: PlanRequest): AlarmPlan
}
