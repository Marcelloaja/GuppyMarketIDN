package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R

class AuctionGridAdapter(
    private val context: Context,
    private val rawItems: List<AuctionItem>,
    private val onClick: (AuctionItem) -> Unit
) : RecyclerView.Adapter<AuctionGridAdapter.ViewHolder>() {

    data class AuctionItem(
        val id: Int,
        val title: String,
        val seller: String,
        val location: String,
        val price: String,
        val bids: Int,
        var secondsRemaining: Long,
        val imageRes: Int,
        val grade: String,
        val category: String = "Semua",
        val status: String = "LIVE"
    )

    private var filteredList: MutableList<AuctionItem> = rawItems.toMutableList()
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            var updated = false
            for (item in filteredList) {
                if (item.status == "LIVE" && item.secondsRemaining > 0) {
                    item.secondsRemaining--
                    updated = true
                }
            }
            if (updated) {
                notifyDataSetChanged()
            }
            handler.postDelayed(this, 1000L)
        }
    }

    init {
        handler.postDelayed(timerRunnable, 1000L)
    }

    fun stopTimer() {
        handler.removeCallbacks(timerRunnable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_auction_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredList[position]
        holder.imgAuction.setImageResource(item.imageRes)
        holder.tvTitle.text = item.title
        holder.tvSeller.text = "${item.seller} · ${item.location}"
        holder.tvPrice.text = item.price
        holder.tvBids.text = "${item.bids} penawaran"
        holder.tvGrade.text = item.grade

        if (item.status == "FINISHED") {
            holder.tvTime.text = "Selesai"
            holder.tvTime.setTextColor(context.getColor(R.color.guppy_text_gray))
        } else if (item.status == "UPCOMING") {
            holder.tvTime.text = "Akan Datang"
            holder.tvTime.setTextColor(context.getColor(R.color.guppy_primary))
        } else {
            holder.tvTime.text = formatTimeLeft(item.secondsRemaining)
            holder.tvTime.setTextColor(context.getColor(R.color.guppy_red))
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun filterItems(
        status: String = "LIVE",
        category: String = "Semua",
        query: String = "",
        grade: String = "All",
        location: String = "All"
    ) {
        val result = rawItems.filter { item ->
            val matchesStatus = item.status.equals(status, ignoreCase = true)

            val matchesCategory = category.equals("Semua", ignoreCase = true) ||
                item.category.lowercase().contains(category.lowercase()) ||
                item.title.lowercase().contains(category.lowercase())

            val matchesQuery = query.isEmpty() ||
                item.title.lowercase().contains(query.lowercase()) ||
                item.seller.lowercase().contains(query.lowercase()) ||
                item.location.lowercase().contains(query.lowercase())

            val matchesGrade = grade.equals("All", ignoreCase = true) ||
                item.grade.lowercase().contains(grade.lowercase())

            val matchesLocation = location.equals("All", ignoreCase = true) ||
                item.location.lowercase().contains(location.lowercase())

            matchesStatus && matchesCategory && matchesQuery && matchesGrade && matchesLocation
        }

        filteredList = result.toMutableList()
        notifyDataSetChanged()
    }

    private fun formatTimeLeft(totalSecs: Long): String {
        if (totalSecs <= 0) return "Selesai"
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d:%02d", hours, mins, secs)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAuction: ImageView = view.findViewById(R.id.imgAuctionItem)
        val tvTitle: TextView = view.findViewById(R.id.tvAuctionItemTitle)
        val tvSeller: TextView = view.findViewById(R.id.tvAuctionItemSeller)
        val tvPrice: TextView = view.findViewById(R.id.tvAuctionItemPrice)
        val tvBids: TextView = view.findViewById(R.id.tvAuctionItemBids)
        val tvTime: TextView = view.findViewById(R.id.tvAuctionItemTime)
        val tvGrade: TextView = view.findViewById(R.id.tvAuctionItemGrade)
    }
}
