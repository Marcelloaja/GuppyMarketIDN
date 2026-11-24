package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.FarmProductAdapter

class DetailFarmActivity : AppCompatActivity() {

    private lateinit var tabVarian: View
    private lateinit var tabLelang: View
    private lateinit var tabUlasan: View
    private lateinit var tabTentang: View

    private lateinit var tvTabVarian: TextView
    private lateinit var tvTabLelang: TextView
    private lateinit var tvTabUlasan: TextView
    private lateinit var tvTabTentang: TextView

    private lateinit var indicatorVarian: View
    private lateinit var indicatorLelang: View
    private lateinit var indicatorUlasan: View
    private lateinit var indicatorTentang: View

    private lateinit var contentVarian: View
    private lateinit var contentLelang: View
    private lateinit var contentUlasan: View
    private lateinit var contentTentang: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_farm)

        // Top Bar Spacing & Insets Adjustment
        val topBarDetail = findViewById<View>(R.id.topBarDetail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBarDetail.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackDetails)
        btnBack.setOnClickListener {
            finish()
        }

        val btnOpenMaps = findViewById<View>(R.id.btnOpenMaps)
        btnOpenMaps?.setOnClickListener {
            BottomDialogMapChoice(
                farmName = "Jakarta Guppy House",
                address = "Jl. Lebak Bulus Raya No. 42, Cilandak, Jakarta Selatan"
            ).show(supportFragmentManager, "BottomDialogMapChoice")
        }

        val btnChatFarm = findViewById<View>(R.id.btnChatFarm)
        btnChatFarm?.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_STORE_NAME", "Jakarta Guppy House")
            }
            startActivity(intent)
        }

        initTabViews()
        setupTabClickListeners()
        setupFarmProductsRecyclerView()
        setupFollowAndShareButtons()

        // Select Varian Tab by Default
        selectTab(0)
    }

    private var isFollowingFarm: Boolean = false

    private fun setupFollowAndShareButtons() {
        val btnFollow = findViewById<TextView>(R.id.btnFollowFarm)
        val btnShare = findViewById<TextView>(R.id.btnShareFarm)

        btnFollow?.setOnClickListener {
            isFollowingFarm = !isFollowingFarm
            if (isFollowingFarm) {
                btnFollow.text = "Mengikuti ✓"
                btnFollow.setBackgroundResource(R.drawable.bg_chip_selected)
                btnFollow.setTextColor(ContextCompat.getColor(this, R.color.guppy_primary))
                Toast.makeText(this, "Berhasil mengikuti Jakarta Guppy House!", Toast.LENGTH_SHORT).show()
            } else {
                btnFollow.text = "+ Ikuti Farm"
                btnFollow.setBackgroundResource(R.drawable.bg_btn_primary)
                btnFollow.setTextColor(ContextCompat.getColor(this, R.color.white))
                Toast.makeText(this, "Batal mengikuti Jakarta Guppy House", Toast.LENGTH_SHORT).show()
            }
        }

        btnShare?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Jakarta Guppy House - Farm Guppy Terpercaya")
                putExtra(Intent.EXTRA_TEXT, "Kunjungi profil Jakarta Guppy House di GuppyMarket: Koleksi guppy kontes dan indukan berkualitas!")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Farm via"))
        }
    }

    private fun initTabViews() {
        tabVarian = findViewById(R.id.tabVarian)
        tabLelang = findViewById(R.id.tabLelang)
        tabUlasan = findViewById(R.id.tabUlasan)
        tabTentang = findViewById(R.id.tabTentang)

        tvTabVarian = findViewById(R.id.tvTabVarian)
        tvTabLelang = findViewById(R.id.tvTabLelang)
        tvTabUlasan = findViewById(R.id.tvTabUlasan)
        tvTabTentang = findViewById(R.id.tvTabTentang)

        indicatorVarian = findViewById(R.id.indicatorVarian)
        indicatorLelang = findViewById(R.id.indicatorLelang)
        indicatorUlasan = findViewById(R.id.indicatorUlasan)
        indicatorTentang = findViewById(R.id.indicatorTentang)

        contentVarian = findViewById(R.id.contentVarian)
        contentLelang = findViewById(R.id.contentLelang)
        contentUlasan = findViewById(R.id.contentUlasan)
        contentTentang = findViewById(R.id.contentTentang)
    }

    private fun setupTabClickListeners() {
        tabVarian.setOnClickListener { selectTab(0) }
        tabLelang.setOnClickListener { selectTab(1) }
        tabUlasan.setOnClickListener { selectTab(2) }
        tabTentang.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(index: Int) {
        val primaryColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val grayColor = ContextCompat.getColor(this, R.color.guppy_text_gray)
        val strokeColor = ContextCompat.getColor(this, R.color.guppy_stroke)
        val density = resources.displayMetrics.density

        val textViews = listOf(tvTabVarian, tvTabLelang, tvTabUlasan, tvTabTentang)
        val indicators = listOf(indicatorVarian, indicatorLelang, indicatorUlasan, indicatorTentang)
        val contents = listOf(contentVarian, contentLelang, contentUlasan, contentTentang)

        textViews.forEachIndexed { i, tv ->
            if (i == index) {
                tv.setTextColor(primaryColor)
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)
            } else {
                tv.setTextColor(grayColor)
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)
            }
        }

        indicators.forEachIndexed { i, view ->
            val lp = view.layoutParams
            if (i == index) {
                view.setBackgroundColor(primaryColor)
                lp.height = (2.5f * density).toInt()
            } else {
                view.setBackgroundColor(strokeColor)
                lp.height = (1f * density).toInt()
            }
            view.layoutParams = lp
        }

        contents.forEachIndexed { i, view ->
            view.visibility = if (i == index) View.VISIBLE else View.GONE
        }
    }

    private fun setupFarmProductsRecyclerView() {
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
