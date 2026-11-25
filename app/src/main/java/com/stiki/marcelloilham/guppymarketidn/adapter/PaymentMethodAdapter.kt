package com.stiki.marcelloilham.guppymarketidn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class PaymentMethodAdapter(
    private val context: Context,
    private var methodList: List<FirestoreRepository.PaymentMethodModel>,
    private val onItemClicked: (FirestoreRepository.PaymentMethodModel) -> Unit,
    private val onSetMainClicked: (FirestoreRepository.PaymentMethodModel) -> Unit,
    private val onDeleteClicked: (FirestoreRepository.PaymentMethodModel) -> Unit
) : RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val containerCard: View = view
        val imgTypeIcon: ImageView = view.findViewById(R.id.imgPaymentTypeIcon)
        val tvProviderName: TextView = view.findViewById(R.id.tvPaymentProviderName)
        val tvMainBadge: TextView = view.findViewById(R.id.tvPaymentMainBadge)
        val tvAccDetails: TextView = view.findViewById(R.id.tvPaymentAccDetails)
        val btnSetMain: TextView = view.findViewById(R.id.btnSetMainPaymentItem)
        val btnDelete: TextView = view.findViewById(R.id.btnDeletePaymentItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_payment_method_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = methodList[position]
        holder.tvProviderName.text = item.providerName

        if (item.type.contains("QRIS", true) || item.providerName.contains("QRIS", true)) {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_qris)
        } else if (item.type.contains("E-Wallet", true) || item.providerName.contains("GoPay", true) || item.providerName.contains("OVO", true) || item.providerName.contains("DANA", true)) {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_ewallet)
        } else {
            holder.imgTypeIcon.setImageResource(R.drawable.ic_payment_card)
        }

        val last4 = if (item.accountNumber.length >= 4) item.accountNumber.takeLast(4) else item.accountNumber
        val ownerSuffix = if (item.accountOwner.isNotEmpty()) " (${item.accountOwner})" else ""
        holder.tvAccDetails.text = "•••• $last4$ownerSuffix"

        if (item.isMain) {
            holder.tvMainBadge.visibility = View.VISIBLE
            holder.btnSetMain.visibility = View.GONE
        } else {
            holder.tvMainBadge.visibility = View.GONE
            holder.btnSetMain.visibility = View.VISIBLE
        }

        holder.containerCard.setOnClickListener { onItemClicked(item) }
        holder.btnSetMain.setOnClickListener { onSetMainClicked(item) }
        holder.btnDelete.setOnClickListener { onDeleteClicked(item) }
    }

    override fun getItemCount(): Int = methodList.size

    fun updateData(newList: List<FirestoreRepository.PaymentMethodModel>) {
        methodList = newList
        notifyDataSetChanged()
    }
}
