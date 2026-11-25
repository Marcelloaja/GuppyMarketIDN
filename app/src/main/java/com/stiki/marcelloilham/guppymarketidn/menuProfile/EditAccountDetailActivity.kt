package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

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
    private var accountOwner: String = "Marcello Aja"
    private var isMain: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_detail)

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
        accountNumber = intent.getStringExtra("EXTRA_ACCOUNT_NUMBER") ?: ""
        accountOwner = intent.getStringExtra("EXTRA_OWNER_NAME") ?: "Marcello Aja"
        isMain = intent.getBooleanExtra("EXTRA_IS_MAIN", false)

        etOwnerName.setText(accountOwner)
        etAccountNumber.setText(accountNumber)
        tvBankChoice.text = providerName
        switchSetMainAccount.isChecked = isMain
    }

    private fun setupListeners() {
        val btnCopy = findViewById<ImageView>(R.id.btnCopyAccNumber)
        btnCopy?.setOnClickListener {
            val num = etAccountNumber.text.toString().trim()
            if (num.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Nomor Rekening", num)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Nomor rekening disalin ke clipboard!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnClear = findViewById<ImageView>(R.id.btnClearAccNumber)
        btnClear?.setOnClickListener {
            etAccountNumber.setText("")
            etAccountNumber.requestFocus()
        }

        val layoutBank = findViewById<View>(R.id.layoutSelectBank)
        layoutBank?.setOnClickListener { showBankChooserDialog() }

        val layoutType = findViewById<View>(R.id.layoutSelectAccountType)
        layoutType?.setOnClickListener { showAccountTypeChooserDialog() }

        val btnSave = findViewById<TextView>(R.id.btnSaveEditAccount)
        btnSave?.setOnClickListener { saveChangesToFirestore() }
    }

    private fun showBankChooserDialog() {
        val banks = arrayOf("BCA", "BNI", "Mandiri", "BRI", "CIMB Niaga", "Permata Bank", "Bank Syariah Indonesia (BSI)", "Bank Danamon")
        AlertDialog.Builder(this)
            .setTitle("Pilih Bank")
            .setItems(banks) { _, which ->
                providerName = banks[which]
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

        if (owner.isEmpty()) {
            etOwnerName.error = "Nama pemilik wajib diisi"
            return
        }

        if (number.isEmpty()) {
            etAccountNumber.error = "Nomor rekening wajib diisi"
            return
        }

        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        val updatedItem = FirestoreRepository.PaymentMethodModel(
            id = methodId,
            type = if (providerName.contains("BCA", true) || providerName.contains("BNI", true) || providerName.contains("Mandiri", true)) "Bank" else "E-Wallet",
            providerName = providerName,
            accountNumber = number,
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
                    Toast.makeText(this, "Data rekening $providerName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            FirestoreRepository.saveUserPaymentMethod(currentUserId, updatedItem) { success ->
                btnSave?.isEnabled = true
                btnSave?.text = "Simpan Perubahan"
                if (success) {
                    Toast.makeText(this, "Data rekening $providerName berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gagal menyimpan. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
