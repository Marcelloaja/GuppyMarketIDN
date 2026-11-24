package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.ArticleAdapter

class DetailArticleActivity : AppCompatActivity() {

    private lateinit var tvCategoryTag: TextView
    private lateinit var tvTitleHeader: TextView
    private lateinit var tvMetaHeader: TextView
    private lateinit var imgHero: ImageView
    private lateinit var recyclerRelated: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_article)

        val topBar = findViewById<View>(R.id.topBarDetailArticle)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackDetailArticle)
        val btnBookmarkTop = findViewById<ImageView>(R.id.btnBookmarkTopArticle)
        val btnBookmarkBottom = findViewById<ImageView>(R.id.btnBookmarkBottom)
        val btnShare = findViewById<View>(R.id.btnShareArticle)
        val btnViewAllRelated = findViewById<TextView>(R.id.btnViewAllRelated)

        btnBack.setOnClickListener { finish() }

        btnBookmarkTop?.setOnClickListener {
            Toast.makeText(this, "Artikel disimpan ke favorit", Toast.LENGTH_SHORT).show()
        }

        btnBookmarkBottom?.setOnClickListener {
            Toast.makeText(this, "Artikel disimpan ke favorit", Toast.LENGTH_SHORT).show()
        }

        btnShare?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Cara menjaga kualitas air untuk guppy kontes")
                putExtra(Intent.EXTRA_TEXT, "Baca artikel menarik ini di GuppyMarket: Cara menjaga kualitas air untuk guppy kontes!")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Artikel via"))
        }

        btnViewAllRelated?.setOnClickListener {
            finish()
        }

        tvCategoryTag = findViewById(R.id.tvArticleCategoryTag)
        tvTitleHeader = findViewById(R.id.tvArticleTitleHeader)
        tvMetaHeader = findViewById(R.id.tvArticleMetaHeader)
        imgHero = findViewById(R.id.imgArticleHero)
        recyclerRelated = findViewById(R.id.recyclerRelatedArticles)

        val title = intent.getStringExtra("EXTRA_ARTICLE_TITLE") ?: "Cara menjaga kualitas air untuk guppy kontes"
        val category = intent.getStringExtra("EXTRA_ARTICLE_CATEGORY") ?: "KUALITAS AIR"
        val imageRes = intent.getIntExtra("EXTRA_ARTICLE_IMAGE", R.drawable.skyblue)

        tvTitleHeader.text = title
        tvCategoryTag.text = category.uppercase()
        if (imageRes != 0) {
            imgHero.setImageResource(imageRes)
        }

        setupRelatedArticles()
    }

    private fun setupRelatedArticles() {
        val relatedList = listOf(
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
            )
        )

        val adapter = ArticleAdapter(this, relatedList)
        recyclerRelated.layoutManager = LinearLayoutManager(this)
        recyclerRelated.adapter = adapter
    }
}
