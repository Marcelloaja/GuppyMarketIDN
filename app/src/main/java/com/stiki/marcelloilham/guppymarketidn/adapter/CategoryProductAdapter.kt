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

class CategoryProductAdapter(
    private val context: Context,
    private var productList: List<CategoryProduct>
) : RecyclerView.Adapter<CategoryProductAdapter.ViewHolder>() {

    data class CategoryProduct(
        val title: String,
        val seller: String,
        val price: String,
        val unit: String,
        val sold: String,
        val imageRes: Int,
        val strain: String = "Semua"
    )

    private var filteredList: List<CategoryProduct> = productList

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgCategoryProduct)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryProductName)
        val tvSeller: TextView = itemView.findViewById(R.id.tvCategoryProductSeller)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCategoryProductPrice)
        val tvUnit: TextView = itemView.findViewById(R.id.tvCategoryProductUnit)
        val tvSold: TextView = itemView.findViewById(R.id.tvCategoryProductSold)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_category_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = filteredList[position]
        holder.imgProduct.setImageResource(product.imageRes)
        holder.tvName.text = product.title
        holder.tvSeller.text = product.seller
        holder.tvPrice.text = product.price
        holder.tvUnit.text = product.unit
        holder.tvSold.text = "Terjual ${product.sold}"

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("ITEM_NAME", product.title)
                putExtra("ITEM_IMAGE", product.imageRes)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = filteredList.size

    fun filterByQueryAndStrain(query: String, strain: String) {
        filteredList = productList.filter { item ->
            val matchesQuery = query.isEmpty() ||
                item.title.lowercase().contains(query.lowercase()) ||
                item.seller.lowercase().contains(query.lowercase())

            val matchesStrain = strain.equals("Semua", ignoreCase = true) ||
                item.title.lowercase().contains(strain.lowercase()) ||
                item.strain.lowercase().contains(strain.lowercase())

            matchesQuery && matchesStrain
        }
        notifyDataSetChanged()
    }
}
