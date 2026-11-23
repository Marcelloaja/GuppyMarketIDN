package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem

class CategoryAdapter(
    private val context: Context,
    private val categories: List<CategoryItem>,
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.category_grid_item, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        holder.tvName.text = item.name
        holder.tvCount.text = item.countText
        holder.imgCategory.setImageResource(item.imageRes)

        holder.itemView.setOnClickListener {
            onCategoryClick(item)
        }
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvCount: TextView = itemView.findViewById(R.id.tvCategoryCount)
    }
}