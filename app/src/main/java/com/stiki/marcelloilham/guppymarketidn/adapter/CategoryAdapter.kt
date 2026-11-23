package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem

class CategoryAdapter(
    private val context: Context,
    private val categories: List<CategoryItem>,
    private val onCategoryClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = -1

    fun setSelectedCategoryName(name: String) {
        val index = categories.indexOfFirst {
            if (name.isEmpty() || name.equals("Semua", ignoreCase = true)) {
                it.name.equals("Semua", ignoreCase = true)
            } else {
                it.name.equals(name, ignoreCase = true)
            }
        }
        if (index != -1) {
            selectedPosition = index
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.category_grid_item, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = categories[position]
        val density = context.resources.displayMetrics.density

        holder.tvName.text = item.name
        holder.tvCount.text = item.countText
        holder.imgCategory.setImageResource(item.imageRes)

        val isSelected = (position == selectedPosition)

        if (isSelected) {
            holder.cardCategory.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            holder.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.guppy_primary)
            holder.cardCategory.strokeWidth = (2 * density).toInt()
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_primary))
        } else {
            holder.cardCategory.setCardBackgroundColor(Color.parseColor("#F8FAF9"))
            holder.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.guppy_stroke)
            holder.cardCategory.strokeWidth = (1 * density).toInt()
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))
        }

        holder.itemView.setOnClickListener {
            val clickAnim = AnimationUtils.loadAnimation(context, R.anim.button_click)
            holder.itemView.startAnimation(clickAnim)

            val previousSelected = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (previousSelected != -1) notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)

            holder.itemView.postDelayed({
                onCategoryClick(item)
            }, 150)
        }
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardCategory: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvCount: TextView = itemView.findViewById(R.id.tvCategoryCount)
    }
}
