package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
            Toast.makeText(this, "Proses checkout berhasil!", Toast.LENGTH_SHORT).show()
        }
    }
}