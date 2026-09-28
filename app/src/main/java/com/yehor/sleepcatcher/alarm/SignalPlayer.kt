package com.yehor.sleepcatcher.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SignalPlayer {

    // Three weak taps: enough to make sleep lighter, not enough to wake up.
    fun softPulse(context: Context) {
        val timings = longArrayOf(0, 300, 700, 300, 700, 300)
        val strength = intArrayOf(0, 60, 0, 60, 0, 60)
        // -1 means play once.
        vibrate(context, VibrationEffect.createWaveform(timings, strength, -1))
    }

    fun startLoudVibration(context: Context) {
        val timings = longArrayOf(0, 800, 400)
        val strength = intArrayOf(0, 255, 0)
        // 0 means repeat from the start until cancel() is called.
        vibrate(context, VibrationEffect.createWaveform(timings, strength, 0))
    }

    fun stopVibration(context: Context) {
        vibrator(context).cancel()
    }

    // Marked as an alarm, so the vibration also works in Do Not Disturb.
    private fun vibrate(context: Context, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator(context).vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            @Suppress("DEPRECATION")
            vibrator(context).vibrate(effect, attributes)
        }
    }

    private fun vibrator(context: Context): Vibrator {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getSystemService(VibratorManager::class.java).defaultVibrator
        }
        @Suppress("DEPRECATION")
        return context.getSystemService(Vibrator::class.java)
    }
}
