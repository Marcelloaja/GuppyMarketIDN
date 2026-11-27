package com.stiki.marcelloilham.guppymarketidn.menuProfile

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
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
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class AddProductActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var tvCategory: TextView
    private lateinit var tvStrain: TextView
    private lateinit var etPrice: EditText
    private lateinit var tvUnit: TextView
    private lateinit var etStock: EditText
    private lateinit var etDescription: EditText

    private var selectedCategory: String = "Ikan Kontes"
    private var selectedStrain: String = "Mosaic"
    private var selectedUnit: String = "per pasang"

    private lateinit var pickGalleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var takeCameraLauncher: ActivityResultLauncher<Intent>
    private var currentMediaSlot: String = "Foto 1"
    private var isSelectingVideo: Boolean = false

    private val selectedPhotoUris = arrayOfNulls<String>(3)
    private val selectedPhotoDataUrls = arrayOfNulls<String>(3)
    private var selectedVideoUri: String = ""
    private val mediaExecutor = Executors.newSingleThreadExecutor()
    private val pendingMediaJobs = AtomicInteger(0)
    private var publishAfterMediaReady = false
    private var realSellerProfile: FirestoreRepository.UserProfileModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        val topBar = findViewById<View>(R.id.topBarAddProduct)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackAddProduct)
        btnBack.setOnClickListener { finish() }

        initMediaLaunchers()
        initViews()
        setupListeners()

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirestoreRepository.fetchUserProfileById(uid) { profile ->
                if (!isFinishing && !isDestroyed) {
                    realSellerProfile = profile
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaExecutor.shutdownNow()
    }

    private fun initMediaLaunchers() {
        pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedUri = result.data?.data
                handleSelectedMedia(uri = selectedUri, bitmap = null)
            }
        }

        takeCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val dataIntent = result.data
                val videoUri = dataIntent?.data
                @Suppress("DEPRECATION")
                val photoBitmap = (dataIntent?.extras?.get("data") as? Bitmap)
                    ?: (dataIntent?.getParcelableExtra("data") as? Bitmap)

                handleSelectedMedia(uri = videoUri, bitmap = photoBitmap)
            }
        }
    }

    private fun showMediaSourceDialog(isVideo: Boolean, slotName: String) {
        currentMediaSlot = slotName
        isSelectingVideo = isVideo

        val options = arrayOf("📸 Buka Kamera Langsung", "🖼️ Pilih dari Galeri HP")
        AlertDialog.Builder(this)
            .setTitle("Upload $slotName")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val intent = Intent(if (isVideo) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE)
                    takeCameraLauncher.launch(intent)
                } else {
                    val intent = Intent(Intent.ACTION_PICK).apply {
                        type = if (isVideo) "video/*" else "image/*"
                    }
                    pickGalleryLauncher.launch(intent)
                }
            }
            .show()
    }

    private fun getFileSizeInMB(uri: Uri?, bitmap: Bitmap?): Float {
        if (bitmap != null) {
            val bytes = bitmap.byteCount.toLong()
            return if (bytes > 0L) bytes / (1024f * 1024f) else 1.5f
        }
        if (uri == null) return 2.4f
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            val sizeIndex = cursor?.getColumnIndex(MediaStore.MediaColumns.SIZE) ?: -1
            var bytes = 0L
            if (cursor != null && sizeIndex != -1 && cursor.moveToFirst()) {
                bytes = cursor.getLong(sizeIndex)
                cursor.close()
            } else {
                val pfd = contentResolver.openFileDescriptor(uri, "r")
                bytes = pfd?.statSize ?: 0L
                pfd?.close()
            }
            if (bytes > 0L) bytes / (1024f * 1024f) else 3.2f
        } catch (_: Exception) {
            3.2f
        }
    }

    private fun handleSelectedMedia(uri: Uri?, bitmap: Bitmap?) {
        val realSizeMB = getFileSizeInMB(uri, bitmap)
        val formattedRealSize = String.format(java.util.Locale.US, "%.1f", realSizeMB)

        if (isSelectingVideo) {
            val ivPreview = findViewById<ImageView>(R.id.ivVideoPreview)
            val layoutPlaceholder = findViewById<View>(R.id.layoutVideoPlaceholder)

            if (uri != null) {
                ivPreview?.setImageURI(uri)
                ivPreview?.visibility = View.VISIBLE
                layoutPlaceholder?.visibility = View.GONE
                val persisted = SafeImageLoader.copyUriToInternalStorage(
                    this, uri, "prod_video_${System.currentTimeMillis()}.mp4"
                )
                selectedVideoUri = persisted?.toString() ?: uri.toString()
            } else if (bitmap != null) {
                ivPreview?.setImageBitmap(bitmap)
                ivPreview?.visibility = View.VISIBLE
                layoutPlaceholder?.visibility = View.GONE
            }
            Toast.makeText(this, "Video $formattedRealSize MB dipilih.", Toast.LENGTH_SHORT).show()
        } else {
            val slotIndex = when (currentMediaSlot) {
                "Foto 2" -> 1
                "Foto 3" -> 2
                else -> 0
            }
            val (ivPreview, layoutPlaceholder) = when (slotIndex) {
                1 -> Pair(findViewById<ImageView>(R.id.ivPhotoPreview2), findViewById<View>(R.id.layoutPhotoPlaceholder2))
                2 -> Pair(findViewById<ImageView>(R.id.ivPhotoPreview3), findViewById<View>(R.id.layoutPhotoPlaceholder3))
                else -> Pair(findViewById<ImageView>(R.id.ivPhotoPreview1), findViewById<View>(R.id.layoutPhotoPlaceholder1))
            }

            if (bitmap != null) {
                ivPreview?.setImageBitmap(bitmap)
                ivPreview?.visibility = View.VISIBLE
                layoutPlaceholder?.visibility = View.GONE
            } else if (uri != null) {
                ivPreview?.setImageURI(uri)
                ivPreview?.visibility = View.VISIBLE
                layoutPlaceholder?.visibility = View.GONE
            }

            pendingMediaJobs.incrementAndGet()
            mediaExecutor.execute {
                val persistedUri = when {
                    bitmap != null -> SafeImageLoader.saveBitmapToInternalStorage(
                        this, bitmap, "prod_photo_${slotIndex}_${System.currentTimeMillis()}.jpg"
                    )?.toString().orEmpty()
                    uri != null -> SafeImageLoader.copyUriToInternalStorage(
                        this, uri, "prod_photo_${slotIndex}_${System.currentTimeMillis()}.jpg"
                    )?.toString() ?: uri.toString()
                    else -> ""
                }
                val encodedDataUrl = when {
                    bitmap != null -> SafeImageLoader.encodeBitmapToDataUrl(bitmap)
                    persistedUri.isNotBlank() -> SafeImageLoader.encodeImageUriToDataUrl(this, persistedUri)
                    uri != null -> SafeImageLoader.encodeImageUriToDataUrl(this, uri.toString())
                    else -> ""
                }
                selectedPhotoUris[slotIndex] = persistedUri
                selectedPhotoDataUrls[slotIndex] = encodedDataUrl

                val remaining = pendingMediaJobs.decrementAndGet()
                runOnUiThread {
                    if (!isFinishing && !isDestroyed && remaining == 0 && publishAfterMediaReady) {
                        publishAfterMediaReady = false
                        publishProduct()
                    }
                }
            }

            Toast.makeText(this, "$currentMediaSlot ($formattedRealSize MB) dipilih.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etProductName)
        tvCategory = findViewById(R.id.tvSelectCategory)
        tvStrain = findViewById(R.id.tvSelectStrain)
        etPrice = findViewById(R.id.etProductPrice)
        tvUnit = findViewById(R.id.tvSelectUnit)
        etStock = findViewById(R.id.etProductStock)
        etDescription = findViewById(R.id.etProductDescription)

        updateStrainAndUnitVisibility(selectedCategory)
    }

    private fun updateStrainAndUnitVisibility(category: String) {
        val layoutStrain = findViewById<View>(R.id.layoutStrainContainer)
        val isFishCategory = category.equals("Ikan Kontes", ignoreCase = true) || category.equals("Indukan Breeder", ignoreCase = true)

        if (isFishCategory) {
            layoutStrain?.visibility = View.VISIBLE
            if (selectedUnit !in listOf("per pasang", "per ekor", "per paket")) {
                selectedUnit = "per pasang"
                tvUnit.text = selectedUnit
            }
        } else {
            layoutStrain?.visibility = View.GONE
            selectedStrain = "Semua"
            if (selectedUnit in listOf("per pasang", "per ekor")) {
                selectedUnit = "per pcs"
                tvUnit.text = selectedUnit
            }
        }
    }

    private fun showCustomStrainDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etCustom = EditText(this).apply {
            hint = "contoh: Super Red Albino High Dorsal"
            isSingleLine = true
        }

        layout.addView(etCustom)

        AlertDialog.Builder(this)
            .setTitle("Ketik Galur / Strain Baru")
            .setView(layout)
            .setPositiveButton("Gunakan Strain Ini") { _, _ ->
                val typedStrain = etCustom.text.toString().trim()
                if (typedStrain.isNotEmpty()) {
                    selectedStrain = typedStrain
                    tvStrain.text = selectedStrain
                    Toast.makeText(this, "Galur '$selectedStrain' berhasil digunakan!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupListeners() {
        setupCurrencyTextWatchers()
        setupNameValidationWatcher()

        val cardVideo = findViewById<View>(R.id.cardUploadVideo)
        val cardPhoto1 = findViewById<View>(R.id.cardUploadPhoto1)
        val cardPhoto2 = findViewById<View>(R.id.cardUploadPhoto2)
        val cardPhoto3 = findViewById<View>(R.id.cardUploadPhoto3)

        cardVideo?.setOnClickListener { showMediaSourceDialog(isVideo = true, slotName = "1 Video Pendek") }
        cardPhoto1?.setOnClickListener { showMediaSourceDialog(isVideo = false, slotName = "Foto Utama 1") }
        cardPhoto2?.setOnClickListener { showMediaSourceDialog(isVideo = false, slotName = "Foto 2") }
        cardPhoto3?.setOnClickListener { showMediaSourceDialog(isVideo = false, slotName = "Foto 3") }

        val categoryOptions = arrayOf("Ikan Kontes", "Pakan Ikan", "Obat & Vitamin", "Sol & Akrilik", "Indukan Breeder", "Aksesoris", "Tanaman Air", "Filter & Pompa")
        tvCategory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Kategori")
                .setItems(categoryOptions) { _, which ->
                    selectedCategory = categoryOptions[which]
                    tvCategory.text = selectedCategory
                    updateStrainAndUnitVisibility(selectedCategory)
                }
                .show()
        }

        val strainOptions = arrayOf("Mosaic", "Albino", "Tuxedo", "Dragon", "Moscow", "Grass", "Koi", "Snakeskin", "Lainnya (Ketik Sendiri)...")
        tvStrain.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Galur Varian")
                .setItems(strainOptions) { _, which ->
                    if (which == strainOptions.lastIndex) {
                        showCustomStrainDialog()
                    } else {
                        selectedStrain = strainOptions[which]
                        tvStrain.text = selectedStrain
                    }
                }
                .show()
        }

        tvUnit.setOnClickListener {
            val isFishCategory = selectedCategory.equals("Ikan Kontes", ignoreCase = true) || selectedCategory.equals("Indukan Breeder", ignoreCase = true)

            val unitOptions = if (isFishCategory) {
                arrayOf("per pasang", "per ekor", "per paket")
            } else {
                arrayOf("per pcs", "per botol", "per bungkus", "per paket", "per set")
            }

            AlertDialog.Builder(this)
                .setTitle("Pilih Satuan Jual")
                .setItems(unitOptions) { _, which ->
                    selectedUnit = unitOptions[which]
                    tvUnit.text = selectedUnit
                }
                .show()
        }

        val btnMinStock = findViewById<View>(R.id.btnMinStock)
        val btnPlusStock = findViewById<View>(R.id.btnPlusStock)

        btnMinStock?.setOnClickListener {
            val currentVal = etStock.text.toString().trim().toIntOrNull() ?: 0
            if (currentVal > 1) {
                etStock.setText((currentVal - 1).toString())
            }
        }

        btnPlusStock?.setOnClickListener {
            val currentVal = etStock.text.toString().trim().toIntOrNull() ?: 0
            etStock.setText((currentVal + 1).toString())
        }

        val tvDescCharCount = findViewById<TextView>(R.id.tvDescCharCount)
        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvDescCharCount?.text = "$len/1000"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val btnPublishBottom = findViewById<TextView>(R.id.btnPublishProductBottom)
        btnPublishBottom?.setOnClickListener {
            publishProduct()
        }
    }

    private fun setupNameValidationWatcher() {
        val ivValidationCheck = findViewById<ImageView>(R.id.ivNameValidationCheck)

        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val inputName = s?.toString()?.trim() ?: ""
                val isValidName = inputName.length >= 5 && isSuitableProductName(inputName)

                if (isValidName) {
                    if (ivValidationCheck?.visibility != View.VISIBLE) {
                        ivValidationCheck?.visibility = View.VISIBLE
                        ivValidationCheck?.alpha = 0f
                        ivValidationCheck?.animate()?.alpha(1f)?.setDuration(200)?.start()
                    }
                } else {
                    ivValidationCheck?.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun isSuitableProductName(name: String): Boolean {
        return name.any { it.isLetter() }
    }

    private fun setupCurrencyTextWatchers() {
        etPrice.addTextChangedListener(object : TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    etPrice.removeTextChangedListener(this)

                    val cleanString = s.toString().replace(".", "").replace(",", "").trim()
                    if (cleanString.isNotEmpty()) {
                        val parsed = cleanString.toLongOrNull() ?: 0L
                        val formatted = java.text.NumberFormat.getInstance(java.util.Locale("in", "ID")).format(parsed)
                        current = formatted
                        etPrice.setText(formatted)
                        etPrice.setSelection(formatted.length)
                    } else {
                        current = ""
                        etPrice.setText("")
                    }

                    etPrice.addTextChangedListener(this)
                }
            }
        })
    }

    private fun publishProduct() {
        val titleText = etTitle.text.toString().trim()
        val priceText = etPrice.text.toString().trim()
        val stockText = etStock.text.toString().trim()

        if (titleText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan nama varian produk", Toast.LENGTH_SHORT).show()
            return
        }

        if (priceText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan harga jual", Toast.LENGTH_SHORT).show()
            return
        }

        if (stockText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan jumlah stok tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Silakan masuk ke akun Anda terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        if (pendingMediaJobs.get() > 0) {
            publishAfterMediaReady = true
            findViewById<TextView>(R.id.btnPublishProductBottom)?.apply {
                isEnabled = false
                text = "Memproses Foto..."
            }
            Toast.makeText(this, "Menunggu foto selesai diproses...", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = currentUser.uid
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val profile = realSellerProfile
        val currentUserName = profile?.farmName?.takeIf { it.isNotBlank() }
            ?: prefs.getString("farm_name", null)?.takeIf { it.isNotBlank() }
            ?: profile?.name?.takeIf { it.isNotBlank() }
            ?: prefs.getString("name", null)?.takeIf { it.isNotBlank() }
            ?: currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")
            ?: "Toko Saya"
        val sellerLocation = profile?.location?.takeIf { it.isNotBlank() }
            ?: prefs.getString("address_sub", null)?.takeIf { it.isNotBlank() }
            ?: ""
        val sellerAvatar = profile?.avatarUri?.takeIf { it.isNotBlank() }
            ?: prefs.getString("avatar_uri", null)
            ?: ""

        val priceAmount = priceText.replace(".", "").replace(",", "").toLongOrNull() ?: 0L
        if (priceAmount <= 0L) {
            Toast.makeText(this, "Masukkan harga jual yang valid.", Toast.LENGTH_SHORT).show()
            return
        }
        val stockAmount = stockText.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val formattedPrice = "Rp ${java.text.NumberFormat.getInstance(java.util.Locale("in", "ID")).format(priceAmount)}"
        val descriptionText = etDescription.text.toString().trim()

        val filledIndices = (0..2).filter { !selectedPhotoUris[it].isNullOrBlank() || !selectedPhotoDataUrls[it].isNullOrBlank() }
        val validPhotoUris = filledIndices.map { selectedPhotoUris[it].orEmpty() }
        val validPhotoDataUrls = filledIndices.map { selectedPhotoDataUrls[it].orEmpty() }

        val newProduct = FirestoreRepository.ProductModel(
            id = "",
            sellerId = currentUserId,
            title = titleText,
            seller = currentUserName,
            price = formattedPrice,
            unit = selectedUnit,
            sold = "0",
            stock = stockAmount,
            soldCount = 0,
            imageRes = R.drawable.bg_image_grid,
            category = selectedCategory,
            strain = selectedStrain,
            grade = "Grade A",
            description = descriptionText,
            location = sellerLocation,
            sellerAvatarUri = sellerAvatar,
            photoUris = validPhotoUris,
            photoDataUrls = validPhotoDataUrls,
            videoUri = selectedVideoUri,
            priceNumeric = priceAmount,
            createdAt = System.currentTimeMillis()
        )

        val btnPublishBottom = findViewById<TextView>(R.id.btnPublishProductBottom)
        btnPublishBottom?.isEnabled = false
        btnPublishBottom?.text = "Menyimpan Produk..."

        FirestoreRepository.createProduct(newProduct) { success ->
            if (isFinishing || isDestroyed) return@createProduct
            btnPublishBottom?.isEnabled = true
            btnPublishBottom?.text = "Tambahkan Varian Sekarang"

            if (success) {
                Toast.makeText(this, "Varian '$titleText' berhasil ditambahkan ke etalase.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menyimpan varian. Periksa koneksi Anda dan coba lagi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
