package com.fraudx.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class HistoryActivity : AppCompatActivity() {

    data class NewsItem(
        val title: String,
        val summary: String,
        val source: String,
        val risk: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "SOS & Scam Alerts"

        findViewById<Button>(R.id.btnSOS).setOnClickListener {
            Toast.makeText(this, "📞 Calling Cyber Crime Helpline 1930...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:1930") })
        }

        findViewById<Button>(R.id.btnSOS112).setOnClickListener {
            Toast.makeText(this, "📞 Calling National Emergency 112...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:112") })
        }

        val progressBar = findViewById<ProgressBar>(R.id.progressNews)
        val tvError     = findViewById<TextView>(R.id.tvNewsError)
        val recycler    = findViewById<RecyclerView>(R.id.recyclerNews)
        recycler.layoutManager = LinearLayoutManager(this)

        progressBar.visibility = View.VISIBLE
        tvError.visibility = View.GONE

        Thread {
            try {
                val news = fetchCyberCrimeNews()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (news.isEmpty()) {
                        tvError.text = "⚠️ No cyber crime news found right now. Try again later."
                        tvError.visibility = View.VISIBLE
                    } else {
                        recycler.adapter = NewsAdapter(news)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    tvError.text = "⚠️ Could not load news. Check internet connection."
                    tvError.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun fetchCyberCrimeNews(): List<NewsItem> {
        val apiKey = "aa213f7474497810ffd1db4d59944d26"

        // Very specific cyber crime India queries only
        val allNews = mutableListOf<NewsItem>()
        try {
            val encoded = java.net.URLEncoder.encode("cyber crime scam fraud India", "UTF-8")
            val url = "https://gnews.io/api/v4/search?q=$encoded&lang=en&country=in&max=10&apikey=$apiKey"
            allNews.addAll(fetchFromUrl(url))
        } catch (e: Exception) {}

        return allNews.distinctBy { it.title }.take(10)
    }

    private fun fetchFromUrl(urlStr: String): List<NewsItem> {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("User-Agent", "Mozilla/5.0")

        if (connection.responseCode != 200) return emptyList()

        val response = BufferedReader(InputStreamReader(connection.inputStream)).readText()
        val articles = JSONObject(response).getJSONArray("articles")
        val list = mutableListOf<NewsItem>()

        for (i in 0 until articles.length()) {
            val a       = articles.getJSONObject(i)
            val title   = a.getString("title")
            val desc    = a.optString("description", "Tap to read more")
            val source  = a.getJSONObject("source").getString("name")

            // Only include if actually about cyber crime / fraud
            val titleLower = title.lowercase()
            val isRelevant = titleLower.contains("fraud") ||
                    titleLower.contains("scam") ||
                    titleLower.contains("cyber") ||
                    titleLower.contains("phishing") ||
                    titleLower.contains("hack") ||
                    titleLower.contains("upi") ||
                    titleLower.contains("otp") ||
                    titleLower.contains("arrest") ||
                    titleLower.contains("bank") ||
                    titleLower.contains("cheat")

            if (!isRelevant) continue

            val risk = when {
                titleLower.contains("upi") ||
                        titleLower.contains("bank")     -> "🔴 Banking Fraud"
                titleLower.contains("phishing") ||
                        titleLower.contains("otp")      -> "🔴 Phishing Alert"
                titleLower.contains("arrest") ||
                        titleLower.contains("police")   -> "🟡 Law Enforcement"
                titleLower.contains("hack")     -> "🔴 Hacking"
                else                            -> "🟠 Cyber Crime"
            }

            list.add(NewsItem(title, desc, source, risk))
        }
        return list
    }

    inner class NewsAdapter(private val items: List<NewsItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(
                LinearLayout(parent.context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 28, 32, 28)
                    setBackgroundResource(R.drawable.card_bg)
                    val lp = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = 20
                    layoutParams = lp
                }
            ) {}

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item      = items[position]
            val container = holder.itemView as LinearLayout
            container.removeAllViews()

            val tvRisk = TextView(container.context).apply {
                text = item.risk
                textSize = 10f
                setTextColor(when {
                    item.risk.contains("🔴") -> 0xFFFF7B7B.toInt()
                    else                     -> 0xFFFFB347.toInt()
                })
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 6
                layoutParams = lp
            }

            val tvTitle = TextView(container.context).apply {
                text = item.title
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(0xFFE6EDF3.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8
                layoutParams = lp
            }

            val tvSummary = TextView(container.context).apply {
                text = item.summary
                textSize = 12f
                setTextColor(0xFF8B949E.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 8
                layoutParams = lp
            }

            val tvSource = TextView(container.context).apply {
                text = "📰 ${item.source}"
                textSize = 10f
                setTextColor(0xFF484F58.toInt())
            }

            container.addView(tvRisk)
            container.addView(tvTitle)
            container.addView(tvSummary)
            container.addView(tvSource)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed(); return true
    }
}