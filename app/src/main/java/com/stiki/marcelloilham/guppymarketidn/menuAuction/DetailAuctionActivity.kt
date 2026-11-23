package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.stiki.marcelloilham.guppymarketidn.R

class DetailAuctionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_auction)

        // Fitur Tombol Back (Kembali)
        val btnBack = findViewById<ImageView>(R.id.btnBackDetail)
        btnBack?.setOnClickListener {
            finish()
        }

        // Data dari intent (bisa dihubungkan dengan ID baru nanti jika datanya dinamis)
        val auctionId = intent.getIntExtra("auction_id", -1)
        val auctionTitle = intent.getStringExtra("auction_title")
        val auctionDescription = intent.getStringExtra("auction_description")
    }
}
