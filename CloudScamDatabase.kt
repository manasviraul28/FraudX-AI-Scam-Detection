package com.fraudx.app

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

object CloudScamDatabase {

    private const val PREF_NAME = "fraudx_cloud_cache"
    private const val KEY_SCAM_NUMBERS = "scam_numbers"

    private val db = FirebaseFirestore.getInstance()

    fun isKnownScamNumber(context: Context, number: String): Boolean {
        val json = getPrefs(context).getString(KEY_SCAM_NUMBERS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                if (arr.getJSONObject(i).getString("number") == number) return true
            }
            false
        } catch (e: Exception) { false }
    }

    fun getCloudRiskLabel(context: Context, number: String): String? {
        val json = getPrefs(context).getString(KEY_SCAM_NUMBERS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("number") == number) {
                    val reports = obj.getInt("reportCount")
                    return when {
                        reports >= 10 -> "🔴 HIGH RISK — Reported by $reports users"
                        reports >= 3  -> "🟡 MEDIUM RISK — Reported by $reports users"
                        else          -> "🟡 Suspicious — $reports user report(s)"
                    }
                }
            }
            null
        } catch (e: Exception) { null }
    }

    fun syncFromCloud(context: Context, onDone: (Int) -> Unit = {}) {
        db.collection("scam_numbers").get()
            .addOnSuccessListener { result ->
                val arr = JSONArray()
                for (doc in result) {
                    arr.put(JSONObject().apply {
                        put("number", doc.getString("number") ?: "")
                        put("reportCount", doc.getLong("reportCount") ?: 0)
                        put("category", doc.getString("category") ?: "Unknown")
                    })
                }
                getPrefs(context).edit()
                    .putString(KEY_SCAM_NUMBERS, arr.toString())
                    .putLong("last_sync", System.currentTimeMillis())
                    .apply()
                onDone(arr.length())
            }
            .addOnFailureListener { onDone(-1) }
    }

    fun reportNumberToCloud(number: String, category: String = "User Report") {
        val docRef = db.collection("scam_numbers").document(number)
        db.runTransaction { transaction ->
            val snap = transaction.get(docRef)
            if (snap.exists()) {
                val count = snap.getLong("reportCount") ?: 0
                transaction.update(docRef, "reportCount", count + 1)
            } else {
                transaction.set(docRef, hashMapOf(
                    "number" to number, "reportCount" to 1,
                    "category" to category,
                    "timestamp" to System.currentTimeMillis()
                ))
            }
        }
    }

    fun getCachedScamCount(context: Context): Int {
        val json = getPrefs(context).getString(KEY_SCAM_NUMBERS, "[]") ?: "[]"
        return try { JSONArray(json).length() } catch (e: Exception) { 0 }
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}