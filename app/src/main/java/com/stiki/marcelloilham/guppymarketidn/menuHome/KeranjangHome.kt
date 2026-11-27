package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import com.google.android.material.imageview.ShapeableImageView

class KeranjangHome : AppCompatActivity() {

    private var cartItems: MutableList<FirestoreRepository.CartItemModel> = mutableListOf()
    private var qty1: Int = 1
    private var price1: Long = 0L
    private var hasItem1: Boolean = false

    private var qty2: Int = 1
    private var price2: Long = 0L
    private var hasItem2: Boolean = false

    private lateinit var cbSelectAll: CheckBox
    private lateinit var cbItem1: CheckBox
    private lateinit var cbItem2: CheckBox

    private lateinit var tvSelectedCount: TextView
    private lateinit var tvQty1: TextView
    private lateinit var tvQty2: TextView

    private lateinit var layoutItem1: View
    private lateinit var dividerItem1: View
    private lateinit var layoutItem2: View

    private lateinit var cardSelectAll: View
    private lateinit var cardFarmContainer: View
    private lateinit var cardOxygenNote: View
    private lateinit var cardSummary: View
    private lateinit var bottomCheckoutBar: View
    private lateinit var layoutEmptyCart: View

    private lateinit var tvSubtotalItemLabel: TextView
    private lateinit var tvSubtotalAmount: TextView
    private lateinit var tvTotalSummaryAmount: TextView
    private lateinit var tvTotalHarga: TextView
    private lateinit var tvTotalSubtext: TextView

    private lateinit var tvCartSellerInitials: TextView
    private lateinit var tvCartSellerName: TextView
    private lateinit var tvCartSellerLocation: TextView

    private lateinit var ivCartItem1: ShapeableImageView
    private lateinit var tvCartTitle1: TextView
    private lateinit var tvCartMeta1: TextView
    private lateinit var tvCartBadge1: TextView
    private lateinit var tvCartPrice1: TextView
    private lateinit var tvCartUnit1: TextView
    private lateinit var tvCartEquiv1: TextView

