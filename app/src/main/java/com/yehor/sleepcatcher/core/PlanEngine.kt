package com.yehor.sleepcatcher.core

// Everything above this line is Android; everything below is arithmetic.
// Tests swap in a fake so they never have to load the native library.
fun interface PlanEngine {
    fun buildPlan(request: PlanRequest): AlarmPlan
}
