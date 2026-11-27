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
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader

class FarmProductAdapter(
    private val context: Context,
    private val items: List<FarmProduct>
) : RecyclerView.Adapter<FarmProductAdapter.ViewHolder>() {

    data class FarmProduct(
        val id: String = "",
        val sellerId: String = "",
        val title: String,
        val grade: String,
        val price: String,
        val unit: String,
        val imageRes: Int,
        val imageUrl: String = "",
        val photoDataUrl: String = ""
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_farm_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val fallback = if (item.imageRes != 0) item.imageRes else R.drawable.bg_image_grid
        SafeImageLoader.loadWithFallbackSafely(
            context,
            holder.imgProduct,
            item.imageUrl,
            item.photoDataUrl,
            fallback,
            320
        )
        holder.tvTitle.text = item.title
        holder.tvGrade.text = item.grade
        holder.tvPrice.text = item.price
        holder.tvUnit.text = item.unit

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

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProduct: ImageView = view.findViewById(R.id.imgProduct)
        val tvTitle: TextView = view.findViewById(R.id.tvProductName)
        val tvGrade: TextView = view.findViewById(R.id.tvProductGrade)
        val tvPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val tvUnit: TextView = view.findViewById(R.id.tvProductUnit)
    }
}
