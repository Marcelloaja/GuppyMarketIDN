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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.AuctionGridAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.HomeCategoryAdapter
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

    private var nearestSecs: Long = 8073L
    private val nearestHandler = Handler(Looper.getMainLooper())
    private var tvNearestHours: TextView? = null
    private var tvNearestMins: TextView? = null
    private var tvNearestSecs: TextView? = null

    private val nearestTimerRunnable = object : Runnable {
        override fun run() {
            if (nearestSecs > 0) {
                nearestSecs--
                val h = nearestSecs / 3600
                val m = (nearestSecs % 3600) / 60
                val s = nearestSecs % 60

                tvNearestHours?.text = String.format("%02d", h)
                tvNearestMins?.text = String.format("%02d", m)
                tvNearestSecs?.text = String.format("%02d", s)

                nearestHandler.postDelayed(this, 1000L)
            }
        }
    }

    private val categoriesList = listOf(
        CategoryItem("Semua", R.drawable.bg_image_grid, "61 lot"),
        CategoryItem("Mosaic", R.drawable.bg_image_grid, "24 lot"),
        CategoryItem("Albino", R.drawable.skyblue, "15 lot"),
        CategoryItem("Tuxedo", R.drawable.gsrts, "10 lot"),
        CategoryItem("Snakeskin", R.drawable.platinumrd, "8 lot"),
        CategoryItem("Koi", R.drawable.mssrl, "5 lot")
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
        setupAuctions()
        setupSearchAndFilter(view)
        setupNearestBanner(view)
        setupSwipeRefresh(view)

        val fabCreateAuction = view.findViewById<View>(R.id.fabCreateAuction)
        fabCreateAuction?.setOnClickListener {
            val intent = Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuAuction.CreateAuctionActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun initViews(view: View) {
        recyclerCategories = view.findViewById(R.id.recyclerAuctionCategories)
        recyclerAuctions = view.findViewById(R.id.recyclerAuctionGrid)

        tabLive = view.findViewById(R.id.tabAuctionLive)
        tabUpcoming = view.findViewById(R.id.tabAuctionUpcoming)
        tabFinished = view.findViewById(R.id.tabAuctionFinished)
        etSearch = view.findViewById(R.id.etSearchAuction)
    }

    private fun setupNearestBanner(view: View) {
        val cardNearest = view.findViewById<View>(R.id.cardNearestBanner)
        tvNearestHours = view.findViewById(R.id.tvNearestHours)
        tvNearestMins = view.findViewById(R.id.tvNearestMins)
        tvNearestSecs = view.findViewById(R.id.tvNearestSecs)

        cardNearest?.setOnClickListener {
            val intent = Intent(requireContext(), DetailAuctionActivity::class.java)
            intent.putExtra("auction_title", "Blue Gold Mosaic")
            startActivity(intent)
        }

        nearestHandler.postDelayed(nearestTimerRunnable, 1000L)
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

            applyFilters()
        }

        tabLive.setOnClickListener { selectTab(tabLive, "LIVE") }
        tabUpcoming.setOnClickListener { selectTab(tabUpcoming, "UPCOMING") }
        tabFinished.setOnClickListener { selectTab(tabFinished, "FINISHED") }
    }

    private fun setupCategories() {
        categoryAdapter = HomeCategoryAdapter(requireContext(), categoriesList) { item, pos ->
            categoryAdapter.setSelectedPosition(pos)
            activeCategory = item.name
            applyFilters()
        }
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerCategories.adapter = categoryAdapter
    }

    private fun setupSearchAndFilter(view: View) {
        val btnFilter = view.findViewById<ImageView>(R.id.filterAuction)

        btnFilter?.setOnClickListener {
            BottomDialogFilter { sort, grade, location, _, _ ->
                activeGrade = grade
                activeLocation = location
                activeSortType = if (sort == "LowToHigh") "PRICE_LOW" else "PRICE_HIGH"
                applyFilters()
            }.show(childFragmentManager, "BottomDialogFilter")
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

    private fun setupAuctions() {
        FirestoreRepository.fetchAuctions { firebaseAuctions ->
            val now = System.currentTimeMillis()
            val finalItems = firebaseAuctions.mapIndexed { idx, a ->
                val calculatedStatus = when {
                    a.endTimeMillis == 0L -> "UPCOMING"
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

            if (isAdded) {
                val liveCount = finalItems.count { it.status == "LIVE" }
                val upcomingCount = finalItems.count { it.status == "UPCOMING" }
                val finishedCount = finalItems.count { it.status == "FINISHED" }

                tabLive.text = "Live ($liveCount)"
                tabUpcoming.text = "Akan Datang ($upcomingCount)"
                tabFinished.text = "Selesai ($finishedCount)"

                // Sync "Lelang Berakhir Terdekat" Banner with real Firestore data
                val liveItems = finalItems.filter { it.status == "LIVE" && it.endTimeMillis > now }
                    .sortedBy { it.endTimeMillis }

                val cardNearest = view?.findViewById<View>(R.id.cardNearestBanner)
                val tvNearestLotCount = view?.findViewById<TextView>(R.id.tvNearestLotCount)

                if (liveItems.isNotEmpty()) {
                    val nearest = liveItems.first()
                    nearestSecs = maxOf(0L, (nearest.endTimeMillis - now) / 1000L)
                    tvNearestLotCount?.text = "${liveItems.size} lot akan ditutup hari ini"

                    cardNearest?.setOnClickListener {
                        val intent = Intent(requireContext(), DetailAuctionActivity::class.java).apply {
                            putExtra("auction_title", nearest.title)
                            putExtra("EXTRA_AUCTION_SELLER", nearest.seller)
                            putExtra("EXTRA_AUCTION_LOCATION", nearest.location)
                            putExtra("EXTRA_AUCTION_PRICE", nearest.price)
                            putExtra("EXTRA_AUCTION_GRADE", nearest.grade)
                            putExtra("EXTRA_AUCTION_IMAGE", nearest.imageRes)
                            putExtra("EXTRA_AUCTION_END_TIME_MILLIS", nearest.endTimeMillis)
                            putExtra("EXTRA_AUCTION_DESCRIPTION", nearest.rawModel?.description ?: "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes.")
                            putExtra("EXTRA_AUCTION_STRAIN", nearest.rawModel?.strain ?: "Mosaic")
                            putExtra("EXTRA_AUCTION_GENDER", nearest.rawModel?.gender ?: "1 Pasang (Pair)")
                            putExtra("EXTRA_AUCTION_AGE", nearest.rawModel?.age ?: "3.5 Bulan")
                            putExtra("EXTRA_AUCTION_QUANTITY", nearest.rawModel?.quantity ?: "1 Pasang")
                            putExtra("EXTRA_AUCTION_CONDITION", nearest.rawModel?.condition ?: "Sangat Lincah, Bebas Jamur")
                            putExtra("EXTRA_AUCTION_QUARANTINE", nearest.rawModel?.quarantine ?: "14 hari di farm")
                        }
                        startActivity(intent)
                    }
                } else {
                    tvNearestLotCount?.text = "Belum ada lot ditutup hari ini"
                    nearestSecs = 0L
                }

                auctionAdapter = AuctionGridAdapter(requireContext(), finalItems) { item ->
                    val intent = Intent(requireContext(), DetailAuctionActivity::class.java).apply {
                        putExtra("auction_title", item.title)
                        putExtra("EXTRA_AUCTION_SELLER", item.seller)
                        putExtra("EXTRA_AUCTION_LOCATION", item.location)
                        putExtra("EXTRA_AUCTION_PRICE", item.price)
                        putExtra("EXTRA_AUCTION_GRADE", item.grade)
                        putExtra("EXTRA_AUCTION_IMAGE", item.imageRes)
                        putExtra("EXTRA_AUCTION_END_TIME_MILLIS", item.endTimeMillis)
                        putExtra("EXTRA_AUCTION_DESCRIPTION", item.rawModel?.description ?: "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes.")
                        putExtra("EXTRA_AUCTION_STRAIN", item.rawModel?.strain ?: "Mosaic")
                        putExtra("EXTRA_AUCTION_GENDER", item.rawModel?.gender ?: "1 Pasang (Pair)")
                        putExtra("EXTRA_AUCTION_AGE", item.rawModel?.age ?: "3.5 Bulan")
                        putExtra("EXTRA_AUCTION_QUANTITY", item.rawModel?.quantity ?: "1 Pasang")
                        putExtra("EXTRA_AUCTION_CONDITION", item.rawModel?.condition ?: "Sangat Lincah, Bebas Jamur")
                        putExtra("EXTRA_AUCTION_QUARANTINE", item.rawModel?.quarantine ?: "14 hari di farm")
                    }
                    startActivity(intent)
                }

                recyclerAuctions.layoutManager = GridLayoutManager(requireContext(), 2)
                recyclerAuctions.adapter = auctionAdapter
                recyclerAuctions.isNestedScrollingEnabled = false

                applyFilters()
            }
        }
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
            setupAuctions()
            swipeRefresh.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 1000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        nearestHandler.removeCallbacks(nearestTimerRunnable)
        if (::auctionAdapter.isInitialized) {
            auctionAdapter.stopTimer()
        }
    }
}
