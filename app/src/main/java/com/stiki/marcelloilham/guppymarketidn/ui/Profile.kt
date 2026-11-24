package com.stiki.marcelloilham.guppymarketidn.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuProfile.MenuProfile

class Profile : Fragment() {

    private lateinit var textName: TextView
    private lateinit var textDescription: TextView
    private lateinit var btnMenuProfile: View

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

        val btnEditProfile = view.findViewById<TextView>(R.id.btnEditProfile)
        val btnLogout = view.findViewById<TextView>(R.id.btnLogout)

        btnEditProfile?.setOnClickListener {
            startActivity(Intent(requireContext(), MenuProfile::class.java))
        }

        btnLogout?.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(requireContext(), "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
        }

        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val userId = firebaseUser.uid
            val userDocRef = db.collection("user").document(userId)

            userDocRef.get().addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name").orEmpty()
                    val description = document.getString("description").orEmpty()

                    if (name.isNotEmpty()) textName.text = name
                    if (description.isNotEmpty()) textDescription.text = description

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
