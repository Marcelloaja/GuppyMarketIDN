package com.stiki.marcelloilham.guppymarketidn.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.adapter.MyAuctionListAdapter
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuProfile.MenuProfile

class Profile : Fragment() {

    private lateinit var textName: TextView
    private lateinit var textDescription: TextView

    private var tvAvgResponseTime: TextView? = null
    private var tvOnTimeDeliveryRate: TextView? = null

    private var tabProfileVarian: View? = null
    private var tabProfileLelang: View? = null
    private var tabProfilePesanan: View? = null
    private var tabProfileUlasan: View? = null

    private var tvTabProfileVarian: TextView? = null
    private var tvTabProfileLelang: TextView? = null
    private var tvTabProfilePesanan: TextView? = null
    private var tvTabProfileUlasan: TextView? = null

    private var indicatorProfileVarian: View? = null
    private var indicatorProfileLelang: View? = null
    private var indicatorProfilePesanan: View? = null
    private var indicatorProfileUlasan: View? = null

    private var layoutContentProfileVarian: View? = null
    private var layoutContentProfileLelang: View? = null
    private var layoutContentProfilePesanan: View? = null
    private var layoutContentProfileUlasan: View? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        textName = view.findViewById(R.id.namaProfile)
        textDescription = view.findViewById(R.id.descriptionProfile)

        tvAvgResponseTime = view.findViewById(R.id.tvAvgResponseTime)
        tvOnTimeDeliveryRate = view.findViewById(R.id.tvOnTimeDeliveryRate)

        val btnEditProfile = view.findViewById<TextView>(R.id.btnEditProfile)
        val btnLogout = view.findViewById<TextView>(R.id.btnLogout)
        val btnTipsPerawatan = view.findViewById<View>(R.id.btnTipsPerawatanProfile)

        btnEditProfile?.setOnClickListener {
            startActivity(Intent(requireContext(), MenuProfile::class.java))
        }

