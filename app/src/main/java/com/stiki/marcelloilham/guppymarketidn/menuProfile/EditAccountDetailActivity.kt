package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader

class EditAccountDetailActivity : AppCompatActivity() {

    private lateinit var etOwnerName: EditText
    private lateinit var etAccountNumber: EditText
    private lateinit var tvBankChoice: TextView
    private lateinit var tvAccountTypeChoice: TextView
    private lateinit var etBranch: EditText
    private lateinit var switchSetMainAccount: SwitchMaterial

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var methodId: String = ""
    private var providerName: String = "BCA"
    private var accountNumber: String = ""
    private var accountOwner: String = ""
    private var methodType: String = "Bank"
    private var isMain: Boolean = false

    private var tempQrisImageUri: Uri? = null
    private lateinit var qrisImagePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_detail)

        qrisImagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                tempQrisImageUri = saveImageToInternalStorage(uri)
                val ivPreview = findViewById<ImageView>(R.id.ivEditQrisPreview)
                if (ivPreview != null) {
                    SafeImageLoader.loadUriSafely(this, ivPreview, tempQrisImageUri?.toString(), R.drawable.ic_qris, 1024)
                }
                Toast.makeText(this, "Foto Barcode QRIS diperbarui!", Toast.LENGTH_SHORT).show()
            }
        }

        val topBar = findViewById<View>(R.id.topBarEditAccount)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackEditAccount)
        btnBack?.setOnClickListener { finish() }

        val btnCancel = findViewById<View>(R.id.btnCancelEditAccount)
        btnCancel?.setOnClickListener { finish() }

        initViews()
        readIntentData()
        setupListeners()
    }

    private fun saveImageToInternalStorage(uri: Uri): Uri {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return uri
            val file = java.io.File(filesDir, "qris_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file)
        } catch (_: Exception) {
            uri
        }
    }

    private fun initViews() {
        etOwnerName = findViewById(R.id.etEditOwnerName)
        etAccountNumber = findViewById(R.id.etEditAccountNumber)
        tvBankChoice = findViewById(R.id.tvEditBankChoice)
        tvAccountTypeChoice = findViewById(R.id.tvEditAccountTypeChoice)
        etBranch = findViewById(R.id.etEditBranch)
        switchSetMainAccount = findViewById(R.id.switchEditSetMainAccount)
    }

    private fun readIntentData() {
        methodId = intent.getStringExtra("EXTRA_METHOD_ID") ?: ""
        providerName = intent.getStringExtra("EXTRA_PROVIDER_NAME") ?: "BCA"
        val passedType = intent.getStringExtra("EXTRA_TYPE") ?: ""
        accountNumber = intent.getStringExtra("EXTRA_ACCOUNT_NUMBER") ?: ""
        accountOwner = intent.getStringExtra("EXTRA_OWNER_NAME") ?: ""
        isMain = intent.getBooleanExtra("EXTRA_IS_MAIN", false)

        val isQrisCheck = providerName.contains("QRIS", true) || passedType.equals("QRIS", true)
        val isBankCheck = providerName.contains("BCA", true) ||
                providerName.contains("BNI", true) ||
                providerName.contains("Mandiri", true) ||
                providerName.contains("BRI", true) ||
                providerName.contains("CIMB", true) ||
                providerName.contains("Permata", true) ||
                providerName.contains("BSI", true) ||
                providerName.contains("Danamon", true) ||
                providerName.contains("Bank", true) ||
                passedType.equals("Bank", true)

        methodType = when {
            isQrisCheck -> "QRIS"
            isBankCheck -> "Bank"
            else -> "E-Wallet"
        }

        etOwnerName.setText(accountOwner)
        etAccountNumber.setText(accountNumber)
        tvBankChoice.text = providerName
        switchSetMainAccount.isChecked = isMain

        applyModeUI()
    }

    private fun applyModeUI() {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvEditHeaderTitle)
        val tvHeaderSub = findViewById<TextView>(R.id.tvEditHeaderSub)
        val tvNumberLabel = findViewById<TextView>(R.id.tvEditNumberLabel)
        val tvProviderLabel = findViewById<TextView>(R.id.tvEditProviderLabel)
        val imgProviderIcon = findViewById<ImageView>(R.id.imgEditProviderIcon)

        val layoutNumber = findViewById<View>(R.id.layoutEditNumberContainer)
        val layoutProvider = findViewById<View>(R.id.layoutEditProviderContainer)
        val layoutType = findViewById<View>(R.id.layoutAccountTypeContainer)
        val layoutBranch = findViewById<View>(R.id.layoutBranchContainer)
        val layoutQris = findViewById<View>(R.id.layoutEditQrisUploadContainer)

        val isQris = methodType.equals("QRIS", true)
        val isEWallet = methodType.equals("E-Wallet", true)

        if (isQris) {
            tvHeaderTitle?.text = "Ubah Data QRIS"
            tvHeaderSub?.text = "Perbarui foto barcode QRIS Toko"
            tvProviderLabel?.text = "PENYEDIA"
            imgProviderIcon?.setImageResource(R.drawable.ic_qris)

            layoutNumber?.visibility = View.GONE
            layoutProvider?.visibility = View.VISIBLE
            layoutType?.visibility = View.GONE
            layoutBranch?.visibility = View.GONE
            layoutQris?.visibility = View.VISIBLE

            val ivPreview = findViewById<ImageView>(R.id.ivEditQrisPreview)
            if (ivPreview != null && (accountNumber.startsWith("content://") || accountNumber.startsWith("file://") || accountNumber.startsWith("/"))) {
                SafeImageLoader.loadUriSafely(this, ivPreview, accountNumber, R.drawable.ic_qris, 1024)
            }
        } else if (isEWallet) {
            tvHeaderTitle?.text = "Ubah Data E-Wallet"
            tvHeaderSub?.text = "Perbarui informasi akun e-wallet penerima pencairan"
            tvNumberLabel?.text = "NOMOR HP / ID AKUN"
            tvProviderLabel?.text = "PENYEDIA E-WALLET"
            imgProviderIcon?.setImageResource(R.drawable.ic_ewallet)

            layoutNumber?.visibility = View.VISIBLE
            layoutProvider?.visibility = View.VISIBLE
            layoutType?.visibility = View.GONE
            layoutBranch?.visibility = View.GONE
            layoutQris?.visibility = View.GONE
        } else {
            tvHeaderTitle?.text = "Ubah Data Rekening"
            tvHeaderSub?.text = "Perbarui informasi rekening penerima pencairan"
            tvNumberLabel?.text = "NOMOR REKENING"
            tvProviderLabel?.text = "BANK"
            imgProviderIcon?.setImageResource(R.drawable.ic_payment_card)

            layoutNumber?.visibility = View.VISIBLE
            layoutProvider?.visibility = View.VISIBLE
            layoutType?.visibility = View.VISIBLE
            layoutBranch?.visibility = View.VISIBLE
            layoutQris?.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        val btnCopy = findViewById<ImageView>(R.id.btnCopyAccNumber)
        btnCopy?.setOnClickListener {
            val num = etAccountNumber.text.toString().trim()
            if (num.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nomor Rekening", num)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Nomor disalin ke clipboard!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnClear = findViewById<ImageView>(R.id.btnClearAccNumber)
        btnClear?.setOnClickListener {
            etAccountNumber.setText("")
            etAccountNumber.requestFocus()
        }

        val layoutBank = findViewById<View>(R.id.layoutSelectBank)
        layoutBank?.setOnClickListener { showProviderChooserDialog() }

        val layoutType = findViewById<View>(R.id.layoutSelectAccountType)
        layoutType?.setOnClickListener { showAccountTypeChooserDialog() }

        val cardQrisUpload = findViewById<View>(R.id.cardEditUploadQris)
        cardQrisUpload?.setOnClickListener {
            qrisImagePickerLauncher.launch("image/*")
        }

        val btnSave = findViewById<TextView>(R.id.btnSaveEditAccount)
        btnSave?.setOnClickListener { saveChangesToFirestore() }
    }

    private fun showProviderChooserDialog() {
        val isQris = methodType.equals("QRIS", true)
        val isEWallet = methodType.equals("E-Wallet", true)

        val options = if (isQris) {
            arrayOf("QRIS All Payment")
        } else if (isEWallet) {
            arrayOf("GoPay", "OVO", "DANA", "ShopeePay", "LinkAja")
        } else {
            arrayOf("BCA", "BNI", "Mandiri", "BRI", "CIMB Niaga", "Permata Bank", "Bank Syariah Indonesia (BSI)", "Bank Danamon")
        }

        AlertDialog.Builder(this)
            .setTitle(if (isQris) "Penyedia QRIS" else if (isEWallet) "Pilih E-Wallet" else "Pilih Bank")
            .setItems(options) { _, which ->
                providerName = options[which]
                tvBankChoice.text = providerName
            }
            .show()
    }

    private fun showAccountTypeChooserDialog() {
        val types = arrayOf("Tabungan", "Giro", "Bisnis / Deposito")
        AlertDialog.Builder(this)
            .setTitle("Pilih Jenis Rekening")
            .setItems(types) { _, which ->
                tvAccountTypeChoice.text = types[which]
            }
            .show()
    }

    private fun saveChangesToFirestore() {
        val owner = etOwnerName.text.toString().trim()
        val number = etAccountNumber.text.toString().trim()
        val isNewMain = switchSetMainAccount.isChecked

        val isQris = methodType.equals("QRIS", true)

        if (owner.isEmpty()) {
            etOwnerName.error = "Nama pemilik wajib diisi"
            return
        }

        if (!isQris && number.isEmpty()) {
            etAccountNumber.error = "Nomor rekening / e-wallet wajib diisi"
            return
        }

        val finalNumber = if (isQris) {
            tempQrisImageUri?.toString() ?: accountNumber
        } else {
            number
        }

        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val updatedItem = FirestoreRepository.PaymentMethodModel(
            id = methodId,
            type = methodType,
            providerName = providerName,
            accountNumber = finalNumber,
            accountOwner = owner,
            isMain = isNewMain
        )

        val btnSave = findViewById<TextView>(R.id.btnSaveEditAccount)
        btnSave?.isEnabled = false
        btnSave?.text = "Menyimpan..."

        if (isNewMain) {
            FirestoreRepository.fetchUserPaymentMethods(currentUserId) { existingList ->
                val updatedList = existingList.map {
                    if (it.id == methodId) updatedItem.copy(isMain = true) else it.copy(isMain = false)
                }
                FirestoreRepository.updateAllPaymentMethods(currentUserId, updatedList) {
                    btnSave?.isEnabled = true
                    btnSave?.text = "Simpan Perubahan"
                    Toast.makeText(this, "Data $providerName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            FirestoreRepository.saveUserPaymentMethod(currentUserId, updatedItem) { success ->
                btnSave?.isEnabled = true
                btnSave?.text = "Simpan Perubahan"
                if (success) {
                    Toast.makeText(this, "Data $providerName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
