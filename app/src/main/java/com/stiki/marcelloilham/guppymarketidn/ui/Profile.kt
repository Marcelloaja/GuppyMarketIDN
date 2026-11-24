package com.stiki.marcelloilham.guppymarketidn.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuProfile.MenuProfile

class Profile : Fragment() {

    private lateinit var textName: TextView
    private lateinit var textDescription: TextView
    private lateinit var btnMenuProfile: View

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
        btnMenuProfile = view.findViewById(R.id.btnMenuProfile)

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

        btnLogout?.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
        }

        initTabs(view)
        setupTabClickListeners()
        selectTab(0)

        fetchUserData()

        btnMenuProfile.setOnClickListener {
            val animContext = context ?: return@setOnClickListener
            val animation = AnimationUtils.loadAnimation(animContext, R.anim.button_click)
            btnMenuProfile.startAnimation(animation)
            btnMenuProfile.postDelayed({
                if (isAdded) {
                    startActivity(Intent(activity, MenuProfile::class.java))
                }
            }, animation.duration)
        }

        return view
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

                    val avgMin = if (totalChats > 0) totalResponseMin / totalChats else 10L
                    val deliveryRate = if (totalOrders > 0) ((ontimeOrders.toDouble() / totalOrders.toDouble()) * 100).toInt() else 98

                    if (name.isNotEmpty()) textName.text = name
                    if (description.isNotEmpty()) textDescription.text = description

                    tvAvgResponseTime?.text = "$avgMin menit"
                    tvOnTimeDeliveryRate?.text = "$deliveryRate%"

                    saveUserDataLocally(name, description)
                    Log.d("Profile", "Data retrieved: Name=$name, Description=$description")
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
}
