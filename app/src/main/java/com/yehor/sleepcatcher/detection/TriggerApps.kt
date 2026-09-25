package com.yehor.sleepcatcher.detection

// The app I scroll right before sleep. Locking the phone while it is open is
// the signal that the night has started.
object TriggerApps {

    // TikTok ships under two package names depending on the region.
    val PACKAGES: Set<String> = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")

    fun isTrigger(packageName: String?): Boolean = packageName != null && packageName in PACKAGES
}
