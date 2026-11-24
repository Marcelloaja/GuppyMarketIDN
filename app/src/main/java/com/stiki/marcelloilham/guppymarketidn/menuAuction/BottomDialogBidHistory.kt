package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stiki.marcelloilham.guppymarketidn.R

class BottomDialogBidHistory : BottomSheetDialogFragment() {

    data class BidHistoryItem(
        val avatarText: String,
        val avatarBgHex: String,
        val userHandle: String,
        val timeAgo: String,
        val amount: String,
        val isHighest: Boolean = false
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_dialog_bid_history, container, false)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseBidHistory)
        btnClose.setOnClickListener { dismiss() }

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerBidHistoryList)

        val sampleBids = listOf(
            BidHistoryItem("BG", "#00897B", "@bimaguppy", "2 menit lalu", "Rp 1.275.000", true),
            BidHistoryItem("DN", "#1565C0", "@danang_guppy", "12 menit lalu", "Rp 1.250.000"),
            BidHistoryItem("MB", "#D85C42", "@mello_breeder", "45 menit lalu", "Rp 1.225.000"),
            BidHistoryItem("AK", "#388E3C", "@agung_kudus", "1 jam lalu", "Rp 1.200.000"),
            BidHistoryItem("FJ", "#7B1FA2", "@fajar_jogja", "2 jam lalu", "Rp 1.175.000"),
            BidHistoryItem("RS", "#F57C00", "@rizky_solo", "3 jam lalu", "Rp 1.150.000"),
            BidHistoryItem("HA", "#0097A7", "@hendra_bdg", "4 jam lalu", "Rp 1.125.000"),
            BidHistoryItem("DR", "#5D4037", "@dani_duri", "5 jam lalu", "Rp 1.100.000"),
            BidHistoryItem("BG", "#00897B", "@bimaguppy", "6 jam lalu", "Rp 1.075.000"),
            BidHistoryItem("DN", "#1565C0", "@danang_guppy", "7 jam lalu", "Rp 1.050.000"),
            BidHistoryItem("MB", "#D85C42", "@mello_breeder", "8 jam lalu", "Rp 1.025.000"),
            BidHistoryItem("AK", "#388E3C", "@agung_kudus", "9 jam lalu", "Rp 1.000.000"),
            BidHistoryItem("FJ", "#7B1FA2", "@fajar_jogja", "10 jam lalu", "Rp 975.000"),
            BidHistoryItem("RS", "#F57C00", "@rizky_solo", "11 jam lalu", "Rp 950.000"),
            BidHistoryItem("HA", "#0097A7", "@hendra_bdg", "12 jam lalu", "Rp 925.000"),
            BidHistoryItem("DR", "#5D4037", "@dani_duri", "13 jam lalu", "Rp 900.000"),
            BidHistoryItem("BG", "#00897B", "@bimaguppy", "14 jam lalu", "Rp 875.000"),
            BidHistoryItem("DN", "#1565C0", "@danang_guppy", "15 jam lalu", "Rp 850.000")
        )

        val adapter = BidHistoryAdapter(requireContext(), sampleBids)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        return view
    }

    class BidHistoryAdapter(
        private val context: Context,
        private val bids: List<BidHistoryItem>
    ) : RecyclerView.Adapter<BidHistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvAvatar: TextView = view.findViewById(R.id.tvBidAvatar)
            val tvUserHandle: TextView = view.findViewById(R.id.tvBidUserHandle)
            val tvHighestTag: TextView = view.findViewById(R.id.tvHighestTag)
            val tvTime: TextView = view.findViewById(R.id.tvBidTime)
            val tvAmount: TextView = view.findViewById(R.id.tvBidAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_bid_history_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = bids[position]
            holder.tvAvatar.text = item.avatarText
            holder.tvAvatar.setBackgroundColor(Color.parseColor(item.avatarBgHex))
            holder.tvUserHandle.text = item.userHandle
            holder.tvTime.text = item.timeAgo
            holder.tvAmount.text = item.amount

            if (item.isHighest) {
                holder.tvHighestTag.visibility = View.VISIBLE
            } else {
                holder.tvHighestTag.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = bids.size
    }
}
