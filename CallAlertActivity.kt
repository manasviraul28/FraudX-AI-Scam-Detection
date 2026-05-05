package com.fraudx.app

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CallAlertActivity : AppCompatActivity() {

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
            "FraudX:CallAlertWakeLock"
        )
        wakeLock?.acquire(60000) // 60 seconds max

        setContentView(R.layout.activity_call_alert)

        val number = intent.getStringExtra("number") ?: "Unknown"
        val risk   = intent.getStringExtra("risk")   ?: "🔵 No Threats Detected"

        val tvNumber   = findViewById<TextView>(R.id.tvAlertNumber)
        val tvRisk     = findViewById<TextView>(R.id.tvAlertRisk)
        val tvMessage  = findViewById<TextView>(R.id.tvAlertMessage)
        val btnBlock   = findViewById<Button>(R.id.btnBlock)
        val btnDismiss = findViewById<Button>(R.id.btnDismiss)

        tvNumber.text = number
        tvRisk.text   = risk

        tvMessage.text = when {
            risk.contains("HIGH")     -> "⚠️ Flagged as scam by AI. Do not share OTP or personal details!"
            risk.contains("MEDIUM")   -> "⚠️ Suspicious number. Stay cautious."
            risk.contains("BLOCKED")  -> "🚫 You have blocked this number previously."
            risk.contains("Helpline") -> "✅ Verified Indian helpline. Safe to answer."
            else                      -> "✅ No threats detected. Appears safe to answer."
        }

        val color = when {
            risk.contains("HIGH")     -> 0xFFFF4444.toInt()
            risk.contains("MEDIUM")   -> 0xFFFFBB33.toInt()
            risk.contains("BLOCKED")  -> 0xFFFF4444.toInt()
            else                      -> 0xFF00C851.toInt()
        }
        tvRisk.setTextColor(color)

        if (risk.contains("HIGH") || risk.contains("MEDIUM") || risk.contains("BLOCKED")) {
            btnBlock.visibility = android.view.View.VISIBLE
            btnBlock.setOnClickListener {
                FraudDatabase(this).blockNumber(number)
                Toast.makeText(this, "🚫 $number blocked!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            btnBlock.visibility = android.view.View.GONE
        }

        btnDismiss.setOnClickListener {
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
        val number = intent.getStringExtra("number") ?: "Unknown"
        val risk   = intent.getStringExtra("risk")   ?: "🔵 No Threats Detected"
        findViewById<TextView>(R.id.tvAlertNumber).text = number
        findViewById<TextView>(R.id.tvAlertRisk).text   = risk
    }

    override fun onBackPressed() {
        // do nothing
    }
}