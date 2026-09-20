package com.yehor.sleepcatcher.time

// Reading the current time through an interface keeps tests repeatable:
// they decide what "now" is instead of depending on when they run.
fun interface Clock {
    fun nowMillis(): Long
}

object WallClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
