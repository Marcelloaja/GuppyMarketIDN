package com.stiki.marcelloilham.guppymarketidn.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.stiki.marcelloilham.guppymarketidn.R

class Maps : Fragment() {
    private lateinit var webView: WebView
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)
        webView = view.findViewById(R.id.webViewMaps)
        setupWebView()
        setupMapControls(view)
        requestLocationPermission()
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

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) = Maps()
    }
}
