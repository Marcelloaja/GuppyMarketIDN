package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.stiki.marcelloilham.guppymarketidn.R

class FarmMapActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvTitle: TextView
    private lateinit var tvCardName: TextView
    private lateinit var tvCardAddress: TextView
    private lateinit var btnStartNav: TextView

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private var isNavigating = false
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_farm_map)

        val topBar = findViewById<View>(R.id.topBarFarmMap)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val farmName = intent.getStringExtra("EXTRA_FARM_NAME") ?: "Jakarta Guppy House"
        val farmAddress = intent.getStringExtra("EXTRA_FARM_ADDRESS") ?: "Jl. Lebak Bulus Raya No. 42, Jakarta"

        tvTitle = findViewById(R.id.tvFarmMapTitle)
        tvCardName = findViewById(R.id.tvFarmMapCardName)
        tvCardAddress = findViewById(R.id.tvFarmMapCardAddress)
        webView = findViewById(R.id.webViewFarmMap)

        val btnBack = findViewById<ImageView>(R.id.btnBackFarmMap)
        btnStartNav = findViewById(R.id.btnFarmMapStartNav)

        tvTitle.text = farmName
        tvCardName.text = farmName
        tvCardAddress.text = farmAddress

        btnBack.setOnClickListener { finish() }

        btnStartNav.setOnClickListener {
            if (isNavigating) {
                webView.evaluateJavascript("javascript:stopNavigation()", null)
                btnStartNav.text = "Mulai Navigasi Rute"
                isNavigating = false
            } else {
                webView.evaluateJavascript("javascript:startNavigation()", null)
                btnStartNav.text = "Hentikan Navigasi"
                isNavigating = true
            }
        }

        setupWebView(farmName)
        checkAndRequestLocationPermission()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(farmName: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("javascript:focusFarmByName('$farmName')", null)
                startRealtimeLocationTracking()
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.loadUrl("file:///android_asset/maps.html")
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealtimeLocationTracking() {
        if (!hasLocationPermission()) return

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 1. Get last known location instantly
        val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val bestLocation = lastGps ?: lastNet

        bestLocation?.let {
            sendLocationToMap(it.longitude, it.latitude)
        }

        // 2. Real-time GPS Listener
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
                2f,
                locationListener!!
            )
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                2000L,
                2f,
                locationListener!!
            )
        }
    }

    private fun sendLocationToMap(lng: Double, lat: Double) {
        if (lat == 0.0 || lng == 0.0) return
        webView.post {
            webView.evaluateJavascript("javascript:updateUserLocation($lng, $lat)", null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            startRealtimeLocationTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::locationManager.isInitialized && locationListener != null) {
            locationManager.removeUpdates(locationListener!!)
        }
    }
}
