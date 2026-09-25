package com.yehor.sleepcatcher.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerAppsTest {

    @Test
    fun bothTikTokPackagesCount() {
        assertTrue(TriggerApps.isTrigger("com.zhiliaoapp.musically"))
        assertTrue(TriggerApps.isTrigger("com.ss.android.ugc.trill"))
    }

    @Test
    fun otherAppsDoNotCount() {
        assertFalse(TriggerApps.isTrigger("com.whatsapp"))
        assertFalse(TriggerApps.isTrigger("com.zhiliaoapp"))
    }

    @Test
    fun anUnknownAppDoesNotCount() {
        assertFalse(TriggerApps.isTrigger(null))
    }
}
