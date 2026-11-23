package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.GridAdapter

class CategoryProductsActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvCount: TextView
    private lateinit var etSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var gridAdapter: GridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_products)

        val categoryName = intent.getStringExtra("EXTRA_CATEGORY_NAME") ?: "Kategori"

        tvTitle = findViewById(R.id.tvCategoryPageTitle)
        tvCount = findViewById(R.id.tvCategoryPageCount)
        etSearch = findViewById(R.id.etSearchCategoryPage)
        recycler = findViewById(R.id.recyclerCategoryProducts)
        tvEmpty = findViewById(R.id.tvEmptyCategoryProducts)

        val btnBack = findViewById<ImageView>(R.id.btnBackCategory)
        val btnCart = findViewById<ImageView>(R.id.btnCartCategoryPage)

        btnBack.setOnClickListener { finish() }
        btnCart.setOnClickListener {
            startActivity(Intent(this, KeranjangHome::class.java))
        }

        tvTitle.text = categoryName

        setupProductsGrid(categoryName)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (::gridAdapter.isInitialized) {
                    gridAdapter.searchItems(s.toString())
                    updateCountAndEmptyState()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupProductsGrid(categoryName: String) {
        val allItems = listOf(
            Item(R.drawable.skyblue, "Sky Blue", "Ikan Kontes", "Rp. 75.000"),
            Item(R.drawable.gsrts, "GSRTS", "Indukan Breeder", "Rp. 100.000"),
            Item(R.drawable.platinumrd, "Platinum RD", "Ikan Kontes", "Rp. 125.000"),
            Item(R.drawable.mssrl, "MSSRL", "Indukan Breeder", "Rp. 95.000"),
            Item(R.drawable.bestseller1, "Pelet Artemia High Protein", "Pakan Ikan", "Rp. 35.000"),
            Item(R.drawable.skyblue, "Obat Biru & Anti Jamur", "Obat & Vitamin", "Rp. 25.000"),
            Item(R.drawable.gsrts, "Solak Aquarium 12cm", "Sol & Akrilik", "Rp. 45.000"),
            Item(R.drawable.platinumrd, "Serokan Halus Guppy", "Aksesoris", "Rp. 15.000")
        )

        val filteredList = if (categoryName.equals("Semua", ignoreCase = true)) {
            allItems
        } else {
            allItems.filter {
                it.type.lowercase().contains(categoryName.lowercase()) ||
                it.name.lowercase().contains(categoryName.lowercase())
            }
        }

        gridAdapter = GridAdapter(this, filteredList.ifEmpty { allItems })
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = gridAdapter

        gridAdapter.setOnItemClickListener(object : GridAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val item = gridAdapter.getItem(position)
                val intent = Intent(this@CategoryProductsActivity, DetailActivity::class.java).apply {
                    putExtra("ITEM_NAME", item.name)
                    putExtra("ITEM_IMAGE", item.image)
                }
                startActivity(intent)
            }
        })

        updateCountAndEmptyState()
    }

    private fun updateCountAndEmptyState() {
        val count = gridAdapter.itemCount
        tvCount.text = "$count produk"
        if (count == 0) {
            recycler.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }
}