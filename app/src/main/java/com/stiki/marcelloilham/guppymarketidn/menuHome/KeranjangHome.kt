package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.stiki.marcelloilham.guppymarketidn.R

class KeranjangHome : AppCompatActivity() {

    private var qty1: Int = 2
    private val price1: Long = 275000L

    private var qty2: Int = 1
    private val price2: Long = 137500L

    private lateinit var cbSelectAll: CheckBox
    private lateinit var cbItem1: CheckBox
    private lateinit var cbItem2: CheckBox

    private lateinit var tvSelectedCount: TextView
    private lateinit var tvQty1: TextView
    private lateinit var tvQty2: TextView

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

        tvSubtotalItemLabel = findViewById(R.id.tvSubtotalItemLabel)
        tvSubtotalAmount = findViewById(R.id.tvSubtotalAmount)
        tvTotalSummaryAmount = findViewById(R.id.tvTotalSummaryAmount)
        tvTotalHarga = findViewById(R.id.tvTotalHarga)
        tvTotalSubtext = findViewById(R.id.tvTotalSubtext)
    }

    private fun setupCheckboxes() {
        cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            cbItem1.isChecked = isChecked
            cbItem2.isChecked = isChecked
            updateCalculations()
        }

        cbItem1.setOnCheckedChangeListener { _, _ -> updateCalculations() }
        cbItem2.setOnCheckedChangeListener { _, _ -> updateCalculations() }
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

    private fun updateCalculations() {
        var selectedItemCount = 0
        var totalAmount: Long = 0L

        if (cbItem1.isChecked) {
            selectedItemCount += 1
            totalAmount += (price1 * qty1)
        }

        if (cbItem2.isChecked) {
            selectedItemCount += 1
            totalAmount += (price2 * qty2)
        }

        tvQty1.text = qty1.toString()
        tvQty2.text = qty2.toString()

        tvSelectedCount.text = "$selectedItemCount item dipilih"
        cbSelectAll.isChecked = (cbItem1.isChecked && cbItem2.isChecked)

        val totalPairs = (if (cbItem1.isChecked) qty1 else 0) + (if (cbItem2.isChecked) qty2 else 0)
        val totalEkor = (if (cbItem1.isChecked) qty1 * 2 else 0) + (if (cbItem2.isChecked) qty2 else 0)

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
