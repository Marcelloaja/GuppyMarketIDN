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
import com.google.android.material.card.MaterialCardView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.RateItem
import com.stiki.marcelloilham.guppymarketidn.api.RatesRequest
import com.stiki.marcelloilham.guppymarketidn.api.RatesResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DetailAuctionActivity : AppCompatActivity() {

    private lateinit var ivMainGallery: ImageView
    private lateinit var vvMainGallery: VideoView
    private lateinit var btnPlayVideoOverlay: ImageView
    private lateinit var tvPhotoCount: TextView

    private lateinit var cardThumbVideo: MaterialCardView
    private lateinit var cardThumb1: MaterialCardView
    private lateinit var cardThumb2: MaterialCardView
    private lateinit var cardThumb3: MaterialCardView

    private lateinit var thumbVideo: View
    private lateinit var thumbImg1: View
    private lateinit var thumbImg2: View
    private lateinit var thumbImg3: View

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
        fetchBiteshipShippingRates()
    }

    private fun initGalleryViews() {
        ivMainGallery = findViewById(R.id.ivMainGallery)
        vvMainGallery = findViewById(R.id.vvMainGallery)
        btnPlayVideoOverlay = findViewById(R.id.btnPlayVideoOverlay)
        tvPhotoCount = findViewById(R.id.tvPhotoCount)

        cardThumbVideo = findViewById(R.id.cardThumbVideo)
        cardThumb1 = findViewById(R.id.cardThumb1)
        cardThumb2 = findViewById(R.id.cardThumb2)
        cardThumb3 = findViewById(R.id.cardThumb3)

        thumbVideo = findViewById(R.id.thumbVideo)
        thumbImg1 = findViewById(R.id.thumbImg1)
        thumbImg2 = findViewById(R.id.thumbImg2)
        thumbImg3 = findViewById(R.id.thumbImg3)
    }

    private fun fetchBiteshipShippingRates() {
        val tvJntShippingCost = findViewById<TextView>(R.id.tvJntShippingCost) ?: return

        val request = RatesRequest(
            originLatitude = -6.5971,
            originLongitude = 106.8060,
            destinationLatitude = -6.2250,
            destinationLongitude = 106.8654,
            couriers = "jnt,jne,sicepat",
            items = listOf(
                RateItem(
                    name = "Ikan Guppy Mosaic Grade A+",
                    value = 1275000L,
                    weight = 500
                )
            )
        )

        val authHeader = "Bearer ${BiteshipClient.API_KEY}"

        BiteshipClient.instance.getShippingRates(authHeader, request)
            .enqueue(object : Callback<RatesResponse> {
                override fun onResponse(
                    call: Call<RatesResponse>,
                    response: Response<RatesResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val pricingList = response.body()?.pricing
                        val jntOption = pricingList?.firstOrNull {
                            it.courierCode.equals("jnt", ignoreCase = true)
                        } ?: pricingList?.firstOrNull()

                        jntOption?.let {
                            val priceFormatted = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                                .format(it.price).replace("Rp", "Rp ").replace(",00", "")
                            tvJntShippingCost.text = priceFormatted
                        }
                    }
                }

                override fun onFailure(call: Call<RatesResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
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

        // Highlight selected thumbnail CardView border with 14dp rounded corners
        val density = resources.displayMetrics.density
        val cards = listOf(cardThumbVideo, cardThumb1, cardThumb2, cardThumb3)

        cards.forEachIndexed { index, card ->
            if (index == position) {
                card.strokeColor = ContextCompat.getColor(this, R.color.guppy_primary)
                card.strokeWidth = (2.5f * density).toInt()
            } else {
                card.strokeColor = ContextCompat.getColor(this, android.R.color.transparent)
                card.strokeWidth = 0
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
