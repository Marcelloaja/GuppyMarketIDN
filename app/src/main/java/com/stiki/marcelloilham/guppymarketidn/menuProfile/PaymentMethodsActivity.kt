package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.os.Bundle
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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var tvMainPaymentName: TextView
    private lateinit var tvMainPaymentAccNumber: TextView

    private lateinit var tvBankBCANumber: TextView
    private lateinit var tvBankBNINumber: TextView
    private lateinit var tvBankMandiriStatus: TextView
    private lateinit var btnActivateMandiri: TextView

    private lateinit var switchGoPay: SwitchMaterial
    private lateinit var switchOVO: SwitchMaterial
    private lateinit var switchDANA: SwitchMaterial
    private lateinit var switchQRIS: SwitchMaterial

    private lateinit var tvWithdrawalAccountName: TextView
    private lateinit var tvWithdrawalSubtitle: TextView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var currentModel = FirestoreRepository.PaymentSettingsModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_methods)

        val topBar = findViewById<View>(R.id.topBarPaymentMethods)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackPaymentMethods)
        btnBack?.setOnClickListener { finish() }

        initViews()
        setupListeners()
        loadPaymentSettingsFromFirestore()
        loadActiveUserBalance()
    }

    private fun initViews() {
        tvMainPaymentName = findViewById(R.id.tvMainPaymentName)
        tvMainPaymentAccNumber = findViewById(R.id.tvMainPaymentAccNumber)

        tvBankBCANumber = findViewById(R.id.tvBankBCANumber)
        tvBankBNINumber = findViewById(R.id.tvBankBNINumber)
        tvBankMandiriStatus = findViewById(R.id.tvBankMandiriStatus)
        btnActivateMandiri = findViewById(R.id.btnActivateMandiri)

        switchGoPay = findViewById(R.id.switchGoPay)
        switchOVO = findViewById(R.id.switchOVO)
        switchDANA = findViewById(R.id.switchDANA)
        switchQRIS = findViewById(R.id.switchQRIS)

        tvWithdrawalAccountName = findViewById(R.id.tvWithdrawalAccountName)
        tvWithdrawalSubtitle = findViewById(R.id.tvWithdrawalSubtitle)
    }

    private fun setupListeners() {
        val cardMain = findViewById<View>(R.id.cardMainPaymentMethod)
        cardMain?.setOnClickListener { showMainPaymentChooserDialog() }

        val rowBCA = findViewById<View>(R.id.rowBankBCA)
        rowBCA?.setOnClickListener { showEditBankDialog("BCA", currentModel.bcaAccountNumber) }

        val rowBNI = findViewById<View>(R.id.rowBankBNI)
        rowBNI?.setOnClickListener { showEditBankDialog("BNI", currentModel.bniAccountNumber) }

        btnActivateMandiri.setOnClickListener {
            showEditBankDialog("Mandiri", "")
        }

        val btnAddPayment = findViewById<View>(R.id.btnAddNewPaymentMethod)
        btnAddPayment?.setOnClickListener { showAddPaymentBottomSheet() }

        val btnSave = findViewById<TextView>(R.id.btnSavePaymentSettings)
        btnSave?.setOnClickListener { saveSettingsToFirestore() }
    }

    private fun loadPaymentSettingsFromFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchPaymentSettings(currentUserId) { model ->
            currentModel = model

            val mainLower = model.mainMethod.lowercase()

            if (model.mainMethod.isNotEmpty() && model.mainMethod != "Belum Diatur") {
                tvMainPaymentName.text = model.mainMethod
                val mainAccNum = when {
                    mainLower.contains("bca") -> model.bcaAccountNumber
                    mainLower.contains("bni") -> model.bniAccountNumber
                    else -> ""
                }
                tvMainPaymentAccNumber.text = if (mainAccNum.isNotEmpty()) "•••• ${mainAccNum.takeLast(4)}" else "Metode pembayaran aktif"
            } else {
                tvMainPaymentName.text = "Belum Ada Metode Utama"
                tvMainPaymentAccNumber.text = "Pilih atau tambah metode pembayaran di bawah"
            }

            tvBankBCANumber.text = if (model.bcaAccountNumber.isNotEmpty()) {
                val suffix = if (mainLower.contains("bca")) " (Utama)" else ""
                "•••• ${model.bcaAccountNumber.takeLast(4)}$suffix"
            } else {
                "Belum diatur"
            }

            tvBankBNINumber.text = if (model.bniAccountNumber.isNotEmpty()) {
                val suffix = if (mainLower.contains("bni")) " (Utama)" else ""
                "•••• ${model.bniAccountNumber.takeLast(4)}$suffix"
            } else {
                "Belum diatur"
            }

            if (model.mandiriActive) {
                val isMandiriMain = mainLower.contains("mandiri")
                tvBankMandiriStatus.text = if (isMandiriMain) "Terhubung (Utama)" else "Terhubung"
                btnActivateMandiri.text = "Kelola"
            } else {
                tvBankMandiriStatus.text = "Belum aktif"
                btnActivateMandiri.text = "Aktifkan"
            }

            switchGoPay.isChecked = model.goPayEnabled
            switchOVO.isChecked = model.ovoEnabled
            switchDANA.isChecked = model.danaEnabled
            switchQRIS.isChecked = model.qrisEnabled

            if (model.withdrawalAccount.isNotEmpty()) {
                tvWithdrawalAccountName.text = model.withdrawalAccount
            } else if (model.bcaAccountNumber.isNotEmpty()) {
                tvWithdrawalAccountName.text = "BCA •••• ${model.bcaAccountNumber.takeLast(4)}"
            } else {
                tvWithdrawalAccountName.text = "Belum Ada Rekening Pencairan"
            }
        }
    }

    private fun loadActiveUserBalance() {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("user").document(currentUserId)
                .get()
                .addOnSuccessListener { doc ->
                    val balance = doc.getLong("active_balance") ?: 0L
                    val formatted = "Rp " + String.format(java.util.Locale.US, "%,d", balance).replace(',', '.')
                    tvWithdrawalSubtitle.text = "Saldo aktif $formatted dapat ditarik ke rekening ini"
                }
        }
    }

    private fun showMainPaymentChooserDialog() {
        val options = arrayOf("BCA Virtual Account", "BNI Virtual Account", "Mandiri Virtual Account", "GoPay / QRIS")
        AlertDialog.Builder(this)
            .setTitle("Pilih Metode Utama")
            .setItems(options) { _, which ->
                val selectedName = options[which]
                currentModel = currentModel.copy(mainMethod = selectedName)
                tvMainPaymentName.text = selectedName

                val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
                FirestoreRepository.savePaymentSettings(currentUserId, currentModel) {
                    loadPaymentSettingsFromFirestore()
                    Toast.makeText(this, "Metode utama diubah menjadi '$selectedName'!", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showEditBankDialog(bankName: String, currentAcc: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etAccNumber = EditText(this).apply {
            hint = "Nomor Rekening $bankName"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentAcc)
        }

        layout.addView(etAccNumber)

        AlertDialog.Builder(this)
            .setTitle("Kelola Rekening $bankName")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val newNumber = etAccNumber.text.toString().trim()
                if (newNumber.isNotEmpty()) {
                    currentModel = when (bankName) {
                        "BCA" -> currentModel.copy(bcaAccountNumber = newNumber)
                        "BNI" -> currentModel.copy(bniAccountNumber = newNumber)
                        else -> currentModel.copy(mandiriActive = true)
                    }

                    val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
                    FirestoreRepository.savePaymentSettings(currentUserId, currentModel) {
                        loadPaymentSettingsFromFirestore()
                        Toast.makeText(this, "Rekening $bankName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showAddPaymentBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_payment_method, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        dialog.setContentView(bottomSheetView)

        val btnClose = bottomSheetView.findViewById<View>(R.id.btnCloseBottomSheet)
        val tabBank = bottomSheetView.findViewById<TextView>(R.id.tabTypeBank)
        val tabEWallet = bottomSheetView.findViewById<TextView>(R.id.tabTypeEWallet)
        val tvProvider = bottomSheetView.findViewById<TextView>(R.id.tvSelectProvider)
        val etOwnerName = bottomSheetView.findViewById<EditText>(R.id.etAccountOwnerName)
        val etAccNumber = bottomSheetView.findViewById<EditText>(R.id.etAccountNumber)
        val switchMain = bottomSheetView.findViewById<SwitchMaterial>(R.id.switchSetMainPayment)
        val btnCancel = bottomSheetView.findViewById<View>(R.id.btnCancelBottomSheet)
        val btnSave = bottomSheetView.findViewById<View>(R.id.btnSaveBottomSheet)

        var selectedTypeIsBank = true
        var selectedProviderName = "BCA"

        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("user").document(currentUser.uid)
                .get().addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: currentUser.displayName ?: "Marcello Aja"
                    etOwnerName?.setText(name)
                }
        }

        btnClose?.setOnClickListener { dialog.dismiss() }
        btnCancel?.setOnClickListener { dialog.dismiss() }

        tabBank?.setOnClickListener {
            selectedTypeIsBank = true
            tabBank.setBackgroundResource(R.drawable.bg_chip_selected)
            tabBank.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
            tabBank.setTextColor(ContextCompat.getColor(this, R.color.white))

            tabEWallet?.background = null
            tabEWallet?.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))

            selectedProviderName = "BCA"
            tvProvider?.text = "BCA"
        }

        tabEWallet?.setOnClickListener {
            selectedTypeIsBank = false
            tabEWallet.setBackgroundResource(R.drawable.bg_chip_selected)
            tabEWallet.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
            tabEWallet.setTextColor(ContextCompat.getColor(this, R.color.white))

            tabBank?.background = null
            tabBank?.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))

            selectedProviderName = "GoPay"
            tvProvider?.text = "GoPay"
        }

        tvProvider?.setOnClickListener {
            val options = if (selectedTypeIsBank) {
                arrayOf("BCA", "BNI", "Mandiri", "BRI", "CIMB Niaga", "Permata Bank")
            } else {
                arrayOf("GoPay", "OVO", "DANA", "ShopeePay", "LinkAja")
            }

            AlertDialog.Builder(this)
                .setTitle(if (selectedTypeIsBank) "Pilih Bank" else "Pilih E-Wallet")
                .setItems(options) { _, which ->
                    selectedProviderName = options[which]
                    tvProvider.text = selectedProviderName
                }
                .show()
        }

        btnSave?.setOnClickListener {
            val owner = etOwnerName?.text?.toString()?.trim() ?: ""
            val number = etAccNumber?.text?.toString()?.trim() ?: ""

            if (number.isEmpty()) {
                etAccNumber?.error = "Nomor rekening / e-wallet wajib diisi"
                return@setOnClickListener
            }

            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"

            currentModel = when (selectedProviderName) {
                "BCA" -> currentModel.copy(bcaAccountNumber = number)
                "BNI" -> currentModel.copy(bniAccountNumber = number)
                "Mandiri" -> currentModel.copy(mandiriActive = true)
                "GoPay" -> currentModel.copy(goPayEnabled = true)
                "OVO" -> currentModel.copy(ovoEnabled = true)
                "DANA" -> currentModel.copy(danaEnabled = true)
                else -> currentModel
            }

            if (switchMain?.isChecked == true) {
                currentModel = currentModel.copy(mainMethod = "$selectedProviderName Virtual Account")
            }

            FirestoreRepository.savePaymentSettings(currentUserId, currentModel) { success ->
                if (success) {
                    Toast.makeText(this, "Metode pembayaran '$selectedProviderName' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    loadPaymentSettingsFromFirestore()
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun saveSettingsToFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val updatedModel = currentModel.copy(
            goPayEnabled = switchGoPay.isChecked,
            ovoEnabled = switchOVO.isChecked,
            danaEnabled = switchDANA.isChecked,
            qrisEnabled = switchQRIS.isChecked
        )

        val btnSave = findViewById<TextView>(R.id.btnSavePaymentSettings)
        btnSave?.isEnabled = false
        btnSave?.text = "Menyimpan Perubahan..."

        FirestoreRepository.savePaymentSettings(currentUserId, updatedModel) { success ->
            btnSave?.isEnabled = true
            btnSave?.text = "Simpan Perubahan"
            if (success) {
                Toast.makeText(this, "Metode pembayaran berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
