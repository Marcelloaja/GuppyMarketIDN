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
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
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
        FirestoreRepository.fetchAuctions { firebaseAuctions ->
            val finalItems = if (firebaseAuctions.isNotEmpty()) {
                firebaseAuctions.mapIndexed { idx, a ->
                    AuctionGridAdapter.AuctionItem(
                        id = idx + 1,
                        title = a.title,
                        seller = a.seller,
                        location = a.location,
                        price = a.currentBid,
                        bids = a.bidsCount,
                        timeLeft = "02:14:35",
                        imageRes = a.imageRes,
                        grade = a.grade
                    )
                }
            } else {
                listOf(
                    AuctionGridAdapter.AuctionItem(1, "Blue Gold Mosaic", "Guppy Nusantara", "Bandung", "Rp 850.000", 12, "00:42:10", R.drawable.bg_image_grid, "Grade A+"),
                    AuctionGridAdapter.AuctionItem(2, "Red Koi Grade S", "Aquatic Lab", "Jakarta", "Rp 1.250.000", 9, "01:05:33", R.drawable.platinumrd, "Grade S"),
                    AuctionGridAdapter.AuctionItem(3, "Sky Blue Albino", "FinFantasy Farm", "Malang", "Rp 550.000", 24, "03:15:00", R.drawable.skyblue, "Grade A"),
                    AuctionGridAdapter.AuctionItem(4, "Golden Tuxedo", "Breeder Lokal", "Surabaya", "Rp 320.000", 5, "05:12:00", R.drawable.gsrts, "Grade B")
                )
            }

            if (isAdded) {
                auctionAdapter = AuctionGridAdapter(requireContext(), finalItems) { item ->
                    val intent = Intent(requireContext(), DetailAuctionActivity::class.java)
                    intent.putExtra("auction_title", item.title)
                    startActivity(intent)
                }

                recyclerAuctions.layoutManager = GridLayoutManager(requireContext(), 2)
                recyclerAuctions.adapter = auctionAdapter
                recyclerAuctions.isNestedScrollingEnabled = false
            }
        }
    }
}
