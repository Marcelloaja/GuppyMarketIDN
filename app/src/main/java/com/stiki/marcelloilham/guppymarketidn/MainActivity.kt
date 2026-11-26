package com.stiki.marcelloilham.guppymarketidn

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.ui.AuctionFragment
import com.stiki.marcelloilham.guppymarketidn.ui.Community
import com.stiki.marcelloilham.guppymarketidn.ui.HomeFragment
import com.stiki.marcelloilham.guppymarketidn.ui.Maps
import com.stiki.marcelloilham.guppymarketidn.ui.Profile

class MainActivity : AppCompatActivity() {

    private lateinit var navview: BottomNavigationView
    private var backPressedTime: Long = 0
    private var currentFragmentTag: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        FirestoreRepository.seedInitialDataIfEmpty()

        navview = findViewById(R.id.nav_view)
        val cardBottomNav = findViewById<View>(R.id.cardBottomNav)

        // Responsif Window Insets: Menyesuaikan posisi Navigasi Melayang terhadap Gesture Bar / Nav Bar HP & Layar Lebar/Tablet
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            cardBottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                val baseBottomMargin = (16 * resources.displayMetrics.density).toInt()
                bottomMargin = systemBars.bottom + baseBottomMargin

                // Responsive width handling for tablets, foldables, and wide screen phones
                val screenWidthPx = resources.displayMetrics.widthPixels
                val maxNavWidthPx = (520 * resources.displayMetrics.density).toInt()
                if (screenWidthPx > maxNavWidthPx) {
                    width = maxNavWidthPx
                } else {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
            insets
        }

        // Check for extras in the intent
        val navigateTo = intent.getStringExtra("navigate_to")
        val navigateToTab = intent.getStringExtra("NAVIGATE_TO_TAB")

        if (navigateToTab == "MAPS" || navigateTo == "MapsFragment") {
            navview.selectedItemId = R.id.navigation_map
            replaceFragment(Maps(), "MapsFragment", false)
        } else if (navigateTo != null) {
            when (navigateTo) {
                "HomeFragment" -> {
                    navview.selectedItemId = R.id.navigation_home
                    replaceFragment(HomeFragment(), "HomeFragment", false)
                }
                "BidFragment" -> {
                    navview.selectedItemId = R.id.navigation_bid
                    replaceFragment(AuctionFragment(), "BidFragment", false)
                }
                "CommunityFragment" -> {
                    navview.selectedItemId = R.id.navigation_community
                    replaceFragment(Community(), "CommunityFragment", false)
                }
                "ProfileFragment" -> {
                    navview.selectedItemId = R.id.navigation_profile
                    replaceFragment(Profile(), "ProfileFragment", false)
                }
            }
        } else {
            // Load default fragment if no intent extras
            replaceFragment(HomeFragment(), "HomeFragment", false)
        }

        navview.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> replaceFragment(HomeFragment(), "HomeFragment", true)
                R.id.navigation_map -> replaceFragment(Maps(), "MapsFragment", true)
                R.id.navigation_bid -> replaceFragment(AuctionFragment(), "BidFragment", true)
                R.id.navigation_community -> replaceFragment(Community(), "CommunityFragment", true)
                R.id.navigation_profile -> replaceFragment(Profile(), "ProfileFragment", true)
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String, addToBackStack: Boolean) {
        if (currentFragmentTag == tag) return

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        if (addToBackStack) {
            fragmentTransaction.replace(R.id.navhost, fragment, tag)
            fragmentTransaction.addToBackStack(tag)
        } else {
            fragmentTransaction.replace(R.id.navhost, fragment, tag)
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        fragmentTransaction.commit()
        currentFragmentTag = tag
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navigateToTab = intent.getStringExtra("NAVIGATE_TO_TAB")
        if (navigateToTab == "MAPS") {
            navview.selectedItemId = R.id.navigation_map
            replaceFragment(Maps(), "MapsFragment", false)
        }
    }

    override fun onBackPressed() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            super.onBackPressed()
        } else {
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }
}
