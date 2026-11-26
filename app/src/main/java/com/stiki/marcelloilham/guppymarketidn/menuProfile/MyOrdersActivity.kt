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
import com.stiki.marcelloilham.guppymarketidn.adapter.MyOrderAdapter
import com.stiki.marcelloilham.guppymarketidn.menuHome.OrderTrackingActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.PaymentPendingActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerMyOrders: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var tvCountDikemas: TextView
    private lateinit var tvCountSiapKirim: TextView
    private lateinit var tvCountDikirim: TextView

    private lateinit var tabSemua: TextView
    private lateinit var tabDikirim: TextView
    private lateinit var tabSelesai: TextView
    private lateinit var tabDibatalkan: TextView

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

        tvCountDikemas = findViewById(R.id.tvCountMyOrderDikemas)
        tvCountSiapKirim = findViewById(R.id.tvCountMyOrderSiapKirim)
        tvCountDikirim = findViewById(R.id.tvCountMyOrderDikirim)

        tabSemua = findViewById(R.id.tabMyOrderSemua)
        tabDikirim = findViewById(R.id.tabMyOrderDikirim)
        tabSelesai = findViewById(R.id.tabMyOrderSelesai)
        tabDibatalkan = findViewById(R.id.tabMyOrderDibatalkan)
    }

    private fun setupTabs() {
        tabSemua.setOnClickListener {
            filterOrders("Semua")
            updateTabStyle(tabSemua)
        }
        tabDikirim.setOnClickListener {
            filterOrders("Dikirim")
            updateTabStyle(tabDikirim)
        }
        tabSelesai.setOnClickListener {
            filterOrders("Selesai")
            updateTabStyle(tabSelesai)
        }
        tabDibatalkan.setOnClickListener {
            filterOrders("Dibatalkan")
            updateTabStyle(tabDibatalkan)
        }
    }

    private fun updateTabStyle(selectedTab: TextView) {
        val tabs = listOf(tabSemua, tabDikirim, tabSelesai, tabDibatalkan)
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

            val countDikemas = orders.count { it.status.contains("Dikemas", true) || it.status.contains("Perlu", true) }
            val countSiap = orders.count { it.status.contains("Siap", true) }
            val countDikirim = orders.count { it.status.contains("Dikirim", true) }

            tvCountDikemas.text = countDikemas.toString()
            tvCountSiapKirim.text = countSiap.toString()
            tvCountDikirim.text = countDikirim.toString()

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
            recyclerMyOrders.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerMyOrders.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val adapter = MyOrderAdapter(this, filtered) { clickedOrder ->
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
