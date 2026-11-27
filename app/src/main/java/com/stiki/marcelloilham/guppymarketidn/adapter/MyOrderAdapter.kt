package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import java.text.NumberFormat
import java.util.Locale

class MyOrderAdapter(
    private val context: Context,
    private val orders: List<FirestoreRepository.OrderModel>,
    private val onOrderActionClicked: (FirestoreRepository.OrderModel) -> Unit
) : RecyclerView.Adapter<MyOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderNumber: TextView = view.findViewById(R.id.tvMyOrderNumber)
        val tvDate: TextView = view.findViewById(R.id.tvMyOrderDate)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvMyOrderStatusBadge)

        val tvProductName: TextView = view.findViewById(R.id.tvMyOrderProductName)
        val tvProductVariant: TextView = view.findViewById(R.id.tvMyOrderProductVariant)
        val tvSellerName: TextView = view.findViewById(R.id.tvMyOrderSellerName)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvMyOrderTotalPrice)

        val ivBottomIcon: ImageView = view.findViewById(R.id.ivMyOrderBottomIcon)
        val tvCourierResi: TextView = view.findViewById(R.id.tvMyOrderCourierResi)
        val btnAction: TextView = view.findViewById(R.id.btnMyOrderAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_my_order_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = orders[position]

        holder.tvOrderNumber.text = item.id
        holder.tvProductName.text = item.productName
        val unitShort = item.unit.removePrefix("per ").trim().ifBlank { "pasang" }
        holder.tvProductVariant.text = "${item.quantity} $unitShort"
        holder.tvSellerName.text = "Penjual: ${item.sellerName.ifEmpty { "Penjual Guppy" }}"

        val formattedPrice = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(item.totalAmount).replace(",00", "").replace("Rp", "Rp ")
        holder.tvTotalPrice.text = formattedPrice

        val statusUpper = item.status.uppercase()

        when {
            statusUpper.contains("DIKIRIM") -> {
                holder.tvStatusBadge.text = "Dikirim"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0F2F1"))
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.guppy_primary))

                holder.tvCourierResi.text = "${item.courier} - ${item.trackingNumber.ifEmpty { "JP8841" }}"
                holder.btnAction.text = "Lacak"
                holder.btnAction.setBackgroundResource(R.drawable.bg_btn_outline)
                holder.btnAction.setTextColor(android.graphics.Color.parseColor("#124332"))
            }
            statusUpper.contains("SIAP") -> {
                holder.tvStatusBadge.text = "Siap Kirim"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F8F9FA"))
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_gray))

                holder.tvCourierResi.text = "Menunggu input resi..."
                holder.btnAction.text = "Tandai Dikirim"
                holder.btnAction.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            statusUpper.contains("SELESAI") -> {
                holder.tvStatusBadge.text = "Selesai"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F8F9FA"))
                holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_gray))

                holder.tvCourierResi.text = "✓ Pesanan selesai diterima"
                holder.btnAction.text = "Lihat Detail"
                holder.btnAction.setBackgroundResource(R.drawable.bg_btn_solid_gray)
                holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))
            }
            else -> {
                holder.tvStatusBadge.text = "Perlu Dikemas"
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE"))
                holder.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#E53935"))

                holder.tvCourierResi.text = "Pesanan sedang disiapkan penjual"
                holder.btnAction.text = "Cetak Label"
                holder.btnAction.setBackgroundResource(R.drawable.bg_btn_outline)
                holder.btnAction.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))
            }
        }

        holder.btnAction.setOnClickListener { onOrderActionClicked(item) }
        holder.itemView.setOnClickListener { onOrderActionClicked(item) }
    }

    override fun getItemCount(): Int = orders.size
}
