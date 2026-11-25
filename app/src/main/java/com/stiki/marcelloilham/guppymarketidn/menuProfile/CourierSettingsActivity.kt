package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.RateItem
import com.stiki.marcelloilham.guppymarketidn.api.RatesRequest
import com.stiki.marcelloilham.guppymarketidn.api.RatesResponse
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourierSettingsActivity : AppCompatActivity() {

    private lateinit var tvActiveCount: TextView
    private lateinit var tvFastestDeliveryNote: TextView

    private lateinit var switchJNERegular: SwitchMaterial
    private lateinit var switchJNEYes: SwitchMaterial
    private lateinit var switchJNTExpress: SwitchMaterial
    private lateinit var switchSiCepatHalu: SwitchMaterial
    private lateinit var switchGrabInstant: SwitchMaterial

    private lateinit var tvPriceJNERegular: TextView
    private lateinit var tvPriceJNEYes: TextView
    private lateinit var tvPriceJNT: TextView
    private lateinit var tvPriceSiCepat: TextView
    private lateinit var tvPriceGrab: TextView

    private lateinit var tvSelectedMainCourier: TextView
    private lateinit var tvPickupSchedule: TextView
    private lateinit var tvPickupAddressSub: TextView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var selectedMainCourier = "JNE Reguler"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courier_settings)

        val topBar = findViewById<View>(R.id.topBarCourierSettings)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackCourierSettings)
        btnBack?.setOnClickListener { finish() }

        initViews()
        setupListeners()
        loadCourierSettingsFromFirestore()
        loadPickupAddress()
        fetchLiveBiteshipRates()
    }

    private fun initViews() {
        tvActiveCount = findViewById(R.id.tvActiveCourierCount)
        tvFastestDeliveryNote = findViewById(R.id.tvFastestDeliveryNote)

        switchJNERegular = findViewById(R.id.switchJNERegular)
        switchJNEYes = findViewById(R.id.switchJNEYes)
        switchJNTExpress = findViewById(R.id.switchJNTExpress)
        switchSiCepatHalu = findViewById(R.id.switchSiCepatHalu)
        switchGrabInstant = findViewById(R.id.switchGrabInstant)

        tvPriceJNERegular = findViewById(R.id.tvPriceJNERegular)
        tvPriceJNEYes = findViewById(R.id.tvPriceJNEYes)
        tvPriceJNT = findViewById(R.id.tvPriceJNT)
        tvPriceSiCepat = findViewById(R.id.tvPriceSiCepat)
        tvPriceGrab = findViewById(R.id.tvPriceGrab)

        tvSelectedMainCourier = findViewById(R.id.tvSelectedMainCourier)
        tvPickupSchedule = findViewById(R.id.tvPickupSchedule)
        tvPickupAddressSub = findViewById(R.id.tvPickupAddressSub)
    }

    private fun setupListeners() {
        val switches = listOf(switchJNERegular, switchJNEYes, switchJNTExpress, switchSiCepatHalu, switchGrabInstant)
        switches.forEach { s ->
            s.setOnCheckedChangeListener { _, _ -> updateActiveCountUI() }
        }

        val cardMainCourier = findViewById<View>(R.id.cardMainCourier)
        cardMainCourier?.setOnClickListener { showMainCourierChooserDialog() }

        val rowSchedule = findViewById<View>(R.id.rowPickupSchedule)
        rowSchedule?.setOnClickListener { showPickupScheduleDialog() }

        val rowAddress = findViewById<View>(R.id.rowOriginAddress)
        rowAddress?.setOnClickListener {
            startActivity(android.content.Intent(this, ShippingAddressActivity::class.java))
        }

        val btnSave = findViewById<TextView>(R.id.btnSaveCourierSettings)
        btnSave?.setOnClickListener { saveSettingsToFirestore() }
    }

    private fun updateActiveCountUI() {
        val count = listOf(switchJNERegular, switchJNEYes, switchJNTExpress, switchSiCepatHalu, switchGrabInstant)
            .count { it.isChecked }
        tvActiveCount.text = "$count dari 5 layanan aktif"
    }

    private fun showMainCourierChooserDialog() {
        val options = arrayOf("JNE Reguler", "JNE YES", "J&T Express", "SiCepat Halu", "GrabExpress Instan")
        AlertDialog.Builder(this)
            .setTitle("Pilih Kurir Utama")
            .setItems(options) { _, which ->
                selectedMainCourier = options[which]
                tvSelectedMainCourier.text = selectedMainCourier
            }
            .show()
    }

    private fun showPickupScheduleDialog() {
        val options = arrayOf("Setiap hari, 09.00–16.00", "Senin - Jumat, 09.00–17.00", "Sabtu & Minggu, 09.00–14.00")
        AlertDialog.Builder(this)
            .setTitle("Atur Jadwal Pickup")
            .setItems(options) { _, which ->
                tvPickupSchedule.text = options[which]
            }
            .show()
    }

    private fun loadCourierSettingsFromFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchCourierSettings(currentUserId) { model ->
            switchJNERegular.isChecked = model.jneRegularEnabled
            switchJNEYes.isChecked = model.jneYesEnabled
            switchJNTExpress.isChecked = model.jntExpressEnabled
            switchSiCepatHalu.isChecked = model.sicepatHaluEnabled
            switchGrabInstant.isChecked = model.grabInstantEnabled

            selectedMainCourier = model.mainCourier
            tvSelectedMainCourier.text = selectedMainCourier
            tvPickupSchedule.text = model.pickupSchedule

            updateActiveCountUI()
        }
    }

    private fun loadPickupAddress() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserAddresses(currentUserId) { addresses ->
            val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            if (defaultAddr != null) {
                tvPickupAddressSub.text = "${defaultAddr.label}, ${defaultAddr.recipientName}"
            } else {
                tvPickupAddressSub.text = "Atur alamat pengiriman"
            }
        }
    }

    private fun saveSettingsToFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val model = FirestoreRepository.CourierSettingsModel(
            jneRegularEnabled = switchJNERegular.isChecked,
            jneYesEnabled = switchJNEYes.isChecked,
            jntExpressEnabled = switchJNTExpress.isChecked,
            sicepatHaluEnabled = switchSiCepatHalu.isChecked,
            grabInstantEnabled = switchGrabInstant.isChecked,
            mainCourier = selectedMainCourier,
            pickupSchedule = tvPickupSchedule.text.toString()
        )

        val btnSave = findViewById<TextView>(R.id.btnSaveCourierSettings)
        btnSave?.isEnabled = false
        btnSave?.text = "Menyimpan Perubahan..."

        FirestoreRepository.saveCourierSettings(currentUserId, model) { success ->
            btnSave?.isEnabled = true
            btnSave?.text = "Simpan Perubahan"
            if (success) {
                Toast.makeText(this, "Pengaturan kurir berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchLiveBiteshipRates() {
        val request = RatesRequest(
            originLatitude = -6.2383,
            originLongitude = 106.9896,
            destinationLatitude = -6.2921,
            destinationLongitude = 106.8060,
            couriers = "jnt,jne,sicepat,grab,gojek",
            items = listOf(RateItem("Kemas Guppy", 150000L, 500))
        )

        BiteshipClient.instance.getShippingRates(
            authorization = BiteshipClient.API_KEY,
            request = request
        ).enqueue(object : Callback<RatesResponse> {
            override fun onResponse(call: Call<RatesResponse>, response: Response<RatesResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val pricings = response.body()?.pricing ?: emptyList()

                    val jneReg = pricings.firstOrNull { it.courierCode.contains("jne", true) && !it.courierServiceName.contains("yes", true) }
                    val jneYes = pricings.firstOrNull { it.courierCode.contains("jne", true) && it.courierServiceName.contains("yes", true) }
                    val jntEx = pricings.firstOrNull { it.courierCode.contains("jnt", true) || it.courierName.contains("j&t", true) }
                    val sicepat = pricings.firstOrNull { it.courierCode.contains("sicepat", true) }
                    val grabInst = pricings.firstOrNull { it.type?.contains("instant", true) == true || it.courierCode.contains("grab", true) || it.courierCode.contains("gojek", true) }

                    runOnUiThread {
                        if (jneReg != null && jneReg.price > 0) {
                            tvPriceJNERegular.text = "estimasi ${jneReg.duration ?: "2–3 hari"} · mulai Rp ${String.format("%,d", jneReg.price).replace(',', '.')}"
                        }
                        if (jneYes != null && jneYes.price > 0) {
                            tvPriceJNEYes.text = "estimasi ${jneYes.duration ?: "1 hari"} · mulai Rp ${String.format("%,d", jneYes.price).replace(',', '.')}"
                        }
                        if (jntEx != null && jntEx.price > 0) {
                            tvPriceJNT.text = "estimasi ${jntEx.duration ?: "2–3 hari"} · mulai Rp ${String.format("%,d", jntEx.price).replace(',', '.')}"
                        }
                        if (sicepat != null && sicepat.price > 0) {
                            tvPriceSiCepat.text = "estimasi ${sicepat.duration ?: "1–2 hari"} · mulai Rp ${String.format("%,d", sicepat.price).replace(',', '.')}"
                        }
                        if (grabInst != null && grabInst.price > 0) {
                            tvPriceGrab.text = "estimasi ${grabInst.duration ?: "2–4 jam"} · mulai Rp ${String.format("%,d", grabInst.price).replace(',', '.')}"
                        }
                    }
                    Log.d("CourierSettings", "Biteship rates loaded: ${pricings.size} pricing options")
                }
            }

            override fun onFailure(call: Call<RatesResponse>, t: Throwable) {
                Log.e("CourierSettings", "Rates request failed: ${t.message}")
            }
        })
    }
}
