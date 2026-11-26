package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.TrackingResponse
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderTrackingActivity : AppCompatActivity() {

    private lateinit var tvProductName: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvProductDetails: TextView
    private lateinit var tvOrderNumber: TextView

    private lateinit var tvCourierName: TextView
    private lateinit var tvResiNumber: TextView

    private lateinit var tvEtaDate: TextView
    private lateinit var tvEtaSubtext: TextView
    private lateinit var tvAddressSub: TextView

    private lateinit var btnConfirmReceived: TextView

    private var trackingNumber = "JT0012398765"
    private var courierCode = "jnt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_tracking)

        val topBar = findViewById<View>(R.id.topBarOrderTracking)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackOrderTracking)
        btnBack?.setOnClickListener { finish() }

        initViews()
        readIntentData()
        setupListeners()
        fetchLiveBiteshipTracking()
    }

    private fun initViews() {
        tvProductName = findViewById(R.id.tvTrackProductName)
        tvStatusBadge = findViewById(R.id.tvTrackStatusBadge)
        tvProductDetails = findViewById(R.id.tvTrackProductDetails)
        tvOrderNumber = findViewById(R.id.tvTrackOrderNumber)

        tvCourierName = findViewById(R.id.tvTrackCourierName)
        tvResiNumber = findViewById(R.id.tvTrackResiNumber)

        tvEtaDate = findViewById(R.id.tvTrackEtaDate)
        tvEtaSubtext = findViewById(R.id.tvTrackEtaSubtext)
        tvAddressSub = findViewById(R.id.tvTrackAddressSub)

        btnConfirmReceived = findViewById(R.id.btnConfirmOrderReceived)
    }

    private fun readIntentData() {
        val orderId = intent.getStringExtra("EXTRA_ORDER_ID") ?: "#GM-20931"
        val productName = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: "Red Dragon Delta"
        val courier = intent.getStringExtra("EXTRA_COURIER") ?: "JNE Express"
        trackingNumber = intent.getStringExtra("EXTRA_TRACKING_NUMBER") ?: "JP8841"

        tvOrderNumber.text = "No. Pesanan: $orderId"
        tvProductName.text = productName
        tvCourierName.text = courier
        tvResiNumber.text = "Resi: $trackingNumber"

        courierCode = when {
            courier.contains("J&T", true) || courier.contains("JNT", true) -> "jnt"
            courier.contains("JNE", true) -> "jne"
            courier.contains("SICEPAT", true) -> "sicepat"
            else -> "gojek"
        }

        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserAddresses(currentUserId) { addresses ->
            val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            if (defaultAddr != null) {
                tvAddressSub.text = "${defaultAddr.label}, ${defaultAddr.recipientName}"
            }
        }
    }

    private fun setupListeners() {
        val btnContact = findViewById<View>(R.id.btnContactCourier)
        btnContact?.setOnClickListener {
            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:081234567890")
            }
            startActivity(phoneIntent)
        }

        val btnCopyResi = findViewById<View>(R.id.btnCopyResiTracking)
        btnCopyResi?.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Nomor Resi", trackingNumber)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Nomor resi $trackingNumber disalin!", Toast.LENGTH_SHORT).show()
        }

        val btnHelp = findViewById<View>(R.id.btnHelpTracking)
        btnHelp?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Bantuan Pengiriman")
                .setMessage("Ikan dikemas menggunakan kantong ganda & oksigen murni murni. Jika timbul kendala pengiriman, hubungi layanan pelanggan GuppyMarket.")
                .setPositiveButton("Mengerti", null)
                .show()
        }

        btnConfirmReceived.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Konfirmasi Pesanan Diterima")
                .setMessage("Apakah ikan sudah Anda terima dalam kondisi hidup & sehat?")
                .setPositiveButton("Ya, Sudah Diterima") { _, _ ->
                    tvStatusBadge.text = "Selesai"
                    btnConfirmReceived.text = "Pesanan Telah Diterima ✓"
                    btnConfirmReceived.isEnabled = false
                    btnConfirmReceived.background = ContextCompat.getDrawable(this, R.drawable.bg_btn_primary)
                    Toast.makeText(this, "Terima kasih! Pesanan diselesaikan.", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun fetchLiveBiteshipTracking() {
        BiteshipClient.instance.getTracking(
            authorization = BiteshipClient.API_KEY,
            waybillId = trackingNumber,
            courierCode = courierCode
        ).enqueue(object : Callback<TrackingResponse> {
            override fun onResponse(call: Call<TrackingResponse>, response: Response<TrackingResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val histories = response.body()?.history ?: emptyList()
                    val latestStatus = histories.lastOrNull()?.status ?: "Dikirim"
                    runOnUiThread {
                        tvStatusBadge.text = latestStatus.replace("_", " ").replaceFirstChar { it.uppercase() }
                    }
                    Log.d("OrderTracking", "Biteship tracking loaded: $latestStatus")
                }
            }

            override fun onFailure(call: Call<TrackingResponse>, t: Throwable) {
                Log.e("OrderTracking", "Tracking call failed: ${t.message}")
            }
        })
    }
}
