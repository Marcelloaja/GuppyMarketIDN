package com.stiki.marcelloilham.guppymarketidn.menuAuction

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

class CreateAuctionActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var tvStrain: TextView
    private lateinit var tvGender: TextView
    private lateinit var etFishAge: EditText
    private lateinit var etFishQuantity: EditText
    private lateinit var etFishCondition: EditText
    private lateinit var etQuarantineDays: EditText

    private lateinit var etOpenBid: EditText
    private lateinit var etBidIncrement: EditText
    private lateinit var etBuyout: EditText
    private lateinit var etDescription: EditText

    private lateinit var chip12h: TextView
    private lateinit var chip24h: TextView
    private lateinit var chip48h: TextView

    private lateinit var ivVideoPreview: ImageView
    private lateinit var layoutVideoPlaceholder: View
    private lateinit var photoPreviews: List<ImageView>
    private lateinit var photoPlaceholders: List<View>

    private var selectedStrain: String = "Mosaic"
    private var selectedGender: String = "1 Pasang (Pair)"
    private var selectedDurationHours: Int = 24

    private var currentPhotoSlotIndex: Int = 0
    private var isSelectingVideo: Boolean = false

    private var selectedVideoUri: String = ""
    private var selectedVideoThumbDataUrl: String = ""
    private val selectedPhotoUris = arrayOfNulls<String>(3)
    private val selectedPhotoDataUrls = arrayOfNulls<String>(3)
    private var pendingCameraFilePath: String = ""

    private val mediaExecutor = Executors.newSingleThreadExecutor()
    private val pendingMediaJobs = java.util.concurrent.atomic.AtomicInteger(0)
    private var publishAfterMediaReady: Boolean = false

    private var realSellerProfile: FirestoreRepository.UserProfileModel? = null

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            handleSelectedMedia(uri, null)
        }
    }

    private val takeCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = result.data?.data
            val pendingFile = pendingCameraFilePath.takeIf { it.isNotBlank() }?.let { File(it) }
            val fallbackUri = if (pendingFile != null && pendingFile.exists() && pendingFile.length() > 0L) {
                Uri.fromFile(pendingFile)
            } else {
                null
            }
            @Suppress("DEPRECATION")
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra("data", Bitmap::class.java)
            } else {
                result.data?.getParcelableExtra("data")
            }
            handleSelectedMedia(resultUri ?: fallbackUri, bitmap)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("state_is_video", isSelectingVideo)
        outState.putInt("state_photo_slot", currentPhotoSlotIndex)
        outState.putString("state_video_uri", selectedVideoUri)
        outState.putString("state_photo_0", selectedPhotoUris[0])
        outState.putString("state_photo_1", selectedPhotoUris[1])
        outState.putString("state_photo_2", selectedPhotoUris[2])
        outState.putString("state_pending_cam_path", pendingCameraFilePath)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaExecutor.shutdownNow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_auction)

        val topBar = findViewById<View>(R.id.topBarCreateAuction)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackCreateAuction)
        btnBack.setOnClickListener { finish() }

        initViews()
        if (savedInstanceState != null) {
            isSelectingVideo = savedInstanceState.getBoolean("state_is_video", false)
            currentPhotoSlotIndex = savedInstanceState.getInt("state_photo_slot", 0)
            selectedVideoUri = savedInstanceState.getString("state_video_uri") ?: ""
            selectedPhotoUris[0] = savedInstanceState.getString("state_photo_0")
            selectedPhotoUris[1] = savedInstanceState.getString("state_photo_1")
            selectedPhotoUris[2] = savedInstanceState.getString("state_photo_2")
            pendingCameraFilePath = savedInstanceState.getString("state_pending_cam_path") ?: ""
            restoreSavedMediaPreviews()
        }
        setupListeners()
        loadRealSellerProfile()
    }

    private fun loadRealSellerProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirestoreRepository.fetchUserProfileById(currentUser.uid) { profile ->
            if (!isFinishing && !isDestroyed) {
                realSellerProfile = profile
            }
        }
    }

    private fun createCameraOutputUri(isVideo: Boolean): Uri? {
        return try {
            val ext = if (isVideo) "mp4" else "jpg"
            val file = File(cacheDir, "cam_${System.currentTimeMillis()}.$ext")
            pendingCameraFilePath = file.absolutePath
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        } catch (_: Throwable) {
            pendingCameraFilePath = ""
            null
        }
    }

    private fun showMediaSourceDialog(isVideo: Boolean, slotIndex: Int) {
        isSelectingVideo = isVideo
        currentPhotoSlotIndex = slotIndex
        val slotLabel = if (isVideo) "1 Video Pendek" else "Foto ${slotIndex + 1}"

        val options = arrayOf("Buka Kamera", "Pilih dari Galeri")
        AlertDialog.Builder(this)
            .setTitle("Upload $slotLabel")
            .setItems(options) { _, which ->
                if (which == 0) {
                    val outputUri = createCameraOutputUri(isVideo)
                    val intent = Intent(if (isVideo) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        if (outputUri != null) {
                            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                    takeCameraLauncher.launch(intent)
                } else {
                    pickMediaLauncher.launch(if (isVideo) "video/*" else "image/*")
                }
            }
            .show()
    }

    private fun restoreSavedMediaPreviews() {
        if (selectedVideoUri.isNotBlank() || selectedPhotoUris.any { !it.isNullOrBlank() }) {
            pendingMediaJobs.incrementAndGet()
            mediaExecutor.execute {
                val vidBitmap = if (selectedVideoUri.isNotBlank()) {
                    SafeImageLoader.decodeVideoThumbnailSafely(this, selectedVideoUri, 512)
                } else null
                val vidDataUrl = if (vidBitmap != null) SafeImageLoader.encodeBitmapToDataUrl(vidBitmap) else ""
                val photoBitmaps = selectedPhotoUris.map { path ->
                    if (!path.isNullOrBlank()) SafeImageLoader.decodeBitmapSafely(this, path, 512) else null
                }
                val photoDataList = photoBitmaps.map { bmp ->
                    if (bmp != null) SafeImageLoader.encodeBitmapToDataUrl(bmp) else null
                }
                runOnUiThread {
                    pendingMediaJobs.decrementAndGet()
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (selectedVideoUri.isNotBlank()) {
                        if (vidDataUrl.isNotBlank()) selectedVideoThumbDataUrl = vidDataUrl
                        applyVideoPreviewState(vidBitmap)
                    }
                    photoBitmaps.forEachIndexed { idx, bmp ->
                        if (bmp != null) {
                            selectedPhotoDataUrls[idx] = photoDataList[idx]
                            applyPhotoPreviewState(idx, bmp)
                        }
                    }
                    if (publishAfterMediaReady && pendingMediaJobs.get() == 0) {
                        publishAfterMediaReady = false
                        publishAuction()
                    }
                }
            }
        }
    }

    private fun handleSelectedMedia(uri: Uri?, cameraBitmap: Bitmap?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "user"
        val ts = System.currentTimeMillis()
        val selectVideo = isSelectingVideo
        val slot = currentPhotoSlotIndex.coerceIn(0, 2)

        if (selectVideo) {
            if (uri == null) {
                Toast.makeText(this, "Gagal membaca video yang dipilih.", Toast.LENGTH_SHORT).show()
                return
            }
            val fileName = "auction_vid_${uid}_$ts.mp4"
            Toast.makeText(this, "Memproses preview video...", Toast.LENGTH_SHORT).show()
            pendingMediaJobs.incrementAndGet()
            mediaExecutor.execute {
                val savedUri = SafeImageLoader.copyUriToInternalStorage(this, uri, fileName)
                val targetUriString = savedUri?.toString() ?: uri.toString()
                val thumbBitmap = SafeImageLoader.decodeVideoThumbnailSafely(this, targetUriString, 512)
                    ?: if (savedUri != null) SafeImageLoader.decodeVideoThumbnailSafely(this, uri.toString(), 512) else null
                val thumbDataUrl = if (thumbBitmap != null) SafeImageLoader.encodeBitmapToDataUrl(thumbBitmap) else ""

                runOnUiThread {
                    pendingMediaJobs.decrementAndGet()
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (savedUri != null) {
                        selectedVideoUri = savedUri.toString()
                        selectedVideoThumbDataUrl = thumbDataUrl
                        applyVideoPreviewState(thumbBitmap)
                        Toast.makeText(this, "Video lelang berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal menyimpan video ke penyimpanan lokal.", Toast.LENGTH_SHORT).show()
                    }
                    if (publishAfterMediaReady && pendingMediaJobs.get() == 0) {
                        publishAfterMediaReady = false
                        publishAuction()
                    }
                }
            }
        } else {
            val fileName = "auction_img_${uid}_${slot}_$ts.jpg"
            pendingMediaJobs.incrementAndGet()
            mediaExecutor.execute {
                val savedUri = when {
                    uri != null -> SafeImageLoader.copyUriToInternalStorage(this, uri, fileName)
                    cameraBitmap != null -> SafeImageLoader.saveBitmapToInternalStorage(this, cameraBitmap, fileName)
                    else -> null
                }
                val previewBitmap = when {
                    savedUri != null -> SafeImageLoader.decodeBitmapSafely(this, savedUri.toString(), 512)
                        ?: uri?.let { SafeImageLoader.decodeBitmapSafely(this, it.toString(), 512) }
                        ?: cameraBitmap
                    cameraBitmap != null -> cameraBitmap
                    else -> null
                }
                val photoDataUrl = if (previewBitmap != null) SafeImageLoader.encodeBitmapToDataUrl(previewBitmap) else ""

                runOnUiThread {
                    pendingMediaJobs.decrementAndGet()
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    if (savedUri != null && previewBitmap != null) {
                        selectedPhotoUris[slot] = savedUri.toString()
                        selectedPhotoDataUrls[slot] = photoDataUrl
                        applyPhotoPreviewState(slot, previewBitmap)
                        Toast.makeText(this, "Foto ${slot + 1} berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
                    } else if (savedUri != null) {
                        selectedPhotoUris[slot] = savedUri.toString()
                        selectedPhotoDataUrls[slot] = photoDataUrl
                        Toast.makeText(this, "Foto tersimpan, namun preview gagal dimuat.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Gagal menyimpan foto.", Toast.LENGTH_SHORT).show()
                    }
                    if (publishAfterMediaReady && pendingMediaJobs.get() == 0) {
                        publishAfterMediaReady = false
                        publishAuction()
                    }
                }
            }
        }
    }

    private fun applyVideoPreviewState(thumbBitmap: Bitmap?) {
        if (thumbBitmap != null) {
            ivVideoPreview.setImageBitmap(thumbBitmap)
            ivVideoPreview.visibility = View.VISIBLE
            layoutVideoPlaceholder.visibility = View.GONE
            ivVideoPreview.requestLayout()
            ivVideoPreview.invalidate()
        } else {
            ivVideoPreview.visibility = View.GONE
            layoutVideoPlaceholder.visibility = View.VISIBLE
            findFirstTextView(layoutVideoPlaceholder)?.text = "Video Siap ✓"
        }
    }

    private fun applyPhotoPreviewState(slot: Int, bitmap: Bitmap) {
        val iv = photoPreviews[slot]
        val placeholder = photoPlaceholders[slot]
        iv.setImageBitmap(bitmap)
        iv.visibility = View.VISIBLE
        placeholder.visibility = View.GONE
        iv.requestLayout()
        iv.invalidate()
    }

    private fun findFirstTextView(view: View): TextView? {
        if (view is TextView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findFirstTextView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }

    private fun initViews() {
        etTitle = findViewById(R.id.etAuctionTitle)
        tvStrain = findViewById(R.id.tvSelectStrain)
        tvGender = findViewById(R.id.tvSelectGender)
        etFishAge = findViewById(R.id.etFishAge)
        etFishQuantity = findViewById(R.id.etFishQuantity)
        etFishCondition = findViewById(R.id.etFishCondition)
        etQuarantineDays = findViewById(R.id.etQuarantineDays)

        etOpenBid = findViewById(R.id.etOpenBidPrice)
        etBidIncrement = findViewById(R.id.etBidIncrementPrice)
        etBuyout = findViewById(R.id.etBuyoutPrice)
        etDescription = findViewById(R.id.etAuctionDescription)

        chip12h = findViewById(R.id.chipDuration12)
        chip24h = findViewById(R.id.chipDuration24)
        chip48h = findViewById(R.id.chipDuration48)

        ivVideoPreview = findViewById(R.id.ivVideoPreview)
        layoutVideoPlaceholder = findViewById(R.id.layoutVideoPlaceholder)
        photoPreviews = listOf(
            findViewById(R.id.ivPhotoPreview1),
            findViewById(R.id.ivPhotoPreview2),
            findViewById(R.id.ivPhotoPreview3)
        )
        photoPlaceholders = listOf(
            findViewById(R.id.layoutPhotoPlaceholder1),
            findViewById(R.id.layoutPhotoPlaceholder2),
            findViewById(R.id.layoutPhotoPlaceholder3)
        )
    }

    private fun setupListeners() {
        setupCurrencyTextWatchers()

        findViewById<View>(R.id.cardUploadVideo)?.setOnClickListener { showMediaSourceDialog(isVideo = true, slotIndex = 0) }
        findViewById<View>(R.id.cardUploadPhoto1)?.setOnClickListener { showMediaSourceDialog(isVideo = false, slotIndex = 0) }
        findViewById<View>(R.id.cardUploadPhoto2)?.setOnClickListener { showMediaSourceDialog(isVideo = false, slotIndex = 1) }
        findViewById<View>(R.id.cardUploadPhoto3)?.setOnClickListener { showMediaSourceDialog(isVideo = false, slotIndex = 2) }

        val strainOptions = arrayOf("Mosaic", "Albino", "Tuxedo", "Dragon", "Moscow", "Grass", "Koi", "Snakeskin", "Lainnya (Ketik Sendiri)...")
        tvStrain.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Galur Varian")
                .setItems(strainOptions) { _, which ->
                    if (which == strainOptions.lastIndex) {
                        showCustomStrainDialog()
                    } else {
                        selectedStrain = strainOptions[which]
                        tvStrain.text = "Galur: $selectedStrain"
                    }
                }
                .show()
        }

        val genderOptions = arrayOf("1 Pasang (Pair)", "Jantan Single", "Betina Single", "Koloni (3 Ekor)")
        tvGender.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pilih Jenis Kelamin")
                .setItems(genderOptions) { _, which ->
                    selectedGender = genderOptions[which]
                    tvGender.text = selectedGender
                }
                .show()
        }

        chip12h.setOnClickListener { selectDuration(chip12h, 12) }
        chip24h.setOnClickListener { selectDuration(chip24h, 24) }
        chip48h.setOnClickListener { selectDuration(chip48h, 48) }

        val tvDescCharCount = findViewById<TextView>(R.id.tvDescCharCount)
        etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvDescCharCount?.text = "$len/1000"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val btnPublishBottom = findViewById<TextView>(R.id.btnPublishBottom)
        btnPublishBottom?.setOnClickListener {
            publishAuction()
        }
    }

    private fun setupCurrencyTextWatchers() {
        val addRupiahWatcher = { editText: EditText ->
            editText.addTextChangedListener(object : TextWatcher {
                private var current = ""
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() != current) {
                        editText.removeTextChangedListener(this)

                        val cleanString = s.toString().replace(".", "").replace(",", "").trim()
                        if (cleanString.isNotEmpty()) {
                            val parsed = cleanString.toLongOrNull() ?: 0L
                            val formatted = NumberFormat.getInstance(Locale("in", "ID")).format(parsed)
                            current = formatted
                            editText.setText(formatted)
                            editText.setSelection(formatted.length)
                        } else {
                            current = ""
                            editText.setText("")
                        }

                        editText.addTextChangedListener(this)
                    }
                }
            })
        }

        addRupiahWatcher(etOpenBid)
        addRupiahWatcher(etBidIncrement)
        addRupiahWatcher(etBuyout)
    }

    private fun showCustomStrainDialog() {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
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
                    tvStrain.text = "Galur: $selectedStrain"
                    Toast.makeText(this, "Galur '$selectedStrain' berhasil digunakan!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun selectDuration(selected: TextView, hours: Int) {
        selectedDurationHours = hours
        val chips = listOf(chip12h, chip24h, chip48h)

        chips.forEach { chip ->
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.bg_segmented_pill_selected)
                chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_primary))
                chip.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_bold)
            } else {
                chip.background = null
                chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_gray))
                chip.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.inter_medium)
            }
        }
    }

    private fun publishAuction() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu untuk membuat lelang.", Toast.LENGTH_SHORT).show()
            return
        }

        val titleText = etTitle.text.toString().trim()
        val obText = etOpenBid.text.toString().trim()

        if (titleText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan judul lelang", Toast.LENGTH_SHORT).show()
            return
        }

        if (obText.isEmpty()) {
            Toast.makeText(this, "Harap masukkan harga Open Bid (OB)", Toast.LENGTH_SHORT).show()
            return
        }

        val ageText = etFishAge.text.toString().trim().ifEmpty { "3.5 Bulan" }
        val qtyText = etFishQuantity.text.toString().trim().ifEmpty { "1 Pasang" }
        val condText = etFishCondition.text.toString().trim().ifEmpty { "Sangat Lincah, Bebas Jamur" }
        val quaranText = etQuarantineDays.text.toString().trim().ifEmpty { "14 hari di farm" }
        val descText = etDescription.text.toString().trim().ifEmpty {
            "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes. Memiliki corak ekor lincah, bebas penyakit, dan sudah melewati masa karantina."
        }

        val obAmount = obText.replace(".", "").replace(",", "").toLongOrNull() ?: 200000L
        val incrementAmount = etBidIncrement.text.toString().replace(".", "").replace(",", "").toLongOrNull()?.coerceAtLeast(1000L) ?: 25000L
        val buyoutAmount = etBuyout.text.toString().replace(".", "").replace(",", "").toLongOrNull() ?: 0L
        val formattedPrice = "Rp ${NumberFormat.getInstance(Locale("in", "ID")).format(obAmount)}"

        val endTimeMillis = System.currentTimeMillis() + (selectedDurationHours * 3600000L)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val profile = realSellerProfile
        val currentUserName = profile?.farmName?.takeIf { it.isNotBlank() }
            ?: prefs.getString("farm_name", null)?.takeIf { it.isNotBlank() }
            ?: profile?.name?.takeIf { it.isNotBlank() }
            ?: prefs.getString("name", null)?.takeIf { it.isNotBlank() }
            ?: currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")
            ?: "Toko Saya"
        val locationSub = profile?.location?.takeIf { it.isNotBlank() }
            ?: prefs.getString("address_sub", null)?.takeIf { it.isNotBlank() }
            ?: "Indonesia"
        val sellerAvatar = profile?.avatarUri?.takeIf { it.isNotBlank() }
            ?: prefs.getString("avatar_uri", null)
            ?: ""

        if (pendingMediaJobs.get() > 0) {
            publishAfterMediaReady = true
            findViewById<TextView>(R.id.btnPublishBottom)?.apply {
                isEnabled = false
                text = "Memproses Media..."
            }
            Toast.makeText(this, "Menunggu video/foto selesai diproses...", Toast.LENGTH_SHORT).show()
            return
        }

        val filledIndices = (0..2).filter { !selectedPhotoUris[it].isNullOrBlank() }
        val validPhotoUris = filledIndices.map { selectedPhotoUris[it]!! }
        val validPhotoDataUrls = filledIndices.map { selectedPhotoDataUrls[it].orEmpty() }

        val newAuction = FirestoreRepository.AuctionModel(
            id = "auction_${System.currentTimeMillis()}",
            sellerId = currentUser.uid,
            title = titleText,
            seller = currentUserName,
            location = locationSub,
            currentBid = formattedPrice,
            currentBidNumeric = obAmount,
            bidsCount = 0,
            endTimeMillis = endTimeMillis,
            imageRes = R.drawable.bg_image_grid,
            grade = "Grade A+",
            description = descText,
            strain = selectedStrain,
            gender = selectedGender,
            age = ageText,
            quantity = qtyText,
            condition = condText,
            quarantine = quaranText,
            bidIncrementNumeric = incrementAmount,
            buyoutPriceNumeric = buyoutAmount,
            videoUri = selectedVideoUri,
            videoThumbDataUrl = selectedVideoThumbDataUrl,
            photoUris = validPhotoUris,
            photoDataUrls = validPhotoDataUrls,
            sellerAvatarUri = sellerAvatar
        )

        val btnPublishBottom = findViewById<TextView>(R.id.btnPublishBottom)
        btnPublishBottom?.isEnabled = false
        btnPublishBottom?.text = if (selectedVideoUri.isNotBlank()) "Mengunggah Media & Lelang..." else "Menerbitkan Lelang..."

        FirestoreRepository.createAuction(newAuction) { success ->
            btnPublishBottom?.isEnabled = true
            btnPublishBottom?.text = "Terbitkan Lelang Sekarang"

            if (success) {
                Toast.makeText(this, "Sesi Lelang '$titleText' berhasil diterbitkan!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Gagal menerbitkan lelang, periksa koneksi Anda.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
