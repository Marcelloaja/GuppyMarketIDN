package com.stiki.marcelloilham.guppymarketidn.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailFarmActivity
import com.stiki.marcelloilham.guppymarketidn.R
import java.util.Locale

class Maps : Fragment() {
    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private var gpsDialog: AlertDialog? = null
    private var tts: TextToSpeech? = null
    private var isNavigating = false
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val SPEECH_REQUEST_CODE = 1002

    private lateinit var bottomSheetFarm: View
    private lateinit var cardSearchContainer: View
    private lateinit var cardNavBanner: View
    private lateinit var tvNavInfoSubtitle: TextView
    private lateinit var tvUserLocationAddress: TextView

    private lateinit var etSearchHome: EditText
    private lateinit var tvNoFarmFound: TextView
    private lateinit var recyclerMapsFarms: RecyclerView
    private lateinit var tvFarmCountBadge: TextView
    private var realFarmsList: List<FirestoreRepository.FarmModel> = emptyList()

    private var lastGeocodedLat: Double = 0.0
    private var lastGeocodedLng: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)
        webView = view.findViewById(R.id.webViewMaps)
        bottomSheetFarm = view.findViewById(R.id.bottomSheetFarm)
        cardSearchContainer = view.findViewById(R.id.cardSearchContainer)
        cardNavBanner = view.findViewById(R.id.cardNavBanner)
        tvNavInfoSubtitle = view.findViewById(R.id.tvNavInfoSubtitle)
        tvUserLocationAddress = view.findViewById(R.id.tvUserLocationAddress)

        setupWebView()
        setupMapControls(view)
        setupSearchAndVoice(view)
        setupTTS()
        checkAndRequestLocationPermission()

        val btnChangeMapAddress = view.findViewById<View>(R.id.btnChangeMapAddress)
        btnChangeMapAddress?.setOnClickListener {
            val intent = Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuProfile.ShippingAddressActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private lateinit var chipTerdekat: TextView
    private lateinit var chipRating: TextView
    private var activeFilterMode = "TERDEKAT"

    private fun setupSearchAndVoice(view: View) {
        etSearchHome = view.findViewById(R.id.etSearchHome)
        tvNoFarmFound = view.findViewById(R.id.tvNoFarmFound)
        tvFarmCountBadge = view.findViewById(R.id.tvFarmCountBadge)
        recyclerMapsFarms = view.findViewById(R.id.recyclerMapsFarms)

        chipTerdekat = view.findViewById(R.id.chipMapsTerdekat)
        chipRating = view.findViewById(R.id.chipMapsRating)

        chipTerdekat?.setOnClickListener {
            activeFilterMode = "TERDEKAT"
            updateFilterTabStyle()
            filterFarms(etSearchHome.text.toString())
        }

        chipRating?.setOnClickListener {
            activeFilterMode = "RATING"
            updateFilterTabStyle()
            filterFarms(etSearchHome.text.toString())
        }

        etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterFarms(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateFilterTabStyle() {
        if (::chipTerdekat.isInitialized && ::chipRating.isInitialized) {
            if (activeFilterMode == "TERDEKAT") {
                chipTerdekat.setBackgroundResource(R.drawable.bg_btn_primary)
                chipTerdekat.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                chipTerdekat.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                chipRating.setBackgroundResource(R.drawable.bg_btn_solid_gray)
                chipRating.backgroundTintList = null
                chipRating.setTextColor(ContextCompat.getColor(requireContext(), R.color.guppy_text_black))
            } else {
                chipRating.setBackgroundResource(R.drawable.bg_btn_primary)
                chipRating.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                chipRating.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

                chipTerdekat.setBackgroundResource(R.drawable.bg_btn_solid_gray)
                chipTerdekat.backgroundTintList = null
                chipTerdekat.setTextColor(ContextCompat.getColor(requireContext(), R.color.guppy_text_black))
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara nama farm guppy...")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, SPEECH_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Voice input tidak didukung di HP ini", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                etSearchHome.setText(spokenText)
                filterFarms(spokenText)
            }
        }
    }

    private fun filterFarms(query: String) {
        val cleanQuery = query.trim().lowercase()

        var filtered = if (cleanQuery.isEmpty()) {
            realFarmsList
        } else {
            realFarmsList.filter {
                it.farmName.lowercase().contains(cleanQuery) || it.location.lowercase().contains(cleanQuery)
            }
        }

        filtered = if (activeFilterMode == "RATING") {
            filtered.sortedByDescending { it.rating }
        } else {
            filtered.sortedBy { farm ->
                farm.distanceKm.replace("km", "").replace(",", ".").trim().toDoubleOrNull() ?: 1.0
            }
        }

        if (::tvFarmCountBadge.isInitialized) {
            tvFarmCountBadge.text = "${filtered.size} Farm"
        }

        if (filtered.isEmpty()) {
            recyclerMapsFarms.visibility = View.GONE
            tvNoFarmFound.text = if (cleanQuery.isNotEmpty()) "Farm '$query' tidak ditemukan.\nCoba kata kunci lokasi lain." else "Belum ada Farm / Gudang Ikan terdaftar."
            tvNoFarmFound.visibility = View.VISIBLE
        } else {
            recyclerMapsFarms.visibility = View.VISIBLE
            tvNoFarmFound.visibility = View.GONE

            val adapter = com.stiki.marcelloilham.guppymarketidn.adapter.HomeFarmAdapter(
                context = requireContext(),
                farms = filtered,
                onVisitClicked = { clickedFarm ->
                    val intent = Intent(requireContext(), DetailFarmActivity::class.java).apply {
                        putExtra("farm_title", clickedFarm.farmName)
                        putExtra("farm_location", clickedFarm.location)
                        putExtra("farm_rating", clickedFarm.rating.toString())
                    }
                    startActivity(intent)
                }
            )
            recyclerMapsFarms.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            recyclerMapsFarms.adapter = adapter
        }
    }

    private fun setupTTS() {
        val appContext = context?.applicationContext ?: return
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val idLocale = Locale("id", "ID")
                val langResult = tts?.setLanguage(idLocale)

                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("MapsTTS", "Indonesian voice data missing, falling back to default locale.")
                    tts?.language = Locale.getDefault()
                }

                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                tts?.setAudioAttributes(audioAttributes)
            } else {
                Log.e("MapsTTS", "TextToSpeech init failed with code: $status")
            }
        }
    }

    private fun speakText(text: String) {
        if (tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GuppyNavTTS")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                checkAndFocusFarmQuery()
                loadFirestoreFarmMarkers()
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Bridge JS for Dynamic Realtime Distance & ETA Updates
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onRouteUpdated(distance: String, duration: String) {
                activity?.runOnUiThread {
                    if (isAdded && ::tvNavInfoSubtitle.isInitialized) {
                        tvNavInfoSubtitle.text = "$distance · Estimasi $duration"
                    }
                }
            }

            @JavascriptInterface
            fun onFarmMarkerClicked(id: String, name: String, lat: Double, lng: Double) {
                activity?.runOnUiThread {
                    if (isAdded) {
                        showFarmDetailModal(id, name, lat, lng)
                    }
                }
            }
        }, "AndroidBridge")

        webView.loadUrl("file:///android_asset/maps.html")
    }

    private fun showFarmDetailModal(id: String, farmName: String, lat: Double, lng: Double) {
        val safeContext = context ?: return
        val dialogView = LayoutInflater.from(safeContext).inflate(R.layout.dialog_farm_marker_detail, null)

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(safeContext)
        dialog.setContentView(dialogView)

        val realFarm = realFarmsList.find { it.id == id || it.farmName.equals(farmName, ignoreCase = true) }

        val actualName = realFarm?.farmName ?: farmName
        val actualLocation = realFarm?.location ?: "Lokasi Farm Toko"
        val actualRating = realFarm?.rating ?: 4.9
        val actualVariants = realFarm?.variantsCount ?: 12

        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val isOpenStatus = currentHour in 8..21
        val openStatusText = if (isOpenStatus) "● Buka · Tutup 21.00" else "● Tutup · Buka 08.00"

        val tvInitials = dialogView.findViewById<TextView>(R.id.tvDialogFarmInitials)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogFarmName)
        val tvLocation = dialogView.findViewById<TextView>(R.id.tvDialogFarmLocation)
        val tvSubtext = dialogView.findViewById<TextView>(R.id.tvDialogFarmSubtext)
        val btnNavigate = dialogView.findViewById<TextView>(R.id.btnDialogNavigate)
        val btnVisitStore = dialogView.findViewById<TextView>(R.id.btnDialogVisitStore)
        val btnClose = dialogView.findViewById<TextView>(R.id.btnDialogClose)

        val initials = actualName.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()

        tvInitials?.text = initials.ifEmpty { "GF" }
        tvName?.text = actualName
        tvLocation?.text = "$actualLocation · Gudang Ikan Resmi"
        val formattedRating = String.format(java.util.Locale.US, "%.1f", actualRating)
        tvSubtext?.text = "⭐ $formattedRating · $actualVariants varian aktif · $openStatusText"

        btnNavigate?.setOnClickListener {
            dialog.dismiss()
            enterNavigationMode()
            speakText("Navigasi ke $actualName dimulai.")
        }

        btnVisitStore?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(safeContext, DetailFarmActivity::class.java).apply {
                putExtra("farm_title", actualName)
                putExtra("farm_location", actualLocation)
                putExtra("farm_rating", actualRating.toString())
                putExtra("farm_id", id)
            }
            startActivity(intent)
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadFirestoreFarmMarkers() {
        FirestoreRepository.fetchRealFarms { farms ->
            realFarmsList = farms
            if (isAdded) {
                activity?.runOnUiThread {
                    if (::tvFarmCountBadge.isInitialized) {
                        tvFarmCountBadge.text = "${farms.size} Farm"
                    }

                    filterFarms(etSearchHome.text.toString())

                    farms.forEach { farm ->
                        val escapedName = farm.farmName.replace("'", "\\'")
                        webView.evaluateJavascript("javascript:addRealFarmMarker('${farm.id}', '$escapedName', ${farm.lat}, ${farm.lng})", null)
                    }
                }
            }
        }
    }

    private fun checkAndFocusFarmQuery() {
        val farmQuery = activity?.intent?.getStringExtra("SEARCH_FARM_QUERY")
        if (!farmQuery.isNullOrEmpty()) {
            webView.evaluateJavascript("javascript:focusFarmByName('$farmQuery')", null)
            activity?.intent?.removeExtra("SEARCH_FARM_QUERY")
        }
    }

    private fun setupMapControls(view: View) {
        val btnZoomIn = view.findViewById<TextView>(R.id.btnZoomIn)
        val btnZoomOut = view.findViewById<TextView>(R.id.btnZoomOut)
        val btnMyLocation = view.findViewById<CardView>(R.id.btnMyLocation)
        val btnStartNavigation: View? = null
        val btnEndNav = view.findViewById<ImageView>(R.id.btnEndNav)
        val btnStopNavText = view.findViewById<TextView>(R.id.btnStopNavText)

        val chipCar = view.findViewById<View>(R.id.chipModeCar)
        val icCar = view.findViewById<ImageView>(R.id.icModeCar)
        val tvCar = view.findViewById<TextView>(R.id.tvModeCar)

        val chipMotor = view.findViewById<View>(R.id.chipModeMotor)
        val icMotor = view.findViewById<ImageView>(R.id.icModeMotor)
        val tvMotor = view.findViewById<TextView>(R.id.tvModeMotor)

        val chipWalk = view.findViewById<View>(R.id.chipModeWalk)
        val icWalk = view.findViewById<ImageView>(R.id.icModeWalk)
        val tvWalk = view.findViewById<TextView>(R.id.tvModeWalk)

        btnZoomIn?.setOnClickListener {
            webView.evaluateJavascript("javascript:zoomIn()", null)
        }

        btnZoomOut?.setOnClickListener {
            webView.evaluateJavascript("javascript:zoomOut()", null)
        }

        btnMyLocation?.setOnClickListener {
            webView.evaluateJavascript("javascript:centerLocation()", null)
        }

        btnStartNavigation?.setOnClickListener {
            toggleFullNavigationMode()
        }

        btnEndNav?.setOnClickListener {
            exitNavigationMode()
        }

        btnStopNavText?.setOnClickListener {
            exitNavigationMode()
        }

        chipCar?.setOnClickListener {
            selectVehicleMode(
                "driving",
                chipCar, icCar, tvCar,
                listOf(Triple(chipMotor, icMotor, tvMotor), Triple(chipWalk, icWalk, tvWalk))
            )
            speakText("Rute mobil diaktifkan.")
        }

        chipMotor?.setOnClickListener {
            selectVehicleMode(
                "bike",
                chipMotor, icMotor, tvMotor,
                listOf(Triple(chipCar, icCar, tvCar), Triple(chipWalk, icWalk, tvWalk))
            )
            speakText("Rute motor diaktifkan.")
        }

        chipWalk?.setOnClickListener {
            selectVehicleMode(
                "foot",
                chipWalk, icWalk, tvWalk,
                listOf(Triple(chipCar, icCar, tvCar), Triple(chipMotor, icMotor, tvMotor))
            )
            speakText("Rute jalan kaki diaktifkan.")
        }
    }

    private fun selectVehicleMode(
        profile: String,
        selectedLayout: View?,
        selectedIcon: ImageView?,
        selectedText: TextView?,
        otherItems: List<Triple<View?, ImageView?, TextView?>>
    ) {
        selectedLayout?.setBackgroundResource(R.drawable.bg_vehicle_active_pill)
        selectedIcon?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.guppy_primary))
        selectedText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.guppy_primary))

        otherItems.forEach { (layout, icon, text) ->
            layout?.setBackgroundResource(android.R.color.transparent)
            icon?.setColorFilter(android.graphics.Color.parseColor("#D0E8DD"))
            text?.setTextColor(android.graphics.Color.parseColor("#D0E8DD"))
        }

        webView.evaluateJavascript("javascript:setRouteProfile('$profile')", null)
    }

    private fun toggleFullNavigationMode() {
        if (!isNavigating) {
            enterNavigationMode()
        } else {
            exitNavigationMode()
        }
    }

    private fun enterNavigationMode() {
        isNavigating = true

        // Hide Bottom Sheet & Search Bar to make map 100% full screen
        bottomSheetFarm.visibility = View.GONE
        cardSearchContainer.visibility = View.GONE
        cardNavBanner.visibility = View.VISIBLE

        // Hide Floating Bottom Navigation Bar in MainActivity
        activity?.findViewById<View>(R.id.cardBottomNav)?.visibility = View.GONE

        // Trigger MapLibre 3D Turn-by-Turn Navigation
        webView.evaluateJavascript("javascript:startNavigation()", null)

        // Indonesian Voice Announcement
        speakText("Navigasi dimulai. Ikuti rute menuju Guppy King Farm.")
    }

    private fun exitNavigationMode() {
        isNavigating = false

        // Show Bottom Sheet & Search Bar
        bottomSheetFarm.visibility = View.VISIBLE
        cardSearchContainer.visibility = View.VISIBLE
        cardNavBanner.visibility = View.GONE

        // Restore Floating Bottom Navigation Bar in MainActivity
        activity?.findViewById<View>(R.id.cardBottomNav)?.visibility = View.VISIBLE

        // Reset MapLibre camera & clear route line
        webView.evaluateJavascript("javascript:stopNavigation()", null)

        // Indonesian Voice Announcement
        speakText("Navigasi dihentikan.")
    }

    private fun checkAndRequestLocationPermission() {
        if (!hasLocationPermission()) {
            @Suppress("DEPRECATION")
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkGpsAndStartTracking()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkGpsAndStartTracking()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            checkGpsAndStartTracking()
        }
        checkAndFocusFarmQuery()
    }

    private fun checkGpsAndStartTracking() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            showForceEnableGpsDialog()
        } else {
            gpsDialog?.dismiss()
            startRealtimeLocationTracking()
        }
    }

    private fun showForceEnableGpsDialog() {
        if (gpsDialog?.isShowing == true) return

        gpsDialog = AlertDialog.Builder(requireContext())
            .setTitle("Lokasi (GPS) Wajib Aktif")
            .setMessage("Untuk mengakses fitur Maps dan menemukan lokasi farm terdekat di sekitar Anda, Anda wajib mengaktifkan Pengaturan Lokasi (GPS) di HP Anda.")
            .setCancelable(false)
            .setPositiveButton("Buka Pengaturan HP") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startRealtimeLocationTracking() {
        if (hasLocationPermission()) {

            // 1. Instantly get last known location
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGps ?: lastNet

            bestLocation?.let {
                sendLocationToMap(it.longitude, it.latitude)
            }

            // 2. Continuous Real-Time Updates
            if (locationListener == null) {
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        sendLocationToMap(location.longitude, location.latitude)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    3f,
                    locationListener!!
                )
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    3f,
                    locationListener!!
                )
            }
        }
    }

    private fun sendLocationToMap(lng: Double, lat: Double) {
        if (lat == 0.0 || lng == 0.0) return
        webView.post {
            webView.evaluateJavascript("javascript:updateUserLocation($lng, $lat)", null)
        }
        updateAddressFromCoordinates(lat, lng)
    }

    private fun updateAddressFromCoordinates(lat: Double, lng: Double) {
        // Run geocoder only if moved > ~50 meters to save resources
        if (Math.abs(lat - lastGeocodedLat) < 0.0005 && Math.abs(lng - lastGeocodedLng) < 0.0005) return

        lastGeocodedLat = lat
        lastGeocodedLng = lng

        Thread {
            try {
                val safeContext = context ?: return@Thread
                val geocoder = Geocoder(safeContext, Locale("id", "ID"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0]
                            val subLoc = addr.subLocality ?: addr.locality ?: addr.subAdminArea ?: "Lokasi Anda"
                            val city = addr.subAdminArea ?: addr.adminArea ?: ""
                            val formattedText = "Lokasi Anda · $subLoc, $city"
                            activity?.runOnUiThread {
                                if (isAdded && ::tvUserLocationAddress.isInitialized) {
                                    tvUserLocationAddress.text = formattedText
                                }
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val subLoc = addr.subLocality ?: addr.locality ?: addr.subAdminArea ?: "Lokasi Anda"
                        val city = addr.subAdminArea ?: addr.adminArea ?: ""
                        val formattedText = "Lokasi Anda · $subLoc, $city"
                        activity?.runOnUiThread {
                            if (isAdded && ::tvUserLocationAddress.isInitialized) {
                                tvUserLocationAddress.text = formattedText
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore Bottom Nav when leaving maps fragment
        activity?.findViewById<View>(R.id.cardBottomNav)?.visibility = View.VISIBLE
        gpsDialog?.dismiss()
        tts?.stop()
        tts?.shutdown()
        locationListener?.let {
            if (::locationManager.isInitialized) {
                locationManager.removeUpdates(it)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) = Maps()
    }
}
