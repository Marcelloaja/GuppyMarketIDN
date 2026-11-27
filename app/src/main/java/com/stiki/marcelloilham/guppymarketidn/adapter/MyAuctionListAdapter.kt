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
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader

class MyAuctionListAdapter(
    private val context: Context,
    private var rawItems: List<FirestoreRepository.AuctionModel>,
    private val onDeleteClicked: (FirestoreRepository.AuctionModel) -> Unit
) : RecyclerView.Adapter<MyAuctionListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgProfileAuctionThumb)
        val tvTitle: TextView = view.findViewById(R.id.tvProfileAuctionTitle)
        val tvBidInfo: TextView = view.findViewById(R.id.tvProfileAuctionBidInfo)
        val tvTime: TextView = view.findViewById(R.id.tvProfileAuctionTime)
        val btnManage: TextView = view.findViewById(R.id.btnManageProfileAuction)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteProfileAuction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_my_auction_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = rawItems[position]
        val safeImage = if (item.imageRes != 0) item.imageRes else R.drawable.bg_image_grid
        val firstPhoto = item.photoUris.firstOrNull()
        val firstPhotoData = item.photoDataUrls.firstOrNull()
        val loadedPhoto = SafeImageLoader.loadWithFallbackSafely(
            context, holder.imgThumb, firstPhoto, firstPhotoData, safeImage, 256
        )
        if (!loadedPhoto) {
            val loadedVideo = SafeImageLoader.loadVideoThumbnailSafely(
                context, holder.imgThumb, item.videoUri, safeImage
            )
            if (!loadedVideo && item.videoThumbDataUrl.isNotBlank()) {
                SafeImageLoader.loadUriSafely(context, holder.imgThumb, item.videoThumbDataUrl, safeImage, 256)
            }
        }
        holder.tvTitle.text = item.title
        holder.tvBidInfo.text = "Bid ${item.currentBid} · ${item.bidsCount} penawar"

        val remainingSecs = maxOf(0L, (item.endTimeMillis - System.currentTimeMillis()) / 1000L)
        val hours = remainingSecs / 3600
        val mins = (remainingSecs % 3600) / 60
        val secs = remainingSecs % 60
        holder.tvTime.text = if (remainingSecs > 0) String.format("Sisa %02d:%02d:%02d", hours, mins, secs) else "Selesai"

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailAuctionActivity::class.java).apply {
                putExtra("EXTRA_AUCTION_ID", item.id)
                putExtra("EXTRA_AUCTION_SELLER_ID", item.sellerId)
                putExtra("auction_title", item.title)
                putExtra("EXTRA_AUCTION_SELLER", item.seller)
                putExtra("EXTRA_AUCTION_LOCATION", item.location)
                putExtra("EXTRA_AUCTION_PRICE", item.currentBid)
                putExtra("EXTRA_AUCTION_PRICE_NUMERIC", item.currentBidNumeric)
                putExtra("EXTRA_AUCTION_BIDS_COUNT", item.bidsCount)
                putExtra("EXTRA_AUCTION_HIGHEST_BIDDER", item.highestBidderName)
                putExtra("EXTRA_AUCTION_INCREMENT", item.bidIncrementNumeric)
                putExtra("EXTRA_AUCTION_GRADE", item.grade)
                putExtra("EXTRA_AUCTION_IMAGE", item.imageRes)
                putExtra("EXTRA_AUCTION_VIDEO_URI", item.videoUri)
                putStringArrayListExtra("EXTRA_AUCTION_PHOTO_URIS", ArrayList(item.photoUris))
                putExtra("EXTRA_AUCTION_SELLER_AVATAR", item.sellerAvatarUri)
                putExtra("EXTRA_AUCTION_END_TIME_MILLIS", item.endTimeMillis)
                putExtra("EXTRA_AUCTION_DESCRIPTION", item.description)
                putExtra("EXTRA_AUCTION_STRAIN", item.strain)
                putExtra("EXTRA_AUCTION_GENDER", item.gender)
                putExtra("EXTRA_AUCTION_AGE", item.age)
                putExtra("EXTRA_AUCTION_QUANTITY", item.quantity)
                putExtra("EXTRA_AUCTION_CONDITION", item.condition)
                putExtra("EXTRA_AUCTION_QUARANTINE", item.quarantine)
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
