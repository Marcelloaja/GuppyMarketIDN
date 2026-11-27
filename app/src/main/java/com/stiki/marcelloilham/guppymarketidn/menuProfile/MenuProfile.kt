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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.intro.Login
import com.stiki.marcelloilham.guppymarketidn.util.AuctionNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.ChatNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader

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

    private var selectedPhotoUri: android.net.Uri? = null
    private var pendingDeleteDialog: AlertDialog? = null
    private var pendingDeleteConfirmInput: EditText? = null
    private var hasJustReauthenticated = false
    private val reauthGoogleLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val user = firebaseAuth.currentUser
        if (user == null) {
            dismissPendingDeleteDialog()
            finalizeLocalAccountCleanupAndExit(null)
            return@registerForActivityResult
        }
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                resetDeleteDialogButtons()
                Toast.makeText(this, "Verifikasi Google dibatalkan.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    hasJustReauthenticated = true
                    performAuthAndDbDelete(user)
                }
                .addOnFailureListener { e ->
                    resetDeleteDialogButtons()
                    Toast.makeText(this, "Verifikasi ulang gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } catch (e: ApiException) {
            resetDeleteDialogButtons()
            if (e.statusCode != 12501) {
                Toast.makeText(this, "Verifikasi Google gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            resetDeleteDialogButtons()
            Toast.makeText(this, "Verifikasi Google gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
    private val photoPickerLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val uid = firebaseAuth.currentUser?.uid ?: "user"
            val pendingFileName = "avatar_pending_$uid.jpg"
            val copiedUri = SafeImageLoader.copyUriToInternalStorage(this, uri, pendingFileName)
            selectedPhotoUri = copiedUri ?: uri
            if (::imgEditAvatar.isInitialized) {
                SafeImageLoader.loadUriSafely(
                    this,
                    imgEditAvatar,
                    selectedPhotoUri.toString(),
                    R.drawable.ic_default_user_avatar
                )
            }
            Toast.makeText(this, "Foto profil dipilih. Klik 'Simpan perubahan' untuk menyimpan.", Toast.LENGTH_SHORT).show()
        }
    }

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_profile)

        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Terjadi kesalahan saat membuka Edit Profil.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        syncLocationFromDefaultAddress()
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
            text = "Ketik 'HAPUS AKUN' di bawah ini untuk menghapus permanen akun, profil farm, alamat, produk, lelang, dan metode pembayaran Anda:"
            setTextColor(ContextCompat.getColor(context, R.color.guppy_red))
            textSize = 13f
        }

        val etConfirmText = EditText(this).apply {
            hint = "HAPUS AKUN"
        }

        layout.addView(tvNotice)
        layout.addView(etConfirmText)

        val dialog = AlertDialog.Builder(this)
            .setTitle("⚠️ Hapus Permanen Akun Farm")
            .setView(layout)
            .setPositiveButton("HAPUS SEKARANG", null)
            .setNegativeButton("Batal", null)
            .create()

        pendingDeleteDialog = dialog
        pendingDeleteConfirmInput = etConfirmText

        dialog.setOnShowListener {
            val btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnCancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.guppy_red))

            btnConfirm.setOnClickListener {
                val inputStr = etConfirmText.text.toString().trim()
                if (!inputStr.equals("HAPUS AKUN", ignoreCase = true)) {
                    etConfirmText.error = "Ketik 'HAPUS AKUN' dengan benar"
                    Toast.makeText(this, "Teks konfirmasi tidak cocok.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val user = firebaseAuth.currentUser
                if (user == null) {
                    dialog.dismiss()
                    finalizeLocalAccountCleanupAndExit(null)
                    return@setOnClickListener
                }

                btnConfirm.isEnabled = false
                btnCancel.isEnabled = false
                etConfirmText.isEnabled = false
                btnConfirm.text = "Menghapus..."

                val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
                val isRecentLogin = hasJustReauthenticated ||
                    (lastSignIn > 0L && (System.currentTimeMillis() - lastSignIn) < 4 * 60_000L)

                if (isRecentLogin) {
                    performAuthAndDbDelete(user)
                } else {
                    promptReauthenticationForAccountDelete(user)
                }
            }
        }

        dialog.setOnDismissListener {
            if (pendingDeleteDialog == dialog) {
                pendingDeleteDialog = null
                pendingDeleteConfirmInput = null
            }
        }

        dialog.show()
    }

    private fun performAuthAndDbDelete(user: com.google.firebase.auth.FirebaseUser) {
        val uid = user.uid
        FirestoreRepository.deleteUserAccountData(uid) { dbSuccess, errorMsg ->
            if (isFinishing || isDestroyed) return@deleteUserAccountData
            if (!dbSuccess) {
                resetDeleteDialogButtons()
                Toast.makeText(
                    this,
                    "Gagal menghapus data akun dari database: ${errorMsg ?: "Coba lagi"}",
                    Toast.LENGTH_LONG
                ).show()
                return@deleteUserAccountData
            }

            user.delete()
                .addOnSuccessListener {
                    if (isFinishing || isDestroyed) return@addOnSuccessListener
                    dismissPendingDeleteDialog()
                    finalizeLocalAccountCleanupAndExit(uid)
                }
                .addOnFailureListener { error ->
                    if (isFinishing || isDestroyed) return@addOnFailureListener
                    if (error is FirebaseAuthRecentLoginRequiredException) {
                        promptReauthenticationForAccountDelete(user)
                    } else {
                        resetDeleteDialogButtons()
                        Toast.makeText(
                            this,
                            "Data database dihapus, tetapi akun login gagal dihapus: ${error.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun promptReauthenticationForAccountDelete(user: com.google.firebase.auth.FirebaseUser) {
        val isGoogleProvider = user.providerData.any {
            it.providerId == GoogleAuthProvider.PROVIDER_ID
        }
        val email = user.email.orEmpty()

        if (isGoogleProvider) {
            AlertDialog.Builder(this)
                .setTitle("Verifikasi Ulang Diperlukan")
                .setMessage("Demi keamanan, silakan verifikasi akun Google Anda sekali lagi untuk menghapus permanen akun login.")
                .setCancelable(false)
                .setPositiveButton("Verifikasi Google") { _, _ ->
                    try {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val client = GoogleSignIn.getClient(this, gso)
                        reauthGoogleLauncher.launch(client.signInIntent)
                    } catch (e: Exception) {
                        resetDeleteDialogButtons()
                        Toast.makeText(this, "Gagal membuka verifikasi Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Batal") { _, _ ->
                    resetDeleteDialogButtons()
                }
                .show()
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 24, 50, 10)
        }
        val tvInfo = TextView(this).apply {
            text = "Masukkan password akun ($email) untuk menyelesaikan penghapusan permanen:"
            textSize = 13f
            setTextColor(ContextCompat.getColor(context, R.color.guppy_text_black))
        }
        val etPassword = EditText(this).apply {
            hint = "Password"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(tvInfo)
        container.addView(etPassword)

        val reauthDialog = AlertDialog.Builder(this)
            .setTitle("Verifikasi Password")
            .setView(container)
            .setCancelable(false)
            .setPositiveButton("Verifikasi & Hapus", null)
            .setNegativeButton("Batal") { _, _ ->
                resetDeleteDialogButtons()
            }
            .create()

        reauthDialog.setOnShowListener {
            val btnOk = reauthDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnCancel = reauthDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            btnOk.setOnClickListener {
                val password = etPassword.text.toString()
                if (password.isBlank() || email.isBlank()) {
                    etPassword.error = "Password wajib diisi"
                    return@setOnClickListener
                }
                btnOk.isEnabled = false
                btnCancel.isEnabled = false
                btnOk.text = "Memverifikasi..."

                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        hasJustReauthenticated = true
                        reauthDialog.dismiss()
                        performAuthAndDbDelete(user)
                    }
                    .addOnFailureListener { e ->
                        btnOk.isEnabled = true
                        btnCancel.isEnabled = true
                        btnOk.text = "Verifikasi & Hapus"
                        Toast.makeText(this, "Password salah atau gagal verifikasi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
        }
        reauthDialog.show()
    }

    private fun resetDeleteDialogButtons() {
        val dialog = pendingDeleteDialog ?: return
        if (!dialog.isShowing) return
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            isEnabled = true
            text = "HAPUS SEKARANG"
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = true
        pendingDeleteConfirmInput?.isEnabled = true
    }

    private fun dismissPendingDeleteDialog() {
        try {
            pendingDeleteDialog?.dismiss()
        } catch (_: Throwable) {
        }
        pendingDeleteDialog = null
        pendingDeleteConfirmInput = null
    }

    private fun finalizeLocalAccountCleanupAndExit(deletedUid: String?) {
        ChatNotificationHelper.stopListening()
        AuctionNotificationHelper.stopListening()

        if (!deletedUid.isNullOrBlank()) {
            filesDir.listFiles()?.forEach { file ->
                if (file.name.contains(deletedUid)) {
                    file.delete()
                }
            }
        }

        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("login_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("auction_notif_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(this, gso).signOut()
        } catch (_: Throwable) {
        }

        firebaseAuth.signOut()
        Toast.makeText(this, "Akun farm dan seluruh data berhasil dihapus permanen.", Toast.LENGTH_LONG).show()

        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
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
                        val avatarDataUrl = doc.getString("avatar_data_url")

                        etOwnerName.setText(name)
                        etFarmName.setText(farmName)
                        etWhatsapp.setText(wa)
                        etFarmDescription.setText(desc)
                        tvCharCount.text = "${desc.length}/300"
                        tvBankAccountInfoSub.text = bank

                        SafeImageLoader.loadWithFallbackSafely(
                            this,
                            imgEditAvatar,
                            avatarUriStr,
                            avatarDataUrl,
                            R.drawable.ic_default_user_avatar
                        )

                        if (loc.isNotEmpty()) {
                            tvFarmLocation.text = loc
                        } else {
                            tvFarmLocation.text = "Belum ada alamat terdaftar"
                        }
                        syncLocationFromDefaultAddress(loc)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun syncLocationFromDefaultAddress(fallbackLoc: String = "") {
        val currentUser = firebaseAuth.currentUser ?: return
        val userId = currentUser.uid
        FirestoreRepository.fetchUserAddresses(userId) { addresses ->
            if (isFinishing || isDestroyed) return@fetchUserAddresses
            val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            if (defaultAddr != null) {
                tvFarmLocation.text = defaultAddr.fullAddress
                val shortSub = if (defaultAddr.fullAddress.contains(",")) {
                    defaultAddr.fullAddress.split(",").takeLast(2).joinToString(",").trim()
                } else {
                    defaultAddr.fullAddress
                }
                getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("address_sub", shortSub)
                    .apply()
            } else if (fallbackLoc.isNotEmpty()) {
                tvFarmLocation.text = fallbackLoc
            } else {
                tvFarmLocation.text = "Belum ada alamat terdaftar"
            }
        }
    }

    private fun copyUriToInternalStorage(uri: android.net.Uri): String? {
        val uid = firebaseAuth.currentUser?.uid ?: "user"
        val newFileName = "avatar_${uid}_${System.currentTimeMillis()}.jpg"
        val copiedUri = SafeImageLoader.copyUriToInternalStorage(this, uri, newFileName) ?: return null

        // Clean up older avatar files for this user after the new file is safely written
        filesDir.listFiles()?.forEach { file ->
            val name = file.name
            if (name != newFileName &&
                (name == "avatar_$uid.jpg" || name == "avatar_pending_$uid.jpg" || name.startsWith("avatar_${uid}_"))
            ) {
                file.delete()
            }
        }
        return copiedUri.toString()
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
        var savedAvatarDataUrl: String? = null
        if (selectedPhotoUri != null) {
            savedAvatarUriString = copyUriToInternalStorage(selectedPhotoUri!!)
            if (savedAvatarUriString != null) {
                val bmp = SafeImageLoader.decodeBitmapSafely(this, savedAvatarUriString, 256)
                if (bmp != null) {
                    savedAvatarDataUrl = SafeImageLoader.encodeBitmapToDataUrl(bmp, maxDimPx = 220, initialQuality = 62)
                }
            }
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
            val cleanLocation = if (locationStr == "Belum ada alamat terdaftar") "" else locationStr
            val userMap = hashMapOf<String, Any>(
                "name" to ownerName,
                "farm_name" to farmName,
                "whatsapp" to whatsappStr,
                "description" to desc
            )
            if (cleanLocation.isNotBlank()) {
                userMap["location"] = cleanLocation
                userMap["full_address"] = cleanLocation
            }
            if (savedAvatarUriString != null) {
                userMap["avatar_uri"] = savedAvatarUriString
                userMap["photo_url"] = savedAvatarUriString
            }
            if (!savedAvatarDataUrl.isNullOrBlank()) {
                userMap["avatar_data_url"] = savedAvatarDataUrl
            }
            db.collection("user").document(user.uid).set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    FirestoreRepository.syncUserFarmLocationToPublic(user.uid)
                }
        }

        Toast.makeText(this, "Profil farm berhasil diperbarui!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
