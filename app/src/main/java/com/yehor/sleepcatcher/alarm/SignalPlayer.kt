package com.yehor.sleepcatcher.alarm

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SignalPlayer {

    // Alarm usage lets the vibration through Do Not Disturb.
    private val alarmAudio: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    // Three gentle taps: enough to lighten sleep, not enough to force a wake-up.
    fun softPulse(context: Context) {
        val timings = longArrayOf(0, 300, 700, 300, 700, 300)
        val amplitudes = intArrayOf(0, 60, 0, 60, 0, 60)
        vibrator(context).vibrate(VibrationEffect.createWaveform(timings, amplitudes, NO_REPEAT), alarmAudio)
    }

    fun startLoudVibration(context: Context) {
        val timings = longArrayOf(0, 800, 400)
        val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
        vibrator(context).vibrate(VibrationEffect.createWaveform(timings, amplitudes, REPEAT_FROM_START), alarmAudio)
    }

    fun stopVibration(context: Context) {
        vibrator(context).cancel()
    }

    private fun vibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private const val NO_REPEAT = -1
    private const val REPEAT_FROM_START = 0
}
