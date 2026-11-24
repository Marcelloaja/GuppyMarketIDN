package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R

class MenuProfile : AppCompatActivity() {

    private lateinit var btnBackProfile: ImageView
    private lateinit var btnSaveBottom: TextView
    private lateinit var btnCancel: TextView

    private lateinit var etOwnerName: EditText
    private lateinit var etFarmName: EditText
    private lateinit var tvFarmLocation: TextView
    private lateinit var etWhatsapp: EditText
    private lateinit var etEmail: EditText
    private lateinit var etFarmDescription: EditText
    private lateinit var tvCharCount: TextView

    private lateinit var btnChangePhoto: TextView
    private lateinit var btnChangePhotoCamera: ImageView
    private lateinit var btnDeleteFarmAccount: TextView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_profile)

        val topBar = findViewById<View>(R.id.topBarEditProfile)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        initViews()
        setupListeners()
        loadProfileData()
    }

    private fun initViews() {
        btnBackProfile = findViewById(R.id.btnBackProfile)
        btnSaveBottom = findViewById(R.id.btnSaveBottom)
        btnCancel = findViewById(R.id.btnCancel)

        etOwnerName = findViewById(R.id.etOwnerName)
        etFarmName = findViewById(R.id.etFarmName)
        tvFarmLocation = findViewById(R.id.tvFarmLocation)
        etWhatsapp = findViewById(R.id.etWhatsapp)
        etEmail = findViewById(R.id.etEmail)
        etFarmDescription = findViewById(R.id.etFarmDescription)
        tvCharCount = findViewById(R.id.tvCharCount)

        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnChangePhotoCamera = findViewById(R.id.btnChangePhotoCamera)
        btnDeleteFarmAccount = findViewById(R.id.btnDeleteFarmAccount)
    }

    private fun setupListeners() {
        btnBackProfile.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        btnSaveBottom.setOnClickListener { saveProfileData() }

        // Character counter for farm description
        etFarmDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len/300"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnChangePhoto.setOnClickListener {
            Toast.makeText(this, "Pilih foto dari galeri", Toast.LENGTH_SHORT).show()
        }
        btnChangePhotoCamera.setOnClickListener {
            Toast.makeText(this, "Buka kamera", Toast.LENGTH_SHORT).show()
        }

        btnDeleteFarmAccount.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Hapus Akun Farm")
                .setMessage("Apakah Anda yakin ingin menghapus akun farm ini? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Hapus") { dialog, _ ->
                    Toast.makeText(this, "Permintaan hapus akun diproses", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    private fun loadProfileData() {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString("name", "Marcello Aja")
        val desc = prefs.getString("description", "Breeder guppy Bekasi. Fokus ikan varian warna dan kualitas sehat, dikirim dengan kemasan oksigen.")

        if (!name.isNullOrEmpty()) etOwnerName.setText(name)
        if (!desc.isNullOrEmpty()) {
            etFarmDescription.setText(desc)
            tvCharCount.text = "${desc.length}/300"
        }
    }

    private fun saveProfileData() {
        val ownerName = etOwnerName.text.toString().trim()
        val farmName = etFarmName.text.toString().trim()
        val desc = etFarmDescription.text.toString().trim()

        if (ownerName.isEmpty()) {
            etOwnerName.error = "Nama pemilik wajib diisi"
            return
        }

        // Save locally
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("name", ownerName)
            putString("farm_name", farmName)
            putString("description", desc)
            apply()
        }

        // Save to Firebase Firestore if logged in
        val user = firebaseAuth.currentUser
        if (user != null) {
            val userMap = hashMapOf<String, Any>(
                "name" to ownerName,
                "farm_name" to farmName,
                "description" to desc
            )
            db.collection("user").document(user.uid).update(userMap)
        }

        Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
