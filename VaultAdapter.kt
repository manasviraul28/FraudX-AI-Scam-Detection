package com.fraudx.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VaultAdapter(private val items: List<VaultItem>) :
    RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

    class VaultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView    = view.findViewById(R.id.tvType)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvRisk: TextView    = view.findViewById(R.id.tvRisk)
        val tvTime: TextView    = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vault, parent, false)
        return VaultViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val item = items[position]
        holder.tvType.text    = item.type
        holder.tvContent.text = item.content
        holder.tvTime.text    = item.timestamp
        holder.tvRisk.text    = item.risk
        holder.tvRisk.setTextColor(when {
            item.risk.contains("HIGH")   -> 0xFFFF7B7B.toInt()
            item.risk.contains("MEDIUM") -> 0xFFFFB347.toInt()
            else                         -> 0xFF58D68D.toInt()
        })
    }

    override fun getItemCount() = items.size
}