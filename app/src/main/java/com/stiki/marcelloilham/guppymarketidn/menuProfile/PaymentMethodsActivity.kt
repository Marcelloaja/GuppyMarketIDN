package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
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
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.PaymentMethodAdapter
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import java.util.Locale

class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var tvMainPaymentName: TextView
    private lateinit var tvMainPaymentAccNumber: TextView

    private lateinit var recyclerPaymentMethods: RecyclerView
    private lateinit var tvEmptyPaymentMethods: TextView
    private lateinit var paymentAdapter: PaymentMethodAdapter

    private lateinit var tvWithdrawalAccountName: TextView
    private lateinit var tvWithdrawalSubtitle: TextView
    private lateinit var tvAdminFeeNotice: TextView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var realPaymentMethodsList: MutableList<FirestoreRepository.PaymentMethodModel> = mutableListOf()
    private var currentBottomSheetView: View? = null
    private var tempQrisImageUri: Uri? = null
    private lateinit var qrisImagePickerLauncher: ActivityResultLauncher<String>

    private fun saveImageToInternalStorage(uri: Uri): String {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return uri.toString()
            val file = java.io.File(filesDir, "qris_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            uri.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_methods)

        qrisImagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val permanentUriString = saveImageToInternalStorage(uri)
                tempQrisImageUri = Uri.parse(permanentUriString)
                val ivPreview = currentBottomSheetView?.findViewById<ImageView>(R.id.ivQrisPreview)
                val placeholder = currentBottomSheetView?.findViewById<View>(R.id.layoutQrisPlaceholder)
                ivPreview?.setImageURI(tempQrisImageUri)
                ivPreview?.visibility = View.VISIBLE
                placeholder?.visibility = View.GONE
                Toast.makeText(this, "Foto Barcode QRIS berhasil diunggah! ✓", Toast.LENGTH_SHORT).show()
            }
        }

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
        loadRealUserPaymentMethods()
        loadActiveUserBalance()
        loadAdminFeeNotice()
    }

    override fun onResume() {
        super.onResume()
        loadRealUserPaymentMethods()
    }

    private fun initViews() {
        tvMainPaymentName = findViewById(R.id.tvMainPaymentName)
        tvMainPaymentAccNumber = findViewById(R.id.tvMainPaymentAccNumber)

        recyclerPaymentMethods = findViewById(R.id.recyclerPaymentMethods)
        tvEmptyPaymentMethods = findViewById(R.id.tvEmptyPaymentMethods)
        recyclerPaymentMethods.layoutManager = LinearLayoutManager(this)

        tvWithdrawalAccountName = findViewById(R.id.tvWithdrawalAccountName)
        tvWithdrawalSubtitle = findViewById(R.id.tvWithdrawalSubtitle)
        tvAdminFeeNotice = findViewById(R.id.tvAdminFeeNotice)
    }

    private fun loadAdminFeeNotice() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchPaymentSettings(currentUserId) { settings ->
            val feeAmount = settings.adminFeeAmount
            val feeText = if (feeAmount > 0) {
                val formattedFee = "Rp " + String.format(Locale.US, "%,d", feeAmount).replace(',', '.')
                "Biaya admin Virtual Account $formattedFee per transaksi"
            } else {
                "Bebas biaya admin Virtual Account (Promo)"
            }
            tvAdminFeeNotice.text = feeText
        }
    }

    private fun setupListeners() {
        val cardMain = findViewById<View>(R.id.cardMainPaymentMethod)
        cardMain?.setOnClickListener { showMainPaymentChooserDialog() }

        val btnAddPayment = findViewById<View>(R.id.btnAddNewPaymentMethod)
        btnAddPayment?.setOnClickListener { showAddPaymentBottomSheet() }

        val btnSave = findViewById<TextView>(R.id.btnSavePaymentSettings)
        btnSave?.setOnClickListener {
            Toast.makeText(this, "Seluruh perubahan metode pembayaran telah tersinkronisasi!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadRealUserPaymentMethods() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserPaymentMethods(currentUserId) { list ->
            val cleanList = list.distinctBy { it.id }

            // Ensure EXACTLY ONE payment method is marked as main if cleanList is not empty!
            val ensuredList = if (cleanList.isNotEmpty() && cleanList.none { it.isMain }) {
                val fixedList = cleanList.mapIndexed { idx, item ->
                    if (idx == 0) item.copy(isMain = true) else item.copy(isMain = false)
                }
                FirestoreRepository.updateAllPaymentMethods(currentUserId, fixedList) {}
                fixedList
            } else {
                cleanList
            }

            realPaymentMethodsList = ensuredList.sortedByDescending { it.isMain }.toMutableList()

            val mainMethod = realPaymentMethodsList.find { it.isMain }
            val tvUtamaBadge = findViewById<TextView>(R.id.tvMainPaymentUtamaBadge)

            if (mainMethod != null) {
                tvMainPaymentName.text = "${mainMethod.providerName} (${mainMethod.type})"
                val last4 = if (mainMethod.accountNumber.length >= 4) mainMethod.accountNumber.takeLast(4) else mainMethod.accountNumber
                tvMainPaymentAccNumber.text = "•••• $last4 (${mainMethod.accountOwner})"
                tvWithdrawalAccountName.text = "${mainMethod.providerName} a.n. ${mainMethod.accountOwner}"
                tvUtamaBadge?.visibility = View.VISIBLE
            } else {
                tvMainPaymentName.text = "Belum Ada Metode Utama"
                tvMainPaymentAccNumber.text = "Pilih atau tambah metode pembayaran di bawah"
                tvWithdrawalAccountName.text = "Belum Ada Rekening Pencairan"
                tvUtamaBadge?.visibility = View.GONE
            }

            if (realPaymentMethodsList.isEmpty()) {
                recyclerPaymentMethods.visibility = View.GONE
                tvEmptyPaymentMethods.visibility = View.VISIBLE
            } else {
                recyclerPaymentMethods.visibility = View.VISIBLE
                tvEmptyPaymentMethods.visibility = View.GONE
            }

            paymentAdapter = PaymentMethodAdapter(
                context = this,
                methodList = realPaymentMethodsList,
                onItemClicked = { selected ->
                    val intent = Intent(this, AccountDetailActivity::class.java).apply {
                        putExtra("EXTRA_METHOD_ID", selected.id)
                        putExtra("EXTRA_PROVIDER_NAME", selected.providerName)
                        putExtra("EXTRA_TYPE", selected.type)
                        putExtra("EXTRA_ACCOUNT_NUMBER", selected.accountNumber)
                        putExtra("EXTRA_OWNER_NAME", selected.accountOwner)
                        putExtra("EXTRA_IS_MAIN", selected.isMain)
                    }
                    startActivity(intent)
                },
                onSetMainClicked = { selected ->
                    setMainPaymentMethod(selected)
                },
                onDeleteClicked = { selected ->
                    deletePaymentMethod(selected)
                }
            )
            recyclerPaymentMethods.adapter = paymentAdapter
        }
    }

    private fun setMainPaymentMethod(selected: FirestoreRepository.PaymentMethodModel) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val updated = realPaymentMethodsList.map {
            it.copy(isMain = (it.id == selected.id))
        }.sortedByDescending { it.isMain }

        realPaymentMethodsList = updated.toMutableList()
        paymentAdapter.updateData(realPaymentMethodsList)

        val mainMethod = realPaymentMethodsList.find { it.isMain }
        val tvUtamaBadge = findViewById<TextView>(R.id.tvMainPaymentUtamaBadge)
        if (mainMethod != null) {
            tvMainPaymentName.text = "${mainMethod.providerName} (${mainMethod.type})"
            val last4 = if (mainMethod.accountNumber.length >= 4) mainMethod.accountNumber.takeLast(4) else mainMethod.accountNumber
            tvMainPaymentAccNumber.text = "•••• $last4 (${mainMethod.accountOwner})"
            tvWithdrawalAccountName.text = "${mainMethod.providerName} a.n. ${mainMethod.accountOwner}"
            tvUtamaBadge?.visibility = View.VISIBLE
        }

        Toast.makeText(this, "'${selected.providerName}' berhasil dijadikan metode utama!", Toast.LENGTH_SHORT).show()

        FirestoreRepository.updateAllPaymentMethods(currentUserId, updated) {}
    }

    private fun updatePaymentEmptyState() {
        if (realPaymentMethodsList.isEmpty()) {
            recyclerPaymentMethods.visibility = View.GONE
            tvEmptyPaymentMethods.visibility = View.VISIBLE
            tvMainPaymentName.text = "Belum Ada Metode Utama"
            tvMainPaymentAccNumber.text = "Pilih atau tambah metode pembayaran di bawah"
            tvWithdrawalAccountName.text = "Belum Ada Rekening Pencairan"
            findViewById<TextView>(R.id.tvMainPaymentUtamaBadge)?.visibility = View.GONE
        }
    }

    private fun deletePaymentMethod(selected: FirestoreRepository.PaymentMethodModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_address_confirm, null)

        val tvHeaderTitle = dialogView.findViewById<TextView>(R.id.tvDeleteDialogTitle)
        val tvSubNotice = dialogView.findViewById<TextView>(R.id.tvDeleteDialogSubNotice)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivDeleteDialogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRecipientName)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvDialogPhone)
        val tvFull = dialogView.findViewById<TextView>(R.id.tvDialogFullAddress)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCancel)
        val btnConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirmDelete)

        tvHeaderTitle?.text = "Hapus rekening ini?"
        tvSubNotice?.text = "Rekening/metode pembayaran ini akan dihapus dari daftar. Tindakan ini tidak bisa dibatalkan."
        ivIcon?.visibility = View.GONE

        tvTitle?.text = selected.providerName
        val last4 = if (selected.accountNumber.length >= 4) selected.accountNumber.takeLast(4) else selected.accountNumber
        tvPhone?.text = "•••• $last4"
        tvFull?.text = if (selected.accountOwner.isNotEmpty()) "Atas nama ${selected.accountOwner}" else "Tipe: ${selected.type}"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel?.setOnClickListener { alertDialog.dismiss() }

        btnConfirm?.setOnClickListener {
            alertDialog.dismiss()

            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
            realPaymentMethodsList.removeIf { it.id == selected.id }
            paymentAdapter.updateData(realPaymentMethodsList)
            updatePaymentEmptyState()

            Toast.makeText(this, "Metode '${selected.providerName}' berhasil dihapus!", Toast.LENGTH_SHORT).show()

            FirestoreRepository.deleteUserPaymentMethod(currentUserId, selected.id) { _ ->
                loadRealUserPaymentMethods()
            }
        }

        alertDialog.show()
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
        if (realPaymentMethodsList.isEmpty()) {
            Toast.makeText(this, "Tambahkan metode pembayaran terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        val options = realPaymentMethodsList.map { "${it.providerName} (${it.accountNumber.takeLast(4)})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Pilih Metode Utama")
            .setItems(options) { _, which ->
                val selected = realPaymentMethodsList[which]
                setMainPaymentMethod(selected)
            }
            .show()
    }

    private fun showCustomProviderDialog(isBank: Boolean, onProvided: (String) -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etCustom = EditText(this).apply {
            hint = if (isBank) "contoh: Bank Jabar Banten (BJB)" else "contoh: iSaku / AstraPay"
            isSingleLine = true
        }

        layout.addView(etCustom)

        val titleText = if (isBank) "Ketik Nama Bank Baru" else "Ketik Nama E-Wallet Baru"

        AlertDialog.Builder(this)
            .setTitle(titleText)
            .setView(layout)
            .setPositiveButton("Gunakan Ini") { _, _ ->
                val typed = etCustom.text.toString().trim()
                if (typed.isNotEmpty()) {
                    onProvided(typed)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateProviderFieldsVisibility(providerName: String, dialogView: View) {
        val layoutAccNum = dialogView.findViewById<View>(R.id.layoutAccountNumberContainer)
        val layoutQrisUpload = dialogView.findViewById<View>(R.id.layoutQrisUploadContainer)

        if (providerName.contains("QRIS", ignoreCase = true)) {
            layoutAccNum?.visibility = View.GONE
            layoutQrisUpload?.visibility = View.VISIBLE
        } else {
            layoutAccNum?.visibility = View.VISIBLE
            layoutQrisUpload?.visibility = View.GONE
        }
    }

    private fun showAddPaymentBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_add_payment_method, null)
        currentBottomSheetView = bottomSheetView
        tempQrisImageUri = null

        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        dialog.setContentView(bottomSheetView)

        val btnClose = bottomSheetView.findViewById<View>(R.id.btnCloseBottomSheet)
        val tabBank = bottomSheetView.findViewById<TextView>(R.id.tabTypeBank)
        val tabEWallet = bottomSheetView.findViewById<TextView>(R.id.tabTypeEWallet)
        val tvProvider = bottomSheetView.findViewById<TextView>(R.id.tvSelectProvider)
        val etOwnerName = bottomSheetView.findViewById<EditText>(R.id.etAccountOwnerName)
        val etAccNumber = bottomSheetView.findViewById<EditText>(R.id.etAccountNumber)
        val cardUploadQris = bottomSheetView.findViewById<View>(R.id.cardUploadQris)
        val switchMain = bottomSheetView.findViewById<SwitchMaterial>(R.id.switchSetMainPayment)
        val btnCancel = bottomSheetView.findViewById<View>(R.id.btnCancelBottomSheet)
        val btnSave = bottomSheetView.findViewById<View>(R.id.btnSaveBottomSheet)

        if (realPaymentMethodsList.isEmpty()) {
            switchMain?.isChecked = true
            switchMain?.isEnabled = false
        } else {
            switchMain?.isChecked = false
            switchMain?.isEnabled = true
        }

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

        cardUploadQris?.setOnClickListener {
            qrisImagePickerLauncher.launch("image/*")
        }

        tabBank?.setOnClickListener {
            selectedTypeIsBank = true
            tabBank.setBackgroundResource(R.drawable.bg_chip_selected)
            tabBank.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#124332"))
            tabBank.setTextColor(ContextCompat.getColor(this, R.color.white))

            tabEWallet?.background = null
            tabEWallet?.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))

            selectedProviderName = "BCA"
            tvProvider?.text = "BCA"
            updateProviderFieldsVisibility(selectedProviderName, bottomSheetView)
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
            updateProviderFieldsVisibility(selectedProviderName, bottomSheetView)
        }

        tvProvider?.setOnClickListener {
            val options = if (selectedTypeIsBank) {
                arrayOf("BCA", "BNI", "Mandiri", "BRI", "CIMB Niaga", "Permata Bank", "Bank Syariah Indonesia (BSI)", "Bank Danamon", "Lainnya (Ketik Bank Baru)...")
            } else {
                arrayOf("GoPay", "OVO", "DANA", "ShopeePay", "LinkAja", "QRIS", "Lainnya (Ketik E-Wallet Baru)...")
            }

            AlertDialog.Builder(this)
                .setTitle(if (selectedTypeIsBank) "Pilih Bank" else "Pilih E-Wallet")
                .setItems(options) { _, which ->
                    if (which == options.lastIndex) {
                        showCustomProviderDialog(selectedTypeIsBank) { typedName ->
                            selectedProviderName = typedName
                            tvProvider.text = selectedProviderName
                            updateProviderFieldsVisibility(selectedProviderName, bottomSheetView)
                        }
                    } else {
                        selectedProviderName = options[which]
                        tvProvider.text = selectedProviderName
                        updateProviderFieldsVisibility(selectedProviderName, bottomSheetView)
                    }
                }
                .show()
        }

        btnSave?.setOnClickListener {
            val owner = etOwnerName?.text?.toString()?.trim() ?: ""
            val number = etAccNumber?.text?.toString()?.trim() ?: ""

            val isQris = selectedProviderName.contains("QRIS", ignoreCase = true)

            if (!isQris && number.isEmpty()) {
                etAccNumber?.error = "Nomor rekening / e-wallet wajib diisi"
                return@setOnClickListener
            }

            if (isQris && tempQrisImageUri == null) {
                Toast.makeText(this, "Harap upload foto barcode QRIS Toko terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()

            val finalAccNumber = if (isQris) {
                tempQrisImageUri?.toString() ?: "Barcode QRIS Terunggah"
            } else {
                number
            }

            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
            val isMain = switchMain?.isChecked == true

            val newMethod = FirestoreRepository.PaymentMethodModel(
                id = "pay_${System.currentTimeMillis()}",
                type = if (isQris) "QRIS" else if (selectedTypeIsBank) "Bank" else "E-Wallet",
                providerName = selectedProviderName,
                accountNumber = finalAccNumber,
                accountOwner = owner,
                isMain = isMain
            )

            Toast.makeText(this, "Metode pembayaran '$selectedProviderName' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()

            if (isMain) {
                val updatedList = realPaymentMethodsList.map { it.copy(isMain = false) } + newMethod
                FirestoreRepository.updateAllPaymentMethods(currentUserId, updatedList) {
                    loadRealUserPaymentMethods()
                }
            } else {
                FirestoreRepository.saveUserPaymentMethod(currentUserId, newMethod) {
                    loadRealUserPaymentMethods()
                }
            }
        }

        dialog.show()
    }


}
