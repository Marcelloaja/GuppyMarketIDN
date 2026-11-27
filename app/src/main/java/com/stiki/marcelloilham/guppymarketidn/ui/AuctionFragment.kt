package com.stiki.marcelloilham.guppymarketidn.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.AuctionGridAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.HomeCategoryAdapter
import com.stiki.marcelloilham.guppymarketidn.menuAuction.CreateAuctionActivity
import com.stiki.marcelloilham.guppymarketidn.menuAuction.DetailAuctionActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogFilter
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem
import com.stiki.marcelloilham.guppymarketidn.menuHome.KeranjangHome
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class AuctionFragment : Fragment() {

    private lateinit var recyclerCategories: RecyclerView
    private lateinit var recyclerAuctions: RecyclerView
    private lateinit var categoryAdapter: HomeCategoryAdapter
    private lateinit var auctionAdapter: AuctionGridAdapter

    private lateinit var tabLive: TextView
    private lateinit var tabUpcoming: TextView
    private lateinit var tabFinished: TextView
    private lateinit var etSearch: EditText

    private var activeStatus: String = "LIVE"
    private var activeCategory: String = "Semua"
    private var activeQuery: String = ""
    private var activeGrade: String = "All"
    private var activeLocation: String = "All"
    private var activeSortType: String = "DEFAULT"

    private var auctionListenerRegistration: ListenerRegistration? = null
    private var latestModels: List<FirestoreRepository.AuctionModel> = emptyList()

    private var nearestEndTimeMillis: Long = 0L
    private val nearestHandler = Handler(Looper.getMainLooper())
    private var tvNearestHours: TextView? = null
    private var tvNearestMins: TextView? = null
    private var tvNearestSecs: TextView? = null
    private var tvNearestLotCount: TextView? = null
    private var cardNearest: View? = null

    private val nearestTimerRunnable = object : Runnable {
        override fun run() {
            if (!isAdded) return
            val now = System.currentTimeMillis()
            if (nearestEndTimeMillis > 0L) {
                val remainingSecs = maxOf(0L, (nearestEndTimeMillis - now) / 1000L)
                renderNearestTimer(remainingSecs)
                if (remainingSecs == 0L) {
                    // Nearest auction just ended: re-sync statuses so it moves from LIVE -> FINISHED
                    syncAuctionData(now)
                }
            } else {
                renderNearestTimer(0L)
            }
            nearestHandler.postDelayed(this, 1000L)
        }
    }

    private val categoriesList = listOf(
        CategoryItem("Semua", R.drawable.bg_image_grid, "Semua"),
        CategoryItem("Mosaic", R.drawable.bg_image_grid, "Mosaic"),
        CategoryItem("Albino", R.drawable.skyblue, "Albino"),
        CategoryItem("Tuxedo", R.drawable.gsrts, "Tuxedo"),
        CategoryItem("Snakeskin", R.drawable.platinumrd, "Snakeskin"),
        CategoryItem("Koi", R.drawable.mssrl, "Koi")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_auction, container, false)

        initViews(view)
        setupTopBar(view)
        setupStatusTabs()
        setupCategories()
        initAuctionRecycler()
        setupSearchAndFilter(view)
        setupNearestBanner(view)
        setupSwipeRefresh(view)

        val fabCreateAuction = view.findViewById<View>(R.id.fabCreateAuction)
        fabCreateAuction?.setOnClickListener {
            startActivity(Intent(requireContext(), CreateAuctionActivity::class.java))
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        startAuctionRealtimeListener()
        if (::auctionAdapter.isInitialized) {
            auctionAdapter.startTimer()
        }
        nearestHandler.removeCallbacks(nearestTimerRunnable)
        nearestHandler.post(nearestTimerRunnable)
    }

    override fun onStop() {
        super.onStop()
        auctionListenerRegistration?.remove()
        auctionListenerRegistration = null
        nearestHandler.removeCallbacks(nearestTimerRunnable)
        if (::auctionAdapter.isInitialized) {
            auctionAdapter.stopTimer()
        }
    }

    private fun initViews(view: View) {
        recyclerCategories = view.findViewById(R.id.recyclerAuctionCategories)
        recyclerAuctions = view.findViewById(R.id.recyclerAuctionGrid)

        tabLive = view.findViewById(R.id.tabAuctionLive)
        tabUpcoming = view.findViewById(R.id.tabAuctionUpcoming)
        tabFinished = view.findViewById(R.id.tabAuctionFinished)
        etSearch = view.findViewById(R.id.etSearchAuction)
    }

    private fun initAuctionRecycler() {
        auctionAdapter = AuctionGridAdapter(requireContext(), emptyList()) { item ->
            openDetailAuction(item)
        }
        recyclerAuctions.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerAuctions.adapter = auctionAdapter
        recyclerAuctions.isNestedScrollingEnabled = false
    }

    private fun setupNearestBanner(view: View) {
        cardNearest = view.findViewById(R.id.cardNearestBanner)
        tvNearestHours = view.findViewById(R.id.tvNearestHours)
        tvNearestMins = view.findViewById(R.id.tvNearestMins)
        tvNearestSecs = view.findViewById(R.id.tvNearestSecs)
        tvNearestLotCount = view.findViewById(R.id.tvNearestLotCount)

        renderNearestTimer(0L)
        tvNearestLotCount?.text = "Memuat lelang aktif..."
        cardNearest?.setOnClickListener {
            Toast.makeText(requireContext(), "Belum ada lelang aktif saat ini", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderNearestTimer(totalSecs: Long) {
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        tvNearestHours?.text = String.format("%02d", h)
        tvNearestMins?.text = String.format("%02d", m)
        tvNearestSecs?.text = String.format("%02d", s)
    }

    private fun setupTopBar(view: View) {
        val btnCart = view.findViewById<ImageView>(R.id.btnCartAuction)
        val btnNotif = view.findViewById<ImageView>(R.id.btnNotifAuction)
        val clickAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.button_click)

        btnCart?.setOnClickListener {
            it.startAnimation(clickAnim)
            startActivity(Intent(requireContext(), KeranjangHome::class.java))
        }

        btnNotif?.setOnClickListener {
            it.startAnimation(clickAnim)
        }
    }

    private fun setupStatusTabs() {
        fun selectTab(selected: TextView, status: String) {
            activeStatus = status
            val activeBg = R.drawable.bg_vehicle_active_pill
            val transparentBg = android.R.color.transparent

            val blackColor = ContextCompat.getColor(requireContext(), R.color.guppy_text_black)
            val grayColor = ContextCompat.getColor(requireContext(), R.color.guppy_text_gray)

            tabLive.setBackgroundResource(if (selected == tabLive) activeBg else transparentBg)
            tabLive.setTextColor(if (selected == tabLive) blackColor else grayColor)
            tabLive.typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), if (selected == tabLive) R.font.inter_bold else R.font.inter_medium)

            tabUpcoming.setBackgroundResource(if (selected == tabUpcoming) activeBg else transparentBg)
            tabUpcoming.setTextColor(if (selected == tabUpcoming) blackColor else grayColor)
            tabUpcoming.typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), if (selected == tabUpcoming) R.font.inter_bold else R.font.inter_medium)

            tabFinished.setBackgroundResource(if (selected == tabFinished) activeBg else transparentBg)
            tabFinished.setTextColor(if (selected == tabFinished) blackColor else grayColor)
            tabFinished.typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), if (selected == tabFinished) R.font.inter_bold else R.font.inter_medium)

            updateSectionHeaderForStatus(status)
            applyFilters()
        }

        tabLive.setOnClickListener { selectTab(tabLive, "LIVE") }
        tabUpcoming.setOnClickListener { selectTab(tabUpcoming, "UPCOMING") }
        tabFinished.setOnClickListener { selectTab(tabFinished, "FINISHED") }
    }

    private fun updateSectionHeaderForStatus(status: String) {
        val tvTitle = view?.findViewById<TextView>(R.id.tvAuctionSectionTitle)
        val tvSubtitle = view?.findViewById<TextView>(R.id.tvAuctionSectionSubtitle)
        val dotLive = view?.findViewById<View>(R.id.dotAuctionSectionLive)

        when (status) {
            "UPCOMING" -> {
                tvTitle?.text = "Lelang Akan Datang"
                tvSubtitle?.text = "Daftar lot yang segera dibuka"
                dotLive?.visibility = View.GONE
            }
            "FINISHED" -> {
                tvTitle?.text = "Lelang Selesai"
                tvSubtitle?.text = "Sesi telah berakhir (penawaran ditutup)"
                dotLive?.visibility = View.GONE
            }
            else -> {
                tvTitle?.text = "Lelang Live Sekarang"
                tvSubtitle?.text = "Penawaran berlangsung real-time"
                dotLive?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupCategories() {
        categoryAdapter = HomeCategoryAdapter(requireContext(), categoriesList) { item, _ ->
            activeCategory = item.name
            applyFilters()
        }
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerCategories.adapter = categoryAdapter
    }

    private fun setupSearchAndFilter(view: View) {
        val btnFilter = view.findViewById<ImageView>(R.id.filterAuction)
        val btnReset = view.findViewById<TextView>(R.id.btnSeeAllAuctionSection)
        val tvSortLabel = view.findViewById<TextView>(R.id.tvAuctionSortLabel)

        btnFilter?.setOnClickListener {
            BottomDialogFilter { sort, grade, location, _, _ ->
                activeGrade = grade
                activeLocation = location
                activeSortType = if (sort == "LowToHigh") "PRICE_LOW" else "PRICE_HIGH"
                tvSortLabel?.text = if (activeSortType == "PRICE_LOW") "Harga terendah ∨" else "Harga tertinggi ∨"
                applyFilters()
            }.show(childFragmentManager, "BottomDialogFilter")
        }

        btnReset?.setOnClickListener {
            activeCategory = "Semua"
            activeQuery = ""
            activeGrade = "All"
            activeLocation = "All"
            activeSortType = "DEFAULT"
            etSearch.setText("")
            tvSortLabel?.text = "Berakhir tercepat ∨"
            categoryAdapter.setSelectedCategoryName("Semua")
            applyFilters()
        }

        tvSortLabel?.setOnClickListener {
            val options = arrayOf("Berakhir tercepat", "Harga terendah", "Harga tertinggi", "Penawaran terbanyak")
            AlertDialog.Builder(requireContext())
                .setTitle("Urutkan Lelang")
                .setItems(options) { _, which ->
                    activeSortType = when (which) {
                        1 -> "PRICE_LOW"
                        2 -> "PRICE_HIGH"
                        3 -> "BIDS_HIGH"
                        else -> "DEFAULT"
                    }
                    tvSortLabel.text = "${options[which]} ∨"
                    applyFilters()
                }
                .show()
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                activeQuery = s.toString().trim()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun startAuctionRealtimeListener() {
        auctionListenerRegistration?.remove()
        auctionListenerRegistration = FirestoreRepository.listenAuctions { firebaseAuctions ->
            if (!isAdded) return@listenAuctions
            latestModels = firebaseAuctions
            syncAuctionData(System.currentTimeMillis())
        }
    }

    private fun syncAuctionData(now: Long = System.currentTimeMillis()) {
        if (!isAdded) return

        val finalItems = latestModels.mapIndexed { idx, a ->
            val calculatedStatus = when {
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
                status = calculatedStatus,
                rawModel = a
            )
        }

        val liveCount = finalItems.count { it.status == "LIVE" }
        val upcomingCount = finalItems.count { it.status == "UPCOMING" }
        val finishedCount = finalItems.count { it.status == "FINISHED" }

        tabLive.text = "Live ($liveCount)"
        tabUpcoming.text = "Akan Datang ($upcomingCount)"
        tabFinished.text = "Selesai ($finishedCount)"

        // Sync "Lelang Berakhir Terdekat" Banner with live Firestore data
        val liveItems = finalItems.filter { it.status == "LIVE" && it.endTimeMillis > now }
            .sortedBy { it.endTimeMillis }

        if (liveItems.isNotEmpty()) {
            val nearest = liveItems.first()
            nearestEndTimeMillis = nearest.endTimeMillis
            val remainingSecs = maxOf(0L, (nearestEndTimeMillis - now) / 1000L)
            renderNearestTimer(remainingSecs)
            tvNearestLotCount?.text = "${liveItems.size} lot aktif · Terdekat: ${nearest.title}"

            cardNearest?.setOnClickListener {
                openDetailAuction(nearest)
            }
        } else {
            nearestEndTimeMillis = 0L
            renderNearestTimer(0L)
            tvNearestLotCount?.text = "Belum ada lelang aktif saat ini"
            cardNearest?.setOnClickListener {
                Toast.makeText(requireContext(), "Belum ada lelang aktif saat ini", Toast.LENGTH_SHORT).show()
            }
        }

        if (::auctionAdapter.isInitialized) {
            auctionAdapter.updateItems(finalItems)
            applyFilters()
        }
    }

    private fun openDetailAuction(item: AuctionGridAdapter.AuctionItem) {
        val model = item.rawModel
        val intent = Intent(requireContext(), DetailAuctionActivity::class.java).apply {
            putExtra("EXTRA_AUCTION_ID", model?.id ?: "")
            putExtra("EXTRA_AUCTION_SELLER_ID", model?.sellerId ?: "")
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
            putExtra("EXTRA_AUCTION_SELLER_AVATAR", model?.sellerAvatarUri ?: "")
            putExtra("EXTRA_AUCTION_END_TIME_MILLIS", item.endTimeMillis)
            putExtra("EXTRA_AUCTION_DESCRIPTION", model?.description ?: "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes.")
            putExtra("EXTRA_AUCTION_STRAIN", model?.strain ?: "Mosaic")
            putExtra("EXTRA_AUCTION_GENDER", model?.gender ?: "1 Pasang (Pair)")
            putExtra("EXTRA_AUCTION_AGE", model?.age ?: "3.5 Bulan")
            putExtra("EXTRA_AUCTION_QUANTITY", model?.quantity ?: "1 Pasang")
            putExtra("EXTRA_AUCTION_CONDITION", model?.condition ?: "Sangat Lincah, Bebas Jamur")
            putExtra("EXTRA_AUCTION_QUARANTINE", model?.quarantine ?: "14 hari di farm")
        }
        startActivity(intent)
    }

    private fun applyFilters() {
        if (::auctionAdapter.isInitialized) {
            auctionAdapter.filterItems(
                status = activeStatus,
                category = activeCategory,
                query = activeQuery,
                grade = activeGrade,
                location = activeLocation,
                sortType = activeSortType
            )

            val tvEmptyState = view?.findViewById<TextView>(R.id.tvEmptyAuctionState)
            if (auctionAdapter.itemCount == 0) {
                recyclerAuctions.visibility = View.GONE
                tvEmptyState?.text = when (activeStatus) {
                    "UPCOMING" -> "Belum ada lelang yang akan datang"
                    "FINISHED" -> "Belum ada riwayat lelang yang selesai"
                    else -> "Belum ada sesi lelang aktif saat ini"
                }
                tvEmptyState?.visibility = View.VISIBLE
            } else {
                recyclerAuctions.visibility = View.VISIBLE
                tvEmptyState?.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeRefresh(view: View) {
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefresh?.setColorSchemeResources(R.color.guppy_primary)
        swipeRefresh?.setOnRefreshListener {
            startAuctionRealtimeListener()
            swipeRefresh.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 800)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        auctionListenerRegistration?.remove()
        auctionListenerRegistration = null
        nearestHandler.removeCallbacks(nearestTimerRunnable)
        if (::auctionAdapter.isInitialized) {
            auctionAdapter.stopTimer()
        }
    }
}
