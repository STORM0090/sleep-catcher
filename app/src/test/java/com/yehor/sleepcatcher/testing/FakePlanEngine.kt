package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.core.AlarmPlan
import com.yehor.sleepcatcher.core.PlanEngine
import com.yehor.sleepcatcher.core.PlanRequest

// Always returns the plan the test gave it and remembers the last request.
class FakePlanEngine(var plan: AlarmPlan) : PlanEngine {

    var lastRequest: PlanRequest? = null

    override fun buildPlan(request: PlanRequest): AlarmPlan {
        lastRequest = request
        return plan
    }
}
