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
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.BestSellerAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.FilterAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.GridAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.HomeCategoryAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.SpaceItemDecoration
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogCategories
import com.stiki.marcelloilham.guppymarketidn.menuHome.BottomDialogFilter
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryProductsActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem
import com.stiki.marcelloilham.guppymarketidn.menuHome.DetailActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.Item
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.stiki.marcelloilham.guppymarketidn.menuHome.KeranjangHome
import java.util.Calendar

class HomeFragment : Fragment() {
    private lateinit var textUserName: TextView
    private lateinit var textGreeting: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var carouselRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var filterHome: ImageView
    private lateinit var btnCartHome: ImageView
    private lateinit var btnNotifHome: ImageView
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
        setupGridViewRecycler()

        return view
    }

    private fun initViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        textUserName = view.findViewById(R.id.textUserName)
        textGreeting = view.findViewById(R.id.textGreeting)
        carouselRecyclerView = view.findViewById(R.id.carouselRecyclerView)
        filterHome = view.findViewById(R.id.filterHome)
        btnCartHome = view.findViewById(R.id.btnCartHome)
        btnNotifHome = view.findViewById(R.id.btnNotifHome)
        etSearchHome = view.findViewById(R.id.etSearchHome)
        btnSeeAllCategories = view.findViewById(R.id.btnSeeAllCategories)
        categoryHorizontalRecycler = view.findViewById(R.id.categoryHorizontalRecycler)
        gridViewRecycler = view.findViewById(R.id.gridViewRecycler)
        tvSectionTitleGrid = view.findViewById(R.id.tvSectionTitleGrid)
        shimmerCarousel = view.findViewById(R.id.shimmerCarousel)
        shimmerGrid = view.findViewById(R.id.shimmerGrid)
        textNoData = view.findViewById(R.id.textNoData)
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

        btnCartHome.setOnClickListener { button ->
            button.startAnimation(clickAnimation)
            val intent = Intent(requireContext(), KeranjangHome::class.java)
            startActivity(intent)
        }

        btnNotifHome.setOnClickListener { button ->
            button.startAnimation(clickAnimation)
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.guppy_primary)
        swipeRefreshLayout.setOnRefreshListener {
            val content = swipeRefreshLayout.getChildAt(0)
            content?.animate()?.alpha(0.6f)?.setDuration(200)?.start()

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

    private fun setupUserName() {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        // 1. INSTANT LOCAL CACHE DISPLAY (0ms Lag)
        val cachedName = prefs.getString("name", null)
        if (!cachedName.isNullOrEmpty()) {
            textUserName.text = "Hi, $cachedName!"
        } else {
            textUserName.text = "Hi, User!"
        }

        // 2. ASYNC BACKGROUND REFRESH FROM FIRESTORE
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("user").document(userId).get()
                .addOnSuccessListener { document ->
                    if (!isAdded) return@addOnSuccessListener
                    val freshName = document.getString("name")
                    if (!freshName.isNullOrEmpty()) {
                        textUserName.text = "Hi, $freshName!"
                        prefs.edit().putString("name", freshName).apply()
                    }
                }
                .addOnFailureListener { exception ->
                    if (!isAdded) return@addOnFailureListener
                    println("Error getting documents: $exception")
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

        var filterQuery = ""
        if (grade != "All") filterQuery = grade
        else if (location != "All") filterQuery = location

        gridAdapter.searchItems(filterQuery)
        toggleNoDataState(filterQuery)
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

    private fun setupGridViewRecycler() {
        // Simulate data loading delay to show shimmer effect
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded) return@postDelayed

            // Stop Shimmer
            shimmerCarousel.stopShimmer()
            shimmerCarousel.visibility = View.GONE
            carouselRecyclerView.visibility = View.VISIBLE

            shimmerGrid.stopShimmer()
            shimmerGrid.visibility = View.GONE
            gridViewRecycler.visibility = View.VISIBLE

            val items = listOf(
                Item(R.drawable.skyblue, "Sky Blue","Albino","Rp. 75.000"),
                Item(R.drawable.gsrts, "GSRTS","Round Tail","Rp. 100.000"),
                Item(R.drawable.platinumrd,"Platinum RD","Halfmoon","Rp. 125.000"),
                Item(R.drawable.mssrl, "MSSRL","Metal Head","Rp. 95.000")
            )

            gridAdapter = GridAdapter(requireContext(), items)
            gridViewRecycler.adapter = gridAdapter

            val layoutManager = GridLayoutManager(requireContext(), 2)
            gridViewRecycler.layoutManager = layoutManager
            gridViewRecycler.isNestedScrollingEnabled = false

            runLayoutAnimation(gridViewRecycler)

            gridAdapter.setOnItemClickListener(object : GridAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    val item = gridAdapter.getItem(position)
                    val intent = Intent(requireContext(), DetailActivity::class.java).apply {
                        putExtra("ITEM_NAME", item.name)
                        putExtra("ITEM_IMAGE", item.image)
                    }
                    startActivity(intent)
                }
            })
        }, 1500)
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
