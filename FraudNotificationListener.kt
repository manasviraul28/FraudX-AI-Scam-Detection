package com.fraudx.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FraudNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val fullText = "$title $text".trim()

        if (fullText.isBlank()) return

        val classifier = FraudClassifier(applicationContext)
        val db = FraudDatabase(applicationContext)

        if (classifier.isHighRisk(fullText)) {
            val riskLabel = classifier.getRiskLabel(fullText)
            db.insertVaultEntry("NOTIFICATION", fullText, riskLabel)
            db.insertLog("NOTIFICATION", "App Notification", fullText.take(60), riskLabel)
        }
    }
}