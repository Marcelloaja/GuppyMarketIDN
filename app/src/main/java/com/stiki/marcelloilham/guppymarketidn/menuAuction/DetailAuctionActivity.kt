package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.stiki.marcelloilham.guppymarketidn.R

class DetailAuctionActivity : AppCompatActivity() {

    private lateinit var ivMainGallery: ImageView
    private lateinit var vvMainGallery: VideoView
    private lateinit var btnPlayVideoOverlay: ImageView
    private lateinit var tvPhotoCount: TextView

    private lateinit var thumbVideo: ShapeableImageView
    private lateinit var thumbImg1: ShapeableImageView
    private lateinit var thumbImg2: ShapeableImageView
    private lateinit var thumbImg3: ShapeableImageView

    private val slideHandler = Handler(Looper.getMainLooper())
    private var slideRunnable: Runnable? = null
    private var currentPosition = 0

    private data class MediaItem(val isVideo: Boolean, val imageRes: Int)

    private val mediaList = listOf(
        MediaItem(isVideo = true, imageRes = R.drawable.bg_image_grid),
        MediaItem(isVideo = false, imageRes = R.drawable.skyblue),
        MediaItem(isVideo = false, imageRes = R.drawable.gsrts),
        MediaItem(isVideo = false, imageRes = R.drawable.platinumrd)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_auction)

        // Fitur Tombol Back (Kembali)
        val btnBack = findViewById<ImageView>(R.id.btnBackDetail)
        btnBack?.setOnClickListener {
            finish()
        }

        initGalleryViews()
        setupGalleryClickListeners()
        selectGalleryItem(0)
    }

    private fun initGalleryViews() {
        ivMainGallery = findViewById(R.id.ivMainGallery)
        vvMainGallery = findViewById(R.id.vvMainGallery)
        btnPlayVideoOverlay = findViewById(R.id.btnPlayVideoOverlay)
        tvPhotoCount = findViewById(R.id.tvPhotoCount)

        thumbVideo = findViewById(R.id.thumbVideo)
        thumbImg1 = findViewById(R.id.thumbImg1)
        thumbImg2 = findViewById(R.id.thumbImg2)
        thumbImg3 = findViewById(R.id.thumbImg3)
    }

    private fun setupGalleryClickListeners() {
        val thumbs = listOf(thumbVideo, thumbImg1, thumbImg2, thumbImg3)

        thumbs.forEachIndexed { index, thumb ->
            thumb.setOnClickListener {
                selectGalleryItem(index)
                resetAutoSlideTimer()
            }
        }
    }

    private fun selectGalleryItem(position: Int) {
        currentPosition = position
        val item = mediaList[position]

        // Update photo count text
        tvPhotoCount.text = "${position + 1}/${mediaList.size}"

        // Update main image / video view
        if (item.isVideo) {
            ivMainGallery.setImageResource(item.imageRes)
            ivMainGallery.visibility = View.VISIBLE
            vvMainGallery.visibility = View.GONE
            btnPlayVideoOverlay.visibility = View.VISIBLE
        } else {
            ivMainGallery.setImageResource(item.imageRes)
            ivMainGallery.visibility = View.VISIBLE
            vvMainGallery.visibility = View.GONE
            btnPlayVideoOverlay.visibility = View.GONE
        }

        // Highlight selected thumbnail border
        val density = resources.displayMetrics.density
        val thumbs = listOf(thumbVideo, thumbImg1, thumbImg2, thumbImg3)

        thumbs.forEachIndexed { index, thumb ->
            if (index == position) {
                thumb.strokeColor = ContextCompat.getColorStateList(this, R.color.guppy_primary)
                thumb.strokeWidth = 2.5f * density
                val pad = (2 * density).toInt()
                thumb.setPadding(pad, pad, pad, pad)
            } else {
                thumb.strokeColor = ContextCompat.getColorStateList(this, android.R.color.transparent)
                thumb.strokeWidth = 0f
                thumb.setPadding(0, 0, 0, 0)
            }
        }
    }

    private fun startAutoSlide() {
        stopAutoSlide()
        slideRunnable = Runnable {
            val nextPosition = (currentPosition + 1) % mediaList.size
            selectGalleryItem(nextPosition)
            slideHandler.postDelayed(slideRunnable!!, 3500)
        }
        slideHandler.postDelayed(slideRunnable!!, 3500)
    }

    private fun resetAutoSlideTimer() {
        stopAutoSlide()
        startAutoSlide()
    }

    private fun stopAutoSlide() {
        slideRunnable?.let { slideHandler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        startAutoSlide()
    }

    override fun onPause() {
        super.onPause()
        stopAutoSlide()
    }
}
