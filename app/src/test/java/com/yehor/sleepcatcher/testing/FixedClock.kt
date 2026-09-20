package com.yehor.sleepcatcher.testing

import com.yehor.sleepcatcher.time.Clock

// A clock that stands still until a test moves it.
class FixedClock(var now: Long) : Clock {
    override fun nowMillis(): Long = now
}
