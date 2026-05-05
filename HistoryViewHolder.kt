package com.fraudx.app

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tvText: TextView = itemView.findViewById(R.id.tvHistoryText)
    fun bind(item: HistoryItem) { tvText.text = item.text }
}