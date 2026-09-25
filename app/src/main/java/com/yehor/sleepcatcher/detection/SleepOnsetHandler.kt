package com.yehor.sleepcatcher.detection

import com.yehor.sleepcatcher.data.SettingsStore
import com.yehor.sleepcatcher.scheduling.CycleAlarmScheduler
import com.yehor.sleepcatcher.scheduling.ScheduleResult

sealed interface OnsetOutcome {
    data object NotArmed : OnsetOutcome
    data object NoUsageAccess : OnsetOutcome
    data class NotTriggerApp(val packageName: String?) : OnsetOutcome
    data class Planned(val result: ScheduleResult) : OnsetOutcome
}

// Decides what a screen-off means. The receiver collects the facts from
// Android and hands them in; nothing in here needs a phone.
class SleepOnsetHandler(
    private val settings: SettingsStore,
    private val scheduler: CycleAlarmScheduler,
) {

    // The package lookup is passed as a function so it only runs when
    // the cheaper checks have passed.
    fun onScreenOff(
        nowMillis: Long,
        hasUsageAccess: Boolean,
        lastForegroundPackage: () -> String?,
    ): OnsetOutcome {
        if (!settings.armed) return OnsetOutcome.NotArmed
        if (!hasUsageAccess) return OnsetOutcome.NoUsageAccess

        val packageName = lastForegroundPackage()
        if (!TriggerApps.isTrigger(packageName)) return OnsetOutcome.NotTriggerApp(packageName)

        return OnsetOutcome.Planned(scheduler.scheduleFromOnset(nowMillis))
    }
}
