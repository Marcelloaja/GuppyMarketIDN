package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R

class AuctionGridAdapter(
    private val context: Context,
    private val items: List<AuctionItem>,
    private val onClick: (AuctionItem) -> Unit
) : RecyclerView.Adapter<AuctionGridAdapter.ViewHolder>() {

    data class AuctionItem(
        val id: Int,
        val title: String,
        val seller: String,
        val location: String,
        val price: String,
        val bids: Int,
        val timeLeft: String,
        val imageRes: Int,
        val grade: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_auction_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imgAuction.setImageResource(item.imageRes)
        holder.tvTitle.text = item.title
        holder.tvSeller.text = "${item.seller} · ${item.location}"
        holder.tvPrice.text = item.price
        holder.tvBids.text = "${item.bids} penawaran"
        holder.tvTime.text = item.timeLeft
        holder.tvGrade.text = item.grade

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

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
