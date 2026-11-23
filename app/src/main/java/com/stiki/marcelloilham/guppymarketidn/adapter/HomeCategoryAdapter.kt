package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem

class HomeCategoryAdapter(
    private val context: Context,
    private val categories: List<CategoryItem>,
    private val onCategoryClick: (CategoryItem, Int) -> Unit
) : RecyclerView.Adapter<HomeCategoryAdapter.HomeCategoryViewHolder>() {

    private var selectedPosition = 0

    fun setSelectedPosition(position: Int) {
        val previous = selectedPosition
        selectedPosition = position
        if (previous in categories.indices) notifyItemChanged(previous)
        if (selectedPosition in categories.indices) notifyItemChanged(selectedPosition)
    }

    fun setSelectedCategoryName(name: String) {
        val index = categories.indexOfFirst {
            if (name.isEmpty() || name.equals("Semua", ignoreCase = true)) {
                it.name.equals("Semua", ignoreCase = true)
            } else {
                it.name.equals(name, ignoreCase = true)
            }
        }
        if (index != -1) {
            setSelectedPosition(index)
        } else {
            setSelectedPosition(0)
        }
    }

    fun getSelectedPosition(): Int = selectedPosition

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeCategoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.category_horizontal_item, parent, false)
        return HomeCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeCategoryViewHolder, position: Int) {
        val item = categories[position]
        val density = context.resources.displayMetrics.density

        holder.tvName.text = item.name
        holder.imgCategory.setImageResource(item.imageRes)

        val isSelected = (position == selectedPosition)

        if (isSelected) {
            holder.imgCategory.strokeColor = ContextCompat.getColorStateList(context, R.color.guppy_primary)
            holder.imgCategory.strokeWidth = 2.5f * density
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_primary))
            holder.tvName.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_bold)
        } else {
            holder.imgCategory.strokeColor = ContextCompat.getColorStateList(context, R.color.guppy_stroke)
            holder.imgCategory.strokeWidth = 1f * density
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_gray))
            holder.tvName.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_medium)
        }

        holder.itemView.setOnClickListener {
            val clickAnim = AnimationUtils.loadAnimation(context, R.anim.button_click)
            holder.itemView.startAnimation(clickAnim)

            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION) {
                setSelectedPosition(currentPos)
                onCategoryClick(item, currentPos)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    class HomeCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgCategory: ShapeableImageView = itemView.findViewById(R.id.imgCategoryHome)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryNameHome)
    }
}