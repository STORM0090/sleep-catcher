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

// Runs while the alarm is armed, only to keep the screen-off receiver registered.
class SleepWatchService : Service() {

    private val receiver = SleepOnsetReceiver()

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifications.ensureChannels(this)
        // The "special use" type only exists since Android 14.
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        }
        try {
            ServiceCompat.startForeground(this, Notifications.WATCH_ID, Notifications.watchNotification(this), type)
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "The system did not let the watch start", e)
            stopSelf()
            return START_NOT_STICKY
        }
        // If the system kills the service, it should start it again.
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, SleepWatchService::class.java))
            } catch (e: IllegalStateException) {
                Log.w(LOG_TAG, "The system did not let the watch start", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SleepWatchService::class.java))
        }
    }
}
