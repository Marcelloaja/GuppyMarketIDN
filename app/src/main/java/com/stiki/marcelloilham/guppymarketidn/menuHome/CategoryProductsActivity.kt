package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.CategoryProductAdapter

class CategoryProductsActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvCount: TextView
    private lateinit var etSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: CategoryProductAdapter

    private var activeStrain: String = "Semua"
    private var activeQuery: String = ""
    private var activeSort: String = "DEFAULT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_products)

        val topBarCategory = findViewById<View>(R.id.topBarCategory)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBarCategory.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = 0
            }
            topBarCategory.setPadding(
                topBarCategory.paddingLeft,
                systemBars.top + (12 * resources.displayMetrics.density).toInt(),
                topBarCategory.paddingRight,
                topBarCategory.paddingBottom
            )
            insets
        }

        val categoryName = intent.getStringExtra("EXTRA_CATEGORY_NAME") ?: "Kontes"

        tvTitle = findViewById(R.id.tvCategoryPageTitle)
        tvCount = findViewById(R.id.tvCategoryPageCount)
        etSearch = findViewById(R.id.etSearchCategoryPage)
        recycler = findViewById(R.id.recyclerCategoryProducts)
        tvEmpty = findViewById(R.id.tvEmptyCategoryProducts)

        val btnBack = findViewById<ImageView>(R.id.btnBackCategory)
        val btnCart = findViewById<View>(R.id.btnCartCategoryPage)

        tvTitle.text = categoryName
        etSearch.hint = "Cari di $categoryName"

        btnBack.setOnClickListener { finish() }
        btnCart.setOnClickListener {
            startActivity(Intent(this, KeranjangHome::class.java))
        }

        setupSortAndFilterButtons()
        setupProductsGrid()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                activeQuery = s.toString().trim()
                adapter.applyAdvancedFilterAndSort(activeQuery, activeStrain, activeSort)
                updateCountAndEmptyState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupSortAndFilterButtons() {
        val chipPopular = findViewById<TextView>(R.id.chipSortPopular)
        val chipLatest = findViewById<TextView>(R.id.chipSortLatest)
        val chipPrice = findViewById<TextView>(R.id.chipSortPrice)
        val chipFilterGalur = findViewById<TextView>(R.id.chipFilterGalur)
        val btnFilter = findViewById<View>(R.id.btnFilterOptions)

        val sortChips = listOf(chipPopular, chipLatest, chipPrice)

        fun selectSort(selected: TextView, sortType: String) {
            activeSort = sortType
            sortChips.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_chip_outline)
                    chip.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.guppy_primary))
                    chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_primary))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_outline)
                    chip.backgroundTintList = null
                    chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
                }
            }
            adapter.applyAdvancedFilterAndSort(activeQuery, activeStrain, activeSort)
            updateCountAndEmptyState()
        }

        chipPopular.setOnClickListener { selectSort(chipPopular, "POPULAR") }
        chipLatest.setOnClickListener { selectSort(chipLatest, "DEFAULT") }

        var isPriceAscending = true
        chipPrice.setOnClickListener {
            if (isPriceAscending) {
                chipPrice.text = "Harga: Termurah ∨"
                selectSort(chipPrice, "PRICE_LOW")
            } else {
                chipPrice.text = "Harga: Termahal ∧"
                selectSort(chipPrice, "PRICE_HIGH")
            }
            isPriceAscending = !isPriceAscending
        }

        // Dropdown Selection for Strain / Galur
        val strainOptions = arrayOf("Semua", "Mosaic", "Albino", "Tuxedo", "Dragon", "Moscow", "Grass")
        chipFilterGalur?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Galur Varian")
                .setItems(strainOptions) { _, which ->
                    activeStrain = strainOptions[which]
                    chipFilterGalur.text = "Galur: $activeStrain ∨"
                    adapter.applyAdvancedFilterAndSort(activeQuery, activeStrain, activeSort)
                    updateCountAndEmptyState()
                }
                .show()
        }

        btnFilter?.setOnClickListener {
            BottomDialogFilter { sort, _, _, _, _ ->
                activeSort = if (sort == "LowToHigh") "PRICE_LOW" else "PRICE_HIGH"
                adapter.applyAdvancedFilterAndSort(activeQuery, activeStrain, activeSort)
                updateCountAndEmptyState()
            }.show(supportFragmentManager, "BottomDialogFilter")
        }
    }

    private fun setupProductsGrid() {
        val productList = listOf(
            CategoryProductAdapter.CategoryProduct(
                title = "Red Dragon Delta",
                seller = "Bekasi Aqua Farm",
                price = "Rp 275.000",
                unit = "per pasang",
                sold = "120",
                imageRes = R.drawable.bg_image_grid,
                strain = "Dragon"
            ),
            CategoryProductAdapter.CategoryProduct(
                title = "Sky Blue High Dorsal",
                seller = "Jakarta Guppy House",
                price = "Rp 150.000",
                unit = "per ekor",
                sold = "86",
                imageRes = R.drawable.skyblue,
                strain = "Blue"
            ),
            CategoryProductAdapter.CategoryProduct(
                title = "Blue Moscow Full",
                seller = "Depok Tail Studio",
                price = "Rp 90.000",
                unit = "per ekor",
                sold = "54",
                imageRes = R.drawable.gsrts,
                strain = "Moscow"
            ),
            CategoryProductAdapter.CategoryProduct(
                title = "Albino Full White",
                seller = "Bogor Guppy Lab",
                price = "Rp 320.000",
                unit = "per pasang",
                sold = "33",
                imageRes = R.drawable.platinumrd,
                strain = "Albino"
            ),
            CategoryProductAdapter.CategoryProduct(
                title = "Blue Grass Full Red",
                seller = "Jakarta Guppy House",
                price = "Rp 450.000",
                unit = "per ekor",
                sold = "21",
                imageRes = R.drawable.bg_image_grid,
                strain = "Grass"
            ),
            CategoryProductAdapter.CategoryProduct(
                title = "Mosaic Premium",
                seller = "Bekasi Aqua Farm",
                price = "Rp 380.000",
                unit = "per pasang",
                sold = "17",
                imageRes = R.drawable.mssrl,
                strain = "Mosaic"
            )
        )

        adapter = CategoryProductAdapter(this, productList)
        recycler.layoutManager = GridLayoutManager(this, 2)
        recycler.adapter = adapter

        updateCountAndEmptyState()
    }

    private fun updateCountAndEmptyState() {
        val count = adapter.itemCount
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
