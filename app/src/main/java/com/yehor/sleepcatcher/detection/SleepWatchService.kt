package com.yehor.sleepcatcher.detection

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.yehor.sleepcatcher.LOG_TAG
import com.yehor.sleepcatcher.Notifications

// Its only job is to stay alive at night so the screen-off receiver exists.
class SleepWatchService : Service() {

    private val onsetReceiver = SleepOnsetReceiver()

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            onsetReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        // The special-use type only exists from Android 14; before that a
        // service of this kind needs no type at all.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, Notifications.WATCH_ID, Notifications.watchNotification(this), type)
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "The system refused to keep watching in the foreground", e)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(onsetReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, SleepWatchService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(LOG_TAG, "The system refused to start watching", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SleepWatchService::class.java))
        }
    }
}
