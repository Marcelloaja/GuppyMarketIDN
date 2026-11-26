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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuProfile.PaymentMethodsActivity
import java.text.NumberFormat
import java.util.Locale

class PaymentPendingActivity : AppCompatActivity() {

    private lateinit var tvOrderNumber: TextView
    private lateinit var tvPaymentDeadlineDate: TextView
    private lateinit var tvTimerHours: TextView
    private lateinit var tvTimerMinutes: TextView
    private lateinit var tvTimerSeconds: TextView

    private lateinit var tvBankBadge: TextView
    private lateinit var tvPaymentMethodTitle: TextView
    private lateinit var tvVirtualAccountNumber: TextView
    private lateinit var tvTotalTransferAmount: TextView
    private lateinit var tvStep3Instruction: TextView

    private lateinit var tvProductName: TextView
    private lateinit var tvProductSub: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvShippingCost: TextView
    private lateinit var tvServiceFee: TextView
    private lateinit var tvTotal: TextView

    private var countDownTimer: CountDownTimer? = null
    private var totalAmount: Long = 577000L
    private var paymentMethodText: String = "Bank BCA Virtual Account"
    private var vaNumberText: String = "3901081234567890"

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

    private fun initViews() {
        tvOrderNumber = findViewById(R.id.tvOrderNumber)
        tvPaymentDeadlineDate = findViewById(R.id.tvPaymentDeadlineDate)
        tvTimerHours = findViewById(R.id.tvTimerHours)
        tvTimerMinutes = findViewById(R.id.tvTimerMinutes)
        tvTimerSeconds = findViewById(R.id.tvTimerSeconds)

        tvBankBadge = findViewById(R.id.tvBankBadge)
        tvPaymentMethodTitle = findViewById(R.id.tvPaymentMethodTitle)
        tvVirtualAccountNumber = findViewById(R.id.tvVirtualAccountNumber)
        tvTotalTransferAmount = findViewById(R.id.tvTotalTransferAmount)
        tvStep3Instruction = findViewById(R.id.tvStep3Instruction)

        tvProductName = findViewById(R.id.tvPendingProductName)
        tvProductSub = findViewById(R.id.tvPendingProductSub)
        tvSubtotal = findViewById(R.id.tvPendingSubtotal)
        tvShippingCost = findViewById(R.id.tvPendingShipping)
        tvServiceFee = findViewById(R.id.tvPendingServiceFee)
        tvTotal = findViewById(R.id.tvPendingTotal)
    }

    private fun readIntentData() {
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID") ?: "#GM-20931"
        val productName = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Red Dragon Delta"
        val quantity = intent.getIntExtra("EXTRA_QUANTITY", 2)
        totalAmount = intent.getLongExtra("EXTRA_TOTAL_AMOUNT", 577000L)
        val shipping = intent.getLongExtra("EXTRA_SHIPPING_COST", 25000L)
        val service = intent.getLongExtra("EXTRA_SERVICE_FEE", 2000L)
        paymentMethodText = intent.getStringExtra("EXTRA_PAYMENT_METHOD") ?: "Bank BCA Virtual Account"

        val subtotal = (275000L * quantity)

        tvOrderNumber.text = "Nomor Pesanan $orderId"
        tvProductName.text = productName
        tvProductSub.text = "$quantity pasang · Setara ${quantity * 2} ekor"

        tvSubtotal.text = formatCurrency(subtotal)
        tvShippingCost.text = formatCurrency(shipping)
        tvServiceFee.text = formatCurrency(service)
        tvTotal.text = formatCurrency(totalAmount)
        tvTotalTransferAmount.text = formatCurrency(totalAmount)

        val upperMethod = paymentMethodText.uppercase()
        if (upperMethod.contains("BCA")) {
            tvBankBadge.text = "BCA"
            vaNumberText = "3901 0812 3456 7890"
        } else if (upperMethod.contains("BNI")) {
            tvBankBadge.text = "BNI"
            vaNumberText = "8808 0812 3456 7890"
        } else if (upperMethod.contains("MANDIRI")) {
            tvBankBadge.text = "MANDIRI"
            vaNumberText = "8902 0812 3456 7890"
        } else {
            tvBankBadge.text = "VA"
            vaNumberText = "7001 0812 3456 7890"
        }

        tvPaymentMethodTitle.text = paymentMethodText
        tvVirtualAccountNumber.text = vaNumberText
        tvStep3Instruction.text = "Masukkan nomor Virtual Account $vaNumberText."
    }

    private fun setupListeners() {
        val btnCopyVa = findViewById<View>(R.id.btnCopyVaNumber)
        btnCopyVa?.setOnClickListener {
            copyToClipboard("Nomor Virtual Account", vaNumberText.replace(" ", ""))
            Toast.makeText(this, "Nomor Virtual Account disalin!", Toast.LENGTH_SHORT).show()
        }

        val btnCopyTotal = findViewById<View>(R.id.btnCopyTotalAmount)
        btnCopyTotal?.setOnClickListener {
            copyToClipboard("Total Transfer", totalAmount.toString())
            Toast.makeText(this, "Nominal transfer disalin!", Toast.LENGTH_SHORT).show()
        }

        val btnChangePayment = findViewById<View>(R.id.btnChangePaymentMethod)
        btnChangePayment?.setOnClickListener {
            val intent = Intent(this, PaymentMethodsActivity::class.java)
            startActivity(intent)
        }

        val btnCheckStatus = findViewById<View>(R.id.btnCheckPaymentStatus)
        btnCheckStatus?.setOnClickListener {
            Toast.makeText(this, "Mengecek status verifikasi pembayaran...", Toast.LENGTH_SHORT).show()
            findViewById<View>(R.id.btnCheckPaymentStatus)?.postDelayed({
                val intent = Intent(this, PaymentSuccessActivity::class.java).apply {
                    putExtra("EXTRA_ORDER_ID", intent.getStringExtra("EXTRA_ORDER_ID") ?: "#GM-20931")
                    putExtra("EXTRA_PRODUCT_NAME", intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Red Dragon Delta")
                    putExtra("EXTRA_QUANTITY", intent.getIntExtra("EXTRA_QUANTITY", 2))
                    putExtra("EXTRA_TOTAL_AMOUNT", intent.getLongExtra("EXTRA_TOTAL_AMOUNT", 577000L))
                    putExtra("EXTRA_SHIPPING_COST", intent.getLongExtra("EXTRA_SHIPPING_COST", 25000L))
                    putExtra("EXTRA_SERVICE_FEE", intent.getLongExtra("EXTRA_SERVICE_FEE", 2000L))
                    putExtra("EXTRA_PAYMENT_METHOD", paymentMethodText)
                }
                startActivity(intent)
                finish()
            }, 1000)
        }
    }

    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(86400000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
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

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}