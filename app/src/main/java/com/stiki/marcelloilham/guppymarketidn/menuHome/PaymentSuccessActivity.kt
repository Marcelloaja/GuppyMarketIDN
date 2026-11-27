package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.imageview.ShapeableImageView
import com.stiki.marcelloilham.guppymarketidn.MainActivity
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    private lateinit var tvOrderNumber: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvBankBadge: TextView
    private lateinit var tvPaymentTime: TextView
    private lateinit var tvTotalPaid: TextView

    private lateinit var tvSellerName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvCourier: TextView
    private lateinit var tvTrackingNumber: TextView
    private lateinit var tvEta: TextView

    private lateinit var ivProductImage: ShapeableImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductSub: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvShipping: TextView
    private lateinit var tvServiceFee: TextView
    private lateinit var tvTotal: TextView

    private var trackingNumberText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_success)

        val topBar = findViewById<View>(R.id.topBarPaymentSuccess)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackPaymentSuccess)
        btnBack?.setOnClickListener { finish() }

        initViews()
        readIntentData()
        setupListeners()
    }

    private fun initViews() {
        tvOrderNumber = findViewById(R.id.tvSuccessOrderNumber)
        tvPaymentMethod = findViewById(R.id.tvSuccessPaymentMethod)
        tvBankBadge = findViewById(R.id.tvSuccessBankBadge)
        tvPaymentTime = findViewById(R.id.tvSuccessPaymentTime)
        tvTotalPaid = findViewById(R.id.tvSuccessTotalPaid)

        tvSellerName = findViewById(R.id.tvSuccessSellerName)
        tvAddress = findViewById(R.id.tvSuccessAddress)
        tvCourier = findViewById(R.id.tvSuccessCourier)
        tvTrackingNumber = findViewById(R.id.tvSuccessTrackingNumber)
        tvEta = findViewById(R.id.tvSuccessEta)

        ivProductImage = findViewById(R.id.ivSuccessProductImage)
        tvProductName = findViewById(R.id.tvSuccessProductName)
        tvProductSub = findViewById(R.id.tvSuccessProductSub)
        tvProductPrice = findViewById(R.id.tvSuccessProductPrice)
        tvSubtotal = findViewById(R.id.tvSuccessSubtotal)
        tvShipping = findViewById(R.id.tvSuccessShipping)
        tvServiceFee = findViewById(R.id.tvSuccessServiceFee)
        tvTotal = findViewById(R.id.tvSuccessTotal)
    }

    private fun readIntentData() {
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID") ?: "#GM-00000"
        val productName = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Pesanan Guppy"
        val quantity = intent.getIntExtra("EXTRA_QUANTITY", 1).coerceAtLeast(1)
        val totalAmount = intent.getLongExtra("EXTRA_TOTAL_AMOUNT", 0L)
        val shippingCost = intent.getLongExtra("EXTRA_SHIPPING_COST", 0L)
        val serviceFee = intent.getLongExtra("EXTRA_SERVICE_FEE", 2000L)
        val unitPriceExtra = intent.getLongExtra("EXTRA_UNIT_PRICE", 0L)
        val unitLabel = intent.getStringExtra("EXTRA_UNIT_LABEL") ?: "per pasang"
        val productImageUrl = intent.getStringExtra("EXTRA_PRODUCT_IMAGE_URL") ?: ""
        val productImageDataUrl = intent.getStringExtra("EXTRA_PRODUCT_IMAGE_DATA_URL") ?: ""
        val courier = intent.getStringExtra("EXTRA_COURIER") ?: "Kurir Pengiriman"
        val shippingEta = intent.getStringExtra("EXTRA_SHIPPING_ETA") ?: ""
        val methodText = intent.getStringExtra("EXTRA_PAYMENT_METHOD") ?: "Pembayaran Penjual"
        val payType = intent.getStringExtra("EXTRA_PAYMENT_TYPE") ?: ""
        val payProvider = intent.getStringExtra("EXTRA_PAYMENT_PROVIDER") ?: ""
        val seller = intent.getStringExtra("EXTRA_SELLER_NAME") ?: "Penjual Guppy"
        val passedAddress = intent.getStringExtra("EXTRA_ADDRESS_TEXT") ?: ""
        val passedTracking = intent.getStringExtra("EXTRA_TRACKING_NUMBER") ?: ""

        val subtotal = if (unitPriceExtra > 0L) {
            unitPriceExtra * quantity
        } else {
            (totalAmount - shippingCost - serviceFee).coerceAtLeast(0L)
        }
        val effectiveUnitPrice = if (unitPriceExtra > 0L) unitPriceExtra else (subtotal / quantity)
        val shortU = unitLabel.removePrefix("per ").trim().ifBlank { "pasang" }

        tvOrderNumber.text = orderId
        tvProductName.text = productName
        tvProductSub.text = if (unitLabel.contains("pasang", true)) {
            "$quantity $shortU · Setara ${quantity * 2} ekor"
        } else {
            "$quantity $shortU"
        }
        tvProductPrice.text = formatCurrency(effectiveUnitPrice)
        SafeImageLoader.loadWithFallbackSafely(
            this,
            ivProductImage,
            productImageUrl,
            productImageDataUrl,
            R.drawable.bg_image_grid,
            256
        )

        tvSubtotal.text = formatCurrency(subtotal)
        tvShipping.text = formatCurrency(shippingCost)
        tvServiceFee.text = formatCurrency(serviceFee)
        tvTotal.text = formatCurrency(totalAmount)
        tvTotalPaid.text = formatCurrency(totalAmount)

        tvSellerName.text = seller
        tvCourier.text = courier
        tvEta.text = shippingEta.removePrefix("Estimasi ").trim().ifBlank { "Menunggu penjemputan kurir" }

        val formattedDate = SimpleDateFormat("EEEE, dd MMMM yyyy · HH:mm 'WIB'", Locale("id", "ID")).format(Date())
        tvPaymentTime.text = formattedDate

        val upperMethod = methodText.uppercase()
        val upperProvider = payProvider.uppercase()
        when {
            payType.equals("QRIS", true) || upperMethod.contains("QRIS") -> {
                tvPaymentMethod.text = "QRIS"
                tvBankBadge.text = payProvider.ifBlank { "QRIS" }.uppercase()
            }
            payType.equals("Bank", true) || upperMethod.contains("BANK") ||
                upperProvider.contains("BCA") || upperProvider.contains("BNI") || upperProvider.contains("MANDIRI") || upperProvider.contains("BRI") -> {
                tvPaymentMethod.text = "Transfer Bank"
                tvBankBadge.text = payProvider.ifBlank { "BANK" }.uppercase()
            }
            else -> {
                tvPaymentMethod.text = methodText
                tvBankBadge.text = payProvider.ifBlank { "E-WALLET" }.uppercase()
            }
        }

        trackingNumberText = passedTracking.ifBlank { "GM" + (1000000000..9999999999).random().toString() }
        tvTrackingNumber.text = trackingNumberText

        if (passedAddress.isNotBlank()) {
            tvAddress.text = if (passedAddress.length > 42) passedAddress.take(42) + "..." else passedAddress
        } else {
            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "user_default"
            FirestoreRepository.fetchUserAddresses(currentUserId, preferServer = false) { addresses ->
                if (isFinishing || isDestroyed) return@fetchUserAddresses
                val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
                tvAddress.text = if (defaultAddr != null) {
                    if (defaultAddr.fullAddress.length > 42) defaultAddr.fullAddress.take(42) + "..." else defaultAddr.fullAddress
                } else {
                    "Alamat belum diatur"
                }
            }
        }
    }

    private fun setupListeners() {
        val btnCopyTracking = findViewById<View>(R.id.btnCopyTrackingNumber)
        btnCopyTracking?.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("No Resi", trackingNumberText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Nomor resi $trackingNumberText disalin!", Toast.LENGTH_SHORT).show()
        }

        val btnTrack = findViewById<View>(R.id.btnTrackOrder)
        btnTrack?.setOnClickListener {
            val trackIntent = Intent(this, OrderTrackingActivity::class.java).apply {
                putExtra("EXTRA_ORDER_ID", intent.getStringExtra("EXTRA_ORDER_ID") ?: "-")
                putExtra("EXTRA_PRODUCT_NAME", intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Pesanan Guppy")
                putExtra("EXTRA_COURIER", intent.getStringExtra("EXTRA_COURIER") ?: "Kurir Pengiriman")
                putExtra("EXTRA_TRACKING_NUMBER", trackingNumberText)
            }
            startActivity(trackIntent)
        }

        val btnHome = findViewById<View>(R.id.btnBackToHome)
        btnHome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    private fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace(",00", "").replace("Rp", "Rp ")
    }
}
