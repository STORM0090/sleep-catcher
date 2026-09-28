package com.yehor.sleepcatcher.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.yehor.sleepcatcher.LOG_TAG
import com.yehor.sleepcatcher.Notifications

// The loud alarm runs as a foreground service: a broadcast receiver is only
// alive for a few seconds, which is not enough to ring until someone answers.
class AlarmRingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val stopWhenNobodyAnswers = Runnable { stopSelf() }
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
                this,
                Notifications.ALARM_ID,
                Notifications.alarmNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "The system refused to show the alarm", e)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!ringing) {
            ringing = true
            // Vibration first: it still wakes the sleeper if the sound fails.
            SignalPlayer.startLoudVibration(this)
            startSound()
            handler.postDelayed(stopWhenNobodyAnswers, MAX_RING_MILLIS)
        }
        return START_NOT_STICKY
    }

    private fun startSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                setWakeMode(this@AlarmRingService, PowerManager.PARTIAL_WAKE_LOCK)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Could not play the alarm sound, vibrating only", e)
            player?.release()
            player = null
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(stopWhenNobodyAnswers)
        player?.run {
            if (isPlaying) stop()
            release()
        }
        player = null
        SignalPlayer.stopVibration(this)
        ringing = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ACTION_STOP = "com.yehor.sleepcatcher.action.STOP_ALARM"
        private const val MAX_RING_MILLIS = 10L * 60 * 1000

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AlarmRingService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(LOG_TAG, "The system refused to start the alarm service", e)
            }
        }

        // Does nothing when the alarm is not ringing.
        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingService::class.java))
        }

        // For the notification button, which can only send an intent.
        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmRingService::class.java).setAction(ACTION_STOP)
    }
}
