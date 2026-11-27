package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.content.Context
import android.content.res.ColorStateList
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
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class BottomDialogBidHistory : BottomSheetDialogFragment() {

    data class BidHistoryItem(
        val avatarText: String,
        val avatarBgHex: String,
        val userHandle: String,
        val timeAgo: String,
        val amount: String,
        val isHighest: Boolean = false
    )

    private var bidsListenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_dialog_bid_history, container, false)

        val btnClose = view.findViewById<ImageView>(R.id.btnCloseBidHistory)
        btnClose.setOnClickListener { dismiss() }

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerBidHistoryList)
        val tvTotalCount = view.findViewById<TextView>(R.id.tvTotalBidCountSub)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyBidHistory)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        val auctionId = arguments?.getString(ARG_AUCTION_ID).orEmpty()
        if (auctionId.isBlank()) {
            tvTotalCount?.text = "0 penawaran masuk"
            recycler.visibility = View.GONE
            tvEmpty?.visibility = View.VISIBLE
            return view
        }

        bidsListenerRegistration = FirestoreRepository.listenAuctionBids(auctionId) { bids ->
            if (!isAdded) return@listenAuctionBids
            tvTotalCount?.text = "${bids.size} penawaran masuk"

            if (bids.isEmpty()) {
                recycler.visibility = View.GONE
                tvEmpty?.visibility = View.VISIBLE
            } else {
                recycler.visibility = View.VISIBLE
                tvEmpty?.visibility = View.GONE

                val palette = listOf("#00897B", "#1565C0", "#D85C42", "#388E3C", "#7B1FA2", "#F57C00")
                val items = bids.mapIndexed { idx, bid ->
                    val name = bid.bidderName.ifBlank { "Penawar" }
                    val initials = name.split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifEmpty { "GM" }
                    val colorHex = palette[idx % palette.size]
                    BidHistoryItem(
                        avatarText = initials,
                        avatarBgHex = colorHex,
                        userHandle = name,
                        timeAgo = formatRelativeTime(bid.timestamp),
                        amount = bid.amountFormatted,
                        isHighest = idx == 0
                    )
                }
                recycler.adapter = BidHistoryAdapter(requireContext(), items)
            }
        }

        return view
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diffMs = maxOf(0L, System.currentTimeMillis() - timestamp)
        val mins = diffMs / 60_000L
        val hours = diffMs / 3_600_000L
        val days = diffMs / 86_400_000L
        return when {
            mins < 1 -> "Baru saja"
            mins < 60 -> "$mins menit lalu"
            hours < 24 -> "$hours jam lalu"
            else -> "$days hari lalu"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bidsListenerRegistration?.remove()
        bidsListenerRegistration = null
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
            holder.tvAvatar.backgroundTintList = ColorStateList.valueOf(Color.parseColor(item.avatarBgHex))
            holder.tvUserHandle.text = item.userHandle
            holder.tvTime.text = item.timeAgo
            holder.tvAmount.text = item.amount
            holder.tvHighestTag.visibility = if (item.isHighest) View.VISIBLE else View.GONE
        }

        override fun getItemCount(): Int = bids.size
    }

    companion object {
        private const val ARG_AUCTION_ID = "arg_auction_id"

        fun newInstance(auctionId: String): BottomDialogBidHistory {
            return BottomDialogBidHistory().apply {
                arguments = Bundle().apply {
                    putString(ARG_AUCTION_ID, auctionId)
                }
            }
        }
    }
}
