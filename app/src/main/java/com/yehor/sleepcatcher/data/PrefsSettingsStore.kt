package com.yehor.sleepcatcher.data

import android.content.Context
import androidx.core.content.edit
import com.yehor.sleepcatcher.core.WakeMode

class PrefsSettingsStore(context: Context) : SettingsStore {

    private val prefs = context.getSharedPreferences("sleep_catcher", Context.MODE_PRIVATE)

    override var mode: WakeMode
        get() = WakeMode.fromNativeId(prefs.getInt("mode", 0)) ?: WakeMode.REFRESHED
        set(value) = prefs.edit { putInt("mode", value.nativeId) }

    override var targetHour: Int
        get() = prefs.getInt("target_hour", 9)
        set(value) = prefs.edit { putInt("target_hour", value) }

    override var targetMinute: Int
        get() = prefs.getInt("target_minute", 0)
        set(value) = prefs.edit { putInt("target_minute", value) }

    override var armed: Boolean
        get() = prefs.getBoolean("armed", false)
        set(value) = prefs.edit { putBoolean("armed", value) }

    override var lastOnsetMillis: Long
        get() = prefs.getLong("last_onset", 0L)
        set(value) = prefs.edit { putLong("last_onset", value) }

    override var scheduledSignalCount: Int
        get() = prefs.getInt("signal_count", 0)
        set(value) = prefs.edit { putInt("signal_count", value) }

    override var plannedFinalWakeMillis: Long
        get() = prefs.getLong("final_wake", 0L)
        set(value) = prefs.edit { putLong("final_wake", value) }
}
