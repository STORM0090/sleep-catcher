package com.yehor.sleepcatcher.detection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

class ForegroundAppDetector(private val context: Context) {

    // There is no permission dialog for usage access, the user turns it on in the settings.
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val op = AppOpsManager.OPSTR_GET_USAGE_STATS
        return appOps.unsafeCheckOpNoThrow(op, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }

    // Package name of the app that was opened last.
    fun lastForegroundPackage(nowMillis: Long): String? {
        val usage = context.getSystemService(UsageStatsManager::class.java)
        // 12 hours back, because scrolling one feed for hours creates no new events.
        val events = usage.queryEvents(nowMillis - 12 * 60 * 60 * 1000L, nowMillis)
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                last = event.packageName
            }
        }
        return last
    }

    // TikTok has two package names, depending on the country.
    fun isTikTok(packageName: String?): Boolean =
        packageName == "com.zhiliaoapp.musically" || packageName == "com.ss.android.ugc.trill"
}
