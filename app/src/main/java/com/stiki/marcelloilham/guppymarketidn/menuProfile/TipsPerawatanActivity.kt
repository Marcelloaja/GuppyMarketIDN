package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.ArticleAdapter

class TipsPerawatanActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ArticleAdapter

    private var activeCategory: String = "Semua"
    private var activeQuery: String = ""

    private lateinit var tabAll: View
    private lateinit var tabWater: View
    private lateinit var tabNutrition: View
    private lateinit var tabBreeding: View

    private lateinit var tvTabAll: TextView
    private lateinit var tvTabWater: TextView
    private lateinit var tvTabNutrition: TextView
    private lateinit var tvTabBreeding: TextView

    private lateinit var indicatorAll: View
    private lateinit var indicatorWater: View
    private lateinit var indicatorNutrition: View
    private lateinit var indicatorBreeding: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips_perawatan)

        val topBar = findViewById<View>(R.id.topBarTips)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackTips)
        val btnBookmarks = findViewById<ImageView>(R.id.btnSavedBookmarks)
        val btnBookmarkHero = findViewById<ImageView>(R.id.btnBookmarkHero)
        val cardHero = findViewById<View>(R.id.cardHeroArticle)

        btnBack.setOnClickListener { finish() }
        btnBookmarks.setOnClickListener {
            Toast.makeText(this, "Artikel tersimpan dibuka", Toast.LENGTH_SHORT).show()
        }
        btnBookmarkHero?.setOnClickListener {
            Toast.makeText(this, "Artikel disimpan ke favorit", Toast.LENGTH_SHORT).show()
        }
        cardHero?.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java).apply {
                putExtra("url", "https://drive.google.com/file/d/1AwopxNnAjbdJPYIPj43WU6jLmaz2ExpL/view?usp=drive_link")
            }
            startActivity(intent)
        }

        etSearch = findViewById(R.id.etSearchTips)
        recycler = findViewById(R.id.recyclerLatestArticles)

        initTabs()
        setupTabClickListeners()
        setupArticlesList()

        selectTab(0)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                activeQuery = s.toString().trim()
                adapter.filterByQueryAndCategory(activeQuery, activeCategory)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initTabs() {
        tabAll = findViewById(R.id.tabTipsAll)
        tabWater = findViewById(R.id.tabTipsWater)
        tabNutrition = findViewById(R.id.tabTipsNutrition)
        tabBreeding = findViewById(R.id.tabTipsBreeding)

        tvTabAll = findViewById(R.id.tvTabTipsAll)
        tvTabWater = findViewById(R.id.tvTabTipsWater)
        tvTabNutrition = findViewById(R.id.tvTabTipsNutrition)
        tvTabBreeding = findViewById(R.id.tvTabTipsBreeding)

        indicatorAll = findViewById(R.id.indicatorTipsAll)
        indicatorWater = findViewById(R.id.indicatorTipsWater)
        indicatorNutrition = findViewById(R.id.indicatorTipsNutrition)
        indicatorBreeding = findViewById(R.id.indicatorTipsBreeding)
    }

    private fun setupTabClickListeners() {
        tabAll.setOnClickListener { selectTab(0) }
        tabWater.setOnClickListener { selectTab(1) }
        tabNutrition.setOnClickListener { selectTab(2) }
        tabBreeding.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(index: Int) {
        val primaryColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val grayColor = ContextCompat.getColor(this, R.color.guppy_text_gray)
        val strokeColor = ContextCompat.getColor(this, R.color.guppy_stroke)
        val density = resources.displayMetrics.density

        val categories = listOf("Semua", "Kualitas Air", "Nutrisi", "Breeding")
        val textViews = listOf(tvTabAll, tvTabWater, tvTabNutrition, tvTabBreeding)
        val indicators = listOf(indicatorAll, indicatorWater, indicatorNutrition, indicatorBreeding)

        activeCategory = categories[index]

        textViews.forEachIndexed { i, tv ->
            if (i == index) {
                tv.setTextColor(primaryColor)
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)
            } else {
                tv.setTextColor(grayColor)
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)
            }
        }

        indicators.forEachIndexed { i, v ->
            val lp = v.layoutParams
            if (i == index) {
                v.setBackgroundColor(primaryColor)
                lp.height = (2.5f * density).toInt()
            } else {
                v.setBackgroundColor(strokeColor)
                lp.height = (1f * density).toInt()
            }
            v.layoutParams = lp
        }

        if (::adapter.isInitialized) {
            adapter.filterByQueryAndCategory(activeQuery, activeCategory)
        }
    }

    private fun setupArticlesList() {
        val articleList = listOf(
            ArticleAdapter.Article(
                title = "Pakan terbaik agar warna ekor makin tajam",
                category = "NUTRISI",
                duration = "3 menit baca",
                imageRes = R.drawable.bestseller1,
                categoryColorHex = "#E65100"
            ),
            ArticleAdapter.Article(
                title = "Panduan memilih indukan untuk breeding pertama",
                category = "BREEDING",
                duration = "5 menit baca",
                imageRes = R.drawable.bg_image_grid,
                categoryColorHex = "#2E7D32"
            ),
            ArticleAdapter.Article(
                title = "Mengenali gejala white spot pada guppy",
                category = "PENYAKIT",
                duration = "4 menit baca",
                imageRes = R.drawable.platinumrd,
                categoryColorHex = "#D32F2F"
            ),
            ArticleAdapter.Article(
                title = "Suhu ideal akuarium untuk guppy dewasa",
                category = "KUALITAS AIR",
                duration = "3 menit baca",
                imageRes = R.drawable.gsrts,
                categoryColorHex = "#0288D1"
            )
        )

        adapter = ArticleAdapter(this, articleList)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }
}
