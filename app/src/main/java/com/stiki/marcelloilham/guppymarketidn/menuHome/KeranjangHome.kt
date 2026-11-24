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
import com.stiki.marcelloilham.guppymarketidn.R

class KeranjangHome : AppCompatActivity() {

    private var qty1: Int = 2
    private val price1: Long = 275000L
    private var hasItem1: Boolean = true

    private var qty2: Int = 1
    private val price2: Long = 137500L
    private var hasItem2: Boolean = true

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
                updateCalculations()
            }
        }

        btnPlus1?.setOnClickListener {
            qty1++
            updateCalculations()
        }

        btnMin2?.setOnClickListener {
            if (qty2 > 1) {
                qty2--
                updateCalculations()
            }
        }

        btnPlus2?.setOnClickListener {
            qty2++
            updateCalculations()
        }
    }

    private fun setupDeleteButtons() {
        val btnDelete1 = findViewById<ImageView>(R.id.btnDelete1)
        val btnDelete2 = findViewById<ImageView>(R.id.btnDelete2)
        val btnDeleteSelected = findViewById<TextView>(R.id.btnDeleteSelected)

        // Single Item 1 Delete
        btnDelete1?.setOnClickListener {
            showSingleDeleteDialog("Red Dragon Delta") {
                hasItem1 = false
                layoutItem1.visibility = View.GONE
                dividerItem1.visibility = View.GONE
                updateCalculations()
            }
        }

        // Single Item 2 Delete
        btnDelete2?.setOnClickListener {
            showSingleDeleteDialog("Half Moon Plakat") {
                hasItem2 = false
                layoutItem2.visibility = View.GONE
                dividerItem1.visibility = View.GONE
                updateCalculations()
            }
        }

        // Batch Selected Delete
        btnDeleteSelected?.setOnClickListener {
            val itemsToDelete = mutableListOf<String>()
            if (hasItem1 && cbItem1.isChecked) itemsToDelete.add("Red Dragon Delta")
            if (hasItem2 && cbItem2.isChecked) itemsToDelete.add("Half Moon Plakat")

            if (itemsToDelete.isEmpty()) {
                Toast.makeText(this, "Pilih setidaknya 1 item untuk dihapus", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showBatchDeleteDialog(itemsToDelete) {
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

        tvSelectedCount.text = "$selectedItemCount item dipilih"

        val activeCount = (if (hasItem1) 1 else 0) + (if (hasItem2) 1 else 0)
        val selectedActiveCount = (if (hasItem1 && cbItem1.isChecked) 1 else 0) + (if (hasItem2 && cbItem2.isChecked) 1 else 0)

        cbSelectAll.isChecked = (activeCount > 0 && selectedActiveCount == activeCount)

        val totalPairs = (if (hasItem1 && cbItem1.isChecked) qty1 else 0) + (if (hasItem2 && cbItem2.isChecked) qty2 else 0)
        val totalEkor = (if (hasItem1 && cbItem1.isChecked) qty1 * 2 else 0) + (if (hasItem2 && cbItem2.isChecked) qty2 else 0)

        tvSubtotalItemLabel.text = "Subtotal ($selectedItemCount item)"
        tvSubtotalAmount.text = formatCurrency(totalAmount)
        tvTotalSummaryAmount.text = formatCurrency(totalAmount)

        tvTotalHarga.text = formatCurrency(totalAmount)
        tvTotalSubtext.text = "$totalPairs pasang · $totalEkor ekor"
    }

    private fun setupCheckoutButton() {
        val btnCheckout = findViewById<TextView>(R.id.btnCheckout)
        btnCheckout?.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun formatCurrency(amount: Long): String {
        return "Rp " + String.format("%,d", amount).replace(',', '.')
    }
}
