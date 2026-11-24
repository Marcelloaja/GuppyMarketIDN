package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.RateItem
import com.stiki.marcelloilham.guppymarketidn.api.RatesRequest
import com.stiki.marcelloilham.guppymarketidn.api.RatesResponse
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogMapChoice
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailFarmActivity
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

    private var tvUserBidAmount: TextView? = null
    private var tvDetailAuctionTimeLeft: TextView? = null

    private var baseCurrentBid: Long = 1275000L
    private var userBidAmount: Long = 1300000L
    private var isFollowing: Boolean = false

    private var detailAuctionEndTimeMillis: Long = System.currentTimeMillis() + 765000L
    private val auctionTimerHandler = Handler(Looper.getMainLooper())
    private val auctionTimerRunnable = object : Runnable {
        override fun run() {
            val remainingSecs = maxOf(0L, (detailAuctionEndTimeMillis - System.currentTimeMillis()) / 1000L)
            if (remainingSecs > 0) {
                val h = remainingSecs / 3600
                val m = (remainingSecs % 3600) / 60
                val s = remainingSecs % 60
                tvDetailAuctionTimeLeft?.text = String.format("%02d : %02d : %02d", h, m, s)
                auctionTimerHandler.postDelayed(this, 1000L)
            } else {
                tvDetailAuctionTimeLeft?.text = "Selesai"
                tvDetailAuctionTimeLeft?.setTextColor(getColor(R.color.guppy_text_gray))
            }
        }
    }

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

        val btnBack = findViewById<ImageView>(R.id.btnBackDetail)
        btnBack?.setOnClickListener {
            finish()
        }

        val passedEndTime = intent.getLongExtra("EXTRA_AUCTION_END_TIME_MILLIS", 0L)
        if (passedEndTime > 0L) {
            detailAuctionEndTimeMillis = passedEndTime
        }

        tvDetailAuctionTimeLeft = findViewById(R.id.tvDetailAuctionTimeLeft)
        tvUserBidAmount = findViewById(R.id.tvUserBidAmount)

        initGalleryViews()
        setupGalleryClickListeners()
        selectGalleryItem(0)

        setupBiddingControls()
        bindAuctionData()
        setupTopBarActions()
        setupStoreNavigation()

        val btnViewAllBids = findViewById<View>(R.id.btnViewAllBidHistory)
        btnViewAllBids?.setOnClickListener {
            BottomDialogBidHistory().show(supportFragmentManager, "BottomDialogBidHistory")
        }

        auctionTimerRunnable.run()
        fetchBiteshipShippingRates()
    }

    private fun bindAuctionData() {
        val title = intent.getStringExtra("auction_title") ?: "Blue Gold Mosaic"
        val seller = intent.getStringExtra("EXTRA_AUCTION_SELLER") ?: "Guppy Nusantara"
        val location = intent.getStringExtra("EXTRA_AUCTION_LOCATION") ?: "Bandung, Jawa Barat"
        val price = intent.getStringExtra("EXTRA_AUCTION_PRICE") ?: "Rp 850.000"
        val grade = intent.getStringExtra("EXTRA_AUCTION_GRADE") ?: "Grade A+"
        val imageRes = intent.getIntExtra("EXTRA_AUCTION_IMAGE", R.drawable.bg_image_grid)
        val description = intent.getStringExtra("EXTRA_AUCTION_DESCRIPTION") ?: "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes. Memiliki corak ekor lincah, bebas penyakit, dan sudah melewati masa karantina."

        val tvCategoryHeader = findViewById<TextView>(R.id.tvAuctionCategoryHeader)
        val tvTitleText = findViewById<TextView>(R.id.tvAuctionTitleText)
        val tvSellerText = findViewById<TextView>(R.id.tvAuctionSellerText)
        val tvLocationText = findViewById<TextView>(R.id.tvAuctionLocationText)
        val tvHighestBidText = findViewById<TextView>(R.id.tvHighestBidAmountText)
        val tvShippingRoute = findViewById<TextView>(R.id.tvShippingRoute)
        val tvDescText = findViewById<TextView>(R.id.tvAuctionDescriptionText)

        tvCategoryHeader?.text = "GUPPY · ${grade.uppercase()}"
        tvTitleText?.text = title
        tvSellerText?.text = seller
        tvLocationText?.text = location
        tvHighestBidText?.text = price.replace("Rp", "").trim()
        tvShippingRoute?.text = "Dikirim dari $location → Jakarta"
        tvDescText?.text = description

        if (imageRes != 0) {
            ivMainGallery.setImageResource(imageRes)
        }

        val numericPrice = price.replace("Rp", "").replace(".", "").replace(" ", "").trim().toLongOrNull() ?: 850000L
        baseCurrentBid = numericPrice
        userBidAmount = numericPrice + 25000L
        updateUserBidUI()
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

    private fun setupTopBarActions() {
        val btnFollowTop = findViewById<View>(R.id.btnFollowAuctionTop)
        val tvFollowTopText = findViewById<TextView>(R.id.tvFollowAuctionTopText)

        btnFollowTop?.setOnClickListener {
            isFollowing = !isFollowing
            if (isFollowing) {
                tvFollowTopText?.text = "Mengikuti ✓"
                btnFollowTop.setBackgroundResource(R.drawable.bg_chip_selected)
                Toast.makeText(this, "Berhasil mengikuti sesi lelang ini", Toast.LENGTH_SHORT).show()
            } else {
                tvFollowTopText?.text = "Ikuti"
                btnFollowTop.setBackgroundResource(R.drawable.bg_chip_ikuti)
                Toast.makeText(this, "Batal mengikuti sesi lelang ini", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupStoreNavigation() {
        val btnVisitStore = findViewById<View>(R.id.btnVisitStoreDetail)
        btnVisitStore?.setOnClickListener {
            val intent = Intent(this, DetailFarmActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBiddingControls() {
        val btnPreset25k = findViewById<View>(R.id.btnPreset25k)
        val btnPreset50k = findViewById<View>(R.id.btnPreset50k)
        val btnPreset100k = findViewById<View>(R.id.btnPreset100k)

        val btnMinBid = findViewById<View>(R.id.btnMinBidAmount)
        val btnPlusBid = findViewById<View>(R.id.btnPlusBidAmount)
        val btnSubmitBid = findViewById<View>(R.id.btnSubmitBidNow)

        updateUserBidUI()

        btnPreset25k?.setOnClickListener {
            userBidAmount += 25000L
            updateUserBidUI()
        }

        btnPreset50k?.setOnClickListener {
            userBidAmount += 50000L
            updateUserBidUI()
        }

        btnPreset100k?.setOnClickListener {
            userBidAmount += 100000L
            updateUserBidUI()
        }

        btnMinBid?.setOnClickListener {
            val minAllowed = baseCurrentBid + 25000L
            if (userBidAmount > minAllowed) {
                userBidAmount -= 25000L
                updateUserBidUI()
            } else {
                Toast.makeText(this, "Minimal penawaran adalah ${formatCurrency(minAllowed)}", Toast.LENGTH_SHORT).show()
            }
        }

        btnPlusBid?.setOnClickListener {
            userBidAmount += 25000L
            updateUserBidUI()
        }

        btnSubmitBid?.setOnClickListener {
            baseCurrentBid = userBidAmount
            Toast.makeText(this, "Penawaran ${formatCurrency(userBidAmount)} Berhasil Pasang!", Toast.LENGTH_LONG).show()
            userBidAmount += 25000L
            updateUserBidUI()
        }
    }

    private fun updateUserBidUI() {
        tvUserBidAmount?.text = formatCurrency(userBidAmount)
    }

    private fun formatCurrency(amount: Long): String {
        return NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .format(amount).replace("Rp", "Rp ").replace(",00", "")
    }

    private fun fetchBiteshipShippingRates() {
        val tvCourierName = findViewById<TextView>(R.id.tvCourierName)
        val tvCourierEtd = findViewById<TextView>(R.id.tvCourierEtd)
        val tvJntShippingCost = findViewById<TextView>(R.id.tvJntShippingCost) ?: return
        val btnChangeShippingLoc = findViewById<View>(R.id.btnChangeShippingLoc)

        btnChangeShippingLoc?.setOnClickListener {
            BottomDialogMapChoice(
                farmName = "Bogor Guppy Lab",
                address = "Jl. Raya Bogor No. 88, Bogor → Jatinegara, Jakarta"
            ).show(supportFragmentManager, "BottomDialogMapChoice")
        }

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
                            tvCourierName?.text = "${it.courierName.uppercase()} ${it.courierServiceCode.uppercase()}"
                            tvCourierEtd?.text = "Estimasi ${it.duration}"
                        }
                    }
                }

                override fun onFailure(call: Call<RatesResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun setupGalleryClickListeners() {
        btnPlayVideoOverlay.setOnClickListener {
            playVideo()
        }

        val thumbs = listOf(thumbVideo, thumbImg1, thumbImg2, thumbImg3)

        thumbs.forEachIndexed { index, thumb ->
            thumb.setOnClickListener {
                if (index == 0) {
                    playVideo()
                } else {
                    selectGalleryItem(index)
                    resetAutoSlideTimer()
                }
            }
        }
    }

    private fun playVideo() {
        btnPlayVideoOverlay.visibility = View.GONE
        ivMainGallery.visibility = View.GONE
        vvMainGallery.visibility = View.VISIBLE

        try {
            val videoUri = Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4")
            vvMainGallery.setVideoURI(videoUri)
            val mediaController = android.widget.MediaController(this)
            mediaController.setAnchorView(vvMainGallery)
            vvMainGallery.setMediaController(mediaController)
            vvMainGallery.setOnPreparedListener { mp ->
                mp.isLooping = true
                vvMainGallery.start()
            }
            vvMainGallery.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Memutar video sampel ikan guppy...", Toast.LENGTH_SHORT).show()
                true
            }
            vvMainGallery.requestFocus()
            vvMainGallery.start()
        } catch (_: Exception) {
            Toast.makeText(this, "Video sedang diputar", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        auctionTimerHandler.removeCallbacks(auctionTimerRunnable)
    }
}
