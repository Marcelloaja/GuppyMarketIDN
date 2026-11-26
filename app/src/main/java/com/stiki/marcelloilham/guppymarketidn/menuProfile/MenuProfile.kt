package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.intro.Login

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
    private lateinit var tvBankAccountInfoSub: TextView

    private lateinit var btnChangePhoto: TextView
    private lateinit var btnChangePhotoCamera: ImageView
    private lateinit var btnDeleteFarmAccount: TextView
    private lateinit var imgEditAvatar: com.google.android.material.imageview.ShapeableImageView

    private lateinit var photoPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var selectedPhotoUri: android.net.Uri? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_profile)

        photoPickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                imgEditAvatar.setImageURI(uri)
                Toast.makeText(this, "Foto profil dipilih. Klik 'Simpan Profil' untuk menyimpan.", Toast.LENGTH_SHORT).show()
            }
        }

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

        imgEditAvatar = findViewById(R.id.imgEditAvatar)

        etOwnerName = findViewById(R.id.etOwnerName)
        etFarmName = findViewById(R.id.etFarmName)
        tvFarmLocation = findViewById(R.id.tvFarmLocation)
        etWhatsapp = findViewById(R.id.etWhatsapp)
        etEmail = findViewById(R.id.etEmail)
        etFarmDescription = findViewById(R.id.etFarmDescription)
        tvCharCount = findViewById(R.id.tvCharCount)
        tvBankAccountInfoSub = findViewById(R.id.tvBankAccountInfoSub)

        btnChangePhoto = findViewById(R.id.btnChangePhoto)
        btnChangePhotoCamera = findViewById(R.id.btnChangePhotoCamera)
        btnDeleteFarmAccount = findViewById(R.id.btnDeleteFarmAccount)
    }

    private fun setupListeners() {
        btnBackProfile.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        btnSaveBottom.setOnClickListener { saveProfileData() }

        etFarmDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len/300"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnChangePhoto.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }
        btnChangePhotoCamera.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        val btnSelectLocation = findViewById<View>(R.id.btnSelectLocation)
        btnSelectLocation?.setOnClickListener {
            val intent = Intent(this, ShippingAddressActivity::class.java)
            startActivity(intent)
        }

        val btnBankAccount = findViewById<View>(R.id.btnBankAccountSetting)
        btnBankAccount?.setOnClickListener {
            showBankAccountDialog()
        }

        val btnChangePin = findViewById<View>(R.id.btnChangeWithdrawPin)
        btnChangePin?.setOnClickListener {
            showChangePinDialog()
        }

        btnDeleteFarmAccount.setOnClickListener {
            showDeleteAccountValidationDialog()
        }
    }

    private fun showBankAccountDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etBankName = EditText(this).apply {
            hint = "Nama Bank (contoh: BCA / Mandiri / BRI)"
        }
        val etAccNumber = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Nomor Rekening / No. E-Wallet"
        }

        layout.addView(etBankName)
        layout.addView(etAccNumber)

        AlertDialog.Builder(this)
            .setTitle("Atur Rekening Pencairan Dana")
            .setView(layout)
            .setPositiveButton("Simpan Rekening") { _, _ ->
                val bank = etBankName.text.toString().trim()
                val acc = etAccNumber.text.toString().trim()
                if (bank.isNotEmpty() && acc.isNotEmpty()) {
                    val formatted = "$bank •••• ${acc.takeLast(4)}"
                    tvBankAccountInfoSub.text = formatted

                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        db.collection("user").document(user.uid).update("bank_account_info", formatted)
                    }
                    Toast.makeText(this, "Rekening $bank berhasil disimpan!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showChangePinDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etPin = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Masukkan 6-Digit PIN Baru"
        }

        layout.addView(etPin)

        AlertDialog.Builder(this)
            .setTitle("Ubah PIN Penarikan Dana")
            .setView(layout)
            .setPositiveButton("Simpan PIN") { _, _ ->
                val pin = etPin.text.toString().trim()
                if (pin.length == 6) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        db.collection("user").document(user.uid).update("withdrawal_pin", pin)
                    }
                    Toast.makeText(this, "PIN penarikan dana berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN harus 6 digit angka", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteAccountValidationDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val tvNotice = TextView(this).apply {
            text = "Ketik 'HAPUS AKUN' di bawah ini untuk memverifikasi penghapusan permanen seluruh data farm Anda:"
            setTextColor(ContextCompat.getColor(context, R.color.guppy_red))
            textSize = 13f
        }

        val etConfirmText = EditText(this).apply {
            hint = "HAPUS AKUN"
        }

        layout.addView(tvNotice)
        layout.addView(etConfirmText)

        AlertDialog.Builder(this)
            .setTitle("⚠️ Hapus Permanen Akun Farm")
            .setView(layout)
            .setPositiveButton("HAPUS SEKARANG") { _, _ ->
                val inputStr = etConfirmText.text.toString().trim()
                if (inputStr.equals("HAPUS AKUN", ignoreCase = true)) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        db.collection("user").document(user.uid).delete()
                        user.delete()
                    }
                    firebaseAuth.signOut()
                    Toast.makeText(this, "Akun farm Anda telah berhasil dihapus.", Toast.LENGTH_LONG).show()

                    val intent = Intent(this, Login::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Teks konfirmasi tidak cocok. Penghapusan dibatalkan.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadProfileData() {
        try {
            val currentUser = firebaseAuth.currentUser ?: return
            etEmail.setText(currentUser.email ?: "")

            val userId = currentUser.uid
            db.collection("user").document(userId).get()
                .addOnSuccessListener { doc ->
                    try {
                        if (isFinishing || isDestroyed) return@addOnSuccessListener
                        val name = doc.getString("name") ?: currentUser.displayName ?: ""
                        val farmName = doc.getString("farm_name") ?: ""
                        val loc = doc.getString("location") ?: doc.getString("full_address") ?: ""
                        val wa = doc.getString("whatsapp") ?: doc.getString("phone") ?: ""
                        val desc = doc.getString("description") ?: ""
                        val bank = doc.getString("bank_account_info") ?: "Belum diatur"
                        val avatarUriStr = doc.getString("avatar_uri") ?: doc.getString("photo_url")

                        etOwnerName.setText(name)
                        etFarmName.setText(farmName)
                        etWhatsapp.setText(wa)
                        etFarmDescription.setText(desc)
                        tvCharCount.text = "${desc.length}/300"
                        tvBankAccountInfoSub.text = bank

                        if (!avatarUriStr.isNullOrEmpty()) {
                            try {
                                imgEditAvatar.setImageURI(android.net.Uri.parse(avatarUriStr))
                            } catch (_: Exception) {}
                        }

                        if (loc.isNotEmpty()) {
                            tvFarmLocation.text = loc
                        } else {
                            // Fetch real addresses if location field is empty
                            FirestoreRepository.fetchUserAddresses(userId) { addresses ->
                                if (!isFinishing && !isDestroyed) {
                                    val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
                                    if (defaultAddr != null) {
                                        tvFarmLocation.text = defaultAddr.fullAddress
                                        db.collection("user").document(userId).update("location", defaultAddr.fullAddress)
                                    } else {
                                        tvFarmLocation.text = "Belum ada alamat terdaftar"
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun copyUriToInternalStorage(uri: android.net.Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(filesDir, "avatar_${firebaseAuth.currentUser?.uid ?: "user"}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            android.net.Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveProfileData() {
        val ownerName = etOwnerName.text.toString().trim()
        val farmName = etFarmName.text.toString().trim()
        val locationStr = tvFarmLocation.text.toString().trim()
        val whatsappStr = etWhatsapp.text.toString().trim()
        val desc = etFarmDescription.text.toString().trim()

        if (ownerName.isEmpty()) {
            etOwnerName.error = "Nama pemilik wajib diisi"
            return
        }

        var savedAvatarUriString: String? = null
        if (selectedPhotoUri != null) {
            savedAvatarUriString = copyUriToInternalStorage(selectedPhotoUri!!) ?: selectedPhotoUri.toString()
        }

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("name", ownerName)
            putString("farm_name", farmName)
            putString("description", desc)
            if (savedAvatarUriString != null) {
                putString("avatar_uri", savedAvatarUriString)
            }
            apply()
        }

        val user = firebaseAuth.currentUser
        if (user != null) {
            val userMap = hashMapOf<String, Any>(
                "name" to ownerName,
                "farm_name" to farmName,
                "location" to locationStr,
                "whatsapp" to whatsappStr,
                "description" to desc
            )
            if (savedAvatarUriString != null) {
                userMap["avatar_uri"] = savedAvatarUriString
                userMap["photo_url"] = savedAvatarUriString
            }
            db.collection("user").document(user.uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
        }

        Toast.makeText(this, "Profil farm berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