        btnTipsPerawatan?.setOnClickListener {
            startActivity(Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuProfile.TipsPerawatanActivity::class.java))
        }

        val btnCreateAuction = view.findViewById<View>(R.id.btnCreateAuctionProfile)
        btnCreateAuction?.setOnClickListener {
            val intent = Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuAuction.CreateAuctionActivity::class.java)
            startActivity(intent)
        }

        val btnAddProductProfile = view.findViewById<View>(R.id.btnAddProductProfile)
        btnAddProductProfile?.setOnClickListener {
            val intent = Intent(requireContext(), com.stiki.marcelloilham.guppymarketidn.menuProfile.AddProductActivity::class.java)
            startActivity(intent)
        }

        btnLogout?.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
        }

        initTabs(view)
        setupTabClickListeners()
        selectTab(0)

        fetchUserData()
        setupProfileAuctions(view)
        setupProfileProducts(view)
        setupProfileOrders(view)

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshProfile)
        swipeRefresh?.setColorSchemeResources(R.color.guppy_primary)
        swipeRefresh?.setOnRefreshListener {
            fetchUserData()
            loadUserAuctions()
            loadUserProducts()
            swipeRefresh.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 1000)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (::recyclerProfileAuctions.isInitialized) {
            loadUserAuctions()
        }
        if (this::recyclerProfileProducts.isInitialized) {
            loadUserProducts()
        }
    }

    private fun initTabs(view: View) {
        tabProfileVarian = view.findViewById(R.id.tabProfileVarian)
        tabProfileLelang = view.findViewById(R.id.tabProfileLelang)
        tabProfilePesanan = view.findViewById(R.id.tabProfilePesanan)
        tabProfileUlasan = view.findViewById(R.id.tabProfileUlasan)

        tvTabProfileVarian = view.findViewById(R.id.tvTabProfileVarian)
        tvTabProfileLelang = view.findViewById(R.id.tvTabProfileLelang)
        tvTabProfilePesanan = view.findViewById(R.id.tvTabProfilePesanan)
        tvTabProfileUlasan = view.findViewById(R.id.tvTabProfileUlasan)

        indicatorProfileVarian = view.findViewById(R.id.indicatorProfileVarian)
        indicatorProfileLelang = view.findViewById(R.id.indicatorProfileLelang)
        indicatorProfilePesanan = view.findViewById(R.id.indicatorProfilePesanan)
        indicatorProfileUlasan = view.findViewById(R.id.indicatorProfileUlasan)

        layoutContentProfileVarian = view.findViewById(R.id.layoutContentProfileVarian)
        layoutContentProfileLelang = view.findViewById(R.id.layoutContentProfileLelang)
        layoutContentProfilePesanan = view.findViewById(R.id.layoutContentProfilePesanan)
        layoutContentProfileUlasan = view.findViewById(R.id.layoutContentProfileUlasan)
    }

    private fun setupTabClickListeners() {
        tabProfileVarian?.setOnClickListener { selectTab(0) }
        tabProfileLelang?.setOnClickListener { selectTab(1) }
        tabProfilePesanan?.setOnClickListener { selectTab(2) }
        tabProfileUlasan?.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(index: Int) {
        val safeContext = context ?: return
        val primaryColor = ContextCompat.getColor(safeContext, R.color.guppy_primary)
        val grayColor = ContextCompat.getColor(safeContext, R.color.guppy_text_gray)
        val strokeColor = ContextCompat.getColor(safeContext, R.color.guppy_stroke)
        val density = resources.displayMetrics.density

        val textViews = listOfNotNull(tvTabProfileVarian, tvTabProfileLelang, tvTabProfilePesanan, tvTabProfileUlasan)
        val indicators = listOfNotNull(indicatorProfileVarian, indicatorProfileLelang, indicatorProfilePesanan, indicatorProfileUlasan)
        val contents = listOfNotNull(layoutContentProfileVarian, layoutContentProfileLelang, layoutContentProfilePesanan, layoutContentProfileUlasan)

        textViews.forEachIndexed { i, tv ->
            if (i == index) {
                tv.setTextColor(primaryColor)
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(safeContext, R.font.inter_bold)
            } else {
                tv.setTextColor(grayColor)
                tv.typeface = androidx.core.content.res.ResourcesCompat.getFont(safeContext, R.font.inter_medium)
            }
        }

        indicators.forEachIndexed { i, v ->
            val lp = v.layoutParams
            if (i == index) {
                v.setBackgroundColor(primaryColor)
                lp.height = (2.5f * density).toInt()
            } else {
                v.setBackgroundColor(strokeColor)
                lp.height = (1f * density).toInt()
            }
            v.layoutParams = lp
        }

        contents.forEachIndexed { i, c ->
            c.visibility = if (i == index) View.VISIBLE else View.GONE
        }
    }

    private fun fetchUserData() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val userId = firebaseUser.uid
            val userDocRef = db.collection("user").document(userId)

            userDocRef.get().addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name").orEmpty()
                    val description = document.getString("description").orEmpty()

                    val totalChats = document.getLong("total_chats") ?: 3L
                    val totalResponseMin = document.getLong("total_response_time_minutes") ?: 30L
                    val totalOrders = document.getLong("total_orders") ?: 100L
                    val ontimeOrders = document.getLong("ontime_orders_count") ?: 98L

                    val activeBalance = document.getLong("active_balance") ?: 0L
                    val pendingBalance = document.getLong("pending_balance") ?: 0L
                    val pendingOrdersCount = document.getLong("pending_orders_count") ?: 0L

                    val avgMin = if (totalChats > 0) totalResponseMin / totalChats else 10L
                    val deliveryRate = if (totalOrders > 0) ((ontimeOrders.toDouble() / totalOrders.toDouble()) * 100).toInt() else 98

                    if (name.isNotEmpty()) textName.text = name
                    if (description.isNotEmpty()) textDescription.text = description

                    val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("in", "ID"))
                    val tvSaldo = view?.findViewById<TextView>(R.id.tvProfileSaldoAktif)
                    val tvTertahan = view?.findViewById<TextView>(R.id.tvProfileDanaTertahan)

                    val tvFollowers = view?.findViewById<TextView>(R.id.tvProfileFollowersCount)
                    val tvSoldItems = view?.findViewById<TextView>(R.id.tvProfileSoldItemsCount)
                    val tvReviewCountMain = view?.findViewById<TextView>(R.id.tvProfileReviewCountMain)
                    val tvRating = view?.findViewById<TextView>(R.id.tvProfileRating)
                    val tvReviewsLabel = view?.findViewById<TextView>(R.id.tvProfileReviewsLabel)

                    val followers = document.getLong("followers_count") ?: 0L
                    val totalSold = document.getLong("total_sold_count") ?: 0L
                    val totalReviews = document.getLong("total_reviews_count") ?: 0L
                    val ratingVal = document.getDouble("average_rating") ?: 0.0

                    tvSaldo?.text = formatter.format(activeBalance).replace("Rp", "Rp ").replace(",00", "")
                    tvTertahan?.text = "Dana tertahan ${formatter.format(pendingBalance).replace("Rp", "Rp ").replace(",00", "")} ($pendingOrdersCount pesanan dikirim)"

                    tvAvgResponseTime?.text = "$avgMin menit"
                    tvOnTimeDeliveryRate?.text = "$deliveryRate%"

                    tvFollowers?.text = followers.toString()
                    tvSoldItems?.text = totalSold.toString()
                    tvReviewCountMain?.text = totalReviews.toString()
                    tvRating?.text = ratingVal.toString()
                    tvReviewsLabel?.text = "dari $totalReviews ulasan"

                    saveUserDataLocally(name, description)
                    Log.d("Profile", "Data retrieved: Name=$name, Description=$description")

                    FirestoreRepository.fetchUserOrders { userOrders ->
                        Log.d("ProfileOrders", "Fetched ${userOrders.size} user orders from Firestore")
                    }
                } else {
                    Log.e("Profile", "No document exists for user.")
                    loadUserDataLocally()
                }
            }.addOnFailureListener { error ->
                Log.e("Profile", "Error retrieving data: ", error)
                loadUserDataLocally()
            }
        } else {
            loadUserDataLocally()
        }
    }

    private fun saveUserDataLocally(name: String, description: String) {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("name", name)
            putString("description", description)
            apply()
        }
    }

    private fun loadUserDataLocally() {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("name", "")
        val savedDesc = prefs.getString("description", "")

        if (!savedName.isNullOrEmpty()) textName.text = savedName
        if (!savedDesc.isNullOrEmpty()) textDescription.text = savedDesc
    }

    private lateinit var recyclerProfileAuctions: RecyclerView
    private lateinit var myAuctionListAdapter: MyAuctionListAdapter
    private lateinit var recyclerProfileProducts: RecyclerView

    private fun setupProfileAuctions(view: View) {
        recyclerProfileAuctions = view.findViewById(R.id.recyclerProfileAuctions)
        recyclerProfileAuctions.layoutManager = LinearLayoutManager(requireContext())

        loadUserAuctions()
    }

    private fun loadUserAuctions() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"

        FirestoreRepository.fetchUserAuctions(currentUserId) { myAuctions ->
            if (isAdded && ::recyclerProfileAuctions.isInitialized) {
                myAuctionListAdapter = MyAuctionListAdapter(requireContext(), myAuctions) { auctionToDelete ->
                    FirestoreRepository.deleteAuction(auctionToDelete.id) { _ ->
                        Toast.makeText(requireContext(), "Lelang '${auctionToDelete.title}' Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                        loadUserAuctions()
                    }
                }
                recyclerProfileAuctions.adapter = myAuctionListAdapter

                val tvEmpty = view?.findViewById<TextView>(R.id.tvEmptyProfileAuctions)
                if (myAuctions.isEmpty()) {
                    recyclerProfileAuctions.visibility = View.GONE
                    tvEmpty?.visibility = View.VISIBLE
                } else {
                    recyclerProfileAuctions.visibility = View.VISIBLE
                    tvEmpty?.visibility = View.GONE
                }
            }
        }
    }

    private fun setupProfileProducts(view: View) {
        recyclerProfileProducts = view.findViewById(R.id.recyclerProfileProducts)
        recyclerProfileProducts.layoutManager = LinearLayoutManager(requireContext())
        loadUserProducts()
    }

    private fun loadUserProducts() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"

        FirestoreRepository.fetchUserProducts(currentUserId) { products ->
            if (isAdded && ::recyclerProfileProducts.isInitialized) {
                val tvEmptyProducts = view?.findViewById<TextView>(R.id.tvEmptyProfileProducts)
                if (products.isEmpty()) {
                    recyclerProfileProducts.visibility = View.GONE
                    tvEmptyProducts?.visibility = View.VISIBLE
                } else {
                    recyclerProfileProducts.visibility = View.VISIBLE
                    tvEmptyProducts?.visibility = View.GONE
                    recyclerProfileProducts.adapter = com.stiki.marcelloilham.guppymarketidn.adapter.ProfileProductAdapter(
                        context = requireContext(),
                        products = products,
                        onManageClicked = { productToEdit ->
                            showEditProductDialog(productToEdit)
                        },
                        onDeleteClicked = { productToDelete ->
                            FirestoreRepository.deleteProduct(productToDelete.id) { success ->
                                if (success) {
                                    Toast.makeText(requireContext(), "Varian '${productToDelete.title}' Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                                    loadUserProducts()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun showEditProductDialog(product: FirestoreRepository.ProductModel) {
        val safeContext = context ?: return
        val layout = android.widget.LinearLayout(safeContext).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val tvPriceLabel = TextView(safeContext).apply {
            text = "Harga Jual Baru (Rp):"
            setTextColor(ContextCompat.getColor(safeContext, R.color.guppy_text_black))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(safeContext, R.font.inter_bold)
        }
        val etPrice = android.widget.EditText(safeContext).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "misal: 150000"
            setText(product.price.replace("Rp", "").replace(".", "").replace(" ", "").trim())
        }

        val tvStockLabel = TextView(safeContext).apply {
            text = "Jumlah Stok Tersedia:"
            setTextColor(ContextCompat.getColor(safeContext, R.color.guppy_text_black))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(safeContext, R.font.inter_bold)
            setPadding(0, 30, 0, 0)
        }
        val etStock = android.widget.EditText(safeContext).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "misal: 10"
            setText(product.sold)
        }

        layout.addView(tvPriceLabel)
        layout.addView(etPrice)
        layout.addView(tvStockLabel)
        layout.addView(etStock)

        androidx.appcompat.app.AlertDialog.Builder(safeContext)
            .setTitle("Kelola Varian ${product.title}")
            .setView(layout)
            .setPositiveButton("Simpan Perubahan") { _, _ ->
                val newPriceInput = etPrice.text.toString().trim()
                val newStockInput = etStock.text.toString().trim()

                val parsedPrice = newPriceInput.toLongOrNull() ?: 150000L
                val formattedPrice = "Rp ${java.text.NumberFormat.getInstance(java.util.Locale("in", "ID")).format(parsedPrice)}"

                FirestoreRepository.updateProductPriceAndStock(product.id, formattedPrice, newStockInput) { success ->
                    if (success) {
                        Toast.makeText(safeContext, "Stok & Harga ${product.title} Berhasil Diperbarui!", Toast.LENGTH_SHORT).show()
                        loadUserProducts()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupProfileOrders(view: View) {
        val recyclerOrders = view.findViewById<RecyclerView>(R.id.recyclerProfileOrders)
        val tvEmptyOrders = view.findViewById<TextView>(R.id.tvEmptyProfileOrders)
        recyclerOrders?.layoutManager = LinearLayoutManager(requireContext())

        FirestoreRepository.fetchUserOrders { orders ->
            if (isAdded && recyclerOrders != null) {
                if (orders.isEmpty()) {
                    recyclerOrders.visibility = View.GONE
                    tvEmptyOrders?.visibility = View.VISIBLE
                } else {
                    recyclerOrders.visibility = View.VISIBLE
                    tvEmptyOrders?.visibility = View.GONE
                    recyclerOrders.adapter = com.stiki.marcelloilham.guppymarketidn.adapter.ProfileOrderAdapter(requireContext(), orders)
                }
            }
        }
    }
}
