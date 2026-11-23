package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
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
            holder.cardCategory.setCardBackgroundColor(ContextCompat.getColor(context, R.color.guppy_primary))
            holder.cardCategory.strokeWidth = 0
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.tvName.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_bold)
            holder.imgCategory.strokeWidth = 0f
        } else {
            holder.cardCategory.setCardBackgroundColor(Color.parseColor("#F5F8F6"))
            holder.cardCategory.strokeColor = ContextCompat.getColor(context, R.color.guppy_stroke)
            holder.cardCategory.strokeWidth = (1f * density).toInt()
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))
            holder.tvName.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_semi_bold)
            holder.imgCategory.strokeWidth = 0f
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
        val cardCategory: MaterialCardView = itemView.findViewById(R.id.cardCategoryHome)
        val imgCategory: ShapeableImageView = itemView.findViewById(R.id.imgCategoryHome)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryNameHome)
    }
}
