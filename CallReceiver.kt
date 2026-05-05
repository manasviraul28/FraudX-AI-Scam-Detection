package com.fraudx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FRAUDX_CALL", "Receiver fired! Action: ${intent.action}")

        try {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            Log.d("FRAUDX_CALL", "State: $state")

            if (state != TelephonyManager.EXTRA_STATE_RINGING) return

            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"
            Log.d("FRAUDX_CALL", "Number: $number")

            // Start service
            try {
                val serviceIntent = Intent(context, FraudXService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("FRAUDX_CALL", "Service start error: ${e.message}")
            }

            val risk = getSmartRisk(context, number)
            Log.d("FRAUDX_CALL", "Risk: $risk")

            FraudDatabase(context).insertLog("CALL", number, "Incoming call", risk)
            if (risk.contains("HIGH") || risk.contains("BLOCKED")) {
                FraudDatabase(context).insertVaultEntry("CALL", "$number\nIncoming call flagged", risk)
            }

            // Launch popup — FLAG_ACTIVITY_NEW_TASK only, nothing else
            val popup = Intent(context, CallAlertActivity::class.java)
            popup.putExtra("number", number)
            popup.putExtra("risk", risk)
            popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(popup)
            Log.d("FRAUDX_CALL", "Popup launched!")

        } catch (e: Exception) {
            Log.e("FRAUDX_CALL", "CRASH: ${e.message}")
        }
    }

    private fun getSmartRisk(context: Context, number: String): String {
        val db = FraudDatabase(context)
        if (db.isBlocked(number)) return "🚫 BLOCKED NUMBER"
        if (db.isWhitelisted(number)) return "✅ Whitelisted — Safe"
        val helpline = HelplineDatabase.getHelplineLabel(number)
        if (helpline != null) return "✅ Verified Helpline — $helpline"
        val cloudRisk = CloudScamDatabase.getCloudRiskLabel(context, number)
        if (cloudRisk != null) return cloudRisk
        return when (RiskEngine.calculateRisk(number)) {
            "HIGH RISK"   -> "🔴 HIGH RISK — Scam Detected"
            "MEDIUM RISK" -> "🟡 MEDIUM RISK — Suspicious Number"
            else          -> "🔵 No Threats Detected"
        }
    }
}