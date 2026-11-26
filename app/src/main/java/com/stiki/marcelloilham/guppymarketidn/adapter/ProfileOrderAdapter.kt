package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class ProfileOrderAdapter(
    private val context: Context,
    private val orders: List<FirestoreRepository.OrderModel>,
    private val onOrderClicked: (FirestoreRepository.OrderModel) -> Unit = {}
) : RecyclerView.Adapter<ProfileOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvProductName: TextView = view.findViewById(R.id.tvOrderProductName)
        val tvOrderInfo: TextView = view.findViewById(R.id.tvOrderInfoSub)
        val tvStatus: TextView = view.findViewById(R.id.tvOrderStatus)
        val btnTrack: TextView = view.findViewById(R.id.btnTrackOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_profile_order_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = orders[position]
        holder.tvProductName.text = item.productName
        val formattedPrice = java.text.NumberFormat.getInstance(java.util.Locale("in", "ID")).format(item.totalAmount)
        holder.tvOrderInfo.text = "${item.quantity} pasang · Rp $formattedPrice"
        holder.tvStatus.text = "${item.status} · ${item.trackingNumber}"

        holder.itemView.setOnClickListener { onOrderClicked(item) }
        holder.btnTrack.setOnClickListener { onOrderClicked(item) }
    }

    override fun getItemCount(): Int = orders.size
}
