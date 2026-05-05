package com.fraudx.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FRAUDX_SMS", "Receiver fired!")

        try {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages == null || messages.isEmpty()) return

            val sender = messages[0].originatingAddress ?: "Unknown"
            val body   = messages.joinToString("") { it.messageBody ?: "" }
            Log.d("FRAUDX_SMS", "From: $sender | $body")

            // Start service
            try {
                val serviceIntent = Intent(context, FraudXService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("FRAUDX_SMS", "Service start error: ${e.message}")
            }

            val db = FraudDatabase(context)
            if (db.isWhitelisted(sender)) return

            val risk = getSmartRisk(context, sender, body)
            Log.d("FRAUDX_SMS", "Risk: $risk")

            db.insertLog("SMS", sender, body.take(60), risk)
            if (risk.contains("HIGH")) {
                db.insertVaultEntry("SMS", "From: $sender\n$body", risk)
            }

            // Launch popup — FLAG_ACTIVITY_NEW_TASK only, nothing else
            val popup = Intent(context, SmsAlertActivity::class.java)
            popup.putExtra("sender", sender)
            popup.putExtra("body", body)
            popup.putExtra("risk", risk)
            popup.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(popup)
            Log.d("FRAUDX_SMS", "Popup launched!")

        } catch (e: Exception) {
            Log.e("FRAUDX_SMS", "CRASH: ${e.message}")
        }
    }

    private fun getSmartRisk(context: Context, sender: String, body: String): String {
        val db = FraudDatabase(context)
        if (db.isBlocked(sender)) return "🚫 BLOCKED NUMBER"
        val helpline = HelplineDatabase.getHelplineLabel(sender)
        if (helpline != null) return "✅ Verified Helpline — $helpline"
        val cloudRisk = CloudScamDatabase.getCloudRiskLabel(context, sender)
        if (cloudRisk != null) return cloudRisk
        return when (RiskEngine.calculateRisk(body)) {
            "HIGH RISK"   -> "🔴 HIGH RISK — Scam Detected"
            "MEDIUM RISK" -> "🟡 MEDIUM RISK — Suspicious"
            else          -> "🔵 No Threats Detected"
        }
    }
}