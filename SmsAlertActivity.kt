package com.fraudx.app

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SmsAlertActivity : AppCompatActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake up screen and show over lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // WakeLock — forces screen on even on Xiaomi
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "FraudX:SmsAlertWakeLock"
        )
        wakeLock?.acquire(60000) // 60 seconds max

        setContentView(R.layout.activity_sms_alert)

        val sender = intent.getStringExtra("sender") ?: "Unknown"
        val body   = intent.getStringExtra("body")   ?: ""
        val risk   = intent.getStringExtra("risk")   ?: "🔵 No Threats Detected"

        findViewById<TextView>(R.id.tvSender).text = sender
        findViewById<TextView>(R.id.tvBody).text   = body
        findViewById<TextView>(R.id.tvRisk).text   = risk

        val color = when {
            risk.contains("HIGH")    -> 0xFFFF4444.toInt()
            risk.contains("MEDIUM")  -> 0xFFFFBB33.toInt()
            risk.contains("BLOCKED") -> 0xFFFF4444.toInt()
            else                     -> 0xFF00C851.toInt()
        }
        findViewById<TextView>(R.id.tvRisk).setTextColor(color)

        val btnBlock = findViewById<Button>(R.id.btnBlock)
        if (risk.contains("HIGH") || risk.contains("MEDIUM") || risk.contains("BLOCKED")) {
            btnBlock.visibility = android.view.View.VISIBLE
            btnBlock.setOnClickListener {
                FraudDatabase(this).blockNumber(sender)
                Toast.makeText(this, "🚫 $sender blocked!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            btnBlock.visibility = android.view.View.GONE
        }

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.release()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sender = intent.getStringExtra("sender") ?: "Unknown"
        val body   = intent.getStringExtra("body")   ?: ""
        val risk   = intent.getStringExtra("risk")   ?: "🔵 No Threats Detected"
        findViewById<TextView>(R.id.tvSender).text = sender
        findViewById<TextView>(R.id.tvBody).text   = body
        findViewById<TextView>(R.id.tvRisk).text   = risk
    }

    override fun onBackPressed() {
        // do nothing — force user to tap Dismiss
    }
}