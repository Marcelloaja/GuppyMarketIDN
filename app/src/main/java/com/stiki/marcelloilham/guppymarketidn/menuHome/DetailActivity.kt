package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.text.NumberFormat
import java.util.Locale

class DetailActivity : AppCompatActivity() {

    private lateinit var btnBackDetail: ImageView
    private lateinit var imageView: ImageView
    private lateinit var textView: TextView
    private lateinit var btnMin: ImageView
    private lateinit var btnPlus: ImageView
    private lateinit var editTextNumber: EditText

    private var tvDetailGradeBadge: TextView? = null
    private var btnFavoriteDetail: ImageView? = null
    private var btnShareDetail: ImageView? = null
    private var tvDetailPhotoCount: TextView? = null
    private var layoutDetailThumbnails: View? = null
    private lateinit var thumbViews: List<ShapeableImageView?>

    private var tvDetailTitle: TextView? = null
    private var tvDetailSpecSubtitle: TextView? = null
    private var tvDetailSellerInitials: TextView? = null
    private var ivDetailSellerAvatar: ShapeableImageView? = null
    private var tvDetailSellerName: TextView? = null
    private var tvDetailSellerRating: TextView? = null
    private var tvDetailSellerReviewCount: TextView? = null
    private var tvDetailSellerLocation: TextView? = null
    private var btnDetailVisitStore: TextView? = null

    private var tvDetailDescription: TextView? = null
    private var btnDetailReadMore: TextView? = null
    private var tvDetailSpec1Value: TextView? = null
    private var tvDetailSpec2Value: TextView? = null
    private var tvDetailSpec3Value: TextView? = null
    private var tvDetailSpecGrade: TextView? = null
    private var tvDetailSpecCondition: TextView? = null

    private var tvDetailVariantTitle: TextView? = null
    private var tvDetailVariantPriceUnit: TextView? = null
    private var tvDetailVariantStock: TextView? = null
    private var tvDetailVariantSubNote: TextView? = null

    private var tvDetailQtyUnitLabel: TextView? = null
    private var tvDetailQtyEquivalentBadge: TextView? = null
    private var tvDetailUnitMultiplier: TextView? = null
    private var tvDetailUnitMultiplierTotal: TextView? = null
    private var tvDetailSubtotalCard: TextView? = null
    private var tvDetailBottomTotal: TextView? = null
    private var tvDetailBottomSubtext: TextView? = null
    private var btnChatSellerDetail: View? = null
    private var btnAddToCartDetail: View? = null
    private var tvAddToCartDetailLabel: TextView? = null
    private var btnBuyNowDetail: TextView? = null

    private var productId: String = ""
    private var sellerId: String = ""
    private var currentProduct: FirestoreRepository.ProductModel? = null
    private var sellerProfile: FirestoreRepository.UserProfileModel? = null
    private var buyerAddress: FirestoreRepository.AddressModel? = null
    private var fallbackImageRes: Int = R.drawable.bg_image_grid

    private var unitPrice: Long = 0L
    private var availableStock: Int = 1
    private var currentQuantity: Int = 1
    private var unitLabel: String = "per pasang"
    private var isFavorite: Boolean = false
    private var isDescriptionExpanded: Boolean = false
    private var isProgrammaticQtyUpdate: Boolean = false
    private var loadedSellerId: String = ""
    private var activePhotoIndex: Int = 0
    private var galleryEntries: List<Pair<String, String>> = emptyList()

