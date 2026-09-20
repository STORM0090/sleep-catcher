package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.PlanEngine
import com.yehor.sleepcatcher.core.PlanRequest

// Returns a fixed plan and remembers what it was asked for.
class FakePlanEngine(var plan: AlarmPlan) : PlanEngine {

    var lastRequest: PlanRequest? = null
        private set

    override fun buildPlan(request: PlanRequest): AlarmPlan {
        lastRequest = request
        return plan
    }
}
