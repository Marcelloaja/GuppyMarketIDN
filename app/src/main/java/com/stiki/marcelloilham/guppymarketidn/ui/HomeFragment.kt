package com.stiki.marcelloilham.guppymarketidn.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import com.stiki.marcelloilham.guppymarketidn.adapter.BestSellerAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.AuctionGridAdapter
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.adapter.FilterAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.GridAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.HomeCategoryAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.SpaceItemDecoration
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogCategories
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogFilter
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryProductsActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem
import com.stiki.marcelloilham.guppymarketidn.menuHome.ChatListActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailFarmActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.Item
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.stiki.marcelloilham.guppymarketidn.menuHome.KeranjangHome
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var textUserName: TextView
    private lateinit var textGreeting: TextView
    private var imgAvatarHome: ImageView? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userListenerRegistration: ListenerRegistration? = null
    private var auctionListenerRegistration: ListenerRegistration? = null
    private var chatBadgeListenerRegistration: ListenerRegistration? = null
    private var productsListenerRegistration: ListenerRegistration? = null
    private var farmListeners: List<ListenerRegistration> = emptyList()
    private lateinit var carouselRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var filterHome: ImageView
    private lateinit var btnCartHome: ImageView
    private lateinit var btnNotifHome: ImageView
    private lateinit var btnChatHome: View
    private var dotUnreadChatHome: View? = null
    private lateinit var filterRecyclerView: RecyclerView
    private lateinit var gridViewRecycler: RecyclerView
    private lateinit var categoryHorizontalRecycler: RecyclerView
    private lateinit var homeCategoryAdapter: HomeCategoryAdapter
    private lateinit var etSearchHome: EditText
    private lateinit var btnSeeAllCategories: TextView
    private lateinit var tvSectionTitleGrid: TextView
    private lateinit var shimmerCarousel: ShimmerFrameLayout
    private lateinit var shimmerGrid: ShimmerFrameLayout
    private lateinit var textNoData: TextView
    private lateinit var gridAdapter: GridAdapter
    private lateinit var recyclerHomeAuctions: RecyclerView
    private lateinit var homeAuctionAdapter: AuctionGridAdapter

    private var currentSelectedCategoryName = "Semua"

    private val categoriesList = listOf(
        CategoryItem("Semua", R.drawable.skyblue, "Semua produk"),
        CategoryItem("Ikan Kontes", R.drawable.skyblue, "45 produk"),
        CategoryItem("Pakan Ikan", R.drawable.gsrts, "32 produk"),
        CategoryItem("Obat & Vitamin", R.drawable.platinumrd, "18 produk"),
        CategoryItem("Sol & Akrilik", R.drawable.mssrl, "24 produk"),
        CategoryItem("Indukan Breeder", R.drawable.skyblue, "29 produk"),
        CategoryItem("Aksesoris", R.drawable.gsrts, "15 produk"),
        CategoryItem("Tanaman Air", R.drawable.platinumrd, "12 produk"),
        CategoryItem("Filter & Pompa", R.drawable.mssrl, "20 produk")
    )

    private val filters = listOf(
        "All", "Solid", "Albino", "Tuxedo", "Grass",
        "Lace", "Cobra", "Mozaik", "Metal Head", "Sword Tail",
        "Round Tail", "Halfmoon", "Crown Tail", "Koi", "Big Ear",
        "Dumbo Ear", "Short Body", "Female"
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initViews(view)
        setupSwipeRefresh()
        setupFirebase()
        setupUserName()
        updateGreeting()
        setupCarousel()
        setupHeaderButtons()
        setupFilterButton()
        setupCategoriesButton()
        setupHorizontalCategories()
        setupSearchBar()
        setupGridViewRecycler(view)
        setupAuctionIntegration(view)
        setupHomeFarms(view)

        return view
    }

    private fun setupHomeFarms(view: View) {
        val recyclerHomeFarms = view.findViewById<RecyclerView>(R.id.recyclerHomeFarms) ?: return
        val tvEmptyHomeFarms = view.findViewById<TextView>(R.id.tvEmptyHomeFarms)

        auth.currentUser?.uid?.let { uid ->
            FirestoreRepository.syncUserFarmLocationToPublic(uid)
        }
        farmListeners.forEach { it.remove() }
        farmListeners = FirestoreRepository.listenRealFarms { realFarms ->
            if (!isAdded) return@listenRealFarms

            if (realFarms.isEmpty()) {
                recyclerHomeFarms.visibility = View.GONE
                tvEmptyHomeFarms?.visibility = View.VISIBLE
            } else {
                recyclerHomeFarms.visibility = View.VISIBLE
                tvEmptyHomeFarms?.visibility = View.GONE

                val adapter = com.stiki.marcelloilham.guppymarketidn.adapter.HomeFarmAdapter(
                    context = requireContext(),
                    farms = realFarms,
                    onVisitClicked = { clickedFarm ->
                        val intent = Intent(requireContext(), DetailFarmActivity::class.java).apply {
                            putExtra("farm_id", clickedFarm.id)
                            putExtra("farm_title", clickedFarm.farmName)
                            putExtra("farm_location", clickedFarm.location)
                            putExtra("farm_rating", clickedFarm.rating.toString())
                        }
                        startActivity(intent)
                    },
                    onRouteClicked = { clickedFarm ->
                        activity?.intent?.putExtra("ROUTE_FARM_ID", clickedFarm.id)
                        val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.nav_view)
                        bottomNav?.selectedItemId = R.id.navigation_map
                    }
                )
                recyclerHomeFarms.layoutManager = LinearLayoutManager(requireContext())
                recyclerHomeFarms.adapter = adapter
            }
        }
    }

    private fun initViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        textUserName = view.findViewById(R.id.textUserName)
        textGreeting = view.findViewById(R.id.textGreeting)
        imgAvatarHome = view.findViewById(R.id.imgAvatarHome)
        carouselRecyclerView = view.findViewById(R.id.carouselRecyclerView)
        filterHome = view.findViewById(R.id.filterHome)
        btnCartHome = view.findViewById(R.id.btnCartHome)
        btnNotifHome = view.findViewById(R.id.btnNotifHome)
        btnChatHome = view.findViewById(R.id.btnChatHome)
        dotUnreadChatHome = view.findViewById(R.id.dotUnreadChatHome)
        etSearchHome = view.findViewById(R.id.etSearchHome)
        btnSeeAllCategories = view.findViewById(R.id.btnSeeAllCategories)
        categoryHorizontalRecycler = view.findViewById(R.id.categoryHorizontalRecycler)
        gridViewRecycler = view.findViewById(R.id.gridViewRecycler)
        tvSectionTitleGrid = view.findViewById(R.id.tvSectionTitleGrid)
        shimmerCarousel = view.findViewById(R.id.shimmerCarousel)
        shimmerGrid = view.findViewById(R.id.shimmerGrid)
        textNoData = view.findViewById(R.id.textNoData)
    }

    private fun setupAuctionIntegration(view: View) {
        val btnSeeAllAuctions = view.findViewById<TextView>(R.id.btnSeeAllHomeAuctions)
        recyclerHomeAuctions = view.findViewById(R.id.recyclerHomeAuctions)
        val tvEmptyHomeAuctions = view.findViewById<TextView>(R.id.tvEmptyHomeAuctions)

        btnSeeAllAuctions?.setOnClickListener {
            val bottomNav = activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.nav_view)
            bottomNav?.selectedItemId = R.id.navigation_bid
        }

        auctionListenerRegistration?.remove()
        auctionListenerRegistration = FirestoreRepository.listenAuctions { firebaseAuctions ->
            if (!isAdded) return@listenAuctions
            val now = System.currentTimeMillis()

            val liveItems = firebaseAuctions.mapIndexed { idx, a ->
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
            }.filter { it.status == "LIVE" && it.endTimeMillis > now }

            if (liveItems.isEmpty()) {
                recyclerHomeAuctions.visibility = View.GONE
                tvEmptyHomeAuctions?.visibility = View.VISIBLE
            } else {
                recyclerHomeAuctions.visibility = View.VISIBLE
                tvEmptyHomeAuctions?.visibility = View.GONE

                if (::recyclerHomeAuctions.isInitialized) {
                    if (::homeAuctionAdapter.isInitialized) {
                        homeAuctionAdapter.updateItems(liveItems)
                    } else {
                        homeAuctionAdapter = AuctionGridAdapter(
                            context = requireContext(),
                            rawItems = liveItems,
                            itemLayoutResId = R.layout.item_home_auction_card
                        ) { item ->
                            val intent = Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuAuction.DetailAuctionActivity::class.java).apply {
                                putExtra("EXTRA_AUCTION_ID", item.rawModel?.id ?: "")
                                putExtra("EXTRA_AUCTION_SELLER_ID", item.rawModel?.sellerId ?: "")
                                putExtra("auction_title", item.title)
                                putExtra("EXTRA_AUCTION_SELLER", item.seller)
                                putExtra("EXTRA_AUCTION_LOCATION", item.location)
                                putExtra("EXTRA_AUCTION_PRICE", item.price)
                                putExtra("EXTRA_AUCTION_PRICE_NUMERIC", item.rawModel?.currentBidNumeric ?: 0L)
                                putExtra("EXTRA_AUCTION_BIDS_COUNT", item.bids)
                                putExtra("EXTRA_AUCTION_HIGHEST_BIDDER", item.rawModel?.highestBidderName ?: "")
                                putExtra("EXTRA_AUCTION_INCREMENT", item.rawModel?.bidIncrementNumeric ?: 25000L)
                                putExtra("EXTRA_AUCTION_GRADE", item.grade)
                                putExtra("EXTRA_AUCTION_IMAGE", item.imageRes)
                                putExtra("EXTRA_AUCTION_VIDEO_URI", item.rawModel?.videoUri ?: "")
                                putStringArrayListExtra("EXTRA_AUCTION_PHOTO_URIS", ArrayList(item.rawModel?.photoUris ?: emptyList()))
                                putExtra("EXTRA_AUCTION_SELLER_AVATAR", item.rawModel?.sellerAvatarUri ?: "")
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

                        recyclerHomeAuctions.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        recyclerHomeAuctions.adapter = homeAuctionAdapter
                    }
                }
            }
        }
    }

    private fun setupHorizontalCategories() {
        homeCategoryAdapter = HomeCategoryAdapter(requireContext(), categoriesList) { item, pos ->
            currentSelectedCategoryName = item.name
            val intent = Intent(requireContext(), CategoryProductsActivity::class.java).apply {
                putExtra("EXTRA_CATEGORY_NAME", item.name)
            }
            startActivity(intent)
        }

        categoryHorizontalRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        categoryHorizontalRecycler.adapter = homeCategoryAdapter
    }

    private fun setupCategoriesButton() {
        btnSeeAllCategories.setOnClickListener {
            val clickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_click)
            btnSeeAllCategories.startAnimation(clickAnimation)

            BottomDialogCategories(currentSelectedCategoryName) { selectedCategory ->
                currentSelectedCategoryName = if (selectedCategory.isEmpty()) "Semua" else selectedCategory

                homeCategoryAdapter.setSelectedCategoryName(currentSelectedCategoryName)

                val selectedPos = homeCategoryAdapter.getSelectedPosition()
                categoryHorizontalRecycler.smoothScrollToPosition(selectedPos)

                val intent = Intent(requireContext(), CategoryProductsActivity::class.java).apply {
                    putExtra("EXTRA_CATEGORY_NAME", currentSelectedCategoryName)
                }
                startActivity(intent)
            }.show(parentFragmentManager, "BottomDialogCategories")
        }
    }

    private fun setupSearchBar() {
        etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (::gridAdapter.isInitialized) {
                    gridAdapter.searchItems(query)
                    toggleNoDataState(query)
                    if (::homeCategoryAdapter.isInitialized) {
                        homeCategoryAdapter.setSelectedCategoryName(query)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun toggleNoDataState(query: String = "") {
        if (::tvSectionTitleGrid.isInitialized) {
            if (query.isNotEmpty() && !query.equals("Semua", ignoreCase = true)) {
                tvSectionTitleGrid.text = "Kategori: $query"
            } else {
                tvSectionTitleGrid.text = "Baru Ditambahkan"
            }
        }

        if (gridAdapter.itemCount == 0) {
            gridViewRecycler.visibility = View.GONE
            textNoData.text = if (query.isNotEmpty()) "Produk '$query' tidak ditemukan" else "Data tidak ditemukan"
            textNoData.visibility = View.VISIBLE
        } else {
            gridViewRecycler.visibility = View.VISIBLE
            textNoData.visibility = View.GONE
        }
    }

    private fun setupHeaderButtons() {
        val clickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_click)

        btnChatHome.setOnClickListener { button ->
            button.startAnimation(clickAnimation)
            startActivity(Intent(requireContext(), ChatListActivity::class.java))
        }

        setupUnreadChatBadge()

        btnCartHome.setOnClickListener { button ->
            button.startAnimation(clickAnimation)
            val intent = Intent(requireContext(), KeranjangHome::class.java)
            startActivity(intent)
        }

        btnNotifHome.setOnClickListener { button ->
            button.startAnimation(clickAnimation)
        }
    }

    override fun onStart() {
        super.onStart()
        view?.let { setupHomeFarms(it) }
        startRealtimeProductsListener()
    }

    override fun onStop() {
        super.onStop()
        userListenerRegistration?.remove()
        userListenerRegistration = null
        auctionListenerRegistration?.remove()
        auctionListenerRegistration = null
        chatBadgeListenerRegistration?.remove()
        chatBadgeListenerRegistration = null
        productsListenerRegistration?.remove()
        productsListenerRegistration = null
        farmListeners.forEach { it.remove() }
        farmListeners = emptyList()
        if (::homeAuctionAdapter.isInitialized) {
            homeAuctionAdapter.stopTimer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListenerRegistration?.remove()
        userListenerRegistration = null
        auctionListenerRegistration?.remove()
        auctionListenerRegistration = null
        chatBadgeListenerRegistration?.remove()
        chatBadgeListenerRegistration = null
        productsListenerRegistration?.remove()
        productsListenerRegistration = null
        farmListeners.forEach { it.remove() }
        farmListeners = emptyList()
        if (::homeAuctionAdapter.isInitialized) {
            homeAuctionAdapter.stopTimer()
        }
        carouselRunnable?.let { carouselHandler.removeCallbacks(it) }
        imgAvatarHome = null
        dotUnreadChatHome = null
    }

    private fun setupUnreadChatBadge() {
        val dotUnread = dotUnreadChatHome ?: return
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            dotUnread.visibility = View.GONE
            return
        }
        chatBadgeListenerRegistration?.remove()
        chatBadgeListenerRegistration = FirestoreRepository.listenUserChatRooms(uid) { rooms ->
            if (!isAdded) return@listenUserChatRooms
            val hasUnread = rooms.any { (it.unreadCounts[uid] ?: 0L) > 0L }
            dotUnread.visibility = if (hasUnread) View.VISIBLE else View.GONE
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.guppy_primary)
        swipeRefreshLayout.setOnRefreshListener {
            val content = swipeRefreshLayout.getChildAt(0)
            content?.animate()?.alpha(0.6f)?.setDuration(200)?.start()

            loadCachedUserHeader()
            setupUserName()

            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefreshLayout.isRefreshing = false

                runLayoutAnimation(gridViewRecycler)

                content?.translationY = 30f
                content?.animate()
                    ?.alpha(1.0f)
                    ?.translationY(0f)
            }, 1000)
        }
    }

    private fun setupFirebase() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun loadCachedUserHeader() {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val cachedName = prefs.getString("name", null)
        val cachedAvatar = prefs.getString("avatar_uri", null)

        if (!cachedName.isNullOrEmpty()) {
            textUserName.text = "Hi, $cachedName!"
        } else {
            textUserName.text = "Hi, User!"
        }

        imgAvatarHome?.let { avatarView ->
            SafeImageLoader.loadUriSafely(safeContext, avatarView, cachedAvatar, R.drawable.ic_default_user_avatar)
        }
    }

    private fun setupUserName() {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // 1. INSTANT LOCAL CACHE DISPLAY (0ms Lag)
        loadCachedUserHeader()

        // 2. REALTIME BACKGROUND LISTEN FROM FIRESTORE
        val userId = auth.currentUser?.uid
        if (userId != null) {
            userListenerRegistration?.remove()
            userListenerRegistration = db.collection("user").document(userId).addSnapshotListener { document, error ->
                if (!isAdded || error != null || document == null || !document.exists()) return@addSnapshotListener
                val freshName = document.getString("name")
                val freshAvatar = document.getString("avatar_uri") ?: document.getString("photo_url")
                val freshAvatarDataUrl = document.getString("avatar_data_url")

                if (!freshName.isNullOrEmpty()) {
                    textUserName.text = "Hi, $freshName!"
                    prefs.edit().putString("name", freshName).apply()
                }

                if (!freshAvatar.isNullOrEmpty() || !freshAvatarDataUrl.isNullOrEmpty()) {
                    val avatarView = imgAvatarHome
                    val currentCtx = context
                    if (avatarView != null && currentCtx != null) {
                        val localLoaded = SafeImageLoader.loadUriSafely(
                            currentCtx,
                            avatarView,
                            freshAvatar,
                            R.drawable.ic_default_user_avatar
                        )
                        if (localLoaded && !freshAvatar.isNullOrBlank()) {
                            prefs.edit().putString("avatar_uri", freshAvatar).apply()
                        } else if (!freshAvatarDataUrl.isNullOrBlank()) {
                            val cloudLoaded = SafeImageLoader.loadUriSafely(
                                currentCtx,
                                avatarView,
                                freshAvatarDataUrl,
                                R.drawable.ic_default_user_avatar
                            )
                            if (cloudLoaded) {
                                prefs.edit().putString("avatar_uri", freshAvatarDataUrl).apply()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        textGreeting.text = when (hour) {
            in 0..11 -> "Good Morning!"
            in 12..16 -> "Good Afternoon!"
            in 17..20 -> "Good Evening!"
            else -> "Good Night!"
        }
    }

    private val carouselHandler = Handler(Looper.getMainLooper())
    private var carouselRunnable: Runnable? = null
    private var currentCarouselPosition = 0

    private fun setupCarousel() {
        val images = listOf(
            R.drawable.bestseller1,
            R.drawable.bestseller1,
            R.drawable.bestseller1
        )

        val adapter = BestSellerAdapter(images)
        carouselRecyclerView.adapter = adapter

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        carouselRecyclerView.layoutManager = layoutManager
        carouselRecyclerView.isNestedScrollingEnabled = false

        if (carouselRecyclerView.onFlingListener == null) {
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(carouselRecyclerView)
        }

        // Add 3D Depth Parallax Zoom-Scale Transformer on Carousel
        carouselRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val center = recyclerView.width / 2f
                for (i in 0 until recyclerView.childCount) {
                    val child = recyclerView.getChildAt(i)
                    val childCenter = (child.left + child.right) / 2f
                    val distanceFromCenter = Math.abs(center - childCenter)
                    val scale = 1f - (distanceFromCenter / recyclerView.width) * 0.12f
                    val alpha = 1f - (distanceFromCenter / recyclerView.width) * 0.25f
                    child.scaleX = scale.coerceAtLeast(0.88f)
                    child.scaleY = scale.coerceAtLeast(0.88f)
                    child.alpha = alpha.coerceAtLeast(0.75f)
                }
            }
        })

        carouselRunnable = Runnable {
            val itemCount = adapter.itemCount
            if (itemCount > 0) {
                currentCarouselPosition = (currentCarouselPosition + 1) % itemCount
                carouselRecyclerView.smoothScrollToPosition(currentCarouselPosition)
                carouselHandler.postDelayed(carouselRunnable!!, 3500)
            }
        }

        startAutoSlide()
    }

    private fun startAutoSlide() {
        carouselRunnable?.let {
            carouselHandler.removeCallbacks(it)
            carouselHandler.postDelayed(it, 3500)
        }
    }

    private fun stopAutoSlide() {
        carouselRunnable?.let {
            carouselHandler.removeCallbacks(it)
        }
    }

    override fun onResume() {
        super.onResume()
        startAutoSlide()
        loadCachedUserHeader()
        setupUserName()
        if (::homeAuctionAdapter.isInitialized) {
            homeAuctionAdapter.startTimer()
        }
        setupUnreadChatBadge()
        view?.let { setupAuctionIntegration(it) }
    }

    override fun onPause() {
        super.onPause()
        stopAutoSlide()
    }

    private fun setupFilterButton() {
        val clickAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.button_click)
        filterHome.setOnClickListener { button ->
            button.startAnimation(clickAnimation)
            BottomDialogFilter { sort, grade, location, minPrice, maxPrice ->
                applyUnifiedFilters(sort, grade, location, minPrice, maxPrice)
            }.show(parentFragmentManager, "BottomDialogFilter")
        }
    }

    private fun applyUnifiedFilters(sort: String, grade: String, location: String, minPrice: Float, maxPrice: Float) {
        if (!::gridAdapter.isInitialized) return

        gridAdapter.applyAdvancedFilter(sort, grade, location, minPrice, maxPrice)
        toggleNoDataState(if (grade != "All") grade else if (location != "All") location else "")
    }

    private fun setupFilterRecyclerView() {
        val adapter = FilterAdapter(requireContext(), filters) { filter ->
            applyFilter(filter)
        }
        filterRecyclerView.adapter = adapter

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        filterRecyclerView.layoutManager = layoutManager
        filterRecyclerView.isNestedScrollingEnabled = false

        // Apply item decoration for spacing
        val spaceDecoration = SpaceItemDecoration(12)
        filterRecyclerView.addItemDecoration(spaceDecoration)
    }

    private fun setupGridViewRecycler(rootView: View) {
        shimmerCarousel.stopShimmer()
        shimmerCarousel.visibility = View.GONE
        carouselRecyclerView.visibility = View.VISIBLE

        rootView.findViewById<View>(R.id.btnSeeAllHomeProducts)?.setOnClickListener {
            val intent = Intent(requireContext(), CategoryProductsActivity::class.java).apply {
                putExtra("EXTRA_CATEGORY_NAME", "Semua")
            }
            startActivity(intent)
        }

        rootView.findViewById<View>(R.id.btnSeeAllHomeTips)?.setOnClickListener {
            startActivity(Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuProfile.TipsPerawatanActivity::class.java))
        }

        gridAdapter = GridAdapter(requireContext(), emptyList())
        gridViewRecycler.adapter = gridAdapter
        gridViewRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        gridViewRecycler.isNestedScrollingEnabled = false

        gridAdapter.setOnItemClickListener(object : GridAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val item = gridAdapter.getItem(position)
                val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                    putExtra("EXTRA_PRODUCT_ID", item.id)
                    putExtra("EXTRA_SELLER_ID", item.sellerId)
                    putExtra("ITEM_NAME", item.name)
                    putExtra("ITEM_IMAGE", item.image)
                }
                startActivity(intent)
            }
        })
    }

    private fun startRealtimeProductsListener() {
        productsListenerRegistration?.remove()
        productsListenerRegistration = FirestoreRepository.listenProducts { realProducts ->
            if (!isAdded) return@listenProducts

            shimmerGrid.stopShimmer()
            shimmerGrid.visibility = View.GONE

            val items = realProducts.map { p ->
                Item(
                    image = if (p.imageRes != 0) p.imageRes else R.drawable.bg_image_grid,
                    name = p.title,
                    type = p.category,
                    price = p.price,
                    id = p.id,
                    sellerId = p.sellerId,
                    seller = p.seller,
                    unit = p.unit.ifBlank { "per pasang" },
                    imageUrl = p.photoUris.firstOrNull().orEmpty(),
                    photoDataUrl = p.photoDataUrls.firstOrNull().orEmpty(),
                    strain = p.strain,
                    location = p.location,
                    priceNumeric = p.priceNumeric,
                    stock = p.effectiveStock(),
                    description = p.description
                )
            }

            gridAdapter.updateItems(items)
            val activeQuery = if (::etSearchHome.isInitialized) etSearchHome.text.toString() else ""
            if (activeQuery.isNotBlank()) {
                gridAdapter.searchItems(activeQuery)
                toggleNoDataState(activeQuery)
            } else if (items.isEmpty()) {
                gridViewRecycler.visibility = View.GONE
                textNoData.text = "Belum ada varian produk tersedia."
                textNoData.visibility = View.VISIBLE
            } else {
                gridViewRecycler.visibility = View.VISIBLE
                textNoData.visibility = View.GONE
            }
        }
    }

    private fun runLayoutAnimation(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val controller = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = controller
        recyclerView.adapter?.notifyDataSetChanged()
        recyclerView.scheduleLayoutAnimation()
    }

    private fun applyFilter(filter: String) {
        gridAdapter.filterItems(filter)
        if (gridAdapter.itemCount == 0) {
            gridViewRecycler.visibility = View.GONE
            textNoData.visibility = View.VISIBLE
        } else {
            gridViewRecycler.visibility = View.VISIBLE
            textNoData.visibility = View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
