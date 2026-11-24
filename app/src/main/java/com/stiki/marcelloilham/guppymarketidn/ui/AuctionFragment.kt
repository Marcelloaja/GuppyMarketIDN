package com.stiki.marcelloilham.guppymarketidn.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.AuctionGridAdapter
import com.stiki.marcelloilham.guppymarketidn.adapter.HomeCategoryAdapter
import com.stiki.marcelloilham.guppymarketidn.menuAuction.DetailAuctionActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.CategoryItem
import com.stiki.marcelloilham.guppymarketidn.menuHome.KeranjangHome

class AuctionFragment : Fragment() {

    private lateinit var recyclerCategories: RecyclerView
    private lateinit var recyclerAuctions: RecyclerView
    private lateinit var categoryAdapter: HomeCategoryAdapter
    private lateinit var auctionAdapter: AuctionGridAdapter

    private val categoriesList = listOf(
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
        setupCategories()
        setupAuctions()
        setupTopBar(view)
        setupSwipeRefresh(view)

        return view
    }

    private fun setupSwipeRefresh(view: View) {
        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefresh?.setColorSchemeResources(R.color.guppy_primary)
        swipeRefresh?.setOnRefreshListener {
            setupAuctions()
            swipeRefresh.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 1000)
        }
    }

    private fun initViews(view: View) {
        recyclerCategories = view.findViewById(R.id.recyclerAuctionCategories)
        recyclerAuctions = view.findViewById(R.id.recyclerAuctionGrid)
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

    private fun setupCategories() {
        categoryAdapter = HomeCategoryAdapter(requireContext(), categoriesList) { item, pos ->
            categoryAdapter.setSelectedPosition(pos)
        }
        recyclerCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerCategories.adapter = categoryAdapter
    }

    private fun setupAuctions() {
        val auctionItems = listOf(
            AuctionGridAdapter.AuctionItem(
                id = 1,
                title = "Blue Gold Mosaic",
                seller = "Guppy Nusantara",
                location = "Bandung",
                price = "Rp 850.000",
                bids = 12,
                timeLeft = "00:42:10",
                imageRes = R.drawable.bg_image_grid,
                grade = "Grade A+"
            ),
            AuctionGridAdapter.AuctionItem(
                id = 2,
                title = "Red Koi Grade S",
                seller = "Aquatic Lab",
                location = "Jakarta",
                price = "Rp 1.250.000",
                bids = 9,
                timeLeft = "01:05:33",
                imageRes = R.drawable.platinumrd,
                grade = "Grade S"
            ),
            AuctionGridAdapter.AuctionItem(
                id = 3,
                title = "Sky Blue Albino",
                seller = "FinFantasy Farm",
                location = "Malang",
                price = "Rp 550.000",
                bids = 24,
                timeLeft = "03:15:00",
                imageRes = R.drawable.skyblue,
                grade = "Grade A"
            ),
            AuctionGridAdapter.AuctionItem(
                id = 4,
                title = "Golden Tuxedo",
                seller = "Breeder Lokal",
                location = "Surabaya",
                price = "Rp 320.000",
                bids = 5,
                timeLeft = "05:12:00",
                imageRes = R.drawable.gsrts,
                grade = "Grade B"
            )
        )

        auctionAdapter = AuctionGridAdapter(requireContext(), auctionItems) { item ->
            val intent = Intent(requireContext(), DetailAuctionActivity::class.java)
            intent.putExtra("auction_title", item.title)
            startActivity(intent)
        }

        recyclerAuctions.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerAuctions.adapter = auctionAdapter
        recyclerAuctions.isNestedScrollingEnabled = false
    }
}
