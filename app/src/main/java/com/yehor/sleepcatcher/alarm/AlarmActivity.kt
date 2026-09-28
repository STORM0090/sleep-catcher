package com.yehor.sleepcatcher.alarm

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.yehor.sleepcatcher.R

// Shown over the lock screen while the alarm rings. The lock screen part is set in the manifest.
class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm)

        findViewById<Button>(R.id.stop_button).setOnClickListener {
            AlarmRingService.stop(this)
            finish()
        }
    }
}
