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

class ShippingAddressAdapter(
    private val context: Context,
    private var addressList: List<FirestoreRepository.AddressModel>,
    private val onSetDefaultClicked: (FirestoreRepository.AddressModel) -> Unit,
    private val onEditClicked: (FirestoreRepository.AddressModel) -> Unit,
    private val onDeleteClicked: (FirestoreRepository.AddressModel) -> Unit
) : RecyclerView.Adapter<ShippingAddressAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgTypeIcon: ImageView = view.findViewById(R.id.imgAddressTypeIcon)
        val tvLabel: TextView = view.findViewById(R.id.tvAddressLabel)
        val tvDefaultBadge: TextView = view.findViewById(R.id.tvDefaultBadge)
        val tvRecipientName: TextView = view.findViewById(R.id.tvRecipientName)
        val tvRecipientPhone: TextView = view.findViewById(R.id.tvRecipientPhone)
        val tvFullAddress: TextView = view.findViewById(R.id.tvFullAddress)
        val btnSetDefault: TextView = view.findViewById(R.id.btnSetDefaultAddress)
        val btnEdit: TextView = view.findViewById(R.id.btnEditAddress)
        val btnDelete: TextView = view.findViewById(R.id.btnDeleteAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_shipping_address_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = addressList[position]
        holder.tvLabel.text = item.label
        holder.tvRecipientName.text = item.recipientName
        holder.tvRecipientPhone.text = item.phone
        holder.tvFullAddress.text = item.fullAddress

        val primaryColor = androidx.core.content.ContextCompat.getColor(context, R.color.guppy_primary)
        val grayColor = androidx.core.content.ContextCompat.getColor(context, R.color.guppy_text_gray)

        if (item.isDefault) {
            holder.tvDefaultBadge.visibility = View.VISIBLE
            holder.btnSetDefault.visibility = View.GONE
            holder.btnDelete.visibility = View.VISIBLE

            holder.imgTypeIcon.setBackgroundResource(R.drawable.bg_circle_image)
            holder.imgTypeIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0F2F1"))
            holder.imgTypeIcon.setImageResource(R.drawable.ic_map_folded)
            holder.imgTypeIcon.imageTintList = android.content.res.ColorStateList.valueOf(primaryColor)
        } else {
            holder.tvDefaultBadge.visibility = View.GONE
            holder.btnSetDefault.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.GONE

            holder.imgTypeIcon.setBackgroundResource(R.drawable.bg_circle_image)
            holder.imgTypeIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F5F5F5"))
            holder.imgTypeIcon.setImageResource(R.drawable.ic_map_folded)
            holder.imgTypeIcon.imageTintList = android.content.res.ColorStateList.valueOf(grayColor)
        }

        holder.btnSetDefault.setOnClickListener { onSetDefaultClicked(item) }
        holder.btnEdit.setOnClickListener { onEditClicked(item) }
        holder.btnDelete.setOnClickListener { onDeleteClicked(item) }
    }

    override fun getItemCount(): Int = addressList.size

    fun updateData(newList: List<FirestoreRepository.AddressModel>) {
        addressList = newList
        notifyDataSetChanged()
    }

    fun updateDataWithMoveAnimation(newList: List<FirestoreRepository.AddressModel>, movedFromPos: Int) {
        addressList = newList
        if (movedFromPos > 0) {
            notifyItemMoved(movedFromPos, 0)
            notifyItemRangeChanged(0, newList.size)
        } else {
            notifyDataSetChanged()
        }
    }
}
