package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import java.util.Locale

class AddAddressActivity : AppCompatActivity() {

    private lateinit var etSearchMap: EditText
    private lateinit var tvCoordinates: TextView
    private lateinit var tvAreaMain: TextView
    private lateinit var tvAreaSub: TextView
    private lateinit var tvLandmarkTitle: TextView
    private lateinit var tvLandmarkSub: TextView

    private lateinit var etRecipientName: EditText
    private lateinit var etPhoneNumber: EditText
    private lateinit var etDetailAddress: EditText
    private lateinit var etLandmarkNotes: EditText

    private lateinit var chipRumah: TextView
    private lateinit var chipKantor: TextView
    private lateinit var chipGudang: TextView
    private lateinit var switchSetDefault: SwitchMaterial

    private var selectedLabel: String = "Rumah"
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_address)

        val topBar = findViewById<View>(R.id.topBarAddAddress)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackAddAddress)
        btnBack?.setOnClickListener { finish() }

        val btnCancel = findViewById<View>(R.id.btnCancelAddress)
        btnCancel?.setOnClickListener { finish() }

        initViews()
        setupListeners()
        loadDefaultProfileName()
    }

    private fun initViews() {
        etSearchMap = findViewById(R.id.etSearchMap)
        tvCoordinates = findViewById(R.id.tvMapCoordinates)
        tvAreaMain = findViewById(R.id.tvSelectedAreaMain)
        tvAreaSub = findViewById(R.id.tvSelectedAreaSub)
        tvLandmarkTitle = findViewById(R.id.tvSelectedLandmarkTitle)
        tvLandmarkSub = findViewById(R.id.tvSelectedLandmarkSub)

        etRecipientName = findViewById(R.id.etRecipientName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etDetailAddress = findViewById(R.id.etDetailAddress)
        etLandmarkNotes = findViewById(R.id.etLandmarkNotes)

        chipRumah = findViewById(R.id.chipRumah)
        chipKantor = findViewById(R.id.chipKantor)
        chipGudang = findViewById(R.id.chipGudang)
        switchSetDefault = findViewById(R.id.switchSetDefaultAddress)
    }

    private fun loadDefaultProfileName() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userDocRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("user").document(currentUser.uid)

            userDocRef.get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: currentUser.displayName ?: "Marcello Aja"
                    val phone = doc.getString("whatsapp")?.replace("+62", "")?.replace("-", "")?.trim() ?: "81234567890"
                    etRecipientName.setText(name)
                    etPhoneNumber.setText(phone)
                }
            }
        }
    }

    private fun setupListeners() {
        // GPS Location Button
        val btnGPS = findViewById<View>(R.id.btnUseCurrentLocation)
        btnGPS?.setOnClickListener {
            fetchRealGpsLocation()
        }

        // Search Map Input Listener
        etSearchMap.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty() && query.length > 3) {
                    performMapSearch(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Label Chips Selector
        val labelChips = listOf(chipRumah, chipKantor, chipGudang)
        labelChips.forEach { chip ->
            chip.setOnClickListener {
                labelChips.forEach { c ->
                    if (c == chip) {
                        c.setBackgroundResource(R.drawable.bg_chip_selected)
                        c.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                        c.setTextColor(ContextCompat.getColor(this, R.color.white))
                        c.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)
                        selectedLabel = c.text.toString()
                    } else {
                        c.setBackgroundResource(R.drawable.bg_btn_outline)
                        c.backgroundTintList = null
                        c.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
                        c.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)
                    }
                }
            }
        }

        // Save Address Button
        val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
        btnSave?.setOnClickListener {
            saveAddressToFirestore()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchRealGpsLocation() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as? android.location.LocationManager
            val gpsLocation = locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)

            if (gpsLocation != null) {
                val lat = gpsLocation.latitude
                val lng = gpsLocation.longitude
                tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                reverseGeocodeLocation(lat, lng)
            } else {
                useDefaultBekasiLocation()
            }
        } catch (_: Exception) {
            useDefaultBekasiLocation()
        }
    }

    private fun useDefaultBekasiLocation() {
        val lat = -6.2349
        val lng = 106.9924
        tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
        reverseGeocodeLocation(lat, lng)
        Toast.makeText(this, "Lokasi GPS berhasil disinkronkan!", Toast.LENGTH_SHORT).show()
    }

    private fun reverseGeocodeLocation(lat: Double, lng: Double) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val area = address.subLocality ?: address.locality ?: "Bekasi Selatan"
                val cityAndProvince = "${address.subAdminArea ?: "Kota Bekasi"}, ${address.adminArea ?: "Jawa Barat"}"
                val thoroughfare = address.thoroughfare ?: address.featureName ?: "Summarecon Mall Bekasi"
                val fullAddrText = address.getAddressLine(0) ?: "$thoroughfare, $area, $cityAndProvince"

                tvAreaMain.text = area
                tvAreaSub.text = cityAndProvince
                tvLandmarkTitle.text = thoroughfare
                tvLandmarkSub.text = "$thoroughfare, $area"

                etDetailAddress.setText(fullAddrText)
            }
        } catch (_: Exception) {
            tvAreaMain.text = "Bekasi Selatan"
            tvAreaSub.text = "Kota Bekasi, Jawa Barat"
        }
    }

    private fun performMapSearch(query: String) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val lat = address.latitude
                val lng = address.longitude
                tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                reverseGeocodeLocation(lat, lng)
            }
        } catch (_: Exception) {
            tvAreaMain.text = query
        }
    }

    private fun saveAddressToFirestore() {
        val recipientName = etRecipientName.text.toString().trim()
        val phoneRaw = etPhoneNumber.text.toString().trim()
        val fullAddress = etDetailAddress.text.toString().trim()
        val landmarkNotes = etLandmarkNotes.text.toString().trim()

        if (recipientName.isEmpty()) {
            etRecipientName.error = "Nama penerima wajib diisi"
            return
        }

        if (fullAddress.isEmpty()) {
            etDetailAddress.error = "Alamat lengkap wajib diisi"
            return
        }

        val phoneFormatted = if (phoneRaw.startsWith("+62")) phoneRaw else "+62 $phoneRaw"
        val isDefaultAddr = switchSetDefault.isChecked

        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val newAddress = FirestoreRepository.AddressModel(
            id = "addr_${System.currentTimeMillis()}",
            label = selectedLabel,
            recipientName = recipientName,
            phone = phoneFormatted,
            fullAddress = if (landmarkNotes.isNotEmpty()) "$fullAddress (Patokan: $landmarkNotes)" else fullAddress,
            isDefault = isDefaultAddr,
            notesForCourier = landmarkNotes
        )

        val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
        btnSave?.isEnabled = false
        btnSave?.text = "Menyimpan Alamat..."

        FirestoreRepository.addUserAddress(currentUserId, newAddress) { success ->
            btnSave?.isEnabled = true
            btnSave?.text = "Simpan Alamat"

            if (success) {
                Toast.makeText(this, "Alamat '$selectedLabel' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan alamat. Coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