    private lateinit var ivCartItem2: ShapeableImageView
    private lateinit var tvCartTitle2: TextView
    private lateinit var tvCartMeta2: TextView
    private lateinit var tvCartBadge2: TextView
    private lateinit var tvCartPrice2: TextView
    private lateinit var tvCartUnit2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keranjang)

        val topBar = findViewById<View>(R.id.topBarKeranjang)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackKeranjang)
        btnBack?.setOnClickListener { finish() }

        initViews()
        setupCheckboxes()
        setupSteppers()
        setupDeleteButtons()
        setupCheckoutButton()

        updateCalculations()
        loadRealCartFromFirestore()
    }

    override fun onResume() {
        super.onResume()
        loadRealCartFromFirestore()
    }

    private fun loadRealCartFromFirestore() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            cartItems.clear()
            hasItem1 = false
            hasItem2 = false
            updateCalculations()
            return
        }
        FirestoreRepository.fetchCartItems(uid) { list ->
            if (isFinishing || isDestroyed) return@fetchCartItems
            cartItems = list.toMutableList()
            hasItem1 = cartItems.isNotEmpty()
            hasItem2 = cartItems.size >= 2
            if (hasItem1) {
                val item1 = cartItems[0]
                qty1 = item1.quantity.coerceAtLeast(1)
                price1 = item1.unitPrice
                val sellerDisplay = item1.sellerName.ifBlank { "Peternak" }
                tvCartSellerName.text = sellerDisplay
                tvCartSellerInitials.text = buildInitials(sellerDisplay)
                tvCartSellerLocation.text = item1.sellerLocation.ifBlank { "Indonesia" }
                bindCartSlot(
                    item1,
                    ivCartItem1,
                    tvCartTitle1,
                    tvCartMeta1,
                    tvCartBadge1,
                    tvCartPrice1,
                    tvCartUnit1
                )
                layoutItem1.visibility = View.VISIBLE
            } else {
                layoutItem1.visibility = View.GONE
            }
            if (hasItem2) {
                val item2 = cartItems[1]
                qty2 = item2.quantity.coerceAtLeast(1)
                price2 = item2.unitPrice
                bindCartSlot(
                    item2,
                    ivCartItem2,
                    tvCartTitle2,
                    tvCartMeta2,
                    tvCartBadge2,
                    tvCartPrice2,
                    tvCartUnit2
                )
                layoutItem2.visibility = View.VISIBLE
                dividerItem1.visibility = View.VISIBLE
            } else {
                layoutItem2.visibility = View.GONE
                dividerItem1.visibility = View.GONE
            }
            updateCalculations()
        }
    }

    private fun bindCartSlot(
        item: FirestoreRepository.CartItemModel,
        ivPhoto: ShapeableImageView,
        tvTitle: TextView,
        tvMeta: TextView,
        tvBadge: TextView,
        tvPrice: TextView,
        tvUnit: TextView
    ) {
        SafeImageLoader.loadWithFallbackSafely(
            this,
            ivPhoto,
            item.imageUrl,
            item.imageDataUrl,
            item.imageRes
        )
        tvTitle.text = item.title.ifBlank { "Produk" }
        val metaParts = listOf(item.category, item.strain).filter { it.isNotBlank() && it != "Semua" }
        tvMeta.text = if (metaParts.isNotEmpty()) metaParts.joinToString(" · ") else item.sellerName.ifBlank { "Guppy" }
        tvBadge.text = if (item.stock > 0) "STOK ${item.stock}" else "PRE-ORDER"
        tvPrice.text = formatCurrency(item.unitPrice)
        val normalizedUnit = item.unit.removePrefix("per ").trim().ifBlank { "pasang" }
        tvUnit.text = "/ $normalizedUnit"
    }

    private fun buildInitials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "GM"
        }
    }

    private fun initViews() {
        cbSelectAll = findViewById(R.id.cbSelectAll)
        cbItem1 = findViewById(R.id.cbItem1)
        cbItem2 = findViewById(R.id.cbItem2)

        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        tvQty1 = findViewById(R.id.tvQty1)
        tvQty2 = findViewById(R.id.tvQty2)

        layoutItem1 = findViewById(R.id.layoutItem1)
        dividerItem1 = findViewById(R.id.dividerItem1)
        layoutItem2 = findViewById(R.id.layoutItem2)

        cardSelectAll = findViewById(R.id.cardSelectAll)
        cardFarmContainer = findViewById(R.id.cardFarmContainer)
        cardOxygenNote = findViewById(R.id.cardOxygenNote)
        cardSummary = findViewById(R.id.cardSummary)
        bottomCheckoutBar = findViewById(R.id.bottomCheckoutBar)
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart)

        tvSubtotalItemLabel = findViewById(R.id.tvSubtotalItemLabel)
        tvSubtotalAmount = findViewById(R.id.tvSubtotalAmount)
        tvTotalSummaryAmount = findViewById(R.id.tvTotalSummaryAmount)
        tvTotalHarga = findViewById(R.id.tvTotalHarga)
        tvTotalSubtext = findViewById(R.id.tvTotalSubtext)

        tvCartSellerInitials = findViewById(R.id.tvCartSellerInitials)
        tvCartSellerName = findViewById(R.id.tvCartSellerName)
        tvCartSellerLocation = findViewById(R.id.tvCartSellerLocation)

        ivCartItem1 = findViewById(R.id.ivCartItem1)
        tvCartTitle1 = findViewById(R.id.tvCartTitle1)
        tvCartMeta1 = findViewById(R.id.tvCartMeta1)
        tvCartBadge1 = findViewById(R.id.tvCartBadge1)
        tvCartPrice1 = findViewById(R.id.tvCartPrice1)
        tvCartUnit1 = findViewById(R.id.tvCartUnit1)
        tvCartEquiv1 = findViewById(R.id.tvCartEquiv1)

        ivCartItem2 = findViewById(R.id.ivCartItem2)
        tvCartTitle2 = findViewById(R.id.tvCartTitle2)
        tvCartMeta2 = findViewById(R.id.tvCartMeta2)
        tvCartBadge2 = findViewById(R.id.tvCartBadge2)
        tvCartPrice2 = findViewById(R.id.tvCartPrice2)
        tvCartUnit2 = findViewById(R.id.tvCartUnit2)
    }

    private fun setupCheckboxes() {
        cbSelectAll.setOnClickListener {
            val isChecked = cbSelectAll.isChecked
            if (hasItem1) cbItem1.isChecked = isChecked
            if (hasItem2) cbItem2.isChecked = isChecked
            updateCalculations()
        }

        cbItem1.setOnClickListener { updateCalculations() }
        cbItem2.setOnClickListener { updateCalculations() }
    }

    private fun setupSteppers() {
        val btnMin1 = findViewById<ImageView>(R.id.btnMin1)
        val btnPlus1 = findViewById<ImageView>(R.id.btnPlus1)
        val btnMin2 = findViewById<ImageView>(R.id.btnMin2)
        val btnPlus2 = findViewById<ImageView>(R.id.btnPlus2)

        btnMin1?.setOnClickListener {
            if (qty1 > 1) {
                qty1--
                syncCartQuantityToFirestore(0, qty1)
                updateCalculations()
            }
        }

        btnPlus1?.setOnClickListener {
            val maxStock = cartItems.getOrNull(0)?.stock ?: Int.MAX_VALUE
            if (maxStock <= 0 || qty1 < maxStock) {
                qty1++
                syncCartQuantityToFirestore(0, qty1)
                updateCalculations()
            } else {
                Toast.makeText(this, "Stok maksimal tersedia $maxStock", Toast.LENGTH_SHORT).show()
            }
        }

        btnMin2?.setOnClickListener {
            if (qty2 > 1) {
                qty2--
                syncCartQuantityToFirestore(1, qty2)
                updateCalculations()
            }
        }

        btnPlus2?.setOnClickListener {
            val maxStock = cartItems.getOrNull(1)?.stock ?: Int.MAX_VALUE
            if (maxStock <= 0 || qty2 < maxStock) {
                qty2++
                syncCartQuantityToFirestore(1, qty2)
                updateCalculations()
            } else {
                Toast.makeText(this, "Stok maksimal tersedia $maxStock", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncCartQuantityToFirestore(index: Int, newQty: Int) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val item = cartItems.getOrNull(index) ?: return
        cartItems[index] = item.copy(quantity = newQty)
        if (uid.isNotBlank() && item.id.isNotBlank()) {
            FirestoreRepository.updateCartItemQuantity(uid, item.id, newQty)
        }
    }

    private fun setupDeleteButtons() {
        val btnDelete1 = findViewById<ImageView>(R.id.btnDelete1)
        val btnDelete2 = findViewById<ImageView>(R.id.btnDelete2)
        val btnDeleteSelected = findViewById<TextView>(R.id.btnDeleteSelected)

        // Single Item 1 Delete
        btnDelete1?.setOnClickListener {
            val itemName = cartItems.getOrNull(0)?.title ?: "item ini"
            showSingleDeleteDialog(itemName) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                cartItems.getOrNull(0)?.id?.let { id ->
                    if (uid.isNotBlank()) FirestoreRepository.deleteCartItems(uid, listOf(id)) {}
                }
                hasItem1 = false
                layoutItem1.visibility = View.GONE
                dividerItem1.visibility = View.GONE
                updateCalculations()
            }
        }

        // Single Item 2 Delete
        btnDelete2?.setOnClickListener {
            val itemName = cartItems.getOrNull(1)?.title ?: "item ini"
            showSingleDeleteDialog(itemName) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                cartItems.getOrNull(1)?.id?.let { id ->
                    if (uid.isNotBlank()) FirestoreRepository.deleteCartItems(uid, listOf(id)) {}
                }
                hasItem2 = false
                layoutItem2.visibility = View.GONE
                dividerItem1.visibility = View.GONE
                updateCalculations()
            }
        }

        // Batch Selected Delete
        btnDeleteSelected?.setOnClickListener {
            val itemsToDelete = mutableListOf<String>()
            val idsToDelete = mutableListOf<String>()
            if (hasItem1 && cbItem1.isChecked) {
                cartItems.getOrNull(0)?.let {
                    itemsToDelete.add(it.title)
                    idsToDelete.add(it.id)
                }
            }
            if (hasItem2 && cbItem2.isChecked) {
                cartItems.getOrNull(1)?.let {
                    itemsToDelete.add(it.title)
                    idsToDelete.add(it.id)
                }
            }

            if (itemsToDelete.isEmpty()) {
                Toast.makeText(this, "Pilih setidaknya 1 item untuk dihapus", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showBatchDeleteDialog(itemsToDelete) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                if (uid.isNotBlank() && idsToDelete.isNotEmpty()) {
                    FirestoreRepository.deleteCartItems(uid, idsToDelete) {}
                }
                if (hasItem1 && cbItem1.isChecked) {
                    hasItem1 = false
                    layoutItem1.visibility = View.GONE
                }
                if (hasItem2 && cbItem2.isChecked) {
                    hasItem2 = false
                    layoutItem2.visibility = View.GONE
                }
                dividerItem1.visibility = View.GONE
                updateCalculations()
            }
        }
    }

    private fun showSingleDeleteDialog(itemName: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Item")
            .setMessage("Apakah Anda yakin ingin menghapus '$itemName' dari keranjang?")
            .setPositiveButton("Hapus") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showBatchDeleteDialog(items: List<String>, onConfirm: () -> Unit) {
        val message = if (items.size == 1) {
            "Apakah Anda yakin ingin menghapus '${items[0]}' dari keranjang?"
        } else {
            "Apakah Anda yakin ingin menghapus ${items.size} item yang dipilih dari keranjang?"
        }

        AlertDialog.Builder(this)
            .setTitle("Hapus Item Dipilih")
            .setMessage(message)
            .setPositiveButton("Hapus") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updateCalculations() {
        if (!hasItem1 && !hasItem2) {
            // All items deleted -> Show Empty Cart State
            cardSelectAll.visibility = View.GONE
            cardFarmContainer.visibility = View.GONE
            cardOxygenNote.visibility = View.GONE
            cardSummary.visibility = View.GONE
            bottomCheckoutBar.visibility = View.GONE
            layoutEmptyCart.visibility = View.VISIBLE
            return
        }

        layoutEmptyCart.visibility = View.GONE
        cardSelectAll.visibility = View.VISIBLE
        cardFarmContainer.visibility = View.VISIBLE
        cardOxygenNote.visibility = View.VISIBLE
        cardSummary.visibility = View.VISIBLE
        bottomCheckoutBar.visibility = View.VISIBLE

        var selectedItemCount = 0
        var totalAmount: Long = 0L

        if (hasItem1 && cbItem1.isChecked) {
            selectedItemCount += 1
            totalAmount += (price1 * qty1)
        }

        if (hasItem2 && cbItem2.isChecked) {
            selectedItemCount += 1
            totalAmount += (price2 * qty2)
        }

        tvQty1.text = qty1.toString()
        tvQty2.text = qty2.toString()

        val unit1 = cartItems.getOrNull(0)?.unit?.removePrefix("per ")?.trim().orEmpty().ifBlank { "pasang" }
        val unit2 = cartItems.getOrNull(1)?.unit?.removePrefix("per ")?.trim().orEmpty().ifBlank { "pasang" }
        tvCartEquiv1.text = "$qty1 $unit1"

        tvSelectedCount.text = "$selectedItemCount item dipilih"

        val activeCount = (if (hasItem1) 1 else 0) + (if (hasItem2) 1 else 0)
        val selectedActiveCount = (if (hasItem1 && cbItem1.isChecked) 1 else 0) + (if (hasItem2 && cbItem2.isChecked) 1 else 0)

        cbSelectAll.isChecked = (activeCount > 0 && selectedActiveCount == activeCount)

        val totalQty = (if (hasItem1 && cbItem1.isChecked) qty1 else 0) + (if (hasItem2 && cbItem2.isChecked) qty2 else 0)
        val totalEkor = (if (hasItem1 && cbItem1.isChecked) qty1 * fishCountPerUnit(unit1) else 0) +
            (if (hasItem2 && cbItem2.isChecked) qty2 * fishCountPerUnit(unit2) else 0)

        tvSubtotalItemLabel.text = "Subtotal ($selectedItemCount item)"
        tvSubtotalAmount.text = formatCurrency(totalAmount)
        tvTotalSummaryAmount.text = formatCurrency(totalAmount)

        tvTotalHarga.text = formatCurrency(totalAmount)
        tvTotalSubtext.text = "$totalQty unit · $totalEkor ekor"
    }

    private fun fishCountPerUnit(unitLabel: String): Int {
        val lower = unitLabel.lowercase()
        return when {
            lower.contains("trio") -> 3
            lower.contains("pasang") -> 2
            else -> 1
        }
    }

    private fun setupCheckoutButton() {
        val btnCheckout = findViewById<TextView>(R.id.btnCheckout)
        btnCheckout?.setOnClickListener {
            val selectedCart = when {
                hasItem1 && cbItem1.isChecked -> cartItems.getOrNull(0)?.copy(quantity = qty1)
                hasItem2 && cbItem2.isChecked -> cartItems.getOrNull(1)?.copy(quantity = qty2)
                else -> null
            }
            if (selectedCart == null) {
                Toast.makeText(this, "Pilih minimal 1 produk untuk checkout.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putExtra("EXTRA_PRODUCT_ID", selectedCart.productId)
                putExtra("EXTRA_SELLER_ID", selectedCart.sellerId)
                putExtra("EXTRA_PRODUCT_NAME", selectedCart.title)
                putExtra("EXTRA_UNIT_PRICE", selectedCart.unitPrice)
                putExtra("EXTRA_UNIT_LABEL", selectedCart.unit)
                putExtra("EXTRA_QUANTITY", selectedCart.quantity)
                putExtra("EXTRA_STOCK", selectedCart.stock)
                putExtra("EXTRA_CATEGORY", selectedCart.category)
                putExtra("EXTRA_STRAIN", selectedCart.strain)
                putExtra("EXTRA_IMAGE_URL", selectedCart.imageUrl)
                putExtra("EXTRA_IMAGE_DATA_URL", selectedCart.imageDataUrl)
                putExtra("EXTRA_IMAGE_RES", selectedCart.imageRes)
                putExtra("EXTRA_SELLER_NAME", selectedCart.sellerName)
                putExtra("EXTRA_SELLER_LOCATION", selectedCart.sellerLocation)
                putStringArrayListExtra("EXTRA_CART_ITEM_IDS", arrayListOf(selectedCart.id))
            }
            startActivity(intent)
        }
    }

    private fun formatCurrency(amount: Long): String {
        return "Rp " + String.format("%,d", amount).replace(',', '.')
    }
}
