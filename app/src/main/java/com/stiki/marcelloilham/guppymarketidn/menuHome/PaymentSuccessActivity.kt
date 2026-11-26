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
import com.stiki.marcelloilham.guppymarketidn.MainActivity
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
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

    private lateinit var tvProductName: TextView
    private lateinit var tvProductSub: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvShipping: TextView
    private lateinit var tvServiceFee: TextView
    private lateinit var tvTotal: TextView

    private var trackingNumberText = "JT0012398765"

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

        tvProductName = findViewById(R.id.tvSuccessProductName)
        tvProductSub = findViewById(R.id.tvSuccessProductSub)
        tvProductPrice = findViewById(R.id.tvSuccessProductPrice)
        tvSubtotal = findViewById(R.id.tvSuccessSubtotal)
        tvShipping = findViewById(R.id.tvSuccessShipping)
        tvServiceFee = findViewById(R.id.tvSuccessServiceFee)
        tvTotal = findViewById(R.id.tvSuccessTotal)
    }

    private fun readIntentData() {
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID") ?: "#GM-20931"
        val productName = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Red Dragon Delta"
        val quantity = intent.getIntExtra("EXTRA_QUANTITY", 2)
        val totalAmount = intent.getLongExtra("EXTRA_TOTAL_AMOUNT", 577000L)
        val shippingCost = intent.getLongExtra("EXTRA_SHIPPING_COST", 25000L)
        val serviceFee = intent.getLongExtra("EXTRA_SERVICE_FEE", 2000L)
        val courier = intent.getStringExtra("EXTRA_COURIER") ?: "J&T Express Reguler"
        val methodText = intent.getStringExtra("EXTRA_PAYMENT_METHOD") ?: "Bank BCA Virtual Account"
        val seller = intent.getStringExtra("EXTRA_SELLER_NAME") ?: "Guppy Farm Bahagia"

        val subtotal = 275000L * quantity

        tvOrderNumber.text = orderId
        tvProductName.text = productName
        tvProductSub.text = "$quantity pasang · Setara ${quantity * 2} ekor"
        tvProductPrice.text = formatCurrency(275000L)

        tvSubtotal.text = formatCurrency(subtotal)
        tvShipping.text = formatCurrency(shippingCost)
        tvServiceFee.text = formatCurrency(serviceFee)
        tvTotal.text = formatCurrency(totalAmount)
        tvTotalPaid.text = formatCurrency(totalAmount)

        tvSellerName.text = seller
        tvCourier.text = courier

        val formattedDate = SimpleDateFormat("EEEE, dd MMMM yyyy · HH:mm 'WIB'", Locale("id", "ID")).format(Date())
        tvPaymentTime.text = formattedDate

        val upperMethod = methodText.uppercase()
        if (upperMethod.contains("BCA")) {
            tvPaymentMethod.text = "Transfer Bank"
            tvBankBadge.text = "BCA VA"
        } else if (upperMethod.contains("BNI")) {
            tvPaymentMethod.text = "Transfer Bank"
            tvBankBadge.text = "BNI VA"
        } else if (upperMethod.contains("MANDIRI")) {
            tvPaymentMethod.text = "Transfer Bank"
            tvBankBadge.text = "MANDIRI VA"
        } else {
            tvPaymentMethod.text = "E-Wallet / QRIS"
            tvBankBadge.text = "INSTAN"
        }

        trackingNumberText = "JT" + (1000000000..9999999999).random().toString()
        tvTrackingNumber.text = trackingNumberText

        // Load user default address
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserAddresses(currentUserId) { addresses ->
            val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            if (defaultAddr != null) {
                val shortAddr = if (defaultAddr.fullAddress.length > 30) {
                    defaultAddr.fullAddress.take(30) + "..."
                } else {
                    defaultAddr.fullAddress
                }
                tvAddress.text = shortAddr
            } else {
                tvAddress.text = "Jl. Melati No. 12, Bandung"
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
            Toast.makeText(this, "Membuka lacak pesanan #$trackingNumberText...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
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
