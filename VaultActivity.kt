package com.fraudx.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VaultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Fraud Vault"

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerVault)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = VaultAdapter(FraudDatabase(this).getAllVaultEntries())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}