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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.PaymentMethodAdapter
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class PaymentMethodsActivity : AppCompatActivity() {

    private lateinit var tvMainPaymentName: TextView
    private lateinit var tvMainPaymentAccNumber: TextView

    private lateinit var recyclerPaymentMethods: RecyclerView
    private lateinit var tvEmptyPaymentMethods: TextView
    private lateinit var paymentAdapter: PaymentMethodAdapter

    private lateinit var switchGoPay: SwitchMaterial
    private lateinit var switchOVO: SwitchMaterial
    private lateinit var switchDANA: SwitchMaterial
    private lateinit var switchQRIS: SwitchMaterial

    private lateinit var tvWithdrawalAccountName: TextView
    private lateinit var tvWithdrawalSubtitle: TextView

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var currentModel = FirestoreRepository.PaymentSettingsModel()
    private var realPaymentMethodsList: MutableList<FirestoreRepository.PaymentMethodModel> = mutableListOf()

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
        loadRealUserPaymentMethods()
        loadActiveUserBalance()
    }

    private fun initViews() {
        tvMainPaymentName = findViewById(R.id.tvMainPaymentName)
        tvMainPaymentAccNumber = findViewById(R.id.tvMainPaymentAccNumber)

        recyclerPaymentMethods = findViewById(R.id.recyclerPaymentMethods)
        tvEmptyPaymentMethods = findViewById(R.id.tvEmptyPaymentMethods)
        recyclerPaymentMethods.layoutManager = LinearLayoutManager(this)

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

        val btnAddPayment = findViewById<View>(R.id.btnAddNewPaymentMethod)
        btnAddPayment?.setOnClickListener { showAddPaymentBottomSheet() }

        val btnSave = findViewById<TextView>(R.id.btnSavePaymentSettings)
        btnSave?.setOnClickListener { saveSettingsToFirestore() }
    }

    private fun loadRealUserPaymentMethods() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserPaymentMethods(currentUserId) { list ->
            realPaymentMethodsList = list.distinctBy { it.id }.toMutableList()

            val mainMethod = realPaymentMethodsList.find { it.isMain } ?: realPaymentMethodsList.firstOrNull()

            if (mainMethod != null) {
                tvMainPaymentName.text = "${mainMethod.providerName} (${mainMethod.type})"
                val last4 = if (mainMethod.accountNumber.length >= 4) mainMethod.accountNumber.takeLast(4) else mainMethod.accountNumber
                tvMainPaymentAccNumber.text = "•••• $last4 (${mainMethod.accountOwner})"
                tvWithdrawalAccountName.text = "${mainMethod.providerName} a.n. ${mainMethod.accountOwner}"
            } else {
                tvMainPaymentName.text = "Belum Ada Metode Utama"
                tvMainPaymentAccNumber.text = "Pilih atau tambah metode pembayaran di bawah"
                tvWithdrawalAccountName.text = "Belum Ada Rekening Pencairan"
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

        FirestoreRepository.updateAllPaymentMethods(currentUserId, updated) {
            loadRealUserPaymentMethods()
            Toast.makeText(this, "'${selected.providerName}' dijadikan metode utama!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deletePaymentMethod(selected: FirestoreRepository.PaymentMethodModel) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.deleteUserPaymentMethod(currentUserId, selected.id) { _ ->
            Toast.makeText(this, "Metode '${selected.providerName}' berhasil dihapus", Toast.LENGTH_SHORT).show()
            loadRealUserPaymentMethods()
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
            val isMain = switchMain?.isChecked == true

            val newMethod = FirestoreRepository.PaymentMethodModel(
                id = "pay_${System.currentTimeMillis()}",
                type = if (selectedTypeIsBank) "Bank" else "E-Wallet",
                providerName = selectedProviderName,
                accountNumber = number,
                accountOwner = owner,
                isMain = isMain
            )

            if (isMain) {
                val updatedList = realPaymentMethodsList.map { it.copy(isMain = false) } + newMethod
                FirestoreRepository.updateAllPaymentMethods(currentUserId, updatedList) {
                    Toast.makeText(this, "Metode pembayaran '$selectedProviderName' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    loadRealUserPaymentMethods()
                    dialog.dismiss()
                }
            } else {
                FirestoreRepository.saveUserPaymentMethod(currentUserId, newMethod) { success ->
                    if (success) {
                        Toast.makeText(this, "Metode pembayaran '$selectedProviderName' berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                        loadRealUserPaymentMethods()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
                    }
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
