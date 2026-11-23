package com.stiki.marcelloilham.guppymarketidn.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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

    private lateinit var bottomSheetFarm: View
    private lateinit var cardSearchContainer: View
    private lateinit var cardNavBanner: View
    private lateinit var tvNavInfoSubtitle: TextView

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

        setupWebView()
        setupMapControls(view)
        setupTTS()
        checkAndRequestLocationPermission()
        return view
    }

    private fun setupTTS() {
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
            }
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GuppyNavTTS")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
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
        }, "AndroidBridge")

        webView.loadUrl("file:///android_asset/maps.html")
    }

    private fun setupMapControls(view: View) {
        val btnZoomIn = view.findViewById<TextView>(R.id.btnZoomIn)
        val btnZoomOut = view.findViewById<TextView>(R.id.btnZoomOut)
        val btnMyLocation = view.findViewById<CardView>(R.id.btnMyLocation)
        val btnStartNavigation = view.findViewById<View>(R.id.btnStartNavigation)
        val btnEndNav = view.findViewById<ImageView>(R.id.btnEndNav)
        val btnStopNavText = view.findViewById<TextView>(R.id.btnStopNavText)

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