    private var productListener: ListenerRegistration? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_limited)

        val topBar = findViewById<View>(R.id.topBarDetail)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        initViews()
        readInitialIntent()
        setupListeners()
        loadBuyerAddressForDistance()
    }

    override fun onStart() {
        super.onStart()
        attachProductRealtimeListener()
    }

    override fun onStop() {
        super.onStop()
        productListener?.remove()
        productListener = null
    }

    private fun initViews() {
        btnBackDetail = findViewById(R.id.btnBackDetails)
        imageView = findViewById(R.id.detail_image)
        textView = findViewById(R.id.detail_text)
        btnMin = findViewById(R.id.btnMin)
        btnPlus = findViewById(R.id.btnPlus)
        editTextNumber = findViewById(R.id.editTextNumber)

        tvDetailGradeBadge = findViewById(R.id.tvDetailGradeBadge)
        btnFavoriteDetail = findViewById(R.id.btnFavoriteDetail)
        btnShareDetail = findViewById(R.id.btnShareDetail)
        tvDetailPhotoCount = findViewById(R.id.tvDetailPhotoCount)
        layoutDetailThumbnails = findViewById(R.id.layoutDetailThumbnails)
        thumbViews = listOf(
            findViewById(R.id.thumbDetail1),
            findViewById(R.id.thumbDetail2),
            findViewById(R.id.thumbDetail3)
        )

        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailSpecSubtitle = findViewById(R.id.tvDetailSpecSubtitle)
        tvDetailSellerInitials = findViewById(R.id.tvDetailSellerInitials)
        ivDetailSellerAvatar = findViewById(R.id.ivDetailSellerAvatar)
        tvDetailSellerName = findViewById(R.id.tvDetailSellerName)
        tvDetailSellerRating = findViewById(R.id.tvDetailSellerRating)
        tvDetailSellerReviewCount = findViewById(R.id.tvDetailSellerReviewCount)
        tvDetailSellerLocation = findViewById(R.id.tvDetailSellerLocation)
        btnDetailVisitStore = findViewById(R.id.btnDetailVisitStore)

        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        btnDetailReadMore = findViewById(R.id.btnDetailReadMore)
        tvDetailSpec1Value = findViewById(R.id.tvDetailSpec1Value)
        tvDetailSpec2Value = findViewById(R.id.tvDetailSpec2Value)
        tvDetailSpec3Value = findViewById(R.id.tvDetailSpec3Value)
        tvDetailSpecGrade = findViewById(R.id.tvDetailSpecGrade)
        tvDetailSpecCondition = findViewById(R.id.tvDetailSpecCondition)

        tvDetailVariantTitle = findViewById(R.id.tvDetailVariantTitle)
        tvDetailVariantPriceUnit = findViewById(R.id.tvDetailVariantPriceUnit)
        tvDetailVariantStock = findViewById(R.id.tvDetailVariantStock)
        tvDetailVariantSubNote = findViewById(R.id.tvDetailVariantSubNote)

        tvDetailQtyUnitLabel = findViewById(R.id.tvDetailQtyUnitLabel)
        tvDetailQtyEquivalentBadge = findViewById(R.id.tvDetailQtyEquivalentBadge)
        tvDetailUnitMultiplier = findViewById(R.id.tvDetailUnitMultiplier)
        tvDetailUnitMultiplierTotal = findViewById(R.id.tvDetailUnitMultiplierTotal)
        tvDetailSubtotalCard = findViewById(R.id.tvDetailSubtotalCard)
        tvDetailBottomTotal = findViewById(R.id.tvDetailBottomTotal)
        tvDetailBottomSubtext = findViewById(R.id.tvDetailBottomSubtext)
        btnChatSellerDetail = findViewById(R.id.btnChatSellerDetail)
        btnAddToCartDetail = findViewById(R.id.btnAddToCartDetail)
        tvAddToCartDetailLabel = findViewById(R.id.tvAddToCartDetailLabel)
        btnBuyNowDetail = findViewById(R.id.btnBuyNowDetail)
    }

    private fun readInitialIntent() {
        productId = intent.getStringExtra("EXTRA_PRODUCT_ID") ?: ""
        sellerId = intent.getStringExtra("EXTRA_SELLER_ID") ?: ""
        val itemName = intent.getStringExtra("ITEM_NAME")
            ?: intent.getStringExtra("item_name")
            ?: "Detail Produk"
        val itemPrice = intent.getStringExtra("item_price") ?: ""
        val itemSeller = intent.getStringExtra("item_seller") ?: ""
        val imgExtra = intent.getIntExtra("ITEM_IMAGE", 0)
            .let { if (it != 0) it else intent.getIntExtra("item_image", 0) }
        fallbackImageRes = if (imgExtra != 0) imgExtra else R.drawable.bg_image_grid

        imageView.setImageResource(fallbackImageRes)
        textView.text = itemName
        tvDetailTitle?.text = itemName
        if (itemSeller.isNotBlank()) {
            tvDetailSellerName?.text = itemSeller
            tvDetailSellerInitials?.text = computeInitials(itemSeller)
        }
        val parsed = parsePrice(itemPrice)
        if (parsed > 0L) {
            unitPrice = parsed
            updateQuantityAndTotals()
        }
    }

    private fun attachProductRealtimeListener() {
        productListener?.remove()
        if (productId.isNotBlank()) {
            productListener = FirestoreRepository.listenProductById(productId) { product ->
                if (isFinishing || isDestroyed) return@listenProductById
                if (product != null) {
                    applyProductData(product)
                } else {
                    Toast.makeText(this, "Produk sudah tidak tersedia.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val fallbackTitle = tvDetailTitle?.text?.toString()?.trim().orEmpty()
            if (fallbackTitle.isNotBlank() && fallbackTitle != "Detail Produk") {
                FirestoreRepository.findProductByTitle(fallbackTitle) { product ->
                    if (isFinishing || isDestroyed || product == null) return@findProductByTitle
                    productId = product.id
                    applyProductData(product)
                    productListener?.remove()
                    productListener = FirestoreRepository.listenProductById(product.id) { updated ->
                        if (!isFinishing && !isDestroyed && updated != null) {
                            applyProductData(updated)
                        }
                    }
                }
            }
        }
    }

    private fun applyProductData(product: FirestoreRepository.ProductModel) {
        currentProduct = product
        productId = product.id.ifBlank { productId }
        sellerId = product.sellerId.ifBlank { sellerId }
        fallbackImageRes = if (product.imageRes != 0) product.imageRes else fallbackImageRes

        unitPrice = if (product.priceNumeric > 0L) product.priceNumeric else parsePrice(product.price)
        unitLabel = product.unit.ifBlank { "per pasang" }
        availableStock = product.effectiveStock()

        if (availableStock > 0 && currentQuantity > availableStock) {
            currentQuantity = availableStock
        } else if (currentQuantity < 1 && availableStock > 0) {
            currentQuantity = 1
        }

        textView.text = product.title
        tvDetailTitle?.text = product.title
        tvDetailGradeBadge?.text = product.category.ifBlank { product.grade }

        val specParts = mutableListOf<String>()
        if (product.category.isNotBlank()) specParts.add(product.category)
        if (product.strain.isNotBlank() && !product.strain.equals("Semua", ignoreCase = true)) {
            specParts.add("Galur ${product.strain}")
        }
        specParts.add("Terjual ${product.soldCount}")
        tvDetailSpecSubtitle?.text = specParts.joinToString(" · ")

        val descText = product.description.ifBlank {
            "${product.title} kategori ${product.category} (${product.strain}). Disediakan oleh ${product.seller.ifBlank { "toko terdaftar" }}."
        }
        tvDetailDescription?.text = descText
        tvDetailDescription?.post {
            val lineCount = tvDetailDescription?.layout?.lineCount ?: 0
            btnDetailReadMore?.visibility = if (lineCount > 3 || descText.length > 140) View.VISIBLE else View.GONE
        }

        tvDetailSpec1Value?.text = product.category.ifBlank { "-" }
        tvDetailSpec2Value?.text = product.strain.ifBlank { "-" }
        tvDetailSpec3Value?.text = "$availableStock ${shortUnit(unitLabel)}"
        tvDetailSpecGrade?.text = product.grade.removePrefix("Grade ").ifBlank { "A" }
        tvDetailSpecCondition?.text = product.condition.ifBlank { "Sehat, bebas penyakit" }

        tvDetailVariantTitle?.text = shortUnit(unitLabel).replaceFirstChar { it.uppercase() }
        tvDetailVariantPriceUnit?.text = "${formatCurrency(unitPrice)} $unitLabel"
        tvDetailVariantStock?.text = if (availableStock > 0) {
            "Stok $availableStock ${shortUnit(unitLabel)}"
        } else {
            "Stok habis"
        }
        tvDetailVariantSubNote?.visibility = if (unitLabel.contains("pasang", true)) View.VISIBLE else View.GONE

        renderGallery(product)
        updateQuantityAndTotals()

        if (sellerId.isNotBlank() && sellerId != loadedSellerId) {
            loadedSellerId = sellerId
            loadSellerProfile(sellerId, product.seller, product.location, product.sellerAvatarUri)
        } else if (sellerProfile == null) {
            bindSellerFallback(product.seller, product.location, product.sellerAvatarUri)
        }
    }

    private fun renderGallery(product: FirestoreRepository.ProductModel) {
        val count = maxOf(product.photoUris.size, product.photoDataUrls.size)
        val entries = (0 until count).mapNotNull { idx ->
            val uri = product.photoUris.getOrNull(idx).orEmpty()
            val data = product.photoDataUrls.getOrNull(idx).orEmpty()
            if (uri.isNotBlank() || data.isNotBlank()) Pair(uri, data) else null
        }

        galleryEntries = entries
        if (entries.isEmpty()) {
            imageView.setImageResource(fallbackImageRes)
            tvDetailPhotoCount?.text = "1/1"
            layoutDetailThumbnails?.visibility = View.GONE
            return
        }

        if (activePhotoIndex >= entries.size) activePhotoIndex = 0
        displayGalleryPhoto(activePhotoIndex)

        if (entries.size <= 1) {
            layoutDetailThumbnails?.visibility = View.GONE
        } else {
            layoutDetailThumbnails?.visibility = View.VISIBLE
            thumbViews.forEachIndexed { idx, thumb ->
                if (thumb == null) return@forEachIndexed
                if (idx < entries.size) {
                    thumb.visibility = View.VISIBLE
                    SafeImageLoader.loadWithFallbackSafely(
                        this,
                        thumb,
                        entries[idx].first,
                        entries[idx].second,
                        fallbackImageRes,
                        180
                    )
                    thumb.setOnClickListener {
                        activePhotoIndex = idx
                        displayGalleryPhoto(idx)
                    }
                } else {
                    thumb.visibility = View.GONE
                }
            }
            highlightSelectedThumbnail(activePhotoIndex)
        }
    }

    private fun displayGalleryPhoto(index: Int) {
        if (index !in galleryEntries.indices) return
        val entry = galleryEntries[index]
        SafeImageLoader.loadWithFallbackSafely(
            this,
            imageView,
            entry.first,
            entry.second,
            fallbackImageRes,
            720
        )
        tvDetailPhotoCount?.text = "${index + 1}/${galleryEntries.size}"
        highlightSelectedThumbnail(index)
    }

    private fun highlightSelectedThumbnail(selectedIndex: Int) {
        val primary = ContextCompat.getColor(this, R.color.guppy_primary)
        val transparent = android.graphics.Color.TRANSPARENT
        val strokePx = 2f * resources.displayMetrics.density
        thumbViews.forEachIndexed { idx, thumb ->
            if (thumb == null) return@forEachIndexed
            if (idx == selectedIndex) {
                thumb.strokeColor = android.content.res.ColorStateList.valueOf(primary)
                thumb.strokeWidth = strokePx
            } else {
                thumb.strokeColor = android.content.res.ColorStateList.valueOf(transparent)
                thumb.strokeWidth = 0f
            }
        }
    }

    private fun loadBuyerAddressForDistance() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirestoreRepository.fetchUserAddresses(currentUid, preferServer = false) { addresses ->
            if (isFinishing || isDestroyed) return@fetchUserAddresses
            buyerAddress = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            refreshSellerLocationLabel()
        }
    }

    private fun loadSellerProfile(
        uid: String,
        fallbackName: String,
        fallbackLocation: String,
        fallbackAvatar: String
    ) {
        FirestoreRepository.fetchUserProfileById(uid) { profile ->
            if (isFinishing || isDestroyed) return@fetchUserProfileById
            if (profile != null) {
                sellerProfile = profile
                val storeName = profile.farmName.ifBlank { profile.name.ifBlank { fallbackName.ifBlank { "Toko Guppy" } } }
                tvDetailSellerName?.text = storeName
                tvDetailSellerInitials?.text = computeInitials(storeName)
                tvDetailSellerRating?.text = String.format(Locale.US, "%.1f", profile.rating)
                tvDetailSellerReviewCount?.text = "(${profile.totalReviewsCount} ulasan)"

                val avatarSource = profile.avatarUri.ifBlank { fallbackAvatar }
                bindSellerAvatar(avatarSource)
                refreshSellerLocationLabel()
            } else {
                bindSellerFallback(fallbackName, fallbackLocation, fallbackAvatar)
            }
        }
    }

    private fun bindSellerFallback(name: String, location: String, avatar: String) {
        val resolvedName = name.ifBlank { "Toko Guppy" }
        tvDetailSellerName?.text = resolvedName
        tvDetailSellerInitials?.text = computeInitials(resolvedName)
        tvDetailSellerRating?.text = "0.0"
        tvDetailSellerReviewCount?.text = "(0 ulasan)"
        tvDetailSellerLocation?.text = shortenAddress(location).ifBlank { "Indonesia" }
        bindSellerAvatar(avatar)
    }

    private fun bindSellerAvatar(avatarUri: String) {
        val iv = ivDetailSellerAvatar ?: return
        val initialsView = tvDetailSellerInitials
        if (avatarUri.isNotBlank()) {
            val loaded = SafeImageLoader.loadUriSafely(this, iv, avatarUri, R.drawable.ic_default_user_avatar, 200)
            iv.visibility = if (loaded) View.VISIBLE else View.GONE
            initialsView?.visibility = if (loaded) View.GONE else View.VISIBLE
        } else {
            iv.visibility = View.GONE
            initialsView?.visibility = View.VISIBLE
        }
    }

    private fun refreshSellerLocationLabel() {
        val seller = sellerProfile
        val baseLoc = seller?.location?.ifBlank { currentProduct?.location.orEmpty() }
            ?: currentProduct?.location.orEmpty()
        val displayLoc = shortenAddress(baseLoc).ifBlank { "Indonesia" }

        val buyer = buyerAddress
        if (seller != null && buyer != null &&
            seller.lat != 0.0 && seller.lng != 0.0 &&
            buyer.lat != 0.0 && buyer.lng != 0.0
        ) {
            val results = FloatArray(1)
            Location.distanceBetween(buyer.lat, buyer.lng, seller.lat, seller.lng, results)
            val distKm = results[0] / 1000.0
            val distStr = if (distKm < 1.0) "${(distKm * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", distKm).replace('.', ',')
            tvDetailSellerLocation?.text = "$displayLoc · $distStr"
        } else {
            tvDetailSellerLocation?.text = displayLoc
        }
    }

    private fun setupListeners() {
        btnBackDetail.setOnClickListener { finish() }

        btnShareDetail?.setOnClickListener {
            val title = currentProduct?.title ?: tvDetailTitle?.text?.toString().orEmpty()
            val priceText = formatCurrency(unitPrice)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "$title ($priceText $unitLabel) di GuppyMarket IDN")
            }
            startActivity(Intent.createChooser(shareIntent, "Bagikan Produk"))
        }

        btnFavoriteDetail?.setOnClickListener {
            isFavorite = !isFavorite
            val tintColor = if (isFavorite) {
                android.graphics.Color.parseColor("#E53935")
            } else {
                ContextCompat.getColor(this, R.color.white)
            }
            btnFavoriteDetail?.setColorFilter(tintColor)
            val msg = if (isFavorite) "Ditambahkan ke favorit" else "Dihapus dari favorit"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        btnDetailReadMore?.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            tvDetailDescription?.maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3
            btnDetailReadMore?.text = if (isDescriptionExpanded) "Tutup" else "Baca selengkapnya"
        }

        btnDetailVisitStore?.setOnClickListener {
            val storeName = sellerProfile?.farmName?.ifBlank { sellerProfile?.name }
                ?: tvDetailSellerName?.text?.toString()
                ?: "Toko Guppy"
            val storeLoc = sellerProfile?.location?.ifBlank { currentProduct?.location }
                ?: tvDetailSellerLocation?.text?.toString()
                ?: "Indonesia"
            val intent = Intent(this, DetailFarmActivity::class.java).apply {
                putExtra("farm_id", sellerId)
                putExtra("farm_title", storeName)
                putExtra("farm_location", storeLoc)
                putExtra("farm_rating", (sellerProfile?.rating ?: 0.0).toString())
            }
            startActivity(intent)
        }

        btnChatSellerDetail?.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (sellerId.isNotBlank() && sellerId == currentUid) {
                Toast.makeText(this, "Ini adalah produk dari toko Anda sendiri.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val storeName = sellerProfile?.farmName?.ifBlank { sellerProfile?.name }
                ?: tvDetailSellerName?.text?.toString()
                ?: "Toko Guppy"
            val storeLoc = sellerProfile?.location?.ifBlank { currentProduct?.location } ?: "Indonesia"
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_STORE_ID", sellerId)
                putExtra("EXTRA_STORE_NAME", storeName)
                putExtra("EXTRA_STORE_LOCATION", storeLoc)
            }
            startActivity(intent)
        }

        btnMin.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_click)
            btnMin.startAnimation(animation)
            if (currentQuantity > 1) {
                currentQuantity--
                updateQuantityAndTotals()
            }
        }

        btnPlus.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_click)
            btnPlus.startAnimation(animation)
            if (availableStock <= 0) {
                Toast.makeText(this, "Stok produk sedang kosong.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentQuantity < availableStock) {
                currentQuantity++
                updateQuantityAndTotals()
            } else {
                Toast.makeText(this, "Maksimal stok tersedia $availableStock.", Toast.LENGTH_SHORT).show()
            }
        }

        editTextNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticQtyUpdate) return
                val entered = s?.toString()?.toIntOrNull() ?: 1
                val maxAllowed = availableStock.coerceAtLeast(1)
                currentQuantity = entered.coerceIn(1, maxAllowed)
                if (entered != currentQuantity) {
                    isProgrammaticQtyUpdate = true
                    editTextNumber.setText(currentQuantity.toString())
                    editTextNumber.setSelection(editTextNumber.text.length)
                    isProgrammaticQtyUpdate = false
                }
                refreshTotalsOnly()
            }
        })

        btnAddToCartDetail?.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (currentUid.isBlank()) {
                Toast.makeText(this, "Silakan login terlebih dahulu untuk menambah ke keranjang.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sellerId.isNotBlank() && sellerId == currentUid) {
                Toast.makeText(this, "Anda tidak dapat memasukkan produk milik sendiri ke keranjang.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (availableStock <= 0) {
                Toast.makeText(this, "Maaf, stok produk ini sudah habis.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (unitPrice <= 0L) {
                Toast.makeText(this, "Data produk sedang dimuat, silakan coba lagi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val product = currentProduct
            val resolvedProductId = productId.ifBlank { product?.id.orEmpty() }
            if (resolvedProductId.isBlank()) {
                Toast.makeText(this, "ID produk belum tersedia.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val storeName = sellerProfile?.farmName?.ifBlank { sellerProfile?.name }
                ?: product?.seller?.ifBlank { tvDetailSellerName?.text?.toString() }
                ?: "Toko Guppy"
            val rawLoc = sellerProfile?.location?.ifBlank { product?.location.orEmpty() }
                ?: product?.location.orEmpty()
            val storeLocation = shortenAddress(rawLoc).ifBlank { "Indonesia" }

            val cartItem = FirestoreRepository.CartItemModel(
                id = resolvedProductId,
                productId = resolvedProductId,
                sellerId = sellerId.ifBlank { product?.sellerId.orEmpty() },
                sellerName = storeName,
                sellerLocation = storeLocation,
                title = product?.title ?: tvDetailTitle?.text?.toString().orEmpty(),
                unit = unitLabel,
                unitPrice = unitPrice,
                quantity = currentQuantity,
                stock = availableStock,
                imageUrl = product?.photoUris?.firstOrNull().orEmpty(),
                imageDataUrl = product?.photoDataUrls?.firstOrNull().orEmpty(),
                imageRes = fallbackImageRes,
                category = product?.category ?: "Ikan Kontes",
                strain = product?.strain ?: "Semua",
                addedAt = System.currentTimeMillis()
            )

            btnAddToCartDetail?.isEnabled = false
            tvAddToCartDetailLabel?.text = "Menyimpan..."

            FirestoreRepository.upsertCartItem(currentUid, cartItem) { success ->
                if (isFinishing || isDestroyed) return@upsertCartItem
                btnAddToCartDetail?.isEnabled = true
                tvAddToCartDetailLabel?.text = "+ Keranjang"
                if (success) {
                    val root = findViewById<View>(R.id.main)
                    if (root != null) {
                        Snackbar.make(root, "Produk ditambahkan ke keranjang.", Snackbar.LENGTH_LONG)
                            .setAction("Lihat") {
                                startActivity(Intent(this, KeranjangHome::class.java))
                            }
                            .show()
                    } else {
                        Toast.makeText(this, "Produk ditambahkan ke keranjang.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Gagal menyimpan ke keranjang. Coba lagi.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnBuyNowDetail?.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (currentUid.isBlank()) {
                Toast.makeText(this, "Silakan login terlebih dahulu untuk checkout.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sellerId.isNotBlank() && sellerId == currentUid) {
                Toast.makeText(this, "Anda tidak dapat membeli produk dari toko Anda sendiri.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (availableStock <= 0) {
                Toast.makeText(this, "Maaf, stok produk ini sudah habis.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val product = currentProduct
            val storeName = sellerProfile?.farmName?.ifBlank { sellerProfile?.name }
                ?: product?.seller?.ifBlank { tvDetailSellerName?.text?.toString() }
                ?: "Toko Guppy"
            val rawLoc = sellerProfile?.location?.ifBlank { product?.location.orEmpty() }
                ?: product?.location.orEmpty()
            val storeLocation = shortenAddress(rawLoc).ifBlank { "Indonesia" }

            val intent = Intent(this, CheckoutActivity::class.java).apply {
                putExtra("EXTRA_PRODUCT_ID", productId)
                putExtra("EXTRA_SELLER_ID", sellerId)
                putExtra("EXTRA_PRODUCT_NAME", product?.title ?: tvDetailTitle?.text?.toString().orEmpty())
                putExtra("EXTRA_UNIT_PRICE", unitPrice)
                putExtra("EXTRA_UNIT_LABEL", unitLabel)
                putExtra("EXTRA_QUANTITY", currentQuantity)
                putExtra("EXTRA_STOCK", availableStock)
                putExtra("EXTRA_CATEGORY", product?.category ?: "Ikan Kontes")
                putExtra("EXTRA_STRAIN", product?.strain ?: "")
                putExtra("EXTRA_IMAGE_URL", product?.photoUris?.firstOrNull().orEmpty())
                putExtra("EXTRA_IMAGE_DATA_URL", product?.photoDataUrls?.firstOrNull().orEmpty())
                putExtra("EXTRA_IMAGE_RES", fallbackImageRes)
                putExtra("EXTRA_SELLER_NAME", storeName)
                putExtra("EXTRA_SELLER_LOCATION", storeLocation)
                putExtra("EXTRA_SELLER_RATING", sellerProfile?.rating ?: 0.0)
                putExtra("EXTRA_SELLER_REVIEWS", sellerProfile?.totalReviewsCount ?: 0)
            }
            startActivity(intent)
        }
    }

    private fun updateQuantityAndTotals() {
        isProgrammaticQtyUpdate = true
        editTextNumber.setText(currentQuantity.toString())
        editTextNumber.setSelection(editTextNumber.text.length)
        isProgrammaticQtyUpdate = false
        refreshTotalsOnly()
    }

    private fun refreshTotalsOnly() {
        val subtotal = unitPrice * currentQuantity
        val shortU = shortUnit(unitLabel)

        tvDetailQtyUnitLabel?.text = shortU
        if (unitLabel.contains("pasang", true)) {
            tvDetailQtyEquivalentBadge?.visibility = View.VISIBLE
            tvDetailQtyEquivalentBadge?.text = "Setara ${currentQuantity * 2} ekor"
            tvDetailBottomSubtext?.text = "$currentQuantity $shortU · ${currentQuantity * 2} ekor"
        } else {
            tvDetailQtyEquivalentBadge?.visibility = View.GONE
            tvDetailBottomSubtext?.text = "$currentQuantity $shortU"
        }

        val formattedUnit = formatCurrency(unitPrice)
        val formattedSub = formatCurrency(subtotal)
        tvDetailUnitMultiplier?.text = "$formattedUnit × $currentQuantity $shortU"
        tvDetailUnitMultiplierTotal?.text = formattedSub
        tvDetailSubtotalCard?.text = formattedSub
        tvDetailBottomTotal?.text = formattedSub

        val isBuyable = availableStock > 0 && unitPrice > 0L
        btnBuyNowDetail?.isEnabled = isBuyable
        btnBuyNowDetail?.alpha = if (isBuyable) 1.0f else 0.5f
        btnBuyNowDetail?.text = if (availableStock <= 0) "Habis" else "Beli"

        btnAddToCartDetail?.isEnabled = isBuyable
        btnAddToCartDetail?.alpha = if (isBuyable) 1.0f else 0.5f
    }

    private fun shortenAddress(rawAddress: String): String {
        val cleaned = rawAddress.replace(Regex("\\b\\d{5}\\b"), "").trim()
        if (cleaned.isBlank()) return ""
        val parts = cleaned.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("Indonesia", ignoreCase = true) }
        if (parts.isEmpty()) return rawAddress.trim()
        val provinceNames = setOf(
            "jawa timur", "jawa barat", "jawa tengah", "dki jakarta", "jakarta",
            "di yogyakarta", "yogyakarta", "banten", "bali", "sumatera utara",
            "sumatera barat", "sumatera selatan", "riau", "kepulauan riau", "lampung",
            "kalimantan barat", "kalimantan timur", "kalimantan selatan", "kalimantan tengah",
            "sulawesi selatan", "sulawesi utara", "sulawesi tengah", "nusa tenggara barat",
            "nusa tenggara timur", "papua", "maluku", "aceh", "jambi", "bengkulu"
        )
        val withoutProvince = if (parts.size >= 2 && provinceNames.contains(parts.last().lowercase(Locale.getDefault()))) {
            parts.dropLast(1)
        } else {
            parts
        }
        return when {
            withoutProvince.size >= 2 -> "${withoutProvince[withoutProvince.size - 2]}, ${withoutProvince.last()}"
            withoutProvince.size == 1 -> withoutProvince.first()
            else -> parts.last()
        }
    }

    private fun shortUnit(unit: String): String {
        return unit.removePrefix("per ").trim().ifBlank { "pasang" }
    }

    private fun parsePrice(priceStr: String): Long {
        return priceStr.replace("Rp", "").replace(".", "").replace(",", "").replace(" ", "").trim().toLongOrNull() ?: 0L
    }

    private fun formatCurrency(amount: Long): String {
        val formatted = NumberFormat.getInstance(Locale("in", "ID")).format(amount)
        return "Rp $formatted"
    }

    private fun computeInitials(name: String): String {
        return name.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .take(2)
            .joinToString("")
            .ifEmpty { "GF" }
    }
}
