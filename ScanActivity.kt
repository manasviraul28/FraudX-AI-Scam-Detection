package com.fraudx.app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ScanActivity : AppCompatActivity() {

    private lateinit var classifier: FraudClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Manual Scan"

        classifier = FraudClassifier(this)

        val etInput  = findViewById<EditText>(R.id.etInput)
        val btnScan  = findViewById<Button>(R.id.btnScan)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        btnScan.setOnClickListener {
            val input = etInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Enter text, number or UPI link to scan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val helpline = HelplineDatabase.getHelplineLabel(input)
            if (helpline != null) {
                tvResult.text = "✅ Verified Helpline\n$helpline"
                tvResult.setTextColor(0xFF58D68D.toInt())
                return@setOnClickListener
            }

            if (UpiScanner.isUpiLink(input)) {
                val upiResult = UpiScanner.analyzeUpiLink(input)
                tvResult.text = upiResult
                tvResult.setTextColor(
                    when {
                        upiResult.contains("HIGH")   -> 0xFFFF7B7B.toInt()
                        upiResult.contains("MEDIUM") -> 0xFFFFB347.toInt()
                        else                         -> 0xFF58D68D.toInt()
                    }
                )
                FraudDatabase(this).insertLog("UPI", input, input, upiResult)
                if (upiResult.contains("HIGH")) {
                    FraudDatabase(this).insertVaultEntry("UPI", input, upiResult)
                    showActionDialog(input)
                }
                return@setOnClickListener
            }

            val risk = classifier.getRiskLabel(input)
            tvResult.text = risk
            tvResult.setTextColor(
                when {
                    risk.contains("HIGH")   -> 0xFFFF7B7B.toInt()
                    risk.contains("MEDIUM") -> 0xFFFFB347.toInt()
                    else                    -> 0xFF58D68D.toInt()
                }
            )

            FraudDatabase(this).insertLog("SMS", input, input, risk)

            if (classifier.isHighRisk(input)) {
                FraudDatabase(this).insertVaultEntry("MANUAL", input, risk)
                showActionDialog(input)
            }
        }
    }

    private fun showActionDialog(input: String) {
        val db = FraudDatabase(this)
        AlertDialog.Builder(this, R.style.DarkAlertDialog)
            .setTitle("⚠️ High Risk Detected")
            .setMessage("What do you want to do?")
            .setPositiveButton("🚫 Block") { _, _ ->
                db.blockNumber(input)
                Toast.makeText(this, "Blocked!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("📋 Report") { _, _ ->
                db.reportScam(input, "User reported")
                CloudScamDatabase.reportNumberToCloud(input)
                Toast.makeText(this, "Reported!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("✅ Whitelist") { _, _ ->
                db.addToWhitelist(input)
                Toast.makeText(this, "Whitelisted!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}