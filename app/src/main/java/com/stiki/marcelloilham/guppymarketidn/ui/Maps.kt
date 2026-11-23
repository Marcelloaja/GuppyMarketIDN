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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.stiki.marcelloilham.guppymarketidn.R

class Maps : Fragment() {
    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)
        webView = view.findViewById(R.id.webViewMaps)
        setupWebView()
        setupMapControls(view)
        checkAndRequestLocationPermission()
        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("file:///android_asset/maps.html")
    }

    private fun setupMapControls(view: View) {
        val btnZoomIn = view.findViewById<TextView>(R.id.btnZoomIn)
        val btnZoomOut = view.findViewById<TextView>(R.id.btnZoomOut)
        val btnMyLocation = view.findViewById<CardView>(R.id.btnMyLocation)

        btnZoomIn?.setOnClickListener {
            webView.evaluateJavascript("javascript:zoomIn()", null)
        }

        btnZoomOut?.setOnClickListener {
            webView.evaluateJavascript("javascript:zoomOut()", null)
        }

        btnMyLocation?.setOnClickListener {
            webView.evaluateJavascript("javascript:centerLocation()", null)
        }
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
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

    private fun checkGpsAndStartTracking() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            showEnableGpsDialog()
        } else {
            startRealtimeLocationTracking()
        }
    }

    private fun showEnableGpsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Aktifkan Lokasi (GPS)")
            .setMessage("Untuk menampilkan titik lokasi Anda dan farm terdekat secara real-time pada peta, silakan aktifkan Lokasi (GPS) di HP Anda.")
            .setPositiveButton("Buka Pengaturan") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Nanti", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startRealtimeLocationTracking() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            // 1. Instantly get last known location so user marker moves immediately
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGps ?: lastNet

            bestLocation?.let {
                sendLocationToMap(it.longitude, it.latitude)
            }

            // 2. Real-time updates listener
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    sendLocationToMap(location.longitude, location.latitude)
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
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
        webView.post {
            webView.evaluateJavascript("javascript:updateUserLocation($lng, $lat)", null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
