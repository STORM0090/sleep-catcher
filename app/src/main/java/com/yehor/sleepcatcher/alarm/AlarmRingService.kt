package com.yehor.sleepcatcher.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.yehor.sleepcatcher.LOG_TAG
import com.yehor.sleepcatcher.Notifications

// The loud alarm is a foreground service, because a receiver only lives for a few seconds.
class AlarmRingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var ringing = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        Notifications.ensureChannels(this)
        try {
            ServiceCompat.startForeground(
                this, Notifications.ALARM_ID, Notifications.alarmNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "The system did not let the alarm start", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!ringing) {
            ringing = true
            // Vibrate first, so the alarm still works if the sound fails.
            SignalPlayer.startLoudVibration(this)
            playSound()
            // Stop by itself after 10 minutes.
            handler.postDelayed({ stopSelf() }, 10 * 60 * 1000L)
        }
        return START_NOT_STICKY
    }

    private fun playSound() {
        var uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (uri == null) return

        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mediaPlayer.setAudioAttributes(attributes)
            mediaPlayer.setDataSource(this, uri)
            mediaPlayer.isLooping = true
            mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Could not play the alarm sound, only vibrating", e)
            mediaPlayer.release()
            player = null
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        SignalPlayer.stopVibration(this)
        ringing = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_STOP = "com.yehor.sleepcatcher.action.STOP_ALARM"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AlarmRingService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(LOG_TAG, "The system did not let the alarm start", e)
            }
        }

        // Does nothing if the alarm is not ringing.
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }

        // For the Stop button in the notification, which can only send an intent.
        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmRingService::class.java).setAction(ACTION_STOP)
    }
}
