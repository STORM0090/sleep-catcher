package com.yehor.sleepcatcher.data

import android.content.Context
import androidx.core.content.edit
import com.yehor.sleepcatcher.core.WakeMode

class PrefsSettingsStore(context: Context) : SettingsStore {

    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override var mode: WakeMode
        get() = WakeMode.fromNativeId(prefs.getInt(KEY_MODE, SettingsDefaults.MODE.nativeId))
            ?: SettingsDefaults.MODE
        set(value) = prefs.edit { putInt(KEY_MODE, value.nativeId) }

    override var targetHour: Int
        get() = prefs.getInt(KEY_TARGET_HOUR, SettingsDefaults.TARGET_HOUR)
        set(value) = prefs.edit { putInt(KEY_TARGET_HOUR, value) }

    override var targetMinute: Int
        get() = prefs.getInt(KEY_TARGET_MINUTE, SettingsDefaults.TARGET_MINUTE)
        set(value) = prefs.edit { putInt(KEY_TARGET_MINUTE, value) }

    override var armed: Boolean
        get() = prefs.getBoolean(KEY_ARMED, false)
        set(value) = prefs.edit { putBoolean(KEY_ARMED, value) }

    override var lastOnsetMillis: Long
        get() = prefs.getLong(KEY_LAST_ONSET, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_ONSET, value) }

    override var scheduledSignalCount: Int
        get() = prefs.getInt(KEY_SIGNAL_COUNT, 0)
        set(value) = prefs.edit { putInt(KEY_SIGNAL_COUNT, value) }

    override var plannedFinalWakeMillis: Long
        get() = prefs.getLong(KEY_FINAL_WAKE, 0L)
        set(value) = prefs.edit { putLong(KEY_FINAL_WAKE, value) }

    private companion object {
        const val FILE_NAME = "sleep_catcher"
        const val KEY_MODE = "mode"
        const val KEY_TARGET_HOUR = "target_hour"
        const val KEY_TARGET_MINUTE = "target_minute"
        const val KEY_ARMED = "armed"
        const val KEY_LAST_ONSET = "last_onset"
        const val KEY_SIGNAL_COUNT = "signal_count"
        const val KEY_FINAL_WAKE = "final_wake"
    }
}
