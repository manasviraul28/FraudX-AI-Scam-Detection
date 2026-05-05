package com.fraudx.app

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BlockedActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var db: FraudDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Blocked Numbers"

        db = FraudDatabase(this)
        recycler = findViewById(R.id.recyclerBlocked)
        recycler.layoutManager = LinearLayoutManager(this)
        loadList()
    }

    private fun loadList() {
        val numbers = db.getAllBlockedNumbers()
        recycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                    setPadding(40, 32, 40, 32)
                    textSize = 15f
                    setTextColor(0xFFE6EDF3.toInt())
                }) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val number = numbers[position]
                (holder.itemView as TextView).text = "🚫  $number"
                holder.itemView.setOnLongClickListener {
                    AlertDialog.Builder(this@BlockedActivity)
                        .setTitle("Unblock?")
                        .setMessage("Remove $number from blocked list?")
                        .setPositiveButton("Unblock") { _, _ ->
                            db.unblockNumber(number)
                            Toast.makeText(this@BlockedActivity, "Unblocked!", Toast.LENGTH_SHORT).show()
                            loadList()
                        }
                        .setNegativeButton("Cancel", null).show()
                    true
                }
            }

            override fun getItemCount() = numbers.size
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}