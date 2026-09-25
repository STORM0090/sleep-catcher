package com.yehor.sleepcatcher.detection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

class ForegroundAppDetector(private val context: Context) {

    // Usage access is a special permission: there is no dialog for it, the
    // user has to switch it on in the system settings.
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // Package of the activity that came to the front most recently.
    fun lastForegroundPackage(nowMillis: Long): String? {
        val usage = context.getSystemService(UsageStatsManager::class.java)
        val events = usage.queryEvents(nowMillis - LOOKBACK_MILLIS, nowMillis)
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

    private companion object {
        // Scrolling one feed creates no new events, so a long evening in
        // TikTok needs a long look back.
        const val LOOKBACK_MILLIS = 12L * 60 * 60 * 1000
    }
}
