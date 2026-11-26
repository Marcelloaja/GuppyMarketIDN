package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.ProfileOrderAdapter
import com.stiki.marcelloilham.guppymarketidn.menuHome.OrderTrackingActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class SellerOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerSellerOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabSemua: TextView
    private lateinit var tabPerluDikemas: TextView
    private lateinit var tabDikirim: TextView
    private lateinit var tabSelesai: TextView

    private var allOrdersList: List<FirestoreRepository.OrderModel> = emptyList()
    private var currentFilter: String = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_orders)

        val topBar = findViewById<View>(R.id.topBarSellerOrders)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackSellerOrders)
        btnBack?.setOnClickListener { finish() }

        initViews()
        setupTabs()
        loadSellerOrders()
    }

    private fun initViews() {
        recyclerSellerOrders = findViewById(R.id.recyclerSellerOrders)
        recyclerSellerOrders.layoutManager = LinearLayoutManager(this)
        tvEmpty = findViewById(R.id.tvEmptySellerOrders)

        tabSemua = findViewById(R.id.tabSellerOrderSemua)
        tabPerluDikemas = findViewById(R.id.tabSellerOrderPerluDikemas)
        tabDikirim = findViewById(R.id.tabSellerOrderDikirim)
        tabSelesai = findViewById(R.id.tabSellerOrderSelesai)
    }

    private fun setupTabs() {
        tabSemua.setOnClickListener {
            filterOrders("Semua")
            updateTabStyle(tabSemua)
        }
        tabPerluDikemas.setOnClickListener {
            filterOrders("Dikemas")
            updateTabStyle(tabPerluDikemas)
        }
        tabDikirim.setOnClickListener {
            filterOrders("Dikirim")
            updateTabStyle(tabDikirim)
        }
        tabSelesai.setOnClickListener {
            filterOrders("Selesai")
            updateTabStyle(tabSelesai)
        }
    }

    private fun updateTabStyle(selectedTab: TextView) {
        val tabs = listOf(tabSemua, tabPerluDikemas, tabDikirim, tabSelesai)
        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.bg_chip_selected)
                tab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                tab.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                tab.setBackgroundResource(R.drawable.bg_btn_outline_pill_gray)
                tab.backgroundTintList = null
                tab.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
            }
        }
    }

    private fun loadSellerOrders() {
        FirestoreRepository.fetchUserOrders { orders ->
            allOrdersList = orders
            filterOrders(currentFilter)
        }
    }

    private fun filterOrders(statusFilter: String) {
        currentFilter = statusFilter
        val filtered = if (statusFilter == "Semua") {
            allOrdersList
        } else {
            allOrdersList.filter { it.status.contains(statusFilter, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            recyclerSellerOrders.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerSellerOrders.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val adapter = ProfileOrderAdapter(this, filtered) { clickedOrder ->
                val intent = Intent(this, OrderTrackingActivity::class.java).apply {
                    putExtra("EXTRA_ORDER_ID", clickedOrder.id)
                    putExtra("EXTRA_PRODUCT_NAME", clickedOrder.productName)
                    putExtra("EXTRA_COURIER", clickedOrder.courier)
                    putExtra("EXTRA_TRACKING_NUMBER", clickedOrder.trackingNumber)
                }
                startActivity(intent)
            }
            recyclerSellerOrders.adapter = adapter
        }
    }
}
