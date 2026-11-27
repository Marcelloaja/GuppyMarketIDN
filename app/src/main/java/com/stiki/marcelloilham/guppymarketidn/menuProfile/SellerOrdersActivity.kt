package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.SellerOrderAdapter
import com.stiki.marcelloilham.guppymarketidn.menuHome.OrderTrackingActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class SellerOrdersActivity : AppCompatActivity() {

    private lateinit var recyclerSellerOrders: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var tvCountPerluDikemas: TextView
    private lateinit var tvCountSiapKirim: TextView
    private lateinit var tvCountDikirim: TextView

    private lateinit var tabSemua: TextView
    private lateinit var tabPerluDikemas: TextView
    private lateinit var tabSiapKirim: TextView
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

        tvCountPerluDikemas = findViewById(R.id.tvCountPerluDikemas)
        tvCountSiapKirim = findViewById(R.id.tvCountSiapKirim)
        tvCountDikirim = findViewById(R.id.tvCountDikirim)

        tabSemua = findViewById(R.id.tabSellerOrderSemua)
        tabPerluDikemas = findViewById(R.id.tabSellerOrderPerluDikemas)
        tabSiapKirim = findViewById(R.id.tabSellerOrderSiapKirim)
        tabDikirim = findViewById(R.id.tabSellerOrderDikirim)
        tabSelesai = findViewById(R.id.tabSellerOrderSelesai)

        val btnHelp = findViewById<View>(R.id.btnHelpSellerOrders)
        btnHelp?.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Panduan Pesanan Toko")
                .setMessage("Kemas ikan menggunakan kantong ganda & oksigen. Masukkan nomor resi ekspedisi lalu tekan 'Tandai Dikirim' agar status terhubung secara otomatis.")
                .setPositiveButton("Mengerti", null)
                .show()
        }
    }

    private fun setupTabs() {
        tabSemua.setOnClickListener {
            filterOrders("Semua")
            updateTabStyle(tabSemua)
        }
        tabPerluDikemas.setOnClickListener {
            filterOrders("Perlu Dikemas")
            updateTabStyle(tabPerluDikemas)
        }
        tabSiapKirim.setOnClickListener {
            filterOrders("Siap Kirim")
            updateTabStyle(tabSiapKirim)
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
        val tabs = listOf(tabSemua, tabPerluDikemas, tabSiapKirim, tabDikirim, tabSelesai)
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

    private fun loadSellerOrders() {
        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchSellerOrders(currentUid) { orders ->
            allOrdersList = orders

            val countDikemas = orders.count { it.status.contains("Dikemas", true) || it.status.contains("Perlu", true) || it.status.contains("Menunggu", true) }
            val countSiap = orders.count { it.status.contains("Siap", true) }
            val countDikirim = orders.count { it.status.contains("Dikirim", true) }

            tvCountPerluDikemas.text = countDikemas.toString()
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
            recyclerSellerOrders.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recyclerSellerOrders.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val adapter = SellerOrderAdapter(
                context = this,
                orders = filtered,
                onMarkShippedClicked = { orderToShip, resiNumber ->
                    FirestoreRepository.updateOrderStatusAndResi(orderToShip.id, "Dikirim", resiNumber) { success ->
                        if (success) {
                            Toast.makeText(this, "Pesanan ${orderToShip.id} dengan Resi $resiNumber Ditandai Dikirim! 🚚", Toast.LENGTH_LONG).show()
                            loadSellerOrders()
                        } else {
                            Toast.makeText(this, "Gagal memperbarui status pesanan.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onPrintLabelClicked = { printOrder ->
                    Toast.makeText(this, "Label pengiriman #${printOrder.id} dicetak ke PDF!", Toast.LENGTH_SHORT).show()
                }
            )
            recyclerSellerOrders.adapter = adapter
        }
    }
}
