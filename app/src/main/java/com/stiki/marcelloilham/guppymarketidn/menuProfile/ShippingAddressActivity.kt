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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.adapter.ShippingAddressAdapter
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository

class ShippingAddressActivity : AppCompatActivity() {

    private lateinit var recyclerAddresses: RecyclerView
    private lateinit var tvTotalCount: TextView
    private lateinit var etCourierNotes: EditText
    private lateinit var adapter: ShippingAddressAdapter

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var addressesList: MutableList<FirestoreRepository.AddressModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shipping_address)

        val topBar = findViewById<View>(R.id.topBarShippingAddress)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackShippingAddress)
        btnBack.setOnClickListener { finish() }

        initViews()
        loadAddresses()
    }

    private fun initViews() {
        recyclerAddresses = findViewById(R.id.recyclerShippingAddresses)
        tvTotalCount = findViewById(R.id.tvTotalAddressCount)
        etCourierNotes = findViewById(R.id.etCourierNotes)

        recyclerAddresses.layoutManager = LinearLayoutManager(this)
        recyclerAddresses.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        val btnAdd = findViewById<TextView>(R.id.btnAddNewAddress)
        btnAdd.setOnClickListener {
            startActivity(android.content.Intent(this, AddAddressActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadAddresses()
    }

    private fun loadAddresses() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"

        FirestoreRepository.fetchUserAddresses(currentUserId) { list ->
            val cleanList = list.distinctBy { it.id }.distinctBy { it.label }

            val ensuredList = if (cleanList.isNotEmpty() && cleanList.none { it.isDefault }) {
                val fixedList = cleanList.mapIndexed { idx, addr ->
                    if (idx == 0) addr.copy(isDefault = true) else addr.copy(isDefault = false)
                }
                FirestoreRepository.updateAllAddresses(currentUserId, fixedList) {}
                fixedList
            } else {
                cleanList
            }

            addressesList = ensuredList.sortedByDescending { it.isDefault }.toMutableList()

            adapter = ShippingAddressAdapter(
                context = this,
                addressList = addressesList,
                onSetDefaultClicked = { addr ->
                    setDefaultAddress(addr)
                },
                onEditClicked = { addr ->
                    showAddressFormDialog(addr)
                },
                onDeleteClicked = { addr ->
                    deleteAddress(addr)
                }
            )
            recyclerAddresses.adapter = adapter
            updateEmptyStateView()
        }
    }

    private fun updateEmptyStateView() {
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyAddressState)
        if (addressesList.isEmpty()) {
            recyclerAddresses.visibility = View.GONE
            tvEmpty?.visibility = View.VISIBLE
        } else {
            recyclerAddresses.visibility = View.VISIBLE
            tvEmpty?.visibility = View.GONE
        }
        tvTotalCount.text = "${addressesList.size} alamat"
    }

    private fun setDefaultAddress(selectedAddr: FirestoreRepository.AddressModel) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"

        val oldIndex = addressesList.indexOfFirst { it.id == selectedAddr.id }

        val updatedList = addressesList.map {
            it.copy(isDefault = (it.id == selectedAddr.id))
        }.sortedByDescending { it.isDefault }

        addressesList = updatedList.toMutableList()
        adapter.updateDataWithMoveAnimation(addressesList, oldIndex)

        // Save atomically via Firestore Batch Write
        FirestoreRepository.updateAllAddresses(currentUserId, updatedList) {}
    }

    private fun deleteAddress(addr: FirestoreRepository.AddressModel) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
        FirestoreRepository.deleteUserAddress(currentUserId, addr.id) { _ ->
            addressesList.removeIf { it.id == addr.id }

            if (addr.isDefault && addressesList.isNotEmpty()) {
                addressesList[0] = addressesList[0].copy(isDefault = true)
                FirestoreRepository.addUserAddress(currentUserId, addressesList[0]) {}
            }

            addressesList = addressesList.sortedByDescending { it.isDefault }.toMutableList()
            adapter.updateData(addressesList)
            updateEmptyStateView()
            Toast.makeText(this, "Alamat '${addr.label}' berhasil dihapus", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddressFormDialog(existingAddr: FirestoreRepository.AddressModel?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etLabel = EditText(this).apply {
            hint = "Label Alamat (contoh: Rumah / Toko / Gudang)"
            setText(existingAddr?.label ?: "")
        }
        val etName = EditText(this).apply {
            hint = "Nama Penerima"
            setText(existingAddr?.recipientName ?: "Marcello Aja")
        }
        val etPhone = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            hint = "Nomor Telepon / WhatsApp"
            setText(existingAddr?.phone ?: "(0812-3456-7890)")
        }
        val etFullAddr = EditText(this).apply {
            hint = "Alamat Lengkap (Jl, No, RT/RW, Kelurahan, Kecamatan, Kota, Kode Pos)"
            setText(existingAddr?.fullAddress ?: "")
        }

        layout.addView(etLabel)
        layout.addView(etName)
        layout.addView(etPhone)
        layout.addView(etFullAddr)

        val isEdit = (existingAddr != null)
        val titleText = if (isEdit) "Ubah Alamat Pengiriman" else "Tambah Alamat Pengiriman Baru"

        AlertDialog.Builder(this)
            .setTitle(titleText)
            .setView(layout)
            .setPositiveButton("Simpan Alamat") { _, _ ->
                val label = etLabel.text.toString().trim().ifEmpty { "Rumah" }
                val name = etName.text.toString().trim().ifEmpty { "Marcello Aja" }
                val phone = etPhone.text.toString().trim().ifEmpty { "(0812-3456-7890)" }
                val addressText = etFullAddr.text.toString().trim()

                if (addressText.isEmpty()) {
                    Toast.makeText(this, "Alamat lengkap wajib diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val currentUserId = firebaseAuth.currentUser?.uid ?: "user_default"
                val isFirstAddress = addressesList.isEmpty()
                val isDefaultVal = existingAddr?.isDefault ?: isFirstAddress

                val newModel = FirestoreRepository.AddressModel(
                    id = existingAddr?.id ?: "addr_${System.currentTimeMillis()}",
                    label = label,
                    recipientName = name,
                    phone = phone,
                    fullAddress = addressText,
                    isDefault = isDefaultVal
                )

                FirestoreRepository.addUserAddress(currentUserId, newModel) { _ ->
                    Toast.makeText(this, "Alamat '$label' berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    loadAddresses()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
