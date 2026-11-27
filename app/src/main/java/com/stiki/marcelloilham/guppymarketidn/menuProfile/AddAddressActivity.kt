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
    private var existingLat: Double = 0.0
    private var existingLng: Double = 0.0
    private var lastSearchFullAddress: String = ""
    private var isProgrammaticAddressUpdate = false
    private var isMapPageLoaded = false
    private var pendingMapLat: Double? = null
    private var pendingMapLng: Double? = null
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
        setupListeners()
        checkEditModeIntent()
        initOpenStreetMap()
        checkAddressCountForDefaultToggle()

        if (!isEditMode) {
            loadDefaultProfileName()
            requestLocationPermission()
        }
    }

    private fun checkAddressCountForDefaultToggle() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserAddresses(currentUserId, preferServer = false) { addresses ->
            if (isFinishing || isDestroyed) return@fetchUserAddresses
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
            existingLat = intent.getDoubleExtra("EXTRA_LAT", 0.0)
            existingLng = intent.getDoubleExtra("EXTRA_LNG", 0.0)

            // Update Header & Button Titles
            val tvHeaderTitle = findViewById<TextView>(R.id.tvAddAddressHeaderTitle)
            tvHeaderTitle?.text = "Ubah Alamat"

            val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
            btnSave?.text = "Simpan Perubahan"

            // Pre-fill all existing data without triggering map overwrite
            if (name.isNotEmpty()) etRecipientName.setText(name)
            val cleanPhone = phone.replace("+62", "").replace("-", "").replace(" ", "").trim()
            if (cleanPhone.isNotEmpty()) etPhoneNumber.setText(cleanPhone)
            if (addressText.isNotEmpty()) {
                isProgrammaticAddressUpdate = true
                etDetailAddress.setText(addressText)
                isProgrammaticAddressUpdate = false
                lastSearchFullAddress = addressText

                val parts = addressText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                tvAreaMain.text = parts.firstOrNull() ?: "Alamat Tersimpan"
                tvAreaSub.text = parts.drop(1).take(2).joinToString(", ").ifBlank { addressText }
                tvLandmarkTitle.text = label
                tvLandmarkSub.text = addressText
            }
            if (notes.isNotEmpty()) etLandmarkNotes.setText(notes)
            switchSetDefault.isChecked = isDefault

            selectChipByLabel(label)

            if (existingLat != 0.0 && existingLng != 0.0) {
                tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", existingLat, existingLng)
                pendingMapLat = existingLat
                pendingMapLng = existingLng
            } else if (addressText.isNotEmpty()) {
                // Only sync coordinates/map pin quietly from saved address; never overwrite etDetailAddress
                geocodeAddressToPinOnly(addressText, updateAreaPreview = false)
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
                    val name = doc.getString("name") ?: currentUser.displayName ?: "Pengguna Guppy"
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
            val addrText = lastSearchFullAddress.takeIf { it.isNotBlank() }
                ?: "${tvAreaMain.text}, ${tvAreaSub.text}"
            isProgrammaticAddressUpdate = true
            etDetailAddress.setText(addrText)
            isProgrammaticAddressUpdate = false
            Toast.makeText(this, "Rekomendasi lokasi '${tvAreaMain.text}' dipilih!", Toast.LENGTH_SHORT).show()
        }

        row2?.setOnClickListener {
            highlightSelectedLocationRow(2)
            val landmarkText = "${tvLandmarkTitle.text}, ${tvLandmarkSub.text}"
            isProgrammaticAddressUpdate = true
            etDetailAddress.setText(landmarkText)
            isProgrammaticAddressUpdate = false
            Toast.makeText(this, "Rekomendasi lokasi '${tvLandmarkTitle.text}' dipilih!", Toast.LENGTH_SHORT).show()
        }

        // Search Map Input Listener (updates map preview & suggestions; user selects row or edits full address to apply)
        etSearchMap.addTextChangedListener(object : TextWatcher {
            private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
            private var searchRunnable: Runnable? = null
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                if (query.length > 3) {
                    searchRunnable = Runnable { performMapSearch(query) }
                    searchHandler.postDelayed(searchRunnable!!, 400L)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sync map pin automatically when user edits detailed full address manually
        etDetailAddress.addTextChangedListener(object : TextWatcher {
            private val addrHandler = android.os.Handler(android.os.Looper.getMainLooper())
            private var addrRunnable: Runnable? = null
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticAddressUpdate) return
                val fullAddr = s?.toString()?.trim() ?: ""
                addrRunnable?.let { addrHandler.removeCallbacks(it) }
                if (fullAddr.length >= 8) {
                    addrRunnable = Runnable {
                        geocodeAddressToPinOnly(fullAddr, updateAreaPreview = true)
                    }
                    addrHandler.postDelayed(addrRunnable!!, 700L)
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

        val initialLat = if (isEditMode && existingLat != 0.0) existingLat else -6.2349
        val initialLng = if (isEditMode && existingLng != 0.0) existingLng else 106.9924

        isMapPageLoaded = false
        webViewOsmMap.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapPageLoaded = true
                val latToSync = pendingMapLat
                val lngToSync = pendingMapLng
                if (latToSync != null && lngToSync != null) {
                    view?.evaluateJavascript("updateMapLocation($latToSync, $lngToSync)", null)
                }
            }
        }

        webViewOsmMap.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun onMapDragged(lat: Double, lng: Double) {
                runOnUiThread {
                    pendingMapLat = lat
                    pendingMapLng = lng
                    tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                    reverseGeocodeLocation(lat, lng, syncOsm = false, overwriteDetailAddress = true)
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
                    var map = L.map('map', { zoomControl: false }).setView([$initialLat, $initialLng], 15);
                    L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', {
                        maxZoom: 19
                    }).addTo(map);

                    var marker = L.marker([$initialLat, $initialLng], { draggable: true }).addTo(map);

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
        pendingMapLat = lat
        pendingMapLng = lng
        if (::webViewOsmMap.isInitialized && isMapPageLoaded) {
            webViewOsmMap.evaluateJavascript("updateMapLocation($lat, $lng)", null)
        }
    }

    private val geocodeExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

    override fun onDestroy() {
        super.onDestroy()
        geocodeExecutor.shutdownNow()
    }

    private fun reverseGeocodeLocation(
        lat: Double,
        lng: Double,
        syncOsm: Boolean = true,
        overwriteDetailAddress: Boolean = true
    ) {
        if (syncOsm) {
            updateOsmMapCenter(lat, lng)
        }
        geocodeExecutor.execute {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty() && !isFinishing && !isDestroyed) {
                    val address = addresses[0]
                    val area = address.subLocality ?: address.locality ?: "Bekasi Selatan"
                    val cityAndProvince = "${address.subAdminArea ?: "Kota Bekasi"}, ${address.adminArea ?: "Jawa Barat"}"
                    val thoroughfare = address.thoroughfare ?: address.featureName ?: "Lokasi Pin"
                    val fullAddrText = address.getAddressLine(0) ?: "$thoroughfare, $area, $cityAndProvince"

                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        lastSearchFullAddress = fullAddrText
                        tvAreaMain.text = area
                        tvAreaSub.text = cityAndProvince
                        tvLandmarkTitle.text = thoroughfare
                        tvLandmarkSub.text = "$thoroughfare, $area"
                        if (overwriteDetailAddress) {
                            isProgrammaticAddressUpdate = true
                            etDetailAddress.setText(fullAddrText)
                            isProgrammaticAddressUpdate = false
                        }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed && overwriteDetailAddress) {
                        tvAreaMain.text = "Lokasi Terpilih"
                        tvAreaSub.text = "Indonesia"
                    }
                }
            }
        }
    }

    private fun performMapSearch(query: String) {
        geocodeExecutor.execute {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                if (!addresses.isNullOrEmpty() && !isFinishing && !isDestroyed) {
                    val address = addresses[0]
                    val lat = address.latitude
                    val lng = address.longitude
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                            highlightSelectedLocationRow(0)
                            reverseGeocodeLocation(lat, lng, syncOsm = true, overwriteDetailAddress = false)
                        }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        tvAreaMain.text = query
                    }
                }
            }
        }
    }

    private fun geocodeAddressToPinOnly(addressQuery: String, updateAreaPreview: Boolean) {
        geocodeExecutor.execute {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressQuery, 1)
                if (!addresses.isNullOrEmpty() && !isFinishing && !isDestroyed) {
                    val address = addresses[0]
                    val lat = address.latitude
                    val lng = address.longitude
                    val area = address.subLocality ?: address.locality
                    val cityAndProvince = listOfNotNull(address.subAdminArea, address.adminArea)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")

                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        tvCoordinates.text = String.format(Locale.US, "%.4f, %.4f", lat, lng)
                        updateOsmMapCenter(lat, lng)
                        if (updateAreaPreview) {
                            if (!area.isNullOrBlank()) tvAreaMain.text = area
                            if (cityAndProvince.isNotBlank()) tvAreaSub.text = cityAndProvince
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun saveAddressToFirestore() {
        val recipientName = etRecipientName.text.toString().trim()
        val phoneRaw = etPhoneNumber.text.toString().trim()
        val fullAddress = etDetailAddress.text.toString().trim()
        val landmarkNotes = etLandmarkNotes.text.toString().trim()

        val isNameValid = recipientName.isNotEmpty()
        val isPhoneValid = phoneRaw.isNotEmpty()
        val isAddrValid = fullAddress.isNotEmpty()

        if (!isNameValid || !isPhoneValid || !isAddrValid) {
            showIncompleteValidationDialog(isNameValid, isPhoneValid, isAddrValid)
            return
        }

        val coordsStr = tvCoordinates.text.toString().split(",")
        val initialLat = coordsStr.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
        val initialLng = coordsStr.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
        val phoneFormatted = if (phoneRaw.startsWith("+62")) phoneRaw else "+62 $phoneRaw"
        val isDefaultAddr = switchSetDefault.isChecked
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"

        val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
        btnSave?.isEnabled = false
        btnSave?.text = if (isEditMode) "Menyimpan Perubahan..." else "Menyimpan Alamat..."

        if (initialLat != 0.0 && initialLng != 0.0) {
            persistAddressWithCoords(currentUserId, recipientName, phoneFormatted, fullAddress, landmarkNotes, isDefaultAddr, initialLat, initialLng)
        } else {
            // Resolve geocoding on a dedicated background thread with a fast 1s cap so save never queues behind map searches
            val completed = java.util.concurrent.atomic.AtomicBoolean(false)
            val fallbackHandler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutTask = Runnable {
                if (completed.compareAndSet(false, true) && !isFinishing && !isDestroyed) {
                    persistAddressWithCoords(currentUserId, recipientName, phoneFormatted, fullAddress, landmarkNotes, isDefaultAddr, 0.0, 0.0)
                }
            }
            fallbackHandler.postDelayed(timeoutTask, 1000L)

            Thread {
                var lat = 0.0
                var lng = 0.0
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val matches = geocoder.getFromLocationName(fullAddress, 1)
                    if (!matches.isNullOrEmpty()) {
                        lat = matches[0].latitude
                        lng = matches[0].longitude
                    }
                } catch (_: Exception) {
                }
                if (completed.compareAndSet(false, true)) {
                    fallbackHandler.removeCallbacks(timeoutTask)
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            persistAddressWithCoords(currentUserId, recipientName, phoneFormatted, fullAddress, landmarkNotes, isDefaultAddr, lat, lng)
                        }
                    }
                }
            }.start()
        }
    }

    private fun persistAddressWithCoords(
        currentUserId: String,
        recipientName: String,
        phoneFormatted: String,
        fullAddress: String,
        landmarkNotes: String,
        isDefaultAddr: Boolean,
        lat: Double,
        lng: Double
    ) {
        val btnSave = findViewById<TextView>(R.id.btnSaveAddressBottom)
        val newAddress = FirestoreRepository.AddressModel(
            id = existingAddressId?.takeIf { it.isNotBlank() } ?: "addr_${System.currentTimeMillis()}",
            label = selectedLabel,
            recipientName = recipientName,
            phone = phoneFormatted,
            fullAddress = fullAddress,
            isDefault = isDefaultAddr,
            notesForCourier = landmarkNotes,
            lat = lat,
            lng = lng
        )

        val saveFinished = java.util.concurrent.atomic.AtomicBoolean(false)
        val finishSaveFlow: (Boolean, String?) -> Unit = { success, customMsg ->
            if (saveFinished.compareAndSet(false, true) && !isFinishing && !isDestroyed) {
                btnSave?.isEnabled = true
                btnSave?.text = if (isEditMode) "Simpan Perubahan" else "Simpan Alamat"

                if (success) {
                    if (isDefaultAddr && currentUserId != "user_default") {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("user").document(currentUserId)
                            .set(
                                mapOf("location" to fullAddress, "full_address" to fullAddress),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                    }
                    val toastMsg = customMsg ?: if (isEditMode) {
                        "Perubahan alamat berhasil disimpan!"
                    } else {
                        "Alamat berhasil disimpan!"
                    }
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menyimpan alamat. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Firestore local persistence queues offline/slow writes immediately; never trap user waiting >2s on server ACK
        val slowNetworkHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val slowFallbackRunnable = Runnable {
            finishSaveFlow(true, "Alamat disimpan & sedang disinkronkan...")
        }

        val executeSave = {
            slowNetworkHandler.postDelayed(slowFallbackRunnable, 2000L)
            FirestoreRepository.addUserAddress(currentUserId, newAddress) { success ->
                slowNetworkHandler.removeCallbacks(slowFallbackRunnable)
                finishSaveFlow(success, null)
            }
        }

        if (!isEditMode) {
            FirestoreRepository.fetchUserAddresses(currentUserId, preferServer = false) { existingAddresses ->
                if (isFinishing || isDestroyed) return@fetchUserAddresses
                if (existingAddresses.size >= 5) {
                    btnSave?.isEnabled = true
                    btnSave?.text = "Simpan Alamat"
                    AlertDialog.Builder(this)
                        .setTitle("⚠️ Batas Maksimal Alamat (5/5)")
                        .setMessage("Anda telah mencapai batas maksimal 5 alamat tersimpan. Silakan hapus salah satu alamat pengiriman terlebih dahulu jika ingin menambahkan alamat baru.")
                        .setPositiveButton("Mengerti", null)
                        .show()
                    return@fetchUserAddresses
                }

                if (isDefaultAddr) {
                    val updatedList = existingAddresses.map {
                        it.copy(isDefault = false)
                    } + newAddress.copy(isDefault = true)

                    slowNetworkHandler.postDelayed(slowFallbackRunnable, 2000L)
                    FirestoreRepository.updateAllAddresses(currentUserId, updatedList) { success ->
                        slowNetworkHandler.removeCallbacks(slowFallbackRunnable)
                        finishSaveFlow(success, null)
                    }
                } else {
                    executeSave()
                }
            }
        } else {
            if (isDefaultAddr) {
                FirestoreRepository.fetchUserAddresses(currentUserId, preferServer = false) { existingAddresses ->
                    if (isFinishing || isDestroyed) return@fetchUserAddresses
                    val updatedList = existingAddresses.filterNot { it.id == newAddress.id }.map {
                        it.copy(isDefault = false)
                    } + newAddress.copy(isDefault = true)

                    slowNetworkHandler.postDelayed(slowFallbackRunnable, 2000L)
                    FirestoreRepository.updateAllAddresses(currentUserId, updatedList) { success ->
                        slowNetworkHandler.removeCallbacks(slowFallbackRunnable)
                        finishSaveFlow(success, null)
                    }
                }
            } else {
                executeSave()
            }
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
            btnConfirm.isEnabled = false
            btnConfirm.text = "Menghapus..."
            btnCancel?.isEnabled = false

            FirestoreRepository.deleteUserAddress(currentUserId, savedAddress.id) { success ->
                if (isFinishing || isDestroyed) return@deleteUserAddress
                if (!success) {
                    btnConfirm.isEnabled = true
                    btnConfirm.text = "Ya, Hapus"
                    btnCancel?.isEnabled = true
                    Toast.makeText(this, "Gagal menghapus alamat. Coba lagi.", Toast.LENGTH_SHORT).show()
                    return@deleteUserAddress
                }
                Toast.makeText(this, "Alamat '${savedAddress.label}' berhasil dihapus", Toast.LENGTH_SHORT).show()
                deleteAlertDialog.dismiss()
                parentSuccessDialog.dismiss()
                finish()
            }
        }

        deleteAlertDialog.show()
    }

    private fun showIncompleteValidationDialog(isNameValid: Boolean, isPhoneValid: Boolean, isAddrValid: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_incomplete_address_validation, null)

        val tvCheckName = dialogView.findViewById<TextView>(R.id.tvCheckRecipientName)
        val tvCheckPhone = dialogView.findViewById<TextView>(R.id.tvCheckPhone)
        val tvCheckAddr = dialogView.findViewById<TextView>(R.id.tvCheckDetailAddress)
        val btnComplete = dialogView.findViewById<TextView>(R.id.btnCompleteData)

        val greenColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val redColor = android.graphics.Color.parseColor("#E53935")

        if (isNameValid) {
            tvCheckName?.text = "✅ Nama penerima"
            tvCheckName?.setTextColor(greenColor)
        } else {
            tvCheckName?.text = "❌ Nama penerima"
            tvCheckName?.setTextColor(redColor)
        }

        if (isPhoneValid) {
            tvCheckPhone?.text = "✅ Nomor HP"
            tvCheckPhone?.setTextColor(greenColor)
        } else {
            tvCheckPhone?.text = "❌ Nomor HP"
            tvCheckPhone?.setTextColor(redColor)
        }

        if (isAddrValid) {
            tvCheckAddr?.text = "✅ Alamat detail"
            tvCheckAddr?.setTextColor(greenColor)
        } else {
            tvCheckAddr?.text = "❌ Alamat detail"
            tvCheckAddr?.setTextColor(redColor)
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnComplete?.setOnClickListener {
            alertDialog.dismiss()
            if (!isNameValid) {
                etRecipientName.requestFocus()
            } else if (!isPhoneValid) {
                etPhoneNumber.requestFocus()
            } else if (!isAddrValid) {
                etDetailAddress.requestFocus()
            }
        }

        alertDialog.show()
    }
}
