package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
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
    private var shippingCost: Long = 18000L
    private val serviceFee: Long = 2000L

    private var selectedCourier: String = "JNT"
    private var costJNT: Long = 18000L
    private var costJNE: Long = 22000L
    private var costSicepat: Long = 20000L
    private var costInstant: Long = 35000L
    private var isPayVA: Boolean = true

    private lateinit var tvQuantity: TextView
    private lateinit var tvQuantitySubtext: TextView
    private lateinit var tvSummarySubtotal: TextView
    private lateinit var tvSummaryShipping: TextView
    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvBottomTotal: TextView
    private lateinit var tvBottomSubtext: TextView

    private lateinit var optionShipJNT: View
    private lateinit var optionShipJNE: View
    private lateinit var optionShipSicepat: View
    private lateinit var optionShipInstant: View

    private lateinit var icRadioShipJNT: ImageView
    private lateinit var icRadioShipJNE: ImageView
    private lateinit var icRadioShipSicepat: ImageView
    private lateinit var icRadioShipInstant: ImageView

    private lateinit var tvShipJNTTitle: TextView
    private lateinit var tvShipJNTEta: TextView
    private lateinit var tvShipJNTPrice: TextView
    private lateinit var tvShipJNETitle: TextView
    private lateinit var tvShipJNEEta: TextView
    private lateinit var tvShipJNEPrice: TextView
    private lateinit var tvShipSicepatTitle: TextView
    private lateinit var tvShipSicepatEta: TextView
    private lateinit var tvShipSicepatPrice: TextView
    private lateinit var tvShipInstantTitle: TextView
    private lateinit var tvShipInstantEta: TextView
    private lateinit var tvShipInstantPrice: TextView

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

        optionShipJNT = findViewById(R.id.optionShipJNT)
        optionShipJNE = findViewById(R.id.optionShipJNE)
        optionShipSicepat = findViewById(R.id.optionShipSicepat)
        optionShipInstant = findViewById(R.id.optionShipInstant)

        icRadioShipJNT = findViewById(R.id.icRadioShipJNT)
        icRadioShipJNE = findViewById(R.id.icRadioShipJNE)
        icRadioShipSicepat = findViewById(R.id.icRadioShipSicepat)
        icRadioShipInstant = findViewById(R.id.icRadioShipInstant)

        tvShipJNTTitle = findViewById(R.id.tvShipJNTTitle)
        tvShipJNTEta = findViewById(R.id.tvShipJNTEta)
        tvShipJNTPrice = findViewById(R.id.tvShipJNTPrice)
        tvShipJNETitle = findViewById(R.id.tvShipJNETitle)
        tvShipJNEEta = findViewById(R.id.tvShipJNEEta)
        tvShipJNEPrice = findViewById(R.id.tvShipJNEPrice)
        tvShipSicepatTitle = findViewById(R.id.tvShipSicepatTitle)
        tvShipSicepatEta = findViewById(R.id.tvShipSicepatEta)
        tvShipSicepatPrice = findViewById(R.id.tvShipSicepatPrice)
        tvShipInstantTitle = findViewById(R.id.tvShipInstantTitle)
        tvShipInstantEta = findViewById(R.id.tvShipInstantEta)
        tvShipInstantPrice = findViewById(R.id.tvShipInstantPrice)

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
        optionShipJNT.setOnClickListener {
            selectedCourier = "JNT"
            shippingCost = costJNT
            updateShippingUI()
            updateCalculations()
        }

        optionShipJNE.setOnClickListener {
            selectedCourier = "JNE"
            shippingCost = costJNE
            updateShippingUI()
            updateCalculations()
        }

        optionShipSicepat.setOnClickListener {
            selectedCourier = "SICEPAT"
            shippingCost = costSicepat
            updateShippingUI()
            updateCalculations()
        }

        optionShipInstant.setOnClickListener {
            selectedCourier = "INSTANT"
            shippingCost = costInstant
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

        val options = listOf(
            Triple(optionShipJNT, icRadioShipJNT, Pair(tvShipJNTTitle, tvShipJNTPrice)),
            Triple(optionShipJNE, icRadioShipJNE, Pair(tvShipJNETitle, tvShipJNEPrice)),
            Triple(optionShipSicepat, icRadioShipSicepat, Pair(tvShipSicepatTitle, tvShipSicepatPrice)),
            Triple(optionShipInstant, icRadioShipInstant, Pair(tvShipInstantTitle, tvShipInstantPrice))
        )

        val keys = listOf("JNT", "JNE", "SICEPAT", "INSTANT")

        options.forEachIndexed { idx, (container, radio, titles) ->
            if (keys[idx] == selectedCourier) {
                container.setBackgroundResource(bgSelected)
                radio.setImageResource(R.drawable.ic_radio_selected)
                titles.first.setTextColor(primaryColor)
                titles.second.setTextColor(primaryColor)
            } else {
                container.setBackgroundResource(bgUnselected)
                radio.setImageResource(R.drawable.ic_radio_unselected)
                titles.first.setTextColor(blackColor)
                titles.second.setTextColor(grayColor)
            }
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
            val courierText = when (selectedCourier) {
                "JNT" -> "J&T Express EZ"
                "JNE" -> "JNE Express YES"
                "SICEPAT" -> "SiCepat BEST"
                else -> "GoSend / GrabExpress Instan"
            }
            val paymentText = if (isPayVA) "Bank BCA Virtual Account" else "GoPay / E-wallet"
            val orderNum = "#GM-${(10000..99999).random()}"

            val order = FirestoreRepository.OrderModel(
                id = orderNum,
                productName = "Red Dragon Delta",
                sellerName = "Bekasi Aqua Farm",
                quantity = quantity,
                totalAmount = total,
                shippingCost = shippingCost,
                courier = courierText,
                paymentMethod = paymentText,
                status = "Menunggu Pembayaran"
            )

            btnPay.isEnabled = false
            btnPay.text = "Memproses..."

            FirestoreRepository.createOrder(order) { success ->
                btnPay.isEnabled = true
                btnPay.text = "Bayar sekarang"

                val intent = Intent(this, PaymentPendingActivity::class.java).apply {
                    putExtra("EXTRA_ORDER_ID", orderNum)
                    putExtra("EXTRA_PRODUCT_NAME", "Red Dragon Delta")
                    putExtra("EXTRA_QUANTITY", quantity)
                    putExtra("EXTRA_TOTAL_AMOUNT", total)
                    putExtra("EXTRA_SHIPPING_COST", shippingCost)
                    putExtra("EXTRA_SERVICE_FEE", serviceFee)
                    putExtra("EXTRA_PAYMENT_METHOD", paymentText)
                }
                startActivity(intent)
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
                    val jntOption = pricings?.firstOrNull { it.courierCode.contains("jnt", true) || it.courierName.contains("j&t", true) }
                    val jneOption = pricings?.firstOrNull { it.courierCode.contains("jne", true) || it.courierName.contains("jne", true) }
                    val sicepatOption = pricings?.firstOrNull { it.courierCode.contains("sicepat", true) || it.courierName.contains("sicepat", true) }
                    val instantOption = pricings?.firstOrNull { it.type?.contains("instant", true) == true || it.courierCode.contains("gojek", true) || it.courierCode.contains("grab", true) }

                    runOnUiThread {
                        if (jntOption != null && jntOption.price > 0) {
                            costJNT = jntOption.price
                            tvShipJNTPrice.text = formatCurrency(costJNT)
                            if (!jntOption.duration.isNullOrEmpty()) {
                                tvShipJNTEta.text = "Estimasi ${jntOption.duration} (Kemasan Oksigen)"
                            }
                        }
                        if (jneOption != null && jneOption.price > 0) {
                            costJNE = jneOption.price
                            tvShipJNEPrice.text = formatCurrency(costJNE)
                            if (!jneOption.duration.isNullOrEmpty()) {
                                tvShipJNEEta.text = "Estimasi ${jneOption.duration}"
                            }
                        }
                        if (sicepatOption != null && sicepatOption.price > 0) {
                            costSicepat = sicepatOption.price
                            tvShipSicepatPrice.text = formatCurrency(costSicepat)
                            if (!sicepatOption.duration.isNullOrEmpty()) {
                                tvShipSicepatEta.text = "Estimasi ${sicepatOption.duration}"
                            }
                        }
                        if (instantOption != null && instantOption.price > 0) {
                            costInstant = instantOption.price
                            tvShipInstantPrice.text = formatCurrency(costInstant)
                            if (!instantOption.duration.isNullOrEmpty()) {
                                tvShipInstantEta.text = "Estimasi tiba ${instantOption.duration}"
                            }
                        }

                        shippingCost = when (selectedCourier) {
                            "JNT" -> costJNT
                            "JNE" -> costJNE
                            "SICEPAT" -> costSicepat
                            else -> costInstant
                        }
                        updateCalculations()
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
