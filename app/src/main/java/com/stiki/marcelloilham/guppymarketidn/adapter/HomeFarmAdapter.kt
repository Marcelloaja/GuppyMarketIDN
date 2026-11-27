package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class HomeFarmAdapter(
    private val context: Context,
    private val farms: List<FirestoreRepository.FarmModel>,
    private val onVisitClicked: (FirestoreRepository.FarmModel) -> Unit,
    private val onRouteClicked: ((FirestoreRepository.FarmModel) -> Unit)? = null
) : RecyclerView.Adapter<HomeFarmAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitials: TextView = view.findViewById(R.id.tvFarmInitials)
        val tvName: TextView = view.findViewById(R.id.tvHomeFarmName)
        val tvDistance: TextView = view.findViewById(R.id.tvHomeFarmDistance)
        val tvRating: TextView = view.findViewById(R.id.tvHomeFarmRating)
        val tvVariants: TextView = view.findViewById(R.id.tvHomeFarmVariants)
        val btnVisit: TextView = view.findViewById(R.id.btnHomeFarmVisit)
        val btnRoute: TextView? = view.findViewById(R.id.btnHomeFarmRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_home_farm_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = farms[position]

        val initials = item.farmName.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()

        holder.tvInitials.text = initials.ifEmpty { "GF" }
        holder.tvName.text = item.farmName
        holder.tvDistance.text = item.distanceKm
        holder.tvRating.text = String.format(java.util.Locale.US, "%.1f", item.rating)
        holder.tvVariants.text = "${item.variantsCount} varian"

        holder.btnVisit.setOnClickListener { onVisitClicked(item) }
        if (onRouteClicked != null) {
            holder.btnRoute?.visibility = View.VISIBLE
            holder.btnRoute?.setOnClickListener { onRouteClicked.invoke(item) }
        } else {
            holder.btnRoute?.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { onVisitClicked(item) }
    }

    override fun getItemCount(): Int = farms.size
}
