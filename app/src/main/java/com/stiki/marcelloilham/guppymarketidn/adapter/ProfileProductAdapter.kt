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
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class ProfileProductAdapter(
    private val context: Context,
    private val products: List<FirestoreRepository.ProductModel>
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
        holder.imgThumb.setImageResource(if (item.imageRes != 0) item.imageRes else R.drawable.bg_image_grid)
        holder.tvTitle.text = item.title
        holder.tvStockInfo.text = "${item.unit} · Stok ${item.sold} terjual"
        holder.tvPrice.text = item.price

        holder.btnManage.setOnClickListener {
            Toast.makeText(context, "Membuka modul kelola stok untuk varian ${item.title}...", Toast.LENGTH_SHORT).show()
        }

        holder.btnDelete.setOnClickListener {
            Toast.makeText(context, "Hapus varian ${item.title} (segera hadir)", Toast.LENGTH_SHORT).show()
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("item_name", item.title)
                putExtra("item_price", item.price)
                putExtra("item_image", item.imageRes)
                putExtra("item_seller", item.seller)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = products.size
}
