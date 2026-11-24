package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuAuction.DetailAuctionActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class ProfileAuctionAdapter(
    private val context: Context,
    private var rawItems: List<FirestoreRepository.AuctionModel>,
    private val onDeleteClicked: (FirestoreRepository.AuctionModel) -> Unit
) : RecyclerView.Adapter<ProfileAuctionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgProfileAuctionThumb)
        val tvTitle: TextView = view.findViewById(R.id.tvProfileAuctionTitle)
        val tvBidInfo: TextView = view.findViewById(R.id.tvProfileAuctionBidInfo)
        val tvTime: TextView = view.findViewById(R.id.tvProfileAuctionTime)
        val btnManage: TextView = view.findViewById(R.id.btnManageProfileAuction)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteProfileAuction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_profile_auction_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = rawItems[position]
        holder.imgThumb.setImageResource(if (item.imageRes != 0) item.imageRes else R.drawable.bg_image_grid)
        holder.tvTitle.text = item.title
        holder.tvBidInfo.text = "Bid ${item.currentBid} · ${item.bidsCount} penawar"

        val remainingSecs = maxOf(0L, (item.endTimeMillis - System.currentTimeMillis()) / 1000L)
        val hours = remainingSecs / 3600
        val mins = (remainingSecs % 3600) / 60
        val secs = remainingSecs % 60
        holder.tvTime.text = if (remainingSecs > 0) String.format("Sisa %02d:%02d:%02d", hours, mins, secs) else "Selesai"

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailAuctionActivity::class.java).apply {
                putExtra("auction_title", item.title)
                putExtra("EXTRA_AUCTION_SELLER", item.seller)
                putExtra("EXTRA_AUCTION_LOCATION", item.location)
                putExtra("EXTRA_AUCTION_PRICE", item.currentBid)
                putExtra("EXTRA_AUCTION_GRADE", item.grade)
                putExtra("EXTRA_AUCTION_IMAGE", item.imageRes)
                putExtra("EXTRA_AUCTION_END_TIME_MILLIS", item.endTimeMillis)
                putExtra("EXTRA_AUCTION_DESCRIPTION", item.description)
            }
            context.startActivity(intent)
        }

        holder.btnManage.setOnClickListener {
            Toast.makeText(context, "Membuka modul kelola lelang untuk ${item.title}...", Toast.LENGTH_SHORT).show()
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClicked(item)
        }
    }

    override fun getItemCount(): Int = rawItems.size

    fun updateData(newList: List<FirestoreRepository.AuctionModel>) {
        rawItems = newList
        notifyDataSetChanged()
    }
}
