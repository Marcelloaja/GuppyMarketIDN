package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.AuctionGridAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.FarmProductAdapter
import com.stiki.marcelloilham.guppymarketidn.menuAuction.DetailAuctionActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.util.Locale

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

    private var farmId: String = ""
    private var farmTitle: String = "Guppy Farm"
    private var farmLocation: String = "Indonesia"
    private var farmAddress: String = ""
    private var farmAvatarUri: String = ""
    private var farmLat: Double = 0.0
    private var farmLng: Double = 0.0

    private var allFarmProducts: List<FirestoreRepository.ProductModel> = emptyList()
    private var activeVariantFilter: String = "Semua"
    private var farmAuctionAdapter: AuctionGridAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_farm)

        val topBarDetail = findViewById<View>(R.id.topBarDetail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBarDetail.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        findViewById<ImageView>(R.id.btnBackDetails).setOnClickListener { finish() }

        farmId = intent.getStringExtra("farm_id") ?: ""
        farmTitle = intent.getStringExtra("farm_title") ?: "Guppy Farm"
        farmLocation = intent.getStringExtra("farm_location") ?: "Indonesia"
        farmAddress = farmLocation

        initTabViews()
        setupTabClickListeners()
        setupVariantFilterChips()
        setupActionButtons()
        bindHeaderSummary()
        loadRealFarmDataFromFirestore()

        selectTab(0)
    }

    private var isFollowingFarm: Boolean = false

    private fun setupActionButtons() {
        findViewById<View>(R.id.btnOpenMaps)?.setOnClickListener {
            BottomDialogMapChoice(
                farmName = farmTitle,
                address = farmAddress.ifBlank { farmLocation }
            ).show(supportFragmentManager, "BottomDialogMapChoice")
        }

        findViewById<View>(R.id.btnChatFarm)?.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (farmId.isNotBlank() && farmId == currentUid) {
                Toast.makeText(this, "Ini adalah toko Anda sendiri.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_STORE_ID", farmId)
                putExtra("EXTRA_STORE_NAME", farmTitle)
                putExtra("EXTRA_STORE_LOCATION", farmLocation)
            }
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSeeAllFarmVariants)?.setOnClickListener {
            val intent = Intent(this, CategoryProductsActivity::class.java).apply {
                putExtra("EXTRA_CATEGORY_NAME", "Semua")
            }
            startActivity(intent)
        }

        val btnFollow = findViewById<TextView>(R.id.btnFollowFarm)
        val btnShare = findViewById<TextView>(R.id.btnShareFarm)

        btnFollow?.setOnClickListener {
            isFollowingFarm = !isFollowingFarm
            if (isFollowingFarm) {
                btnFollow.text = "Mengikuti ✓"
                btnFollow.setBackgroundResource(R.drawable.bg_chip_selected)
                btnFollow.setTextColor(ContextCompat.getColor(this, R.color.guppy_primary))
                Toast.makeText(this, "Berhasil mengikuti $farmTitle!", Toast.LENGTH_SHORT).show()
            } else {
                btnFollow.text = "+ Ikuti Farm"
                btnFollow.setBackgroundResource(R.drawable.bg_btn_primary)
                btnFollow.setTextColor(ContextCompat.getColor(this, R.color.white))
                Toast.makeText(this, "Batal mengikuti $farmTitle", Toast.LENGTH_SHORT).show()
            }
        }

        btnShare?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "$farmTitle - Farm Guppy Terpercaya")
                putExtra(Intent.EXTRA_TEXT, "Kunjungi profil $farmTitle ($farmLocation) di GuppyMarket!")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Farm via"))
        }
    }

    private fun loadRealFarmDataFromFirestore() {
        if (farmId.isNotBlank()) {
            FirestoreRepository.fetchUserProfileById(farmId) { profile ->
                if (isFinishing || isDestroyed) return@fetchUserProfileById
                if (profile != null) {
                    applyRealProfile(profile)
                }
                loadFarmProductsAndAuctions(farmId)
            }
        } else {
            FirestoreRepository.findUserProfileByStoreName(farmTitle) { profile ->
                if (isFinishing || isDestroyed) return@findUserProfileByStoreName
                if (profile != null) {
                    farmId = profile.uid
                    applyRealProfile(profile)
                    loadFarmProductsAndAuctions(profile.uid)
                } else {
                    loadFarmProductsAndAuctions("")
                }
            }
        }
    }

    private fun applyRealProfile(profile: FirestoreRepository.UserProfileModel) {
        farmTitle = profile.farmName.ifBlank { profile.name.ifBlank { farmTitle } }
        if (profile.location.isNotBlank()) farmLocation = profile.location
        if (profile.fullAddress.isNotBlank()) farmAddress = profile.fullAddress
        farmAvatarUri = profile.avatarUri
        farmLat = profile.lat
        farmLng = profile.lng

        bindHeaderSummary(profile)
    }

    private fun bindHeaderSummary(profile: FirestoreRepository.UserProfileModel? = null) {
        val ivAvatar = findViewById<ImageView>(R.id.ivFarmDetailAvatar)
        val tvInitials = findViewById<TextView>(R.id.tvFarmDetailInitials)
        val tvName = findViewById<TextView>(R.id.tvFarmDetailName)
        val tvLocation = findViewById<TextView>(R.id.tvFarmDetailLocation)
        val tvRating = findViewById<TextView>(R.id.tvFarmDetailRating)
        val tvReviewCount = findViewById<TextView>(R.id.tvFarmDetailReviewCount)
        val tvBio = findViewById<TextView>(R.id.tvFarmDetailBio)
        val tvAboutAddr = findViewById<TextView>(R.id.tvFarmAboutAddress)
        val tvAboutNotes = findViewById<TextView>(R.id.tvFarmAboutNotes)

        tvName?.text = farmTitle
        tvLocation?.text = farmLocation.ifBlank { "Indonesia" }
        tvAboutAddr?.text = farmAddress.ifBlank { farmLocation.ifBlank { "Alamat belum dilengkapi" } }
        tvAboutNotes?.text = if (profile?.name?.isNotBlank() == true) {
            "Dikelola oleh ${profile.name}"
        } else {
            "Farm Terdaftar GuppyMarket"
        }

        val initials = farmTitle.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "GF" }
        tvInitials?.text = initials

        if (farmAvatarUri.isNotBlank() && ivAvatar != null) {
            val loaded = SafeImageLoader.loadUriSafely(this, ivAvatar, farmAvatarUri, R.drawable.ic_default_user_avatar, 256)
            tvInitials?.visibility = if (loaded) View.GONE else View.VISIBLE
        } else {
            tvInitials?.visibility = View.VISIBLE
        }

        if (profile != null) {
            val ratingFormatted = String.format(Locale.US, "%.1f", profile.rating)
            tvRating?.text = ratingFormatted
            tvReviewCount?.text = "(${profile.totalReviewsCount} ulasan)"
            findViewById<TextView>(R.id.tvFarmUlasanBigRating)?.text = ratingFormatted
            findViewById<TextView>(R.id.tvFarmUlasanCount)?.text = "${profile.totalReviewsCount} ulasan"

            tvBio?.text = profile.description.ifBlank {
                "Breeder & seller guppy terdaftar di GuppyMarket IDN."
            }

            findViewById<TextView>(R.id.tvFarmStatSold)?.text = profile.totalSoldCount.toString()

            val yearsText = if (profile.createdAt > 0L) {
                val years = ((System.currentTimeMillis() - profile.createdAt) / (365L * 24 * 60 * 60 * 1000)).toInt()
                if (years >= 1) "$years thn" else "Baru"
            } else {
                "Baru"
            }
            findViewById<TextView>(R.id.tvFarmStatYears)?.text = yearsText

            val rate = if (profile.totalOrders > 0) {
                ((profile.ontimeOrdersCount.toDouble() / profile.totalOrders.toDouble()) * 100).toInt()
            } else {
                100
            }
            findViewById<TextView>(R.id.tvFarmStatGuarantee)?.text = "$rate%"
        }
    }

    private fun loadFarmProductsAndAuctions(uid: String) {
        if (uid.isBlank()) {
            allFarmProducts = emptyList()
            renderFilteredProducts()
            renderFarmAuctions(emptyList())
            return
        }

        FirestoreRepository.fetchUserProducts(uid) { products ->
            if (isFinishing || isDestroyed) return@fetchUserProducts
            allFarmProducts = products
            findViewById<TextView>(R.id.tvFarmStatVariants)?.text = products.size.toString()
            renderFilteredProducts()
        }

        FirestoreRepository.fetchUserAuctions(uid) { auctions ->
            if (isFinishing || isDestroyed) return@fetchUserAuctions
            renderFarmAuctions(auctions)
        }
    }

    private fun setupVariantFilterChips() {
        val chipAll = findViewById<TextView>(R.id.chipFarmFilterAll) ?: return
        val chipMosaic = findViewById<TextView>(R.id.chipFarmFilterMosaic) ?: return
        val chipAlbino = findViewById<TextView>(R.id.chipFarmFilterAlbino) ?: return
        val chipTuxedo = findViewById<TextView>(R.id.chipFarmFilterTuxedo) ?: return
        val chipSnakeskin = findViewById<TextView>(R.id.chipFarmFilterSnakeskin) ?: return

        val chips = listOf(chipAll, chipMosaic, chipAlbino, chipTuxedo, chipSnakeskin)

        fun selectChip(selected: TextView, filterName: String) {
            activeVariantFilter = filterName
            chips.forEach { c ->
                if (c == selected) {
                    c.setBackgroundResource(R.drawable.bg_filter_green)
                    c.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    c.setBackgroundResource(R.drawable.bg_chip_outline)
                    c.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
                }
            }
            renderFilteredProducts()
        }

        chipAll.setOnClickListener { selectChip(chipAll, "Semua") }
        chipMosaic.setOnClickListener { selectChip(chipMosaic, "Mosaic") }
        chipAlbino.setOnClickListener { selectChip(chipAlbino, "Albino") }
        chipTuxedo.setOnClickListener { selectChip(chipTuxedo, "Tuxedo") }
        chipSnakeskin.setOnClickListener { selectChip(chipSnakeskin, "Snakeskin") }
    }

    private fun renderFilteredProducts() {
        val recycler = findViewById<RecyclerView>(R.id.recyclerFarmProducts)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyFarmProducts)

        val filtered = if (activeVariantFilter.equals("Semua", ignoreCase = true)) {
            allFarmProducts
        } else {
            allFarmProducts.filter {
                it.strain.contains(activeVariantFilter, ignoreCase = true) ||
                    it.category.contains(activeVariantFilter, ignoreCase = true) ||
                    it.title.contains(activeVariantFilter, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            recycler.visibility = View.GONE
            tvEmpty?.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            tvEmpty?.visibility = View.GONE
            recycler.layoutManager = GridLayoutManager(this, 2)
            recycler.isNestedScrollingEnabled = false

            val mapped = filtered.map { p ->
                FarmProductAdapter.FarmProduct(
                    id = p.id,
                    sellerId = p.sellerId,
                    title = p.title,
                    grade = p.category,
                    price = p.price,
                    unit = p.unit,
                    imageRes = if (p.imageRes != 0) p.imageRes else R.drawable.bg_image_grid,
                    imageUrl = p.photoUris.firstOrNull().orEmpty(),
                    photoDataUrl = p.photoDataUrls.firstOrNull().orEmpty()
                )
            }
            recycler.adapter = FarmProductAdapter(this, mapped)
        }
    }

    private fun renderFarmAuctions(auctions: List<FirestoreRepository.AuctionModel>) {
        val recycler = findViewById<RecyclerView>(R.id.recyclerFarmAuctions)
        val emptyLayout = findViewById<View>(R.id.layoutEmptyFarmAuctions)
        val now = System.currentTimeMillis()

        val mapped = auctions.mapIndexed { idx, a ->
            val status = when {
                a.endTimeMillis <= 0L -> "UPCOMING"
                a.endTimeMillis <= now -> "FINISHED"
                else -> "LIVE"
            }
            AuctionGridAdapter.AuctionItem(
                id = idx + 1,
                title = a.title,
                seller = a.seller,
                location = a.location,
                price = a.currentBid,
                bids = a.bidsCount,
                endTimeMillis = a.endTimeMillis,
                imageRes = a.imageRes,
                grade = a.grade,
                category = a.strain,
                status = status,
                rawModel = a
            )
        }

        if (mapped.isEmpty()) {
            recycler.visibility = View.GONE
            emptyLayout?.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            emptyLayout?.visibility = View.GONE
            farmAuctionAdapter?.stopTimer()
            farmAuctionAdapter = AuctionGridAdapter(this, mapped) { item ->
                val model = item.rawModel
                val intent = Intent(this, DetailAuctionActivity::class.java).apply {
                    putExtra("EXTRA_AUCTION_ID", model?.id ?: "")
                    putExtra("EXTRA_AUCTION_SELLER_ID", model?.sellerId ?: farmId)
                    putExtra("auction_title", item.title)
                    putExtra("EXTRA_AUCTION_SELLER", item.seller)
                    putExtra("EXTRA_AUCTION_LOCATION", item.location)
                    putExtra("EXTRA_AUCTION_PRICE", item.price)
                    putExtra("EXTRA_AUCTION_PRICE_NUMERIC", model?.currentBidNumeric ?: 0L)
                    putExtra("EXTRA_AUCTION_BIDS_COUNT", item.bids)
                    putExtra("EXTRA_AUCTION_HIGHEST_BIDDER", model?.highestBidderName ?: "")
                    putExtra("EXTRA_AUCTION_INCREMENT", model?.bidIncrementNumeric ?: 25000L)
                    putExtra("EXTRA_AUCTION_GRADE", item.grade)
                    putExtra("EXTRA_AUCTION_IMAGE", item.imageRes)
                    putExtra("EXTRA_AUCTION_VIDEO_URI", model?.videoUri ?: "")
                    putStringArrayListExtra("EXTRA_AUCTION_PHOTO_URIS", ArrayList(model?.photoUris ?: emptyList()))
                    putExtra("EXTRA_AUCTION_SELLER_AVATAR", model?.sellerAvatarUri ?: farmAvatarUri)
                    putExtra("EXTRA_AUCTION_END_TIME_MILLIS", item.endTimeMillis)
                    putExtra("EXTRA_AUCTION_DESCRIPTION", model?.description ?: "")
                    putExtra("EXTRA_AUCTION_STRAIN", model?.strain ?: "Mosaic")
                    putExtra("EXTRA_AUCTION_GENDER", model?.gender ?: "1 Pasang (Pair)")
                    putExtra("EXTRA_AUCTION_AGE", model?.age ?: "3.5 Bulan")
                    putExtra("EXTRA_AUCTION_QUANTITY", model?.quantity ?: "1 Pasang")
                    putExtra("EXTRA_AUCTION_CONDITION", model?.condition ?: "Sangat Lincah")
                    putExtra("EXTRA_AUCTION_QUARANTINE", model?.quarantine ?: "14 hari di farm")
                }
                startActivity(intent)
            }
            recycler.layoutManager = GridLayoutManager(this, 2)
            recycler.adapter = farmAuctionAdapter
            recycler.isNestedScrollingEnabled = false
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

    override fun onDestroy() {
        super.onDestroy()
        farmAuctionAdapter?.stopTimer()
    }
}
