package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuHome.Item
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader

class GridAdapter(
    private val context: Context,
    private var items: List<Item>
) : RecyclerView.Adapter<GridAdapter.GridViewHolder>() {

    private var listener: OnItemClickListener? = null
    private var originalItems: List<Item> = items

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    fun updateItems(newItems: List<Item>) {
        originalItems = newItems
        items = newItems
        notifyDataSetChanged()
    }

    fun filterItems(filter: String) {
        items = if (filter == "All") {
            originalItems
        } else {
            originalItems.filter {
                it.type.equals(filter, ignoreCase = true) ||
                    it.strain.contains(filter, ignoreCase = true) ||
                    it.name.contains(filter, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    fun searchItems(query: String) {
        val cleanQuery = query.trim().lowercase()
        items = if (cleanQuery.isEmpty()) {
            originalItems
        } else {
            originalItems.filter {
                it.name.lowercase().contains(cleanQuery) ||
                    it.type.lowercase().contains(cleanQuery) ||
                    it.seller.lowercase().contains(cleanQuery) ||
                    it.strain.lowercase().contains(cleanQuery) ||
                    it.location.lowercase().contains(cleanQuery) ||
                    it.price.lowercase().contains(cleanQuery)
            }
        }
        notifyDataSetChanged()
    }

    private fun parsePrice(item: Item): Long {
        if (item.priceNumeric > 0L) return item.priceNumeric
        return item.price.replace("Rp.", "").replace("Rp", "").replace(".", "").replace(" ", "").trim().toLongOrNull() ?: 0L
    }

    fun applyAdvancedFilter(sort: String, grade: String, location: String, minPrice: Float, maxPrice: Float) {
        var filtered = originalItems.filter { item ->
            val matchesGrade = grade.equals("All", ignoreCase = true) ||
                item.name.contains(grade, true) ||
                item.strain.contains(grade, true) ||
                item.type.contains(grade, true)

            val matchesLocation = location.equals("All", ignoreCase = true) ||
                item.location.contains(location, true) ||
                item.seller.contains(location, true) ||
                item.name.contains(location, true)

            val numericPrice = parsePrice(item).toFloat()
            val matchesPrice = numericPrice == 0f || (numericPrice in minPrice..maxPrice)

            matchesGrade && matchesLocation && matchesPrice
        }

        filtered = when (sort) {
            "LowToHigh" -> filtered.sortedBy { parsePrice(it) }
            "HighToLow" -> filtered.sortedByDescending { parsePrice(it) }
            else -> filtered
        }

        items = filtered
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Item {
        return items[position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val item = items[position]
        holder.nameProductGrid.text = item.name
        holder.showTypeGrid.text = if (item.seller.isNotBlank()) {
            "${item.type} · ${item.seller}"
        } else {
            item.type
        }
        holder.showPriceGrid.text = item.price
        holder.tvUnitGrid.text = item.unit.ifBlank { "per pasang" }
        val fallback = if (item.image != 0) item.image else R.drawable.bg_image_grid
        SafeImageLoader.loadWithFallbackSafely(
            context,
            holder.itemImage,
            item.imageUrl,
            item.photoDataUrl,
            fallback,
            360
        )

        holder.buttonViewDetail.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                listener?.onItemClick(pos)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImage: ImageView = itemView.findViewById(R.id.item_image)
        val nameProductGrid: TextView = itemView.findViewById(R.id.nameProductGrid)
        val showTypeGrid: TextView = itemView.findViewById(R.id.showTypeGrid)
        val showPriceGrid: TextView = itemView.findViewById(R.id.showPriceGrid)
        val tvUnitGrid: TextView = itemView.findViewById(R.id.tvUnitGrid)
        val buttonViewDetail: View = itemView.findViewById(R.id.button_view_detail)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(pos)
                }
            }
        }
    }
}
