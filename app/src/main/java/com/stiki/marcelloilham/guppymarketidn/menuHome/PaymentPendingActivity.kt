package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentPendingActivity : AppCompatActivity() {

    private lateinit var tvOrderNumber: TextView
    private lateinit var tvPaymentDeadlineDate: TextView
    private lateinit var tvTimerHours: TextView
    private lateinit var tvTimerMinutes: TextView
    private lateinit var tvTimerSeconds: TextView

    private lateinit var tvBankBadge: TextView
    private lateinit var tvPaymentMethodTitle: TextView
    private lateinit var layoutQrisPaymentBox: View
    private lateinit var tvPendingQrisLabel: TextView
    private lateinit var ivPendingQrisImage: ShapeableImageView
    private lateinit var tvPendingQrisOwner: TextView
    private lateinit var cardAccountTransferBox: View
    private lateinit var tvAccountTransferLabel: TextView
    private lateinit var tvVirtualAccountNumber: TextView
    private lateinit var tvPendingAccountOwner: TextView
    private lateinit var tvTotalTransferAmount: TextView
    private lateinit var tvStep1Instruction: TextView
    private lateinit var tvStep2Instruction: TextView
    private lateinit var tvStep3Instruction: TextView
    private lateinit var tvStep4Instruction: TextView

    private lateinit var ivPendingProductImage: ShapeableImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductSub: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvShippingCost: TextView
    private lateinit var tvServiceFee: TextView
    private lateinit var tvTotal: TextView

    private var countDownTimer: CountDownTimer? = null
    private var orderDocId: String = ""
    private var orderIdText: String = ""
    private var sellerId: String = ""
    private var productId: String = ""
    private var productName: String = ""
    private var productImageUrl: String = ""
    private var productImageDataUrl: String = ""
    private var unitPrice: Long = 0L
    private var unitLabel: String = "per pasang"
    private var quantity: Int = 1
    private var shippingCost: Long = 0L
    private var serviceFee: Long = 2000L
    private var totalAmount: Long = 0L
    private var courierText: String = ""
    private var shippingEta: String = ""
    private var sellerName: String = ""
    private var addressText: String = ""

    private var paymentMethodText: String = ""
    private var paymentType: String = ""
    private var paymentProvider: String = ""
    private var paymentAccountNumber: String = ""
    private var paymentAccountOwner: String = ""
    private var paymentQrisDataUrl: String = ""
    private var paymentDeadlineMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_pending)

        val topBar = findViewById<View>(R.id.topBarPaymentPending)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackPaymentPending)
        btnBack?.setOnClickListener { finish() }

        initViews()
        readIntentData()
        setupListeners()
        startCountdownTimer()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun initViews() {
        tvOrderNumber = findViewById(R.id.tvOrderNumber)
        tvPaymentDeadlineDate = findViewById(R.id.tvPaymentDeadlineDate)
        tvTimerHours = findViewById(R.id.tvTimerHours)
        tvTimerMinutes = findViewById(R.id.tvTimerMinutes)
        tvTimerSeconds = findViewById(R.id.tvTimerSeconds)

        tvBankBadge = findViewById(R.id.tvBankBadge)
        tvPaymentMethodTitle = findViewById(R.id.tvPaymentMethodTitle)
        layoutQrisPaymentBox = findViewById(R.id.layoutQrisPaymentBox)
        tvPendingQrisLabel = findViewById(R.id.tvPendingQrisLabel)
        ivPendingQrisImage = findViewById(R.id.ivPendingQrisImage)
        tvPendingQrisOwner = findViewById(R.id.tvPendingQrisOwner)
        cardAccountTransferBox = findViewById(R.id.cardAccountTransferBox)
        tvAccountTransferLabel = findViewById(R.id.tvAccountTransferLabel)
        tvVirtualAccountNumber = findViewById(R.id.tvVirtualAccountNumber)
        tvPendingAccountOwner = findViewById(R.id.tvPendingAccountOwner)
        tvTotalTransferAmount = findViewById(R.id.tvTotalTransferAmount)
        tvStep1Instruction = findViewById(R.id.tvStep1Instruction)
        tvStep2Instruction = findViewById(R.id.tvStep2Instruction)
        tvStep3Instruction = findViewById(R.id.tvStep3Instruction)
        tvStep4Instruction = findViewById(R.id.tvStep4Instruction)

        ivPendingProductImage = findViewById(R.id.ivPendingProductImage)
        tvProductName = findViewById(R.id.tvPendingProductName)
        tvProductSub = findViewById(R.id.tvPendingProductSub)
        tvSubtotal = findViewById(R.id.tvPendingSubtotal)
        tvShippingCost = findViewById(R.id.tvPendingShipping)
        tvServiceFee = findViewById(R.id.tvPendingServiceFee)
        tvTotal = findViewById(R.id.tvPendingTotal)
    }

    private fun readIntentData() {
        orderDocId = intent.getStringExtra("EXTRA_ORDER_DOC_ID") ?: ""
        orderIdText = intent.getStringExtra("EXTRA_ORDER_ID") ?: "#GM-00000"
        sellerId = intent.getStringExtra("EXTRA_SELLER_ID") ?: ""
        productId = intent.getStringExtra("EXTRA_PRODUCT_ID") ?: ""
        productName = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Pesanan Guppy"
        productImageUrl = intent.getStringExtra("EXTRA_PRODUCT_IMAGE_URL") ?: ""
        productImageDataUrl = intent.getStringExtra("EXTRA_PRODUCT_IMAGE_DATA_URL") ?: ""
        quantity = intent.getIntExtra("EXTRA_QUANTITY", 1).coerceAtLeast(1)
        totalAmount = intent.getLongExtra("EXTRA_TOTAL_AMOUNT", 0L)
        shippingCost = intent.getLongExtra("EXTRA_SHIPPING_COST", 0L)
        serviceFee = intent.getLongExtra("EXTRA_SERVICE_FEE", 2000L)
        unitPrice = intent.getLongExtra("EXTRA_UNIT_PRICE", 0L)
        unitLabel = intent.getStringExtra("EXTRA_UNIT_LABEL") ?: "per pasang"
        courierText = intent.getStringExtra("EXTRA_COURIER") ?: ""
        shippingEta = intent.getStringExtra("EXTRA_SHIPPING_ETA") ?: ""
        sellerName = intent.getStringExtra("EXTRA_SELLER_NAME") ?: ""
        addressText = intent.getStringExtra("EXTRA_ADDRESS_TEXT") ?: ""

        paymentMethodText = intent.getStringExtra("EXTRA_PAYMENT_METHOD") ?: ""
        paymentType = intent.getStringExtra("EXTRA_PAYMENT_TYPE") ?: ""
        paymentProvider = intent.getStringExtra("EXTRA_PAYMENT_PROVIDER") ?: ""
        paymentAccountNumber = intent.getStringExtra("EXTRA_PAYMENT_ACCOUNT_NUMBER") ?: ""
        paymentAccountOwner = intent.getStringExtra("EXTRA_PAYMENT_ACCOUNT_OWNER") ?: ""
        paymentQrisDataUrl = intent.getStringExtra("EXTRA_PAYMENT_QRIS_DATA_URL") ?: ""
        paymentDeadlineMillis = intent.getLongExtra("EXTRA_PAYMENT_DEADLINE", 0L)
        if (paymentDeadlineMillis <= 0L) {
            paymentDeadlineMillis = System.currentTimeMillis() + 86400000L
        }

        val subtotal = if (unitPrice > 0L) {
            unitPrice * quantity
        } else {
            (totalAmount - shippingCost - serviceFee).coerceAtLeast(0L)
        }
        val shortU = unitLabel.removePrefix("per ").trim().ifBlank { "pasang" }

        tvOrderNumber.text = "Nomor Pesanan $orderIdText"
        tvProductName.text = productName
        tvProductSub.text = if (unitLabel.contains("pasang", true)) {
            "$quantity $shortU · Setara ${quantity * 2} ekor"
        } else {
            "$quantity $shortU"
        }

        SafeImageLoader.loadWithFallbackSafely(
            this,
            ivPendingProductImage,
            productImageUrl,
            productImageDataUrl,
            R.drawable.bg_image_grid,
            256
        )

        tvSubtotal.text = formatCurrency(subtotal)
        tvShippingCost.text = formatCurrency(shippingCost)
        tvServiceFee.text = formatCurrency(serviceFee)
        tvTotal.text = formatCurrency(totalAmount)
        tvTotalTransferAmount.text = formatCurrency(totalAmount)

        val deadlineFormatted = SimpleDateFormat("EEEE, dd MMMM yyyy · HH:mm 'WIB'", Locale("id", "ID"))
            .format(Date(paymentDeadlineMillis))
        tvPaymentDeadlineDate.text = deadlineFormatted

        renderPaymentMethodDetails()
        backfillMissingDataFromFirestore()
    }

    private fun backfillMissingDataFromFirestore() {
        if ((productImageUrl.isBlank() && productImageDataUrl.isBlank()) && productId.isNotBlank()) {
            FirestoreRepository.fetchProductById(productId) { prod ->
                if (isFinishing || isDestroyed || prod == null) return@fetchProductById
                productImageUrl = prod.photoUris.firstOrNull().orEmpty()
                productImageDataUrl = prod.photoDataUrls.firstOrNull().orEmpty()
                SafeImageLoader.loadWithFallbackSafely(
                    this,
                    ivPendingProductImage,
                    productImageUrl,
                    productImageDataUrl,
                    if (prod.imageRes != 0) prod.imageRes else R.drawable.bg_image_grid,
                    256
                )
            }
        }

        val needsPaymentBackfill = sellerId.isNotBlank() &&
            ((paymentType.equals("QRIS", true) || paymentMethodText.contains("QRIS", true)) && paymentQrisDataUrl.isBlank() && paymentAccountNumber.isBlank() ||
                paymentAccountNumber.isBlank())
        if (needsPaymentBackfill) {
            FirestoreRepository.fetchUserPaymentMethods(sellerId) { methods ->
                if (isFinishing || isDestroyed || methods.isEmpty()) return@fetchUserPaymentMethods
                val matched = methods.find { m ->
                    (paymentProvider.isNotBlank() && m.providerName.equals(paymentProvider, true)) ||
                        (paymentMethodText.isNotBlank() && paymentMethodText.contains(m.providerName, true))
                } ?: methods.find { it.isMain } ?: methods.first()
                applySellerPaymentMethod(matched, persistToOrder = false)
            }
        }
    }

    private fun applySellerPaymentMethod(method: FirestoreRepository.PaymentMethodModel, persistToOrder: Boolean) {
        val isQris = method.type.equals("QRIS", true) || method.providerName.contains("QRIS", true)
        paymentType = when {
            isQris -> "QRIS"
            method.type.equals("Bank", true) -> "Bank"
            else -> method.type.ifBlank { "E-Wallet" }
        }
        paymentProvider = method.providerName
        paymentAccountNumber = method.accountNumber
        paymentAccountOwner = method.accountOwner
        paymentQrisDataUrl = method.qrisDataUrl
        paymentMethodText = if (isQris) {
            "QRIS (${method.providerName})"
        } else {
            "${method.providerName} ($paymentType)"
        }
        renderPaymentMethodDetails()

        if (persistToOrder && orderDocId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("orders").document(orderDocId)
                .update(
                    mapOf(
                        "paymentMethod" to paymentMethodText,
                        "paymentType" to paymentType,
                        "paymentProvider" to paymentProvider,
                        "paymentAccountNumber" to paymentAccountNumber,
                        "paymentAccountOwner" to paymentAccountOwner,
                        "paymentQrisDataUrl" to paymentQrisDataUrl
                    )
                )
        }
    }

    private fun renderPaymentMethodDetails() {
        val isQris = paymentType.equals("QRIS", true) ||
            paymentProvider.contains("QRIS", true) ||
            paymentMethodText.contains("QRIS", true)
        val isBank = !isQris && (paymentType.equals("Bank", true) ||
            paymentMethodText.contains("Bank", true) ||
            paymentProvider.contains("BCA", true) ||
            paymentProvider.contains("BNI", true) ||
            paymentProvider.contains("Mandiri", true) ||
            paymentProvider.contains("BRI", true))

        val providerDisplay = paymentProvider.ifBlank {
            paymentMethodText.ifBlank { "Pembayaran Penjual" }
        }

        tvPaymentMethodTitle.text = if (isQris) {
            "QRIS $providerDisplay"
        } else {
            paymentMethodText.ifBlank { providerDisplay }
        }

        if (isQris) {
            tvBankBadge.text = "QRIS"
            layoutQrisPaymentBox.visibility = View.VISIBLE
            cardAccountTransferBox.visibility = View.GONE

            val ownerDisplay = paymentAccountOwner.ifBlank { sellerName.ifBlank { "Toko Penjual" } }
            tvPendingQrisLabel.text = "PINDAI KODE QRIS $providerDisplay".uppercase(Locale.getDefault())
            tvPendingQrisOwner.text = "Merchant: $ownerDisplay"

            ivPendingQrisImage.colorFilter = null
            ivPendingQrisImage.imageTintList = null
            SafeImageLoader.loadWithFallbackSafely(
                this,
                ivPendingQrisImage,
                paymentAccountNumber,
                paymentQrisDataUrl,
                R.drawable.ic_qris,
                1024
            )

            tvStep1Instruction.text = "Buka aplikasi mobile banking atau e-wallet yang mendukung QRIS."
            tvStep2Instruction.text = "Pilih menu Pindai / Scan QRIS, lalu arahkan ke kode QRIS penjual di atas."
            tvStep3Instruction.text = "Pastikan nama merchant ($ownerDisplay) dan nominal ${formatCurrency(totalAmount)} sudah sesuai."
            tvStep4Instruction.text = "Konfirmasi pembayaran dengan PIN Anda, lalu ketuk tombol Cek status pembayaran."
        } else {
            tvBankBadge.text = when {
                providerDisplay.contains("BCA", true) -> "BCA"
                providerDisplay.contains("BNI", true) -> "BNI"
                providerDisplay.contains("MANDIRI", true) -> "MANDIRI"
                providerDisplay.contains("BRI", true) -> "BRI"
                isBank -> "BANK"
                else -> providerDisplay.take(8).uppercase(Locale.getDefault())
            }

            layoutQrisPaymentBox.visibility = View.GONE
            cardAccountTransferBox.visibility = View.VISIBLE

            tvAccountTransferLabel.text = if (isBank) {
                "NOMOR REKENING ${providerDisplay.uppercase(Locale.getDefault())}"
            } else {
                "NOMOR ${providerDisplay.uppercase(Locale.getDefault())} TUJUAN"
            }

            val accNum = paymentAccountNumber.ifBlank { "Belum tersedia" }
            tvVirtualAccountNumber.text = accNum

            if (paymentAccountOwner.isNotBlank()) {
                tvPendingAccountOwner.visibility = View.VISIBLE
                tvPendingAccountOwner.text = "Atas nama $paymentAccountOwner"
            } else {
                tvPendingAccountOwner.visibility = View.GONE
            }

            val appName = if (isBank) "mobile banking / ATM $providerDisplay atau bank lain" else "aplikasi $providerDisplay"
            val destLabel = if (isBank) "rekening $providerDisplay" else "akun $providerDisplay"
            val ownerSuffix = if (paymentAccountOwner.isNotBlank()) " a.n. $paymentAccountOwner" else ""

            tvStep1Instruction.text = "Buka aplikasi $appName Anda."
            tvStep2Instruction.text = "Pilih menu Transfer ke $destLabel."
            tvStep3Instruction.text = "Masukkan nomor tujuan $accNum$ownerSuffix."
            tvStep4Instruction.text = "Masukkan nominal tepat ${formatCurrency(totalAmount)}, konfirmasi detail transaksi, lalu selesaikan pembayaran."
        }
    }

    private fun setupListeners() {
        val btnCopyVa = findViewById<View>(R.id.btnCopyVaNumber)
        btnCopyVa?.setOnClickListener {
            val numberToCopy = paymentAccountNumber.trim()
            if (numberToCopy.isBlank()) {
                Toast.makeText(this, "Nomor rekening belum tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            copyToClipboard("Nomor Tujuan", numberToCopy.replace(" ", ""))
            Toast.makeText(this, "Nomor tujuan disalin!", Toast.LENGTH_SHORT).show()
        }

        val btnCopyTotal = findViewById<View>(R.id.btnCopyTotalAmount)
        btnCopyTotal?.setOnClickListener {
            copyToClipboard("Total Transfer", totalAmount.toString())
            Toast.makeText(this, "Nominal transfer disalin!", Toast.LENGTH_SHORT).show()
        }

        val btnChangePayment = findViewById<View>(R.id.btnChangePaymentMethod)
        btnChangePayment?.setOnClickListener {
            if (sellerId.isBlank()) {
                Toast.makeText(this, "Daftar metode pembayaran penjual tidak tersedia.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FirestoreRepository.fetchUserPaymentMethods(sellerId) { methods ->
                if (isFinishing || isDestroyed) return@fetchUserPaymentMethods
                val clean = methods.distinctBy { it.id }.sortedByDescending { it.isMain }
                if (clean.isEmpty()) {
                    Toast.makeText(this, "Penjual hanya memiliki 1 atau belum ada metode pembayaran.", Toast.LENGTH_SHORT).show()
                    return@fetchUserPaymentMethods
                }
                val labels = clean.map { m ->
                    val isQris = m.type.equals("QRIS", true) || m.providerName.contains("QRIS", true)
                    if (isQris) {
                        "QRIS (${m.providerName}) · ${m.accountOwner.ifBlank { sellerName }}"
                    } else {
                        "${m.providerName} (${m.accountNumber})"
                    }
                }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle("Pilih Metode Pembayaran Penjual")
                    .setItems(labels) { _, idx ->
                        applySellerPaymentMethod(clean[idx], persistToOrder = true)
                        Toast.makeText(this, "Metode pembayaran diubah ke ${clean[idx].providerName}", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        val btnCheckStatus = findViewById<TextView>(R.id.btnCheckPaymentStatus)
        btnCheckStatus?.setOnClickListener {
            btnCheckStatus.isEnabled = false
            Toast.makeText(this, "Memverifikasi pembayaran...", Toast.LENGTH_SHORT).show()
            val generatedResi = "GM" + (1000000000..9999999999).random().toString()

            val launchSuccess = {
                if (!isFinishing && !isDestroyed) {
                    val intent = Intent(this, PaymentSuccessActivity::class.java).apply {
                        putExtra("EXTRA_ORDER_ID", orderIdText)
                        putExtra("EXTRA_PRODUCT_NAME", productName)
                        putExtra("EXTRA_PRODUCT_IMAGE_URL", productImageUrl)
                        putExtra("EXTRA_PRODUCT_IMAGE_DATA_URL", productImageDataUrl)
                        putExtra("EXTRA_UNIT_PRICE", if (unitPrice > 0L) unitPrice else ((totalAmount - shippingCost - serviceFee).coerceAtLeast(0L) / quantity))
                        putExtra("EXTRA_UNIT_LABEL", unitLabel)
                        putExtra("EXTRA_QUANTITY", quantity)
                        putExtra("EXTRA_TOTAL_AMOUNT", totalAmount)
                        putExtra("EXTRA_SHIPPING_COST", shippingCost)
                        putExtra("EXTRA_SERVICE_FEE", serviceFee)
                        putExtra("EXTRA_COURIER", courierText)
                        putExtra("EXTRA_SHIPPING_ETA", shippingEta)
                        putExtra("EXTRA_SELLER_NAME", sellerName)
                        putExtra("EXTRA_ADDRESS_TEXT", addressText)
                        putExtra("EXTRA_TRACKING_NUMBER", generatedResi)
                        putExtra("EXTRA_PAYMENT_METHOD", paymentMethodText)
                        putExtra("EXTRA_PAYMENT_TYPE", paymentType)
                        putExtra("EXTRA_PAYMENT_PROVIDER", paymentProvider)
                    }
                    startActivity(intent)
                    finish()
                }
            }

            if (orderDocId.isNotBlank()) {
                FirestoreRepository.confirmOrderPayment(
                    orderDocId = orderDocId,
                    productId = productId,
                    quantity = quantity,
                    trackingNumber = generatedResi
                ) {
                    launchSuccess()
                }
            } else {
                btnCheckStatus.postDelayed({ launchSuccess() }, 600)
            }
        }
    }

    private fun startCountdownTimer() {
        val remaining = (paymentDeadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60))
                val minutes = (millisUntilFinished / (1000 * 60)) % 60
                val seconds = (millisUntilFinished / 1000) % 60

                tvTimerHours.text = String.format(Locale.US, "%02d", hours)
                tvTimerMinutes.text = String.format(Locale.US, "%02d", minutes)
                tvTimerSeconds.text = String.format(Locale.US, "%02d", seconds)
            }

            override fun onFinish() {
                tvTimerHours.text = "00"
                tvTimerMinutes.text = "00"
                tvTimerSeconds.text = "00"
            }
        }.start()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    private fun formatCurrency(amount: Long): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(amount).replace(",00", "").replace("Rp", "Rp ")
    }
}
