package com.fraudx.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter(private var items: List<LogItem>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView    = view.findViewById(R.id.tvLogIcon)
        val tvNumber: TextView  = view.findViewById(R.id.tvLogNumber)
        val tvPreview: TextView = view.findViewById(R.id.tvLogPreview)
        val tvRisk: TextView    = view.findViewById(R.id.tvLogRisk)
        val tvTime: TextView    = view.findViewById(R.id.tvLogTime)
    }

    fun updateLogs(newItems: List<LogItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = items[position]
        holder.tvIcon.text = when (item.type) {
            "CALL" -> "📞"
            "SMS"  -> "💬"
            "UPI"  -> "💳"
            else   -> "🔔"
        }
        holder.tvNumber.text  = item.number
        holder.tvPreview.text = item.preview.take(60) + if (item.preview.length > 60) "…" else ""
        holder.tvRisk.text    = item.risk
        holder.tvRisk.setTextColor(when {
            item.risk.contains("HIGH")    -> 0xFFFF7B7B.toInt()
            item.risk.contains("MEDIUM")  -> 0xFFFFB347.toInt()
            item.risk.contains("BLOCKED") -> 0xFFFF4444.toInt()
            else                          -> 0xFF58D68D.toInt()
        })
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(item.timestamp))
    }

    override fun getItemCount() = items.size
}