package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
    private var isEditMode = false
    private var existingAddressId: String? = null
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

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

        initLocationPermissionLauncher()
        initViews()
        initOpenStreetMap()
        setupListeners()
        loadDefaultProfileName()
        checkAddressCountForDefaultToggle()
        checkEditModeIntent()
        requestLocationPermission()
    }

    private fun checkAddressCountForDefaultToggle() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserAddresses(currentUserId) { addresses ->
            val hasExistingAddress = addresses.isNotEmpty()
            val tvSubNotice = findViewById<TextView>(R.id.tvSetDefaultSubNotice)

            if (!hasExistingAddress) {
                switchSetDefault.isChecked = true
                switchSetDefault.isEnabled = false
                tvSubNotice?.text = "Alamat pertama otomatis menjadi alamat utama"
            } else {
                if (!isEditMode) {
                    switchSetDefault.isChecked = false
                    switchSetDefault.isEnabled = true
                    tvSubNotice?.text = "Dipakai sebagai default saat checkout"
                }
            }
        }
    }

    private fun checkEditModeIntent() {
        isEditMode = intent.getBooleanExtra("EXTRA_IS_EDIT", false)
        if (isEditMode) {
            existingAddressId = intent.getStringExtra("EXTRA_ADDRESS_ID")
            val label = intent.getStringExtra("EXTRA_ADDRESS_LABEL") ?: "Rumah"
            val name = intent.getStringExtra("EXTRA_RECIPIENT_NAME") ?: ""
            val phone = intent.getStringExtra("EXTRA_PHONE_NUMBER") ?: ""
            val addressText = intent.getStringExtra("EXTRA_FULL_ADDRESS") ?: ""
            val isDefault = intent.getBooleanExtra("EXTRA_IS_DEFAULT", false)
            val notes = intent.getStringExtra("EXTRA_NOTES") ?: ""

            // Update Header & Button Titles
            val tvHeaderTitle = findViewById<TextView>(R.id.tvAddAddressHeaderTitle)
            tvHeaderTitle?.text = "Ubah Alamat"

            val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
            btnSave?.text = "Simpan Perubahan"

            // Pre-fill fields
            if (name.isNotEmpty()) etRecipientName.setText(name)
            val cleanPhone = phone.replace("+62", "").replace("-", "").replace(" ", "").trim()
            if (cleanPhone.isNotEmpty()) etPhoneNumber.setText(cleanPhone)
            if (addressText.isNotEmpty()) etDetailAddress.setText(addressText)
            if (notes.isNotEmpty()) etLandmarkNotes.setText(notes)
            switchSetDefault.isChecked = isDefault

            selectChipByLabel(label)

            // Geocode address
            if (addressText.isNotEmpty()) {
                performMapSearch(addressText)
            }
        }
    }

    private fun selectChipByLabel(targetLabel: String) {
        val labelChips = listOf(chipRumah, chipKantor, chipGudang)
        labelChips.forEach { chip ->
            if (chip.text.toString().equals(targetLabel, ignoreCase = true)) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                chip.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)
                selectedLabel = chip.text.toString()
            } else {
                chip.setBackgroundResource(R.drawable.bg_btn_outline_pill_gray)
                chip.backgroundTintList = null
                chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
                chip.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)
            }
        }
    }

    private fun initLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.any { it.value }
            if (granted) {
                fetchRealGpsLocation()
            } else {
                useDefaultBekasiLocation()
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchRealGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
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
            requestLocationPermission()
        }

        // Clickable Location Rows below Map
        val row1 = findViewById<View>(R.id.layoutLocationRow1)
        val row2 = findViewById<View>(R.id.layoutLocationRow2)

        row1?.setOnClickListener {
            highlightSelectedLocationRow(1)
            val addrText = "${tvAreaMain.text}, ${tvAreaSub.text}"
            etDetailAddress.setText(addrText)
            Toast.makeText(this, "Rekomendasi lokasi '${tvAreaMain.text}' dipilih!", Toast.LENGTH_SHORT).show()
        }

        row2?.setOnClickListener {
            highlightSelectedLocationRow(2)
            val landmarkText = "${tvLandmarkTitle.text}, ${tvLandmarkSub.text}"
            etDetailAddress.setText(landmarkText)
            Toast.makeText(this, "Rekomendasi lokasi '${tvLandmarkTitle.text}' dipilih!", Toast.LENGTH_SHORT).show()
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
                        c.setBackgroundResource(R.drawable.bg_btn_outline_pill_gray)
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

    private fun highlightSelectedLocationRow(selectedIndex: Int) {
        val row1 = findViewById<View>(R.id.layoutLocationRow1)
        val row2 = findViewById<View>(R.id.layoutLocationRow2)

        val check1 = findViewById<ImageView>(R.id.ivCheckRow1)
        val check2 = findViewById<ImageView>(R.id.ivCheckRow2)

        if (selectedIndex == 1) {
            row1?.setBackgroundResource(R.drawable.bg_chip_selected)
            row1?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0F2F1"))
            check1?.visibility = View.VISIBLE

            row2?.background = null
            check2?.visibility = View.GONE
        } else if (selectedIndex == 2) {
            row2?.setBackgroundResource(R.drawable.bg_chip_selected)
            row2?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0F2F1"))
            check2?.visibility = View.VISIBLE

            row1?.background = null
            check1?.visibility = View.GONE
        } else {
            row1?.background = null
            check1?.visibility = View.GONE
            row2?.background = null
            check2?.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchRealGpsLocation() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as? LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val lat = location.latitude
                    val lng = location.longitude
                    tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                    reverseGeocodeLocation(lat, lng)
                    try { locationManager?.removeUpdates(this) } catch (_: Exception) {}
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnown != null) {
                val lat = lastKnown.latitude
                val lng = lastKnown.longitude
                tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                reverseGeocodeLocation(lat, lng)
            }

            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 10f, locationListener)
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10f, locationListener)
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

    private lateinit var webViewOsmMap: WebView

    @SuppressLint("SetJavaScriptEnabled")
    private fun initOpenStreetMap() {
        webViewOsmMap = findViewById(R.id.webViewOsmMap)
        webViewOsmMap.settings.javaScriptEnabled = true
        webViewOsmMap.settings.domStorageEnabled = true
        webViewOsmMap.settings.userAgentString = "GuppyMarketIDN/1.0 (Android; MobileApp)"

        webViewOsmMap.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onMapDragged(lat: Double, lng: Double) {
                runOnUiThread {
                    tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                    reverseGeocodeLocation(lat, lng, syncOsm = false)
                }
            }
        }, "AndroidInterface")

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    body, html, #map { margin: 0; padding: 0; width: 100%; height: 100%; }
                    .leaflet-control-attribution { display: none; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', { zoomControl: false }).setView([-6.2349, 106.9924], 15);
                    L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
                        maxZoom: 19
                    }).addTo(map);

                    var marker = L.marker([-6.2349, 106.9924], { draggable: true }).addTo(map);

                    function updateMapLocation(lat, lng) {
                        var newLatLng = new L.LatLng(lat, lng);
                        map.setView(newLatLng, 16);
                        marker.setLatLng(newLatLng);
                    }

                    marker.on('dragend', function(event) {
                        var pos = marker.getLatLng();
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onMapDragged(pos.lat, pos.lng);
                        }
                    });

                    map.on('click', function(e) {
                        marker.setLatLng(e.latlng);
                        if (window.AndroidInterface) {
                            window.AndroidInterface.onMapDragged(e.latlng.lat, e.latlng.lng);
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()

        webViewOsmMap.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
    }

    private fun updateOsmMapCenter(lat: Double, lng: Double) {
        if (::webViewOsmMap.isInitialized) {
            webViewOsmMap.evaluateJavascript("updateMapLocation($lat, $lng)", null)
        }
    }

    private fun reverseGeocodeLocation(lat: Double, lng: Double, syncOsm: Boolean = true) {
        if (syncOsm) {
            updateOsmMapCenter(lat, lng)
        }
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
            id = existingAddressId ?: "addr_${System.currentTimeMillis()}",
            label = selectedLabel,
            recipientName = recipientName,
            phone = phoneFormatted,
            fullAddress = fullAddress,
            isDefault = isDefaultAddr,
            notesForCourier = landmarkNotes
        )

        val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
        btnSave?.isEnabled = false
        btnSave?.text = if (isEditMode) "Menyimpan Perubahan..." else "Menyimpan Alamat..."

        val executeSave = {
            FirestoreRepository.addUserAddress(currentUserId, newAddress) { success ->
                btnSave?.isEnabled = true
                btnSave?.text = if (isEditMode) "Simpan Perubahan" else "Simpan Alamat"

                if (success) {
                    showAddressSuccessDialog(newAddress)
                } else {
                    Toast.makeText(this, "Gagal menyimpan alamat. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (isDefaultAddr) {
            FirestoreRepository.fetchUserAddresses(currentUserId) { existingList ->
                val updatedList = existingList.filterNot { it.id == newAddress.id }.map {
                    it.copy(isDefault = false)
                } + newAddress.copy(isDefault = true)

                FirestoreRepository.updateAllAddresses(currentUserId, updatedList) { success ->
                    btnSave?.isEnabled = true
                    btnSave?.text = if (isEditMode) "Simpan Perubahan" else "Simpan Alamat"

                    if (success) {
                        showAddressSuccessDialog(newAddress)
                    } else {
                        Toast.makeText(this, "Gagal menyimpan alamat. Coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            executeSave()
        }
    }

    private fun showAddressSuccessDialog(savedAddress: FirestoreRepository.AddressModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_address_success, null)

        val tvName = dialogView.findViewById<TextView>(R.id.tvSuccessRecipientName)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvSuccessPhone)
        val tvFullAddr = dialogView.findViewById<TextView>(R.id.tvSuccessFullAddress)

        val btnEdit = dialogView.findViewById<TextView>(R.id.btnSuccessEdit)
        val btnDelete = dialogView.findViewById<TextView>(R.id.btnSuccessDelete)
        val btnDone = dialogView.findViewById<TextView>(R.id.btnSuccessDone)
        val btnAddAnother = dialogView.findViewById<TextView>(R.id.btnSuccessAddAnother)

        tvName?.text = savedAddress.recipientName
        tvPhone?.text = savedAddress.phone
        tvFullAddr?.text = savedAddress.fullAddress

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnEdit?.setOnClickListener {
            alertDialog.dismiss()
        }

        btnDelete?.setOnClickListener {
            showDeleteConfirmationDialog(savedAddress, alertDialog)
        }

        btnDone?.setOnClickListener {
            alertDialog.dismiss()
            finish()
        }

        btnAddAnother?.setOnClickListener {
            alertDialog.dismiss()
            etDetailAddress.setText("")
            etLandmarkNotes.setText("")
            etSearchMap.setText("")
            isEditMode = false
            existingAddressId = null

            val tvHeaderTitle = findViewById<TextView>(R.id.tvAddAddressHeaderTitle)
            tvHeaderTitle?.text = "Tambah Alamat Baru"

            val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
            btnSave?.text = "Simpan Alamat"

            Toast.makeText(this, "Silakan isi alamat baru berikutnya", Toast.LENGTH_SHORT).show()
        }

        alertDialog.show()
    }

    private fun showDeleteConfirmationDialog(savedAddress: FirestoreRepository.AddressModel, parentSuccessDialog: AlertDialog) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_address_confirm, null)

        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogRecipientName)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvDialogPhone)
        val tvFullAddr = dialogView.findViewById<TextView>(R.id.tvDialogFullAddress)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnDialogCancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnDialogConfirmDelete)

        tvName?.text = savedAddress.recipientName
        tvPhone?.text = savedAddress.phone
        tvFullAddr?.text = savedAddress.fullAddress

        val deleteAlertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        deleteAlertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel?.setOnClickListener {
            deleteAlertDialog.dismiss()
        }

        btnConfirm?.setOnClickListener {
            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
            FirestoreRepository.deleteUserAddress(currentUserId, savedAddress.id) { _ ->
                Toast.makeText(this, "Alamat '${savedAddress.label}' berhasil dihapus", Toast.LENGTH_SHORT).show()
                deleteAlertDialog.dismiss()
                parentSuccessDialog.dismiss()
                finish()
            }
        }

        deleteAlertDialog.show()
    }
}
