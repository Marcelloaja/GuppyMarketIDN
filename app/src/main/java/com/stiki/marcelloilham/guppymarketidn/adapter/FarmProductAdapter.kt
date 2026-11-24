package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R

class FarmProductAdapter(
    private val context: Context,
    private val items: List<FarmProduct>
) : RecyclerView.Adapter<FarmProductAdapter.ViewHolder>() {

    data class FarmProduct(
        val title: String,
        val grade: String,
        val price: String,
        val unit: String,
        val imageRes: Int
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_farm_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imgProduct.setImageResource(item.imageRes)
        holder.tvTitle.text = item.title
        holder.tvGrade.text = item.grade
        holder.tvPrice.text = item.price
        holder.tvUnit.text = item.unit
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
