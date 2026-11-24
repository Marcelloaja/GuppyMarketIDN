package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.PricingOption
import com.stiki.marcelloilham.guppymarketidn.api.RateItem
import com.stiki.marcelloilham.guppymarketidn.api.RatesRequest
import com.stiki.marcelloilham.guppymarketidn.api.RatesResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutActivity : AppCompatActivity() {

    private var quantity: Int = 2
    private val unitPrice: Long = 275000L
    private var shippingCost: Long = 25000L
    private val serviceFee: Long = 2000L
    private var isInstantShipping: Boolean = true
    private var isPayVA: Boolean = true

    private lateinit var tvQuantity: TextView
    private lateinit var tvQuantitySubtext: TextView
    private lateinit var tvSummarySubtotal: TextView
    private lateinit var tvSummaryShipping: TextView
    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvBottomTotal: TextView
    private lateinit var tvBottomSubtext: TextView

    private lateinit var optionShipInstant: View
    private lateinit var optionShipRegular: View
    private lateinit var icRadioShipInstant: ImageView
    private lateinit var icRadioShipRegular: ImageView
    private lateinit var tvShipInstantTitle: TextView
    private lateinit var tvShipInstantPrice: TextView
    private lateinit var tvShipRegularTitle: TextView
    private lateinit var tvShipRegularPrice: TextView

    private lateinit var optionPayVA: View
    private lateinit var optionPayEWallet: View
    private lateinit var icRadioPayVA: ImageView
    private lateinit var icRadioPayEWallet: ImageView
    private lateinit var tvPayVATitle: TextView
    private lateinit var tvPayEWalletTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        val topBar = findViewById<View>(R.id.topBarCheckout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackCheckout)
        btnBack.setOnClickListener { finish() }

        initViews()
        setupStepper()
        setupShippingOptions()
        setupPaymentOptions()
        setupPayButton()

        // Fetch real-time Biteship shipping rates in background
        fetchBiteshipShippingRates()

        updateCalculations()
    }

    private fun initViews() {
        tvQuantity = findViewById(R.id.tvCheckoutQuantity)
        tvQuantitySubtext = findViewById(R.id.tvCheckoutQuantitySubtext)
        tvSummarySubtotal = findViewById(R.id.tvSummarySubtotal)
        tvSummaryShipping = findViewById(R.id.tvSummaryShipping)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        tvBottomTotal = findViewById(R.id.tvBottomTotal)
        tvBottomSubtext = findViewById(R.id.tvBottomSubtext)

        optionShipInstant = findViewById(R.id.optionShipInstant)
        optionShipRegular = findViewById(R.id.optionShipRegular)
        icRadioShipInstant = findViewById(R.id.icRadioShipInstant)
        icRadioShipRegular = findViewById(R.id.icRadioShipRegular)
        tvShipInstantTitle = findViewById(R.id.tvShipInstantTitle)
        tvShipInstantPrice = findViewById(R.id.tvShipInstantPrice)
        tvShipRegularTitle = findViewById(R.id.tvShipRegularTitle)
        tvShipRegularPrice = findViewById(R.id.tvShipRegularPrice)

        optionPayVA = findViewById(R.id.optionPayVA)
        optionPayEWallet = findViewById(R.id.optionPayEWallet)
        icRadioPayVA = findViewById(R.id.icRadioPayVA)
        icRadioPayEWallet = findViewById(R.id.icRadioPayEWallet)
        tvPayVATitle = findViewById(R.id.tvPayVATitle)
        tvPayEWalletTitle = findViewById(R.id.tvPayEWalletTitle)
    }

    private fun setupStepper() {
        val btnMin = findViewById<ImageView>(R.id.btnCheckoutMin)
        val btnPlus = findViewById<ImageView>(R.id.btnCheckoutPlus)

        btnMin.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateCalculations()
            }
        }

        btnPlus.setOnClickListener {
            quantity++
            updateCalculations()
        }
    }

    private fun setupShippingOptions() {
        optionShipInstant.setOnClickListener {
            isInstantShipping = true
            shippingCost = 25000L
            updateShippingUI()
            updateCalculations()
        }

        optionShipRegular.setOnClickListener {
            isInstantShipping = false
            shippingCost = 15000L
            updateShippingUI()
            updateCalculations()
        }
    }

    private fun updateShippingUI() {
        val bgSelected = R.drawable.bg_variant_card_selected
        val bgUnselected = R.drawable.bg_variant_card_unselected
        val primaryColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val blackColor = ContextCompat.getColor(this, R.color.guppy_text_black)
        val grayColor = ContextCompat.getColor(this, R.color.guppy_text_gray)

        if (isInstantShipping) {
            optionShipInstant.setBackgroundResource(bgSelected)
            icRadioShipInstant.setImageResource(R.drawable.ic_radio_selected)
            tvShipInstantTitle.setTextColor(primaryColor)
            tvShipInstantPrice.setTextColor(primaryColor)

            optionShipRegular.setBackgroundResource(bgUnselected)
            icRadioShipRegular.setImageResource(R.drawable.ic_radio_unselected)
            tvShipRegularTitle.setTextColor(blackColor)
            tvShipRegularPrice.setTextColor(grayColor)
        } else {
            optionShipInstant.setBackgroundResource(bgUnselected)
            icRadioShipInstant.setImageResource(R.drawable.ic_radio_unselected)
            tvShipInstantTitle.setTextColor(blackColor)
            tvShipInstantPrice.setTextColor(grayColor)

            optionShipRegular.setBackgroundResource(bgSelected)
            icRadioShipRegular.setImageResource(R.drawable.ic_radio_selected)
            tvShipRegularTitle.setTextColor(primaryColor)
            tvShipRegularPrice.setTextColor(primaryColor)
        }
    }

    private fun setupPaymentOptions() {
        optionPayVA.setOnClickListener {
            isPayVA = true
            updatePaymentUI()
        }

        optionPayEWallet.setOnClickListener {
            isPayVA = false
            updatePaymentUI()
        }
    }

    private fun updatePaymentUI() {
        val bgSelected = R.drawable.bg_variant_card_selected
        val bgUnselected = R.drawable.bg_variant_card_unselected
        val primaryColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val blackColor = ContextCompat.getColor(this, R.color.guppy_text_black)

        if (isPayVA) {
            optionPayVA.setBackgroundResource(bgSelected)
            icRadioPayVA.setImageResource(R.drawable.ic_radio_selected)
            tvPayVATitle.setTextColor(primaryColor)

            optionPayEWallet.setBackgroundResource(bgUnselected)
            icRadioPayEWallet.setImageResource(R.drawable.ic_radio_unselected)
            tvPayEWalletTitle.setTextColor(blackColor)
        } else {
            optionPayVA.setBackgroundResource(bgUnselected)
            icRadioPayVA.setImageResource(R.drawable.ic_radio_unselected)
            tvPayVATitle.setTextColor(blackColor)

            optionPayEWallet.setBackgroundResource(bgSelected)
            icRadioPayEWallet.setImageResource(R.drawable.ic_radio_selected)
            tvPayEWalletTitle.setTextColor(primaryColor)
        }
    }

    private fun updateCalculations() {
        val subtotal = unitPrice * quantity
        val total = subtotal + shippingCost + serviceFee
        val totalFishCount = quantity * 2

        tvQuantity.text = quantity.toString()
        tvQuantitySubtext.text = "$quantity pasang · Setara $totalFishCount ekor"

        tvSummarySubtotal.text = formatCurrency(subtotal)
        tvSummaryShipping.text = formatCurrency(shippingCost)
        tvSummaryTotal.text = formatCurrency(total)

        tvBottomTotal.text = formatCurrency(total)
        tvBottomSubtext.text = "$quantity pasang · $totalFishCount ekor"
    }

    private fun setupPayButton() {
        val btnPay = findViewById<TextView>(R.id.btnPayNow)
        btnPay.setOnClickListener {
            val total = (unitPrice * quantity) + shippingCost + serviceFee
            val courierText = if (isInstantShipping) "Kurir Instan" else "Reguler"
            val paymentText = if (isPayVA) "Transfer Bank (VA)" else "E-wallet"

            val order = FirestoreRepository.OrderModel(
                productName = "Red Dragon Delta",
                sellerName = "Bekasi Aqua Farm",
                quantity = quantity,
                totalAmount = total,
                shippingCost = shippingCost,
                courier = courierText,
                paymentMethod = paymentText,
                status = "Dikirim"
            )

            btnPay.isEnabled = false
            btnPay.text = "Memproses..."

            FirestoreRepository.createOrder(order) { success ->
                btnPay.isEnabled = true
                btnPay.text = "Bayar sekarang"

                Toast.makeText(
                    this,
                    "Pembayaran ${formatCurrency(total)} Berhasil! Pesanan Dicatat ke Sistem.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    private fun fetchBiteshipShippingRates() {
        val request = RatesRequest(
            originLatitude = -6.2383,
            originLongitude = 106.9896, // Bekasi
            destinationLatitude = -6.2921,
            destinationLongitude = 106.8060, // Jakarta
            couriers = "jnt,jne,sicepat,grab,gojek",
            items = listOf(
                RateItem("Red Dragon Delta Guppy", unitPrice * quantity, 500 * quantity)
            )
        )

        BiteshipClient.instance.getShippingRates(
            authorization = BiteshipClient.API_KEY,
            request = request
        ).enqueue(object : Callback<RatesResponse> {
            override fun onResponse(call: Call<RatesResponse>, response: Response<RatesResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val pricings = response.body()?.pricing
                    val instantOption = pricings?.firstOrNull { it.serviceTypeMatches("instant") || it.type?.contains("instant", true) == true }
                    val regularOption = pricings?.firstOrNull { it.serviceTypeMatches("reguler") || it.type?.contains("reguler", true) == true }

                    runOnUiThread {
                        if (instantOption != null && instantOption.price > 0) {
                            tvShipInstantPrice.text = formatCurrency(instantOption.price)
                        }
                        if (regularOption != null && regularOption.price > 0) {
                            tvShipRegularPrice.text = formatCurrency(regularOption.price)
                        }
                    }
                    Log.d("BiteshipCheckout", "Biteship rates loaded: ${pricings?.size} options")
                } else {
                    Log.d("BiteshipCheckout", "Rates response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<RatesResponse>, t: Throwable) {
                Log.e("BiteshipCheckout", "Rates call failed: ${t.message}")
            }
        })
    }

    private fun PricingOption.serviceTypeMatches(type: String): Boolean {
        return courierServiceName.contains(type, ignoreCase = true) ||
               courierServiceCode.contains(type, ignoreCase = true)
    }

    private fun formatCurrency(amount: Long): String {
        return "Rp " + String.format("%,d", amount).replace(',', '.')
    }
}
