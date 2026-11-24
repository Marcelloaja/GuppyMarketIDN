package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private var isNavigating = false

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
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(farmName: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("javascript:focusFarmByName('$farmName')", null)
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.loadUrl("file:///android_asset/maps.html")
    }
}
