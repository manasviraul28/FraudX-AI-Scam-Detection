package com.fraudx.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log

class FraudXService : Service() {

    // nullable — NOT lateinit, so crash is impossible
    var classifier: FraudClassifier? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): FraudXService = this@FraudXService
    }

    companion object {
        var instance: FraudXService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d("FRAUDX_SERVICE", "Service starting — loading TFLite")
        try {
            classifier = FraudClassifier(applicationContext)
            Log.d("FRAUDX_SERVICE", "TFLite loaded OK")
        } catch (e: Exception) {
            Log.e("FRAUDX_SERVICE", "TFLite failed — using RiskEngine: ${e.message}")
            classifier = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("FRAUDX_SERVICE", "Task removed — restarting service")
        val restartIntent = Intent(applicationContext, FraudXService::class.java)
        startService(restartIntent)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.d("FRAUDX_SERVICE", "Service destroyed — restarting")
        instance = null
        val restartIntent = Intent(applicationContext, FraudXService::class.java)
        startService(restartIntent)
        super.onDestroy()
    }

    fun classifyText(text: String): Float {
        return try {
            classifier?.classifyText(text) ?: fallbackScore(text)
        } catch (e: Exception) {
            fallbackScore(text)
        }
    }

    fun getRiskLabel(text: String): String {
        return try {
            classifier?.getRiskLabel(text) ?: fallbackRisk(text)
        } catch (e: Exception) {
            fallbackRisk(text)
        }
    }

    fun isHighRisk(text: String): Boolean {
        return try {
            classifier?.isHighRisk(text) ?: (fallbackScore(text) >= 0.75f)
        } catch (e: Exception) {
            RiskEngine.calculateRisk(text) == "HIGH RISK"
        }
    }

    private fun fallbackScore(text: String): Float {
        return when (RiskEngine.calculateRisk(text)) {
            "HIGH RISK"   -> 0.92f
            "MEDIUM RISK" -> 0.55f
            else          -> 0.1f
        }
    }

    private fun fallbackRisk(text: String): String {
        return when (RiskEngine.calculateRisk(text)) {
            "HIGH RISK"   -> "🔴 HIGH RISK — Scam Detected"
            "MEDIUM RISK" -> "🟡 MEDIUM RISK — Suspicious"
            else          -> "🔵 No Threats Detected"
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "fraudx_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FraudX Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "FraudX AI is actively protecting your device"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("🛡️ FraudX AI Active")
            .setContentText("AI protection running — monitoring calls & SMS")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }
}