package com.fraudx.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var logAdapter: LogAdapter
    private var allLogs = mutableListOf<LogItem>()
    private var currentTab = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Start background service
        val serviceIntent = Intent(this, FraudXService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Request permissions
        requestPermissions()

        // Setup RecyclerView — matches recyclerLog in XML
        val recycler = findViewById<RecyclerView>(R.id.recyclerLog)
        recycler.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(mutableListOf())
        recycler.adapter = logAdapter

        // Load logs
        refreshLogs()

        // Scan existing calls and SMS
        scanExistingCallLogs()
        scanExistingMessages()

        // Stats
        updateStats()

        // Top scan button
        findViewById<android.widget.Button>(R.id.btnScanTop).setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }

        // Demo data button — matches btnDemoData in XML
        findViewById<android.widget.Button>(R.id.btnDemoData).setOnClickListener {
            loadDemoData()
            Toast.makeText(this, "✅ Demo data loaded!", Toast.LENGTH_SHORT).show()
        }

        // Tabs — matches tabAll, tabCalls, tabSms, tabThreats in XML
        setupTabs()

        // Bottom nav — matches navHome, navVault, navScan, navBlocked, navHistory in XML
        setupBottomNav()

        // Firebase sync
        CloudScamDatabase.syncFromCloud(this)
    }

    private fun setupTabs() {
        val tabAll     = findViewById<TextView>(R.id.tabAll)
        val tabCalls   = findViewById<TextView>(R.id.tabCalls)
        val tabSms     = findViewById<TextView>(R.id.tabSms)
        val tabThreats = findViewById<TextView>(R.id.tabThreats)

        fun resetTabs() {
            tabAll.setTextColor(0xFF8B949E.toInt())
            tabCalls.setTextColor(0xFF8B949E.toInt())
            tabSms.setTextColor(0xFF8B949E.toInt())
            tabThreats.setTextColor(0xFF8B949E.toInt())
            tabAll.setBackgroundResource(0)
            tabCalls.setBackgroundResource(0)
            tabSms.setBackgroundResource(0)
            tabThreats.setBackgroundResource(0)
        }

        fun selectTab(tab: TextView, tabName: String) {
            resetTabs()
            tab.setTextColor(0xFF58A6FF.toInt())
            tab.setBackgroundResource(R.drawable.tab_selected)
            currentTab = tabName
            filterLogs()
        }

        tabAll.setOnClickListener     { selectTab(tabAll, "ALL") }
        tabCalls.setOnClickListener   { selectTab(tabCalls, "CALL") }
        tabSms.setOnClickListener     { selectTab(tabSms, "SMS") }
        tabThreats.setOnClickListener { selectTab(tabThreats, "HIGH") }

        // Set initial selection
        tabAll.setTextColor(0xFF58A6FF.toInt())
        tabAll.setBackgroundResource(R.drawable.tab_selected)
    }

    private fun setupBottomNav() {
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            // already home
        }
        findViewById<LinearLayout>(R.id.navVault).setOnClickListener {
            startActivity(Intent(this, VaultActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navScan).setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navBlocked).setOnClickListener {
            startActivity(Intent(this, BlockedActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun refreshLogs() {
        val db = FraudDatabase(this)
        allLogs = db.getAllLogs().toMutableList()
        filterLogs()
        updateStats()
    }

    private fun filterLogs() {
        val filtered = when (currentTab) {
            "CALL" -> allLogs.filter { it.type == "CALL" }
            "SMS"  -> allLogs.filter { it.type == "SMS" }
            "HIGH" -> allLogs.filter { it.risk.contains("HIGH") }
            else   -> allLogs
        }
        logAdapter.updateLogs(filtered)
    }

    private fun updateStats() {
        val db      = FraudDatabase(this)
        val logs    = db.getAllLogs()
        val calls   = logs.count { it.type == "CALL" }
        val sms     = logs.count { it.type == "SMS" }
        val threats = logs.count { it.risk.contains("HIGH") }

        // Matches tvCallCount, tvSmsCount, tvThreatCount in XML
        findViewById<TextView>(R.id.tvCallCount)?.text   = calls.toString()
        findViewById<TextView>(R.id.tvSmsCount)?.text    = sms.toString()
        findViewById<TextView>(R.id.tvThreatCount)?.text = threats.toString()
    }

    private fun loadDemoData() {
        val db = FraudDatabase(this)

        db.insertLog("CALL", "+919876543210", "Incoming call", "🔴 HIGH RISK — Scam Detected")
        db.insertLog("CALL", "+918765432109", "Incoming call", "🔴 HIGH RISK — Scam Detected")
        db.insertLog("CALL", "+917654321098", "Incoming call", "🟡 MEDIUM RISK — Suspicious Number")
        db.insertLog("CALL", "1930",          "Incoming call", "✅ Verified Helpline — Cyber Crime")
        db.insertLog("CALL", "112",           "Incoming call", "✅ Verified Helpline — National Emergency")

        db.insertLog("SMS", "VM-HDFCBK",      "Your OTP is 847291 for transaction of Rs.15000. Do not share.", "🔴 HIGH RISK — Scam Detected")
        db.insertLog("SMS", "+919123456789",  "Congratulations! You won Rs.5,00,000 in KBC lottery. Click http://bit.ly/claim now!", "🔴 HIGH RISK — Scam Detected")
        db.insertLog("SMS", "+918888888888",  "Your KYC is expired. Update now or account will be blocked.", "🔴 HIGH RISK — Scam Detected")
        db.insertLog("SMS", "DM-AMAZON",      "Your Amazon order has been shipped. Track at amazon.in", "🔵 No Threats Detected")

        db.insertVaultEntry("SMS",  "From: +919123456789\nCongratulations! You won Rs.5,00,000 in KBC lottery!", "🔴 HIGH RISK — Scam Detected")
        db.insertVaultEntry("CALL", "+919876543210\nIncoming call flagged by AI", "🔴 HIGH RISK — Scam Detected")

        db.blockNumber("+919876543210")
        db.blockNumber("+918765432109")
        db.blockNumber("+919123456789")
        db.blockNumber("+918888888888")
        db.blockNumber("+917654321098")

        refreshLogs()
    }

    private fun scanExistingCallLogs() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) return

        Thread {
            try {
                val db = FraudDatabase(this)
                val cursor = contentResolver.query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        android.provider.CallLog.Calls.NUMBER,
                        android.provider.CallLog.Calls.TYPE,
                        android.provider.CallLog.Calls.DATE
                    ),
                    null, null,
                    "${android.provider.CallLog.Calls.DATE} DESC"
                ) ?: return@Thread

                var count = 0
                while (cursor.moveToNext() && count < 20) {
                    val number    = cursor.getString(0) ?: "Unknown"
                    val riskLabel = when (RiskEngine.calculateRisk(number)) {
                        "HIGH RISK"   -> "🔴 HIGH RISK — Scam Detected"
                        "MEDIUM RISK" -> "🟡 MEDIUM RISK — Suspicious"
                        else          -> "🔵 No Threats Detected"
                    }
                    db.insertLog("CALL", number, "Past call log", riskLabel)
                    count++
                }
                cursor.close()
                runOnUiThread { refreshLogs() }
            } catch (e: Exception) {
                android.util.Log.e("FRAUDX", "Call log error: ${e.message}")
            }
        }.start()
    }

    private fun scanExistingMessages() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) return

        Thread {
            try {
                val db = FraudDatabase(this)
                val cursor = contentResolver.query(
                    android.net.Uri.parse("content://sms/inbox"),
                    arrayOf("address", "body", "date"),
                    null, null,
                    "date DESC"
                ) ?: return@Thread

                var count = 0
                while (cursor.moveToNext() && count < 20) {
                    val sender    = cursor.getString(0) ?: "Unknown"
                    val body      = cursor.getString(1) ?: ""
                    val riskLabel = when (RiskEngine.calculateRisk(body)) {
                        "HIGH RISK"   -> "🔴 HIGH RISK — Scam Detected"
                        "MEDIUM RISK" -> "🟡 MEDIUM RISK — Suspicious"
                        else          -> "🔵 No Threats Detected"
                    }
                    db.insertLog("SMS", sender, body.take(60), riskLabel)
                    count++
                }
                cursor.close()
                runOnUiThread { refreshLogs() }
            } catch (e: Exception) {
                android.util.Log.e("FRAUDX", "SMS scan error: ${e.message}")
            }
        }.start()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_NUMBERS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLogs()
        updateStats()
    }
}