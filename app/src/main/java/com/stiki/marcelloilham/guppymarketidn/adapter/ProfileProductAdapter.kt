package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader

class ProfileProductAdapter(
    private val context: Context,
    private val products: List<FirestoreRepository.ProductModel>,
    private val onManageClicked: (FirestoreRepository.ProductModel) -> Unit,
    private val onDeleteClicked: (FirestoreRepository.ProductModel) -> Unit
) : RecyclerView.Adapter<ProfileProductAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgThumb: ImageView = view.findViewById(R.id.imgProfileProductThumb)
        val tvTitle: TextView = view.findViewById(R.id.tvProfileProductTitle)
        val tvStockInfo: TextView = view.findViewById(R.id.tvProfileProductStockInfo)
        val tvPrice: TextView = view.findViewById(R.id.tvProfileProductPrice)
        val btnManage: TextView = view.findViewById(R.id.btnManageProfileProduct)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteProfileProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_profile_product_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = products[position]
        val fallback = if (item.imageRes != 0) item.imageRes else R.drawable.bg_image_grid
        SafeImageLoader.loadWithFallbackSafely(
            context,
            holder.imgThumb,
            item.photoUris.firstOrNull(),
            item.photoDataUrls.firstOrNull(),
            fallback,
            256
        )
        holder.tvTitle.text = item.title
        holder.tvStockInfo.text = "${item.unit} · Stok ${item.effectiveStock()} · Terjual ${item.soldCount}"
        holder.tvPrice.text = item.price

        holder.btnManage.setOnClickListener {
            onManageClicked(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClicked(item)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("EXTRA_PRODUCT_ID", item.id)
                putExtra("EXTRA_SELLER_ID", item.sellerId)
                putExtra("ITEM_NAME", item.title)
                putExtra("ITEM_IMAGE", fallback)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = products.size
}
