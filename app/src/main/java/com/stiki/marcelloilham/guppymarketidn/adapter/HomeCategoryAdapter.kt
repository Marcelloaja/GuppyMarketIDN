package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
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

    // Pastel colorful background palette matching the user's mockup design
    private val pastelColors = listOf(
        "#E8F5E9", // Mint Green (Semua)
        "#FCE4EC", // Soft Pink (Ikan Kontes)
        "#EDE7F6", // Soft Purple (Pakan Ikan)
        "#FFF8E1", // Soft Yellow (Obat & Vitamin)
        "#E3F2FD", // Soft Blue (Sol & Akrilik)
        "#FFE0B2", // Soft Orange (Indukan)
        "#E0F7FA", // Soft Cyan (Aksesoris)
        "#E8F5E9", // Soft Light Green (Tanaman Air)
        "#E8EAF6"  // Soft Indigo (Filter & Pompa)
    )

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
        val isSelected = (position == selectedPosition)

        holder.tvName.text = item.name
        holder.imgCategory.setImageResource(item.imageRes)

        // Pick pastel color based on position
        val colorHex = pastelColors[position % pastelColors.size]
        val parsedColor = Color.parseColor(colorHex)

        if (isSelected) {
            holder.containerBgCircle.background.setColorFilter(ContextCompat.getColor(context, R.color.guppy_primary), PorterDuff.Mode.SRC_IN)
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_primary))
            holder.tvName.typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter_bold)
        } else {
            holder.containerBgCircle.background.setColorFilter(parsedColor, PorterDuff.Mode.SRC_IN)
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))
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
        val containerBgCircle: FrameLayout = itemView.findViewById(R.id.containerBgCircle)
        val imgCategory: ShapeableImageView = itemView.findViewById(R.id.imgCategoryHome)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryNameHome)
    }
}
