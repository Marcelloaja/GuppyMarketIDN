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
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuProfile.MenuProfile

class Profile : Fragment() {

    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var textPhone: TextView
    private lateinit var textDescription: TextView
    private lateinit var btnMenuProfile: ImageView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        textName = view.findViewById(R.id.namaProfile)
        textEmail = view.findViewById(R.id.emailProfile)
        textPhone = view.findViewById(R.id.phoneProfile)
        textDescription = view.findViewById(R.id.descriptionProfile)
        btnMenuProfile = view.findViewById(R.id.btnMenuProfile)

        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            val userId = firebaseUser.uid
            val userDocRef = db.collection("user").document(userId)

            userDocRef.get().addOnSuccessListener { document ->
                if (isAdded && document != null && document.exists()) {
                    val name = document.getString("name").orEmpty()
                    val email = document.getString("email").orEmpty()
                    val phone = document.getString("phone").orEmpty()
                    val description = document.getString("description").orEmpty()

                    textName.text = if (name.isNotEmpty()) name else textName.text
                    textEmail.text = if (email.isNotEmpty()) email else textEmail.text
                    textPhone.text = if (phone.isNotEmpty()) phone else textPhone.text
                    textDescription.text = if (description.isNotEmpty()) description else textDescription.text

                    saveUserDataLocally(name, email, phone, description)
                    Log.d("Profile", "Data retrieved: Name=$name, Email=$email, Phone=$phone, Description=$description")
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

    private fun saveUserDataLocally(name: String, email: String, phone: String, description: String) {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("name", name)
            putString("email", email)
            putString("phone", phone)
            putString("description", description)
            apply()
        }
    }

    private fun loadUserDataLocally() {
        val safeContext = context ?: return
        val prefs = safeContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("name", "")
        val savedEmail = prefs.getString("email", "")
        val savedPhone = prefs.getString("phone", "")
        val savedDesc = prefs.getString("description", "")

        if (!savedName.isNullOrEmpty()) textName.text = savedName
        if (!savedEmail.isNullOrEmpty()) textEmail.text = savedEmail
        if (!savedPhone.isNullOrEmpty()) textPhone.text = savedPhone
        if (!savedDesc.isNullOrEmpty()) textDescription.text = savedDesc
    }
}
