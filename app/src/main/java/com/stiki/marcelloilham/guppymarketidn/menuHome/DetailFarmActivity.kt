package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.FarmProductAdapter

class DetailFarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_farm)

        val btnBack = findViewById<ImageView>(R.id.btnBackDetails)
        btnBack.setOnClickListener {
            finish()
        }

        val recyclerFarmProducts = findViewById<RecyclerView>(R.id.recyclerFarmProducts)
        recyclerFarmProducts.layoutManager = GridLayoutManager(this, 2)

        val productList = listOf(
            FarmProductAdapter.FarmProduct(
                title = "Sky Blue High Dorsal",
                grade = "Grade A",
                price = "Rp 150.000",
                unit = "per ekor",
                imageRes = R.drawable.bg_image_grid
            ),
            FarmProductAdapter.FarmProduct(
                title = "Red Dragon Delta",
                grade = "Grade A+",
                price = "Rp 275.000",
                unit = "per pasang",
                imageRes = R.drawable.skyblue
            ),
            FarmProductAdapter.FarmProduct(
                title = "Blue Moscow Full",
                grade = "Grade B",
                price = "Rp 90.000",
                unit = "per ekor",
                imageRes = R.drawable.gsrts
            ),
            FarmProductAdapter.FarmProduct(
                title = "Albino Full White",
                grade = "Grade S",
                price = "Rp 320.000",
                unit = "per pasang",
                imageRes = R.drawable.platinumrd
            )
        )

        val adapter = FarmProductAdapter(this, productList)
        recyclerFarmProducts.adapter = adapter
        recyclerFarmProducts.isNestedScrollingEnabled = false
    }
}
