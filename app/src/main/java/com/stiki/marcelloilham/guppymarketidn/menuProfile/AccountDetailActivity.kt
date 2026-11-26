package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private var methodType: String = "Bank"
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
        val passedType = intent.getStringExtra("EXTRA_TYPE") ?: ""
        accountNumber = intent.getStringExtra("EXTRA_ACCOUNT_NUMBER") ?: ""
        accountOwner = intent.getStringExtra("EXTRA_OWNER_NAME") ?: "Marcello Aja"
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

        updateUI()
    }

    private fun updateUI() {
        val tvHeaderTitle = findViewById<TextView>(R.id.tvAccountDetailHeaderTitle)
        val tvHeaderSub = findViewById<TextView>(R.id.tvAccountDetailHeaderSub)
        val tvBoxLabel = findViewById<TextView>(R.id.tvDetailBoxLabel)
        val imgTypeIcon = findViewById<ImageView>(R.id.imgDetailTypeIcon)

        val cardBankInfo = findViewById<View>(R.id.cardAccountInfoContainer)
        val cardEWalletInfo = findViewById<View>(R.id.cardEWalletInfoContainer)
        val cardBalance = findViewById<View>(R.id.cardBalanceContainer)
        val cardQris = findViewById<View>(R.id.cardQrisContainer)

        val isQris = methodType.equals("QRIS", ignoreCase = true)
        val isBank = methodType.equals("Bank", ignoreCase = true)
        val isEWallet = methodType.equals("E-Wallet", ignoreCase = true)

        if (isQris) {
            tvHeaderTitle?.text = "Detail QRIS"
            tvHeaderSub?.text = "QRIS Toko Guppy Farm"
            tvProviderName.text = providerName
            tvBoxLabel?.text = "NMID / BARCODE"
            imgTypeIcon?.setImageResource(R.drawable.ic_qris)

            cardQris?.visibility = View.VISIBLE
            cardBankInfo?.visibility = View.GONE
            cardEWalletInfo?.visibility = View.GONE
            cardBalance?.visibility = View.GONE

            val ivBarcode = findViewById<ImageView>(R.id.ivQrisBarcodeImage)
            if (ivBarcode != null) {
                if (accountNumber.isNotEmpty() && (accountNumber.startsWith("file://") || accountNumber.startsWith("content://") || accountNumber.startsWith("/"))) {
                    try {
                        val uri = android.net.Uri.parse(accountNumber)
                        ivBarcode.colorFilter = null
                        ivBarcode.imageTintList = null
                        ivBarcode.setImageURI(uri)
                    } catch (e: Exception) {
                        Log.e("AccountDetail", "Error loading QRIS image: ${e.message}")
                        ivBarcode.setImageResource(R.drawable.ic_qris)
                    }
                } else {
                    ivBarcode.setImageResource(R.drawable.ic_qris)
                }
            }
        } else if (isEWallet) {
            tvHeaderTitle?.text = "Detail E-Wallet"
            tvHeaderSub?.text = "$providerName - Terhubung"
            tvProviderName.text = providerName
            tvBoxLabel?.text = "NOMOR HP / ID AKUN"
            imgTypeIcon?.setImageResource(R.drawable.ic_ewallet)

            val tvEWalletOwner = findViewById<TextView>(R.id.tvEWalletOwnerName)
            val tvEWalletPhone = findViewById<TextView>(R.id.tvEWalletPhone)
            tvEWalletOwner?.text = accountOwner
            tvEWalletPhone?.text = accountNumber

            cardQris?.visibility = View.GONE
            cardBankInfo?.visibility = View.GONE
            cardEWalletInfo?.visibility = View.VISIBLE
            cardBalance?.visibility = View.GONE
        } else {
            tvHeaderTitle?.text = "Detail Rekening"
            tvHeaderSub?.text = "Kelola rekening penerima pencairan"
            tvProviderName.text = providerName
            tvBoxLabel?.text = "NOMOR REKENING"
            imgTypeIcon?.setImageResource(R.drawable.ic_payment_card)

            cardQris?.visibility = View.GONE
            cardBankInfo?.visibility = View.VISIBLE
            cardEWalletInfo?.visibility = View.GONE
            cardBalance?.visibility = View.VISIBLE
        }

        tvOwnerName.text = accountOwner
        switchMainAccount.isChecked = isMain

        if (isMain) {
            tvUtamaBadge.visibility = View.VISIBLE
        } else {
            tvUtamaBadge.visibility = View.GONE
        }

        val last4 = if (accountNumber.length >= 4) accountNumber.takeLast(4) else accountNumber
        tvFullAccNumber.text = if (isQris) "KODE QRIS TERDAFTAR" else "•••• $last4    $accountNumber"
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

    override fun onResume() {
        super.onResume()
        if (methodId.isNotEmpty()) {
            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
            FirestoreRepository.fetchUserPaymentMethods(currentUserId) { list ->
                val updated = list.find { it.id == methodId }
                if (updated != null) {
                    providerName = updated.providerName
                    accountNumber = updated.accountNumber
                    accountOwner = updated.accountOwner
                    isMain = updated.isMain
                    updateUI()
                }
            }
        }
    }

    private fun setupListeners() {
        switchMainAccount.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isMain) {
                isMain = isChecked
                if (isMain) {
                    tvUtamaBadge.visibility = View.VISIBLE
                    updateMainPaymentInFirestore()
                    Toast.makeText(this, "'$providerName' dijadikan metode utama!", Toast.LENGTH_SHORT).show()
                } else {
                    tvUtamaBadge.visibility = View.GONE
                }
            }
        }

        val btnDownload = findViewById<View>(R.id.btnDownloadQris)
        btnDownload?.setOnClickListener {
            Toast.makeText(this, "Foto Barcode QRIS disalin ke folder Unduhan!", Toast.LENGTH_SHORT).show()
        }

        val btnShare = findViewById<View>(R.id.btnShareQris)
        btnShare?.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "QRIS Toko - $providerName")
                putExtra(Intent.EXTRA_TEXT, "Scan QRIS toko Guppy Farm untuk melakukan pembayaran cepat!")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Kode QRIS via"))
        }

        val btnEdit = findViewById<View>(R.id.btnEditAccountDetails)
        btnEdit?.setOnClickListener {
            val intent = Intent(this, EditAccountDetailActivity::class.java).apply {
                putExtra("EXTRA_METHOD_ID", methodId)
                putExtra("EXTRA_PROVIDER_NAME", providerName)
                putExtra("EXTRA_ACCOUNT_NUMBER", accountNumber)
                putExtra("EXTRA_OWNER_NAME", accountOwner)
                putExtra("EXTRA_IS_MAIN", isMain)
            }
            startActivity(intent)
        }

        val btnDelete = findViewById<View>(R.id.btnDeleteAccount)
        btnDelete?.setOnClickListener { showDeleteConfirmDialog() }
    }

    private fun updateMainPaymentInFirestore() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.fetchUserPaymentMethods(currentUserId) { list ->
            val updated = list.map {
                if (it.id == methodId) it.copy(isMain = true) else it.copy(isMain = false)
            }
            FirestoreRepository.updateAllPaymentMethods(currentUserId, updated) {}
        }
    }

    private fun showDeleteConfirmDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_address_confirm, null)

        val tvHeaderTitle = dialogView.findViewById<TextView>(R.id.tvDeleteDialogTitle)
        val tvSubNotice = dialogView.findViewById<TextView>(R.id.tvDeleteDialogSubNotice)
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivDeleteDialogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogRecipientName)
        val tvSub = dialogView.findViewById<TextView>(R.id.tvDialogPhone)
        val tvFull = dialogView.findViewById<TextView>(R.id.tvDialogFullAddress)
        val btnCancel = dialogView.findViewById<View>(R.id.btnDialogCancel)
        val btnConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirmDelete)

        tvHeaderTitle?.text = "Hapus rekening ini?"
        tvSubNotice?.text = "Rekening/metode pembayaran ini akan dihapus dari daftar. Tindakan ini tidak bisa dibatalkan."
        ivIcon?.visibility = View.GONE

        tvTitle?.text = providerName
        val last4 = if (accountNumber.length >= 4) accountNumber.takeLast(4) else accountNumber
        tvSub?.text = "•••• $last4"
        tvFull?.text = if (accountOwner.isNotEmpty()) "Atas nama $accountOwner" else "Rekening/E-wallet tersimpan"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel?.setOnClickListener { alertDialog.dismiss() }

        btnConfirm?.setOnClickListener {
            alertDialog.dismiss()
            Toast.makeText(this, "Metode '$providerName' berhasil dihapus!", Toast.LENGTH_SHORT).show()
            finish()

            val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
            FirestoreRepository.deleteUserPaymentMethod(currentUserId, methodId) {}
        }

        alertDialog.show()
    }


}
