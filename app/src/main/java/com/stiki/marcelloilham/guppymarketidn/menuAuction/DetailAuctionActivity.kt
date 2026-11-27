package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.RateItem
import com.stiki.marcelloilham.guppymarketidn.api.RatesRequest
import com.stiki.marcelloilham.guppymarketidn.api.RatesResponse
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogMapChoice
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailFarmActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.AuctionNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
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

    private lateinit var thumbVideo: ImageView
    private lateinit var thumbImg1: ImageView
    private lateinit var thumbImg2: ImageView
    private lateinit var thumbImg3: ImageView
    private var ivThumbVideoPlayIcon: View? = null

    private var ivDetailSellerMiniAvatar: ImageView? = null
    private var ivDetailStoreAvatar: ImageView? = null
    private var tvDetailSellerRating: TextView? = null
    private var tvDetailSellerReviewCount: TextView? = null

    private var tvUserBidAmount: TextView? = null
    private var tvDetailAuctionTimeLeft: TextView? = null
    private var btnSubmitBidNow: TextView? = null
    private var tvAuctionClosedNotice: TextView? = null
    private var layoutBidPresetsRow: View? = null
    private var layoutBidInputControls: View? = null

    private var layoutStatusBadge: View? = null
    private var dotStatusBadge: View? = null
    private var tvStatusBadge: TextView? = null
    private var tvDetailHighestBidMeta: TextView? = null
    private var tvHighestBidAmountText: TextView? = null
    private var btnViewAllBidHistory: TextView? = null
    private var layoutRecentBidsContainer: LinearLayout? = null
    private var tvEmptyRecentBids: TextView? = null

    private var auctionId: String = ""
    private var auctionTitle: String = "Blue Gold Mosaic"
    private var auctionSellerId: String = ""
    private var auctionSeller: String = "Guppy Nusantara"
    private var auctionLocation: String = "Bandung, Jawa Barat"
    private var auctionSellerAvatarUri: String = ""
    private var auctionVideoUri: String = ""
    private var auctionVideoThumbDataUrl: String = ""
    private var auctionVideoChunksCount: Int = 0
    private var auctionPhotoUris: List<String> = emptyList()
    private var auctionPhotoDataUrls: List<String> = emptyList()
    private var auctionFallbackImageRes: Int = R.drawable.bg_image_grid
    private var lastRenderedMediaSignature: String = ""
    private var isDownloadingVideo: Boolean = false

    private var baseCurrentBid: Long = 0L
    private var bidIncrement: Long = 25000L
    private var currentBidsCount: Int = 0
    private var userBidAmount: Long = 25000L
    private var isFollowing: Boolean = false
    private var isSubmittingBid: Boolean = false
    private var isDescriptionExpanded: Boolean = false
    private var isVideoPlaying: Boolean = false
    private var loadedSellerProfileUid: String = ""

    private var detailAuctionEndTimeMillis: Long = 0L
    private var auctionListenerRegistration: ListenerRegistration? = null
    private var bidsListenerRegistration: ListenerRegistration? = null

    private val auctionTimerHandler = Handler(Looper.getMainLooper())
    private val auctionTimerRunnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            if (detailAuctionEndTimeMillis <= 0L) {
                tvDetailAuctionTimeLeft?.text = "Akan Datang"
                tvDetailAuctionTimeLeft?.setTextColor(ContextCompat.getColor(this@DetailAuctionActivity, R.color.guppy_primary))
                updateAuctionBidAvailability(now)
            } else {
                val remainingSecs = maxOf(0L, (detailAuctionEndTimeMillis - now) / 1000L)
                if (remainingSecs > 0L) {
                    val h = remainingSecs / 3600
                    val m = (remainingSecs % 3600) / 60
                    val s = remainingSecs % 60
                    tvDetailAuctionTimeLeft?.text = String.format("%02d : %02d : %02d", h, m, s)
                    tvDetailAuctionTimeLeft?.setTextColor(ContextCompat.getColor(this@DetailAuctionActivity, R.color.guppy_red))
                    updateAuctionBidAvailability(now)
                    auctionTimerHandler.postDelayed(this, 1000L)
                } else {
                    tvDetailAuctionTimeLeft?.text = "Selesai"
                    tvDetailAuctionTimeLeft?.setTextColor(ContextCompat.getColor(this@DetailAuctionActivity, R.color.guppy_text_gray))
                    updateAuctionBidAvailability(now)
                }
            }
        }
    }

    private val slideHandler = Handler(Looper.getMainLooper())
    private var slideRunnable: Runnable? = null
    private var currentPosition = 0

    private data class MediaItem(
        val isVideo: Boolean,
        val mediaUri: String = "",
        val thumbDataUrl: String = "",
        val fallbackRes: Int = R.drawable.bg_image_grid
    )

    private var mediaList: List<MediaItem> = listOf(MediaItem(isVideo = false))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_auction)

        val btnBack = findViewById<ImageView>(R.id.btnBackDetail)
        btnBack?.setOnClickListener {
            finish()
        }

        auctionId = intent.getStringExtra("EXTRA_AUCTION_ID") ?: ""
        detailAuctionEndTimeMillis = intent.getLongExtra("EXTRA_AUCTION_END_TIME_MILLIS", 0L)

        tvDetailAuctionTimeLeft = findViewById(R.id.tvDetailAuctionTimeLeft)
        tvUserBidAmount = findViewById(R.id.tvUserBidAmount)
        btnSubmitBidNow = findViewById(R.id.btnSubmitBidNow)
        tvAuctionClosedNotice = findViewById(R.id.tvAuctionClosedNotice)
        layoutBidPresetsRow = findViewById(R.id.layoutBidPresetsRow)
        layoutBidInputControls = findViewById(R.id.layoutBidInputControls)
        layoutStatusBadge = findViewById(R.id.layoutDetailStatusBadge)
        dotStatusBadge = findViewById(R.id.dotDetailStatusBadge)
        tvStatusBadge = findViewById(R.id.tvDetailStatusBadge)
        tvDetailHighestBidMeta = findViewById(R.id.tvDetailHighestBidMeta)
        tvHighestBidAmountText = findViewById(R.id.tvHighestBidAmountText)
        btnViewAllBidHistory = findViewById(R.id.btnViewAllBidHistory)
        layoutRecentBidsContainer = findViewById(R.id.layoutRecentBidsContainer)
        tvEmptyRecentBids = findViewById(R.id.tvEmptyRecentBids)

        ivDetailSellerMiniAvatar = findViewById(R.id.ivDetailSellerMiniAvatar)
        ivDetailStoreAvatar = findViewById(R.id.ivDetailStoreAvatar)
        tvDetailSellerRating = findViewById(R.id.tvDetailSellerRating)
        tvDetailSellerReviewCount = findViewById(R.id.tvDetailSellerReviewCount)

        initGalleryViews()
        setupBiddingControls()
        bindInitialIntentData()
        setupTopBarActions()
        setupStoreNavigation()

        btnViewAllBidHistory?.setOnClickListener {
            BottomDialogBidHistory.newInstance(auctionId)
                .show(supportFragmentManager, "BottomDialogBidHistory")
        }

        auctionTimerRunnable.run()
        fetchBiteshipShippingRates()
    }

    override fun onStart() {
        super.onStart()
        startRealtimeListeners()
    }

    override fun onStop() {
        super.onStop()
        auctionListenerRegistration?.remove()
        auctionListenerRegistration = null
        bidsListenerRegistration?.remove()
        bidsListenerRegistration = null
        stopVideoPlayback()
    }

    private fun startRealtimeListeners() {
        if (auctionId.isBlank()) return

        auctionListenerRegistration?.remove()
        auctionListenerRegistration = FirestoreRepository.listenAuctionById(auctionId) { model ->
            if (isFinishing || isDestroyed || model == null) return@listenAuctionById
            applyAuctionModel(model)
        }

        bidsListenerRegistration?.remove()
        bidsListenerRegistration = FirestoreRepository.listenAuctionBids(auctionId) { bids ->
            if (isFinishing || isDestroyed) return@listenAuctionBids
            renderRecentBidsPreview(bids)
        }
    }

    private fun bindInitialIntentData() {
        auctionTitle = intent.getStringExtra("auction_title") ?: "Lelang Guppy"
        auctionSellerId = intent.getStringExtra("EXTRA_AUCTION_SELLER_ID") ?: ""
        auctionSeller = intent.getStringExtra("EXTRA_AUCTION_SELLER") ?: "Guppy Farm"
        auctionLocation = intent.getStringExtra("EXTRA_AUCTION_LOCATION") ?: "Indonesia"
        auctionSellerAvatarUri = intent.getStringExtra("EXTRA_AUCTION_SELLER_AVATAR") ?: ""
        auctionVideoUri = intent.getStringExtra("EXTRA_AUCTION_VIDEO_URI") ?: ""
        auctionPhotoUris = intent.getStringArrayListExtra("EXTRA_AUCTION_PHOTO_URIS") ?: emptyList()

        val price = intent.getStringExtra("EXTRA_AUCTION_PRICE") ?: "Rp 0"
        val priceNumeric = intent.getLongExtra("EXTRA_AUCTION_PRICE_NUMERIC", 0L)
        val bidsCount = intent.getIntExtra("EXTRA_AUCTION_BIDS_COUNT", 0)
        val highestBidder = intent.getStringExtra("EXTRA_AUCTION_HIGHEST_BIDDER") ?: ""
        bidIncrement = intent.getLongExtra("EXTRA_AUCTION_INCREMENT", 25000L).coerceAtLeast(1000L)
        val grade = intent.getStringExtra("EXTRA_AUCTION_GRADE") ?: "Grade A+"
        auctionFallbackImageRes = intent.getIntExtra("EXTRA_AUCTION_IMAGE", R.drawable.bg_image_grid)
        val strain = intent.getStringExtra("EXTRA_AUCTION_STRAIN") ?: "Mosaic"
        val gender = intent.getStringExtra("EXTRA_AUCTION_GENDER") ?: "1 Pasang (Pair)"
        val age = intent.getStringExtra("EXTRA_AUCTION_AGE") ?: "3.5 Bulan"
        val quantity = intent.getStringExtra("EXTRA_AUCTION_QUANTITY") ?: "1 Pasang"
        val condition = intent.getStringExtra("EXTRA_AUCTION_CONDITION") ?: "Sangat Lincah, Bebas Jamur"
        val quarantine = intent.getStringExtra("EXTRA_AUCTION_QUARANTINE") ?: "14 hari di farm"
        val description = intent.getStringExtra("EXTRA_AUCTION_DESCRIPTION")
            ?: "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes."

        val numericPrice = if (priceNumeric > 0L) priceNumeric else parsePriceNumeric(price)
        baseCurrentBid = numericPrice
        currentBidsCount = bidsCount
        userBidAmount = getMinAllowedBid()

        bindUIValues(
            title = auctionTitle,
            seller = auctionSeller,
            location = auctionLocation,
            formattedPrice = if (numericPrice > 0L) formatCurrency(numericPrice) else price,
            bidsCount = bidsCount,
            highestBidder = highestBidder,
            grade = grade,
            imageRes = auctionFallbackImageRes,
            strain = strain,
            gender = gender,
            age = age,
            quantity = quantity,
            condition = condition,
            quarantine = quarantine,
            description = description
        )
        syncRealSellerProfile()
    }

    private fun applyAuctionModel(model: FirestoreRepository.AuctionModel) {
        auctionTitle = model.title.ifBlank { auctionTitle }
        auctionSellerId = model.sellerId.ifBlank { auctionSellerId }
        auctionSeller = model.seller.ifBlank { auctionSeller }
        auctionLocation = model.location.ifBlank { auctionLocation }
        if (model.sellerAvatarUri.isNotBlank()) {
            auctionSellerAvatarUri = model.sellerAvatarUri
        }
        auctionVideoUri = model.videoUri
        auctionVideoThumbDataUrl = model.videoThumbDataUrl
        auctionVideoChunksCount = model.videoChunksCount
        auctionPhotoUris = model.photoUris
        auctionPhotoDataUrls = model.photoDataUrls
        auctionFallbackImageRes = if (model.imageRes != 0) model.imageRes else R.drawable.bg_image_grid
        detailAuctionEndTimeMillis = model.endTimeMillis
        bidIncrement = model.bidIncrementNumeric.coerceAtLeast(1000L)

        val numericPrice = if (model.currentBidNumeric > 0L) {
            model.currentBidNumeric
        } else {
            parsePriceNumeric(model.currentBid)
        }
        baseCurrentBid = numericPrice
        currentBidsCount = model.bidsCount

        val minAllowed = getMinAllowedBid()
        if (userBidAmount < minAllowed) {
            userBidAmount = minAllowed
        }

        bindUIValues(
            title = auctionTitle,
            seller = auctionSeller,
            location = auctionLocation,
            formattedPrice = if (numericPrice > 0L) formatCurrency(numericPrice) else model.currentBid,
            bidsCount = model.bidsCount,
            highestBidder = model.highestBidderName,
            grade = model.grade,
            imageRes = auctionFallbackImageRes,
            strain = model.strain,
            gender = model.gender,
            age = model.age,
            quantity = model.quantity,
            condition = model.condition,
            quarantine = model.quarantine,
            description = model.description
        )
        syncRealSellerProfile()

        auctionTimerHandler.removeCallbacks(auctionTimerRunnable)
        auctionTimerRunnable.run()
    }

    private fun syncRealSellerProfile() {
        val sellerUid = auctionSellerId
        if (sellerUid.isBlank()) {
            applySellerAvatars(auctionSellerAvatarUri)
            return
        }
        if (loadedSellerProfileUid == sellerUid) return
        loadedSellerProfileUid = sellerUid

        FirestoreRepository.fetchUserProfileById(sellerUid) { profile ->
            if (isFinishing || isDestroyed || profile == null) return@fetchUserProfileById

            auctionSeller = profile.farmName.ifBlank { profile.name.ifBlank { auctionSeller } }
            if (profile.location.isNotBlank()) {
                auctionLocation = profile.location
            }
            if (profile.avatarUri.isNotBlank()) {
                auctionSellerAvatarUri = profile.avatarUri
            }

            findViewById<TextView>(R.id.tvAuctionSellerText)?.text = auctionSeller
            findViewById<TextView>(R.id.tvAuctionLocationText)?.text = auctionLocation
            findViewById<TextView>(R.id.tvDetailStoreName)?.text = auctionSeller
            findViewById<TextView>(R.id.tvShippingRoute)?.text = "Dikirim dari $auctionLocation → Jakarta"

            val ratingStr = String.format(Locale.US, "%.1f", profile.rating)
            tvDetailSellerRating?.text = ratingStr
            tvDetailSellerReviewCount?.text = "(${profile.totalReviewsCount} ulasan)"

            val soldLabel = "${profile.totalSoldCount} terjual"
            findViewById<TextView>(R.id.tvDetailStoreStats)?.text =
                "$auctionLocation · $soldLabel · ★ $ratingStr"

            applySellerAvatars(auctionSellerAvatarUri)
        }
    }

    private fun applySellerAvatars(avatarUri: String) {
        ivDetailSellerMiniAvatar?.let {
            SafeImageLoader.loadUriSafely(this, it, avatarUri, R.drawable.ic_default_user_avatar, 128)
        }
        ivDetailStoreAvatar?.let {
            SafeImageLoader.loadUriSafely(this, it, avatarUri, R.drawable.ic_default_user_avatar, 256)
        }
    }

    private fun bindUIValues(
        title: String,
        seller: String,
        location: String,
        formattedPrice: String,
        bidsCount: Int,
        highestBidder: String,
        grade: String,
        imageRes: Int,
        strain: String,
        gender: String,
        age: String,
        quantity: String,
        condition: String,
        quarantine: String,
        description: String
    ) {
        val tvLotSubtitle = findViewById<TextView>(R.id.tvDetailLotSubtitle)
        val tvCategoryHeader = findViewById<TextView>(R.id.tvAuctionCategoryHeader)
        val tvTitleText = findViewById<TextView>(R.id.tvAuctionTitleText)
        val tvSellerText = findViewById<TextView>(R.id.tvAuctionSellerText)
        val tvLocationText = findViewById<TextView>(R.id.tvAuctionLocationText)
        val tvShippingRoute = findViewById<TextView>(R.id.tvShippingRoute)

        tvLotSubtitle?.text = if (auctionId.isNotBlank()) "Lot #${auctionId.takeLast(6).uppercase()}" else "Lot Lelang"
        tvCategoryHeader?.text = "GUPPY ${strain.uppercase()} · ${grade.uppercase()}"
        tvTitleText?.text = title
        tvSellerText?.text = seller
        tvLocationText?.text = location
        tvHighestBidAmountText?.text = formattedPrice.replace("Rp", "").trim()
        tvShippingRoute?.text = "Dikirim dari $location → Jakarta"

        tvDetailHighestBidMeta?.text = when {
            bidsCount <= 0 -> "Belum ada penawaran"
            highestBidder.isNotBlank() -> "$bidsCount penawaran · dari $highestBidder"
            else -> "$bidsCount penawaran masuk"
        }
        btnViewAllBidHistory?.text = "Lihat semua ($bidsCount)"

        findViewById<TextView>(R.id.tvDetailStrain)?.text = strain
        findViewById<TextView>(R.id.tvDetailGender)?.text = gender
        findViewById<TextView>(R.id.tvDetailAge)?.text = age
        findViewById<TextView>(R.id.tvDetailQuantity)?.text = quantity
        findViewById<TextView>(R.id.tvDetailCondition)?.text = condition
        findViewById<TextView>(R.id.tvDetailQuarantine)?.text = quarantine

        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val btnReadMore = findViewById<TextView>(R.id.btnReadMoreDescription)
        tvDescription?.text = description
        btnReadMore?.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            tvDescription?.maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4
            btnReadMore.text = if (isDescriptionExpanded) "Tampilkan lebih sedikit" else "Baca selengkapnya"
        }

        findViewById<TextView>(R.id.tvDetailStoreName)?.text = seller
        findViewById<TextView>(R.id.tvDetailStoreStats)?.text = "$location · Farm Terverifikasi"
        findViewById<TextView>(R.id.tvDetailMinIncrementHint)?.text = "Min. kenaikan ${formatCurrency(bidIncrement)}"

        applySellerAvatars(auctionSellerAvatarUri)
        rebuildMediaGallery(if (imageRes != 0) imageRes else R.drawable.bg_image_grid)

        updateUserBidUI()
        updateAuctionBidAvailability()
    }

    private fun rebuildMediaGallery(fallbackRes: Int) {
        val signature = "$auctionVideoUri|$auctionVideoThumbDataUrl|$auctionVideoChunksCount|${auctionPhotoUris.joinToString(",")}|${auctionPhotoDataUrls.joinToString(",")}|$fallbackRes"
        if (signature == lastRenderedMediaSignature && mediaList.isNotEmpty()) {
            return
        }
        lastRenderedMediaSignature = signature

        val items = mutableListOf<MediaItem>()
        val playableVideoUri = if (auctionVideoUri.isNotBlank() && isPlayableMediaAvailable(auctionVideoUri)) auctionVideoUri else ""
        if (playableVideoUri.isNotBlank() || auctionVideoThumbDataUrl.isNotBlank() || auctionVideoChunksCount > 0) {
            items.add(
                MediaItem(
                    isVideo = true,
                    mediaUri = playableVideoUri,
                    thumbDataUrl = auctionVideoThumbDataUrl,
                    fallbackRes = fallbackRes
                )
            )
        }

        val maxPhotoSlots = maxOf(auctionPhotoUris.size, auctionPhotoDataUrls.size)
        for (i in 0 until maxPhotoSlots) {
            val localUri = auctionPhotoUris.getOrNull(i).orEmpty()
            val dataUrl = auctionPhotoDataUrls.getOrNull(i).orEmpty()
            val validLocal = if (localUri.isNotBlank() && isPlayableMediaAvailable(localUri)) localUri else ""
            if (validLocal.isNotBlank() || dataUrl.isNotBlank()) {
                items.add(
                    MediaItem(
                        isVideo = false,
                        mediaUri = validLocal,
                        thumbDataUrl = dataUrl,
                        fallbackRes = fallbackRes
                    )
                )
            }
        }
        if (items.isEmpty()) {
            items.add(MediaItem(isVideo = false, mediaUri = "", thumbDataUrl = "", fallbackRes = fallbackRes))
        }

        mediaList = items

        val cards = listOf(cardThumbVideo, cardThumb1, cardThumb2, cardThumb3)
        val thumbs = listOf(thumbVideo, thumbImg1, thumbImg2, thumbImg3)

        cards.forEachIndexed { idx, card ->
            if (idx < mediaList.size) {
                card.visibility = View.VISIBLE
                val item = mediaList[idx]
                if (idx == 0) {
                    ivThumbVideoPlayIcon?.visibility = if (item.isVideo) View.VISIBLE else View.GONE
                }
                if (item.isVideo) {
                    val loaded = SafeImageLoader.loadVideoThumbnailSafely(this, thumbs[idx], item.mediaUri, item.fallbackRes)
                    if (!loaded && item.thumbDataUrl.isNotBlank()) {
                        SafeImageLoader.loadUriSafely(this, thumbs[idx], item.thumbDataUrl, item.fallbackRes, 256)
                    }
                } else {
                    SafeImageLoader.loadWithFallbackSafely(
                        this, thumbs[idx], item.mediaUri, item.thumbDataUrl, item.fallbackRes, 256
                    )
                }
            } else {
                card.visibility = View.GONE
            }
        }

        setupGalleryClickListeners()
        if (currentPosition >= mediaList.size) currentPosition = 0
        if (!isVideoPlaying) {
            selectGalleryItem(currentPosition)
        }
    }

    private fun isPlayableMediaAvailable(uriStr: String): Boolean {
        return try {
            val uri = Uri.parse(uriStr)
            when {
                uriStr.startsWith("http://", ignoreCase = true) ||
                    uriStr.startsWith("https://", ignoreCase = true) -> true
                uri.scheme == "file" || uriStr.startsWith("/") -> {
                    val path = uri.path ?: uriStr
                    val file = File(path)
                    file.exists() && file.canRead()
                }
                else -> uri.scheme == "content"
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun renderRecentBidsPreview(bids: List<FirestoreRepository.BidModel>) {
        val container = layoutRecentBidsContainer ?: return
        container.removeAllViews()

        if (bids.isEmpty()) {
            container.visibility = View.GONE
            tvEmptyRecentBids?.visibility = View.VISIBLE
            return
        }

        container.visibility = View.VISIBLE
        tvEmptyRecentBids?.visibility = View.GONE

        val topBids = bids.take(3)
        val inflater = LayoutInflater.from(this)
        topBids.forEachIndexed { idx, bid ->
            val row = inflater.inflate(R.layout.item_bid_history_row, container, false)
            val tvAvatar = row.findViewById<TextView>(R.id.tvBidAvatar)
            val tvHandle = row.findViewById<TextView>(R.id.tvBidUserHandle)
            val tvHighest = row.findViewById<TextView>(R.id.tvHighestTag)
            val tvTime = row.findViewById<TextView>(R.id.tvBidTime)
            val tvAmount = row.findViewById<TextView>(R.id.tvBidAmount)

            val name = bid.bidderName.ifBlank { "Penawar" }
            val initials = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifEmpty { "GM" }

            tvAvatar.text = initials
            tvAvatar.backgroundTintList = ColorStateList.valueOf(
                Color.parseColor(if (idx == 0) "#00897B" else "#757575")
            )
            tvHandle.text = name
            tvHighest.visibility = if (idx == 0) View.VISIBLE else View.GONE
            tvTime.text = formatRelativeTime(bid.timestamp)
            tvAmount.text = bid.amountFormatted

            container.addView(row)

            if (idx < topBids.lastIndex) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
                    )
                    setBackgroundColor(ContextCompat.getColor(this@DetailAuctionActivity, R.color.guppy_stroke))
                }
                container.addView(divider)
            }
        }
    }

    private fun formatRelativeTime(timestamp: Long): String {
        val diffMs = maxOf(0L, System.currentTimeMillis() - timestamp)
        val mins = diffMs / 60_000L
        val hours = diffMs / 3_600_000L
        val days = diffMs / 86_400_000L
        return when {
            mins < 1 -> "Baru saja"
            mins < 60 -> "$mins menit lalu"
            hours < 24 -> "$hours jam lalu"
            else -> "$days hari lalu"
        }
    }

    private fun isOwnAuction(): Boolean {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        return currentUid.isNotBlank() && auctionSellerId.isNotBlank() && currentUid == auctionSellerId
    }

    private fun isAuctionLive(now: Long = System.currentTimeMillis()): Boolean {
        return detailAuctionEndTimeMillis > now && !isOwnAuction()
    }

    private fun getMinAllowedBid(): Long {
        return if (currentBidsCount == 0) baseCurrentBid else baseCurrentBid + bidIncrement
    }

    private fun updateAuctionBidAvailability(now: Long = System.currentTimeMillis()) {
        val isUpcoming = detailAuctionEndTimeMillis <= 0L
        val isFinished = detailAuctionEndTimeMillis in 1L..now
        val ownAuction = isOwnAuction()
        val canBid = !isUpcoming && !isFinished && !ownAuction

        when {
            isUpcoming -> {
                tvStatusBadge?.text = "AKAN DATANG"
                dotStatusBadge?.visibility = View.GONE
                layoutStatusBadge?.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.guppy_primary))
            }
            isFinished -> {
                tvStatusBadge?.text = "SELESAI"
                dotStatusBadge?.visibility = View.GONE
                layoutStatusBadge?.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.guppy_text_gray))
            }
            else -> {
                tvStatusBadge?.text = "LIVE"
                dotStatusBadge?.visibility = View.VISIBLE
                layoutStatusBadge?.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.guppy_red))
            }
        }

        if (canBid) {
            layoutBidPresetsRow?.visibility = View.VISIBLE
            layoutBidInputControls?.visibility = View.VISIBLE
            tvAuctionClosedNotice?.visibility = View.GONE

            btnSubmitBidNow?.isEnabled = !isSubmittingBid
            btnSubmitBidNow?.alpha = if (isSubmittingBid) 0.7f else 1.0f
            btnSubmitBidNow?.text = if (isSubmittingBid) "Memproses Penawaran..." else "Pasang Penawaran"
            btnSubmitBidNow?.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.guppy_primary))
        } else {
            layoutBidPresetsRow?.visibility = View.GONE
            layoutBidInputControls?.visibility = View.GONE
            tvAuctionClosedNotice?.visibility = View.VISIBLE

            when {
                isFinished -> {
                    tvAuctionClosedNotice?.text = "Sesi lelang ini telah selesai. Penawaran sudah ditutup."
                    btnSubmitBidNow?.text = "Lelang Telah Selesai"
                }
                isUpcoming -> {
                    tvAuctionClosedNotice?.text = "Sesi lelang ini belum dimulai. Penawaran belum dibuka."
                    btnSubmitBidNow?.text = "Lelang Belum Dimulai"
                }
                ownAuction -> {
                    tvAuctionClosedNotice?.text = "Ini adalah lelang Anda sendiri. Pembuat lelang tidak diperbolehkan memasang penawaran."
                    btnSubmitBidNow?.text = "Lelang Milik Anda"
                }
            }

            btnSubmitBidNow?.isEnabled = false
            btnSubmitBidNow?.alpha = 0.65f
            btnSubmitBidNow?.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.guppy_text_gray))
        }
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
        ivThumbVideoPlayIcon = findViewById(R.id.ivThumbVideoPlayIcon)

        btnPlayVideoOverlay.setOnClickListener {
            val currentItem = mediaList.getOrNull(currentPosition)
            if (currentItem != null && currentItem.isVideo) {
                playOrDownloadVideo(currentItem)
            }
        }

        ivMainGallery.setOnClickListener {
            val currentItem = mediaList.getOrNull(currentPosition)
            if (currentItem != null && currentItem.isVideo) {
                playOrDownloadVideo(currentItem)
            }
        }

        vvMainGallery.setOnClickListener {
            if (vvMainGallery.isPlaying) {
                vvMainGallery.pause()
                btnPlayVideoOverlay.visibility = View.VISIBLE
            } else {
                vvMainGallery.start()
                btnPlayVideoOverlay.visibility = View.GONE
            }
        }
    }

    private fun playOrDownloadVideo(item: MediaItem) {
        if (item.mediaUri.isNotBlank()) {
            startVideoPlayback(item.mediaUri)
            return
        }

        if (auctionId.isBlank()) {
            Toast.makeText(this, "ID lelang tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        val cachedFile = File(cacheDir, "auction_cloud_vid_${auctionId}.mp4")
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            startVideoPlayback(Uri.fromFile(cachedFile).toString())
            return
        }

        if (isDownloadingVideo) {
            Toast.makeText(this, "Sedang mengunduh video...", Toast.LENGTH_SHORT).show()
            return
        }

        isDownloadingVideo = true
        Toast.makeText(this, "Mengunduh video lelang...", Toast.LENGTH_SHORT).show()
        FirestoreRepository.downloadAuctionVideoToCache(auctionId, cachedFile) { success ->
            isDownloadingVideo = false
            if (isFinishing || isDestroyed) return@downloadAuctionVideoToCache
            if (success) {
                startVideoPlayback(Uri.fromFile(cachedFile).toString())
            } else {
                Toast.makeText(
                    this,
                    "Video belum tersinkron (buat ulang lelang dengan versi terbaru).",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startVideoPlayback(videoUriStr: String) {
        try {
            stopAutoSlide()
            isVideoPlaying = true
            ivMainGallery.visibility = View.GONE
            btnPlayVideoOverlay.visibility = View.GONE
            vvMainGallery.visibility = View.VISIBLE
            vvMainGallery.setVideoURI(Uri.parse(videoUriStr))
            vvMainGallery.setOnPreparedListener { mp ->
                mp.isLooping = true
                vvMainGallery.start()
            }
            vvMainGallery.setOnErrorListener { _, _, _ ->
                stopVideoPlayback()
                if (mediaList.getOrNull(currentPosition)?.isVideo == true) {
                    btnPlayVideoOverlay.visibility = View.VISIBLE
                }
                Toast.makeText(this, "Video tidak dapat diputar.", Toast.LENGTH_SHORT).show()
                true
            }
        } catch (_: Exception) {
            stopVideoPlayback()
        }
    }

    private fun stopVideoPlayback() {
        isVideoPlaying = false
        try {
            if (vvMainGallery.isPlaying) {
                vvMainGallery.stopPlayback()
            }
        } catch (_: Exception) {
        }
        vvMainGallery.visibility = View.GONE
        ivMainGallery.visibility = View.VISIBLE
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
            val intent = Intent(this, DetailFarmActivity::class.java).apply {
                putExtra("farm_id", auctionSellerId)
                putExtra("farm_title", auctionSeller)
                putExtra("farm_location", auctionLocation)
            }
            startActivity(intent)
        }
    }

    private fun setupBiddingControls() {
        val btnPreset25k = findViewById<View>(R.id.btnPreset25k)
        val btnPreset50k = findViewById<View>(R.id.btnPreset50k)
        val btnPreset100k = findViewById<View>(R.id.btnPreset100k)

        val btnMinBid = findViewById<View>(R.id.btnMinBidAmount)
        val btnPlusBid = findViewById<View>(R.id.btnPlusBidAmount)

        updateUserBidUI()

        btnPreset25k?.setOnClickListener {
            if (!isAuctionLive()) return@setOnClickListener
            userBidAmount += 25000L
            updateUserBidUI()
        }

        btnPreset50k?.setOnClickListener {
            if (!isAuctionLive()) return@setOnClickListener
            userBidAmount += 50000L
            updateUserBidUI()
        }

        btnPreset100k?.setOnClickListener {
            if (!isAuctionLive()) return@setOnClickListener
            userBidAmount += 100000L
            updateUserBidUI()
        }

        btnMinBid?.setOnClickListener {
            if (!isAuctionLive()) return@setOnClickListener
            val minAllowed = getMinAllowedBid()
            if (userBidAmount - bidIncrement >= minAllowed) {
                userBidAmount -= bidIncrement
                updateUserBidUI()
            } else {
                Toast.makeText(this, "Minimal penawaran adalah ${formatCurrency(minAllowed)}", Toast.LENGTH_SHORT).show()
            }
        }

        btnPlusBid?.setOnClickListener {
            if (!isAuctionLive()) return@setOnClickListener
            userBidAmount += bidIncrement
            updateUserBidUI()
        }

        btnSubmitBidNow?.setOnClickListener {
            val now = System.currentTimeMillis()
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            if (firebaseUser == null) {
                Toast.makeText(this, "Silakan login terlebih dahulu untuk memasang penawaran.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isOwnAuction()) {
                Toast.makeText(this, "Pembuat lelang tidak diperbolehkan memasang penawaran pada lelang sendiri.", Toast.LENGTH_SHORT).show()
                updateAuctionBidAvailability(now)
                return@setOnClickListener
            }
            if (detailAuctionEndTimeMillis <= 0L) {
                Toast.makeText(this, "Lelang belum dimulai.", Toast.LENGTH_SHORT).show()
                updateAuctionBidAvailability(now)
                return@setOnClickListener
            }
            if (now >= detailAuctionEndTimeMillis) {
                Toast.makeText(this, "Lelang sudah selesai, tidak dapat melakukan penawaran lagi.", Toast.LENGTH_SHORT).show()
                updateAuctionBidAvailability(now)
                return@setOnClickListener
            }
            if (isSubmittingBid) return@setOnClickListener

            val minAllowed = getMinAllowedBid()
            if (userBidAmount < minAllowed) {
                userBidAmount = minAllowed
                updateUserBidUI()
            }

            if (auctionId.isBlank()) {
                Toast.makeText(this, "ID lelang tidak ditemukan.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val bidderName = prefs.getString("name", null)?.takeIf { it.isNotBlank() }
                ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email?.substringBefore("@")
                ?: "Penawar Guppy"
            val bidderId = firebaseUser.uid

            isSubmittingBid = true
            updateAuctionBidAvailability()

            FirestoreRepository.placeBid(
                auctionId = auctionId,
                bidAmount = userBidAmount,
                bidderId = bidderId,
                bidderName = bidderName
            ) { success, message, updatedEndTime ->
                if (isFinishing || isDestroyed) return@placeBid
                isSubmittingBid = false
                if (success) {
                    AuctionNotificationHelper.onUserPlacedBid(this, bidderId, auctionId)
                    baseCurrentBid = userBidAmount
                    currentBidsCount += 1
                    userBidAmount += bidIncrement
                    if (updatedEndTime > 0L) {
                        detailAuctionEndTimeMillis = updatedEndTime
                    }
                    updateUserBidUI()
                }
                updateAuctionBidAvailability()
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateUserBidUI() {
        tvUserBidAmount?.text = formatCurrency(userBidAmount)
    }

    private fun parsePriceNumeric(priceStr: String): Long {
        return priceStr.replace("Rp", "").replace(".", "").replace(",", "")
            .replace("(Start)", "").replace(" ", "").trim().toLongOrNull() ?: 0L
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
                farmName = auctionSeller,
                address = "$auctionLocation → Jakarta"
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
                    name = auctionTitle,
                    value = baseCurrentBid.coerceAtLeast(100000L),
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
        val cards = listOf(cardThumbVideo, cardThumb1, cardThumb2, cardThumb3)
        val thumbs = listOf(thumbVideo, thumbImg1, thumbImg2, thumbImg3)

        cards.forEachIndexed { index, card ->
            val onThumbClick = View.OnClickListener {
                if (index < mediaList.size) {
                    selectGalleryItem(index)
                    resetAutoSlideTimer()
                }
            }
            card.setOnClickListener(onThumbClick)
            thumbs[index].setOnClickListener(onThumbClick)
            if (index == 0) {
                ivThumbVideoPlayIcon?.setOnClickListener(onThumbClick)
            }
        }
    }

    private fun selectGalleryItem(position: Int) {
        if (mediaList.isEmpty()) return
        currentPosition = position.coerceIn(0, mediaList.lastIndex)
        val item = mediaList[currentPosition]

        stopVideoPlayback()
        tvPhotoCount.text = "${currentPosition + 1}/${mediaList.size}"

        if (item.isVideo) {
            val loaded = SafeImageLoader.loadVideoThumbnailSafely(this, ivMainGallery, item.mediaUri, item.fallbackRes)
            if (!loaded && item.thumbDataUrl.isNotBlank()) {
                SafeImageLoader.loadUriSafely(this, ivMainGallery, item.thumbDataUrl, item.fallbackRes, 1024)
            }
            ivMainGallery.visibility = View.VISIBLE
            vvMainGallery.visibility = View.GONE
            btnPlayVideoOverlay.visibility = View.VISIBLE
        } else {
            if (item.mediaUri.isNotBlank() || item.thumbDataUrl.isNotBlank()) {
                SafeImageLoader.loadWithFallbackSafely(
                    this, ivMainGallery, item.mediaUri, item.thumbDataUrl, item.fallbackRes, 1024
                )
            } else {
                ivMainGallery.setImageResource(item.fallbackRes)
            }
            ivMainGallery.visibility = View.VISIBLE
            vvMainGallery.visibility = View.GONE
            btnPlayVideoOverlay.visibility = View.GONE
        }

        val density = resources.displayMetrics.density
        val cards = listOf(cardThumbVideo, cardThumb1, cardThumb2, cardThumb3)

        cards.forEachIndexed { index, card ->
            if (index == currentPosition) {
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
        if (mediaList.size <= 1 || isVideoPlaying) return
        slideRunnable = Runnable {
            if (!isVideoPlaying && mediaList.size > 1) {
                val nextPosition = (currentPosition + 1) % mediaList.size
                selectGalleryItem(nextPosition)
                slideHandler.postDelayed(slideRunnable!!, 3500)
            }
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
        AuctionNotificationHelper.activeAuctionDetailId = auctionId
        AuctionNotificationHelper.cancelAuctionNotification(this, auctionId)
        startAutoSlide()
    }

    override fun onPause() {
        super.onPause()
        if (AuctionNotificationHelper.activeAuctionDetailId == auctionId) {
            AuctionNotificationHelper.activeAuctionDetailId = null
        }
        stopAutoSlide()
        stopVideoPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        auctionTimerHandler.removeCallbacks(auctionTimerRunnable)
    }
}
