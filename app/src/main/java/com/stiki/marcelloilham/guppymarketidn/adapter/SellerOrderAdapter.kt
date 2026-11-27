package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import java.text.NumberFormat
import java.util.Locale

class SellerOrderAdapter(
    private val context: Context,
    private val orders: List<FirestoreRepository.OrderModel>,
    private val onMarkShippedClicked: (FirestoreRepository.OrderModel, String) -> Unit,
    private val onPrintLabelClicked: (FirestoreRepository.OrderModel) -> Unit
) : RecyclerView.Adapter<SellerOrderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderNumber: TextView = view.findViewById(R.id.tvSellerOrderNumber)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvSellerStatusBadge)
        val tvDeadline: TextView = view.findViewById(R.id.tvSellerDeadline)
        val tvBuyerName: TextView = view.findViewById(R.id.tvSellerBuyerName)
        val tvBuyerCity: TextView = view.findViewById(R.id.tvSellerBuyerCity)
        val tvProductName: TextView = view.findViewById(R.id.tvSellerProductName)
        val tvProductVariant: TextView = view.findViewById(R.id.tvSellerProductVariant)
        val tvTotalPrice: TextView = view.findViewById(R.id.tvSellerTotalPrice)

        val layoutActionForm: View = view.findViewById(R.id.layoutSellerActionForm)
        val chipJne: TextView = view.findViewById(R.id.chipJne)
        val chipJnt: TextView = view.findViewById(R.id.chipJnt)
        val chipSicepat: TextView = view.findViewById(R.id.chipSicepat)
        val etResiInput: EditText = view.findViewById(R.id.etSellerResiInput)
        val btnPrintLabel: TextView = view.findViewById(R.id.btnSellerPrintLabel)
        val btnMarkShipped: TextView = view.findViewById(R.id.btnSellerMarkShipped)

        val layoutShippedBox: View = view.findViewById(R.id.layoutSellerShippedBox)
        val tvShippedCourierResi: TextView = view.findViewById(R.id.tvSellerShippedCourierResi)
        val btnViewResi: TextView = view.findViewById(R.id.btnSellerViewResi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_seller_order_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = orders[position]

        holder.tvOrderNumber.text = item.id
        holder.tvBuyerName.text = item.recipientName.ifBlank { "Pembeli" }
        holder.tvBuyerCity.text = item.shippingAddress.takeIf { it.isNotBlank() }?.let {
            if (it.contains(",")) it.split(",").takeLast(2).joinToString(",").trim() else it
        } ?: "-"
        holder.tvProductName.text = item.productName
        val unitShort = item.unit.removePrefix("per ").trim().ifBlank { "pasang" }
        holder.tvProductVariant.text = "${item.quantity} $unitShort"

        val formattedPrice = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(item.totalAmount).replace(",00", "").replace("Rp", "Rp ")
        holder.tvTotalPrice.text = formattedPrice

        val statusUpper = item.status.uppercase()

        if (statusUpper.contains("DIKIRIM") || statusUpper.contains("SELESAI")) {
            holder.tvStatusBadge.text = if (statusUpper.contains("SELESAI")) "Selesai" else "Dikirim"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
            holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0"))
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))

            holder.layoutActionForm.visibility = View.GONE
            holder.layoutShippedBox.visibility = View.VISIBLE
            holder.tvShippedCourierResi.text = "${item.courier} · ${item.trackingNumber.ifEmpty { "JT0012398765" }}"
        } else {
            holder.tvStatusBadge.text = if (statusUpper.contains("SIAP")) "Siap Kirim" else "Perlu Dikemas"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
            holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF3E0"))
            holder.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#E65100"))

            holder.layoutActionForm.visibility = View.VISIBLE
            holder.layoutShippedBox.visibility = View.GONE

            if (item.trackingNumber.isNotEmpty()) {
                holder.etResiInput.setText(item.trackingNumber)
            }
        }

        holder.btnPrintLabel.setOnClickListener { onPrintLabelClicked(item) }

        holder.btnMarkShipped.setOnClickListener {
            val enteredResi = holder.etResiInput.text.toString().trim()
            if (enteredResi.isEmpty()) {
                holder.etResiInput.error = "Masukkan nomor resi terlebih dahulu"
                Toast.makeText(context, "Harap isi nomor resi pengiriman!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onMarkShippedClicked(item, enteredResi)
        }

        holder.btnViewResi.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Resi", item.trackingNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Nomor resi ${item.trackingNumber} disalin!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = orders.size
}
