package com.yehor.sleepcatcher.detection

sealed interface BootAction {
    data object DoNothing : BootAction
    data object StartWatch : BootAction
    data class StartWatchAndRestore(val onsetMillis: Long) : BootAction
}

// A reboot wipes every pending alarm and stops every service, but the
// settings file survives. This decides what to bring back from it.
object BootRestorePolicy {

    fun decide(
        armed: Boolean,
        lastOnsetMillis: Long,
        plannedFinalWakeMillis: Long,
        nowMillis: Long,
    ): BootAction = when {
        !armed -> BootAction.DoNothing
        lastOnsetMillis == 0L || plannedFinalWakeMillis <= nowMillis -> BootAction.StartWatch
        else -> BootAction.StartWatchAndRestore(lastOnsetMillis)
    }
}
