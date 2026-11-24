package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stiki.marcelloilham.guppymarketidn.R

class KeranjangHome : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keranjang)

        val btnBack = findViewById<ImageView>(R.id.btnBackKeranjang)
        val btnCheckout = findViewById<TextView>(R.id.btnCheckout)

        btnBack?.setOnClickListener {
            finish()
        }

        btnCheckout?.setOnClickListener {
            val intent = Intent(this, CheckoutActivity::class.java)
            startActivity(intent)
        }
    }
}
