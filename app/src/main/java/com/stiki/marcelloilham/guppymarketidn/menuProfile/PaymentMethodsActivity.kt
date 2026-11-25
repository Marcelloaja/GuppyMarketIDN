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
        btnAddPayment?.setOnClickListener { showAddPaymentDialog() }

        val btnSave = findViewById<TextView>(R.id.btnSavePaymentSettings)
        btnSave?.setOnClickListener { saveSettingsToFirestore() }
    }

    private fun loadPaymentSettingsFromFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchPaymentSettings(currentUserId) { model ->
            currentModel = model

            tvMainPaymentName.text = model.mainMethod
            tvMainPaymentAccNumber.text = "•••• ${model.bcaAccountNumber.takeLast(4)}"

            tvBankBCANumber.text = "•••• ${model.bcaAccountNumber.takeLast(4)} (Utama)"
            tvBankBNINumber.text = "•••• ${model.bniAccountNumber.takeLast(4)}"

            if (model.mandiriActive) {
                tvBankMandiriStatus.text = "Terhubung"
                btnActivateMandiri.text = "Kelola"
            } else {
                tvBankMandiriStatus.text = "Belum aktif"
                btnActivateMandiri.text = "Aktifkan"
            }

            switchGoPay.isChecked = model.goPayEnabled
            switchOVO.isChecked = model.ovoEnabled
            switchDANA.isChecked = model.danaEnabled
            switchQRIS.isChecked = model.qrisEnabled

            tvWithdrawalAccountName.text = model.withdrawalAccount
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
                currentModel = currentModel.copy(mainMethod = options[which])
                tvMainPaymentName.text = currentModel.mainMethod
            }
            .show()
    }

    private fun showEditBankDialog(bankName: String, currentAcc: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etAccNumber = EditText(this).apply {
            hint = "Nomor Rekening / Virtual Account $bankName"
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
                    loadPaymentSettingsFromFirestore()
                    Toast.makeText(this, "Rekening $bankName diperbarui!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showAddPaymentDialog() {
        val options = arrayOf("Tambah Rekening Bank Baru", "Hubungkan E-Wallet Lain (ShopeePay, LinkAja)", "Daftarkan QRIS Toko Baru")
        AlertDialog.Builder(this)
            .setTitle("Tambah Metode Pembayaran Baru")
            .setItems(options) { _, which ->
                Toast.makeText(this, "Memproses pilihan '${options[which]}'...", Toast.LENGTH_SHORT).show()
            }
            .show()
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
