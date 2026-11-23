package com.stiki.marcelloilham.guppymarketidn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.stiki.marcelloilham.guppymarketidn.intro.Intro
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {
    private val SPLASH_TIME_OUT: Long = 3200 // 3.2 detik
    private val prefs by lazy { getSharedPreferences("login_prefs", Context.MODE_PRIVATE) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        val webView = findViewById<WebView>(R.id.webViewSplash)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("file:///android_asset/splash.html")

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = if (FirebaseAuth.getInstance().currentUser != null) {
                Intent(this, MainActivity::class.java).apply {
                    putExtra("navigate_to", "HomeFragment")
                }
            } else {
                Intent(this, Intro::class.java)
            }
            startActivity(intent)
            finish()
        }, SPLASH_TIME_OUT)
    }
}
