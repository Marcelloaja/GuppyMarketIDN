package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
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
import java.util.Locale

class AccountDetailActivity : AppCompatActivity() {

    private lateinit var tvProviderName: TextView
    private lateinit var tvUtamaBadge: TextView
    private lateinit var tvFullAccNumber: TextView
    private lateinit var tvOwnerName: TextView
    private lateinit var tvActiveBalance: TextView
    private lateinit var tvWithdrawableBalance: TextView
    private lateinit var switchMainAccount: SwitchMaterial

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var methodId: String = ""
    private var providerName: String = "BCA"
    private var accountNumber: String = ""
    private var accountOwner: String = "Marcello Aja"
    private var isMain: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_detail)

        val topBar = findViewById<View>(R.id.topBarAccountDetail)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackAccountDetail)
        btnBack?.setOnClickListener { finish() }

        initViews()
        readIntentData()
        setupListeners()
        loadActiveUserBalance()
    }

    private fun initViews() {
        tvProviderName = findViewById(R.id.tvDetailProviderName)
        tvUtamaBadge = findViewById(R.id.tvDetailUtamaBadge)
        tvFullAccNumber = findViewById(R.id.tvDetailFullAccNumber)
        tvOwnerName = findViewById(R.id.tvDetailOwnerName)
        tvActiveBalance = findViewById(R.id.tvDetailActiveBalance)
        tvWithdrawableBalance = findViewById(R.id.tvDetailWithdrawableBalance)
        switchMainAccount = findViewById(R.id.switchDetailMainAccount)
    }

    private fun readIntentData() {
        methodId = intent.getStringExtra("EXTRA_METHOD_ID") ?: ""
        providerName = intent.getStringExtra("EXTRA_PROVIDER_NAME") ?: "BCA"
        accountNumber = intent.getStringExtra("EXTRA_ACCOUNT_NUMBER") ?: "821044564821"
        accountOwner = intent.getStringExtra("EXTRA_OWNER_NAME") ?: "Marcello Aja"
        isMain = intent.getBooleanExtra("EXTRA_IS_MAIN", false)

        updateUI()
    }

    private fun updateUI() {
        tvProviderName.text = providerName
        tvOwnerName.text = accountOwner
        switchMainAccount.isChecked = isMain

        if (isMain) {
            tvUtamaBadge.visibility = View.VISIBLE
        } else {
            tvUtamaBadge.visibility = View.GONE
        }

        val last4 = if (accountNumber.length >= 4) accountNumber.takeLast(4) else accountNumber
        tvFullAccNumber.text = "•••• $last4    $accountNumber"
    }

    private fun loadActiveUserBalance() {
        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("user").document(currentUserId)
                .get()
                .addOnSuccessListener { doc ->
                    val balance = doc.getLong("active_balance") ?: 0L
                    val formatted = "Rp " + String.format(Locale.US, "%,d", balance).replace(',', '.')
                    tvActiveBalance.text = formatted
                    tvWithdrawableBalance.text = formatted
                }
        }
    }

    private fun setupListeners() {
        val btnEdit = findViewById<View>(R.id.btnEditAccountDetails)
        btnEdit?.setOnClickListener { showEditDetailsDialog() }

        val btnDelete = findViewById<View>(R.id.btnDeleteAccount)
        btnDelete?.setOnClickListener { showDeleteConfirmDialog() }

        val btnSave = findViewById<TextView>(R.id.btnSaveAccountDetails)
        btnSave?.setOnClickListener { saveChangesToFirestore() }
    }

    private fun showEditDetailsDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etNumber = EditText(this).apply {
            hint = "Nomor Rekening / Akun E-Wallet"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(accountNumber)
        }

        val etOwner = EditText(this).apply {
            hint = "Nama Pemilik Rekening"
            isSingleLine = true
            setText(accountOwner)
        }

        layout.addView(etOwner)
        layout.addView(etNumber)

        AlertDialog.Builder(this)
            .setTitle("Ubah Data $providerName")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val newNum = etNumber.text.toString().trim()
                val newOwner = etOwner.text.toString().trim()
                if (newNum.isNotEmpty()) {
                    accountNumber = newNum
                    accountOwner = if (newOwner.isNotEmpty()) newOwner else accountOwner
                    updateUI()
                    Toast.makeText(this, "Data $providerName diperbarui!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_address_confirm, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRecipientName)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvDialogPhone)
        val tvFull = dialogView.findViewById<TextView>(R.id.tvDialogFullAddress)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCancel)
        val btnConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirmDelete)

        tvTitle?.text = providerName
        tvSub?.text = "•••• ${accountNumber.takeLast(4)}"
        tvFull?.text = "Rekening/E-wallet atas nama $accountOwner"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel?.setOnClickListener { alertDialog.dismiss() }

        btnConfirm?.setOnClickListener {
            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
            FirestoreRepository.deleteUserPaymentMethod(currentUserId, methodId) { _ ->
                Toast.makeText(this, "Metode $providerName berhasil dihapus", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
                finish()
            }
        }

        alertDialog.show()
    }

    private fun saveChangesToFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val isNewMain = switchMainAccount.isChecked

        val updatedItem = FirestoreRepository.PaymentMethodModel(
            id = methodId,
            type = if (providerName.contains("BCA", true) || providerName.contains("BNI", true) || providerName.contains("Mandiri", true)) "Bank" else "E-Wallet",
            providerName = providerName,
            accountNumber = accountNumber,
            accountOwner = accountOwner,
            isMain = isNewMain
        )

        val btnSave = findViewById<TextView>(R.id.btnSaveAccountDetails)
        btnSave?.isEnabled = false
        btnSave?.text = "Menyimpan Perubahan..."

        if (isNewMain) {
            FirestoreRepository.fetchUserPaymentMethods(currentUserId) { existingList ->
                val updatedList = existingList.map {
                    if (it.id == methodId) updatedItem.copy(isMain = true) else it.copy(isMain = false)
                }
                FirestoreRepository.updateAllPaymentMethods(currentUserId, updatedList) {
                    btnSave?.isEnabled = true
                    btnSave?.text = "Simpan Perubahan"
                    Toast.makeText(this, "Detail rekening $providerName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            FirestoreRepository.saveUserPaymentMethod(currentUserId, updatedItem) { success ->
                btnSave?.isEnabled = true
                btnSave?.text = "Simpan Perubahan"
                if (success) {
                    Toast.makeText(this, "Detail rekening $providerName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
