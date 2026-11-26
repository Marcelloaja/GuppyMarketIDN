package com.stiki.marcelloilham.guppymarketidn.menuProfile

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.ProfileOrderAdapter
import com.stiki.marcelloilham.guppymarketidn.menuHome.OrderTrackingActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.PaymentPendingActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerMyOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tabSemua: TextView
    private lateinit var tabMenunggu: TextView
    private lateinit var tabDikirim: TextView
    private lateinit var tabSelesai: TextView

    private var allOrdersList: List<FirestoreRepository.OrderModel> = emptyList()
    private var currentFilter: String = "Semua"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        val topBar = findViewById<View>(R.id.topBarMyOrders)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackMyOrders)
        btnBack?.setOnClickListener { finish() }

        initViews()
        setupTabs()
        loadUserOrders()
    }

    private fun initViews() {
        recyclerMyOrders = findViewById(R.id.recyclerMyOrders)
        recyclerMyOrders.layoutManager = LinearLayoutManager(this)
        tvEmpty = findViewById(R.id.tvEmptyMyOrders)

        tabSemua = findViewById(R.id.tabMyOrderSemua)
        tabMenunggu = findViewById(R.id.tabMyOrderMenunggu)
        tabDikirim = findViewById(R.id.tabMyOrderDikirim)
        tabSelesai = findViewById(R.id.tabMyOrderSelesai)
    }

    private fun setupTabs() {
        tabSemua.setOnClickListener {
            filterOrders("Semua")
            updateTabStyle(tabSemua)
        }
        tabMenunggu.setOnClickListener {
            filterOrders("Menunggu Pembayaran")
            updateTabStyle(tabMenunggu)
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
        val tabs = listOf(tabSemua, tabMenunggu, tabDikirim, tabSelesai)
        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.bg_btn_primary)
                tab.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
                tab.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                tab.setBackgroundResource(R.drawable.bg_btn_solid_gray)
                tab.backgroundTintList = null
                tab.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
            }
        }
    }

    private fun loadUserOrders() {
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
            allOrdersList.filter { it.status.equals(statusFilter, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            recyclerMyOrders.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerMyOrders.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val adapter = ProfileOrderAdapter(this, filtered) { clickedOrder ->
                if (clickedOrder.status.contains("Menunggu", true)) {
                    val intent = Intent(this, PaymentPendingActivity::class.java).apply {
                        putExtra("EXTRA_ORDER_ID", clickedOrder.id)
                        putExtra("EXTRA_PRODUCT_NAME", clickedOrder.productName)
                        putExtra("EXTRA_QUANTITY", clickedOrder.quantity)
                        putExtra("EXTRA_TOTAL_AMOUNT", clickedOrder.totalAmount)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this, OrderTrackingActivity::class.java).apply {
                        putExtra("EXTRA_ORDER_ID", clickedOrder.id)
                        putExtra("EXTRA_PRODUCT_NAME", clickedOrder.productName)
                        putExtra("EXTRA_COURIER", clickedOrder.courier)
                        putExtra("EXTRA_TRACKING_NUMBER", clickedOrder.trackingNumber)
                    }
                    startActivity(intent)
                }
            }
            recyclerMyOrders.adapter = adapter
        }
    }
}
