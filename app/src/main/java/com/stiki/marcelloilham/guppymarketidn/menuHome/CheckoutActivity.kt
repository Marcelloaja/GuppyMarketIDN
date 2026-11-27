package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.api.BiteshipClient
import com.stiki.marcelloilham.guppymarketidn.api.PricingOption
import com.stiki.marcelloilham.guppymarketidn.api.RateItem
import com.stiki.marcelloilham.guppymarketidn.api.RatesRequest
import com.stiki.marcelloilham.guppymarketidn.api.RatesResponse
import com.stiki.marcelloilham.guppymarketidn.menuProfile.ShippingAddressActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    private data class ShippingOptionState(
        var enabledBySeller: Boolean = true,
        var availableFromBiteship: Boolean = false,
        var price: Long = 0L,
        var title: String = "",
        var eta: String = ""
    )

    private var productId: String = ""
    private var sellerId: String = ""
    private var productTitle: String = ""
    private var productCategory: String = "Ikan Kontes"
    private var productStrain: String = ""
    private var unitLabel: String = "per pasang"
    private var unitPrice: Long = 0L
    private var quantity: Int = 1
    private var availableStock: Int = 1
    private var productImageUrl: String = ""
    private var productImageDataUrl: String = ""
    private var productImageRes: Int = R.drawable.bg_image_grid

    private var sellerName: String = ""
    private var sellerLocation: String = "Indonesia"
    private var sellerRating: Double = 0.0
    private var sellerReviews: Int = 0
    private var sellerLat: Double = 0.0
    private var sellerLng: Double = 0.0

    private var buyerAddress: FirestoreRepository.AddressModel? = null
    private var cartProductIdsToClear: ArrayList<String> = arrayListOf()

    private var shippingCost: Long = 0L
    private var shippingLoading: Boolean = false
    private val serviceFee: Long = 2000L
    private var ratesRequestSeq: Int = 0

    private var selectedCourier: String = "JNT"
    private var sellerCourierSettings = FirestoreRepository.CourierSettingsModel()
    private val courierStates = mutableMapOf(
        "JNT" to ShippingOptionState(title = "J&T Express"),
        "JNE" to ShippingOptionState(title = "JNE Express"),
        "SICEPAT" to ShippingOptionState(title = "SiCepat Express"),
        "INSTANT" to ShippingOptionState(title = "GoSend / GrabExpress (Instan)")
    )

    private var sellerPaymentMethods: List<FirestoreRepository.PaymentMethodModel> = emptyList()
    private var selectedPaymentMethod: FirestoreRepository.PaymentMethodModel? = null
    private var hasLoadedSellerData: Boolean = false

    private lateinit var ivProductImage: ShapeableImageView
    private lateinit var tvProductTitle: TextView
    private lateinit var tvProductMeta: TextView
    private lateinit var tvUnitBadge: TextView
    private lateinit var tvUnitPrice: TextView
    private lateinit var tvUnitSuffix: TextView

    private lateinit var tvSellerInitials: TextView
    private lateinit var ivSellerAvatar: ShapeableImageView
    private lateinit var tvSellerName: TextView
    private lateinit var tvSellerRating: TextView
    private lateinit var tvSellerReviews: TextView
    private lateinit var tvSellerLocation: TextView

    private lateinit var tvRecipientNamePhone: TextView
    private lateinit var tvFullAddress: TextView
    private lateinit var btnChangeAddress: TextView

    private lateinit var tvQuantity: TextView
    private lateinit var tvQuantitySubtext: TextView
    private lateinit var tvSummarySubtotal: TextView
    private lateinit var tvSummaryShipping: TextView
    private lateinit var tvSummaryServiceFee: TextView
    private lateinit var tvSummaryTotal: TextView
    private lateinit var tvBottomTotal: TextView
    private lateinit var tvBottomSubtext: TextView

    private lateinit var tvShippingStatusNote: TextView
    private lateinit var optionShipJNT: View
    private lateinit var optionShipJNE: View
    private lateinit var optionShipSicepat: View
    private lateinit var optionShipInstant: View

    private lateinit var icRadioShipJNT: ImageView
    private lateinit var icRadioShipJNE: ImageView
    private lateinit var icRadioShipSicepat: ImageView
    private lateinit var icRadioShipInstant: ImageView

    private lateinit var tvShipJNTTitle: TextView
    private lateinit var tvShipJNTEta: TextView
    private lateinit var tvShipJNTPrice: TextView
    private lateinit var tvShipJNETitle: TextView
    private lateinit var tvShipJNEEta: TextView
    private lateinit var tvShipJNEPrice: TextView
    private lateinit var tvShipSicepatTitle: TextView
    private lateinit var tvShipSicepatEta: TextView
    private lateinit var tvShipSicepatPrice: TextView
    private lateinit var tvShipInstantTitle: TextView
    private lateinit var tvShipInstantEta: TextView
    private lateinit var tvShipInstantPrice: TextView

    private lateinit var tvCheckoutPaymentStatus: TextView
    private lateinit var layoutSellerPaymentMethods: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        val topBar = findViewById<View>(R.id.topBarCheckout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackCheckout)
        btnBack.setOnClickListener { finish() }

        initViews()
        readIntentData()
        setupStepper()
        setupAddressSection()
        setupShippingOptions()
        setupPayButton()

        bindProductAndSellerUI()
        refreshProductFromFirestore()
        if (sellerId.isNotBlank()) {
            loadAllSellerCheckoutData()
        }
        loadBuyerAddress()
        updateCalculations()
    }

    override fun onResume() {
        super.onResume()
        loadBuyerAddress()
        if (sellerId.isNotBlank()) {
            loadSellerPaymentMethods()
        }
    }

    private fun initViews() {
        ivProductImage = findViewById(R.id.ivCheckoutProductImage)
        tvProductTitle = findViewById(R.id.tvCheckoutProductTitle)
        tvProductMeta = findViewById(R.id.tvCheckoutProductMeta)
        tvUnitBadge = findViewById(R.id.tvCheckoutUnitBadge)
        tvUnitPrice = findViewById(R.id.tvCheckoutUnitPrice)
        tvUnitSuffix = findViewById(R.id.tvCheckoutUnitSuffix)

        tvSellerInitials = findViewById(R.id.tvCheckoutSellerInitials)
        ivSellerAvatar = findViewById(R.id.ivCheckoutSellerAvatar)
        tvSellerName = findViewById(R.id.tvCheckoutSellerName)
        tvSellerRating = findViewById(R.id.tvCheckoutSellerRating)
        tvSellerReviews = findViewById(R.id.tvCheckoutSellerReviews)
        tvSellerLocation = findViewById(R.id.tvCheckoutSellerLocation)

        tvRecipientNamePhone = findViewById(R.id.tvRecipientNamePhone)
        tvFullAddress = findViewById(R.id.tvFullAddress)
        btnChangeAddress = findViewById(R.id.btnChangeAddress)

        tvQuantity = findViewById(R.id.tvCheckoutQuantity)
        tvQuantitySubtext = findViewById(R.id.tvCheckoutQuantitySubtext)
        tvSummarySubtotal = findViewById(R.id.tvSummarySubtotal)
        tvSummaryShipping = findViewById(R.id.tvSummaryShipping)
        tvSummaryServiceFee = findViewById(R.id.tvSummaryServiceFee)
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal)
        tvBottomTotal = findViewById(R.id.tvBottomTotal)
        tvBottomSubtext = findViewById(R.id.tvBottomSubtext)

        tvShippingStatusNote = findViewById(R.id.tvShippingStatusNote)
        optionShipJNT = findViewById(R.id.optionShipJNT)
        optionShipJNE = findViewById(R.id.optionShipJNE)
        optionShipSicepat = findViewById(R.id.optionShipSicepat)
        optionShipInstant = findViewById(R.id.optionShipInstant)

        icRadioShipJNT = findViewById(R.id.icRadioShipJNT)
        icRadioShipJNE = findViewById(R.id.icRadioShipJNE)
        icRadioShipSicepat = findViewById(R.id.icRadioShipSicepat)
        icRadioShipInstant = findViewById(R.id.icRadioShipInstant)

        tvShipJNTTitle = findViewById(R.id.tvShipJNTTitle)
        tvShipJNTEta = findViewById(R.id.tvShipJNTEta)
        tvShipJNTPrice = findViewById(R.id.tvShipJNTPrice)
        tvShipJNETitle = findViewById(R.id.tvShipJNETitle)
        tvShipJNEEta = findViewById(R.id.tvShipJNEEta)
        tvShipJNEPrice = findViewById(R.id.tvShipJNEPrice)
        tvShipSicepatTitle = findViewById(R.id.tvShipSicepatTitle)
        tvShipSicepatEta = findViewById(R.id.tvShipSicepatEta)
        tvShipSicepatPrice = findViewById(R.id.tvShipSicepatPrice)
        tvShipInstantTitle = findViewById(R.id.tvShipInstantTitle)
        tvShipInstantEta = findViewById(R.id.tvShipInstantEta)
        tvShipInstantPrice = findViewById(R.id.tvShipInstantPrice)

        tvCheckoutPaymentStatus = findViewById(R.id.tvCheckoutPaymentStatus)
        layoutSellerPaymentMethods = findViewById(R.id.layoutSellerPaymentMethods)
    }

    private fun readIntentData() {
        productId = intent.getStringExtra("EXTRA_PRODUCT_ID") ?: ""
        sellerId = intent.getStringExtra("EXTRA_SELLER_ID") ?: ""
        productTitle = intent.getStringExtra("EXTRA_PRODUCT_NAME") ?: ""
        productCategory = intent.getStringExtra("EXTRA_CATEGORY") ?: "Ikan Kontes"
        productStrain = intent.getStringExtra("EXTRA_STRAIN") ?: ""
        unitLabel = intent.getStringExtra("EXTRA_UNIT_LABEL") ?: "per pasang"
        unitPrice = intent.getLongExtra("EXTRA_UNIT_PRICE", 0L)
        quantity = intent.getIntExtra("EXTRA_QUANTITY", 1).coerceAtLeast(1)
        availableStock = intent.getIntExtra("EXTRA_STOCK", 99).coerceAtLeast(1)
        productImageUrl = intent.getStringExtra("EXTRA_IMAGE_URL") ?: ""
        productImageDataUrl = intent.getStringExtra("EXTRA_IMAGE_DATA_URL") ?: ""
        productImageRes = intent.getIntExtra("EXTRA_IMAGE_RES", R.drawable.bg_image_grid)

        sellerName = intent.getStringExtra("EXTRA_SELLER_NAME") ?: ""
        sellerLocation = intent.getStringExtra("EXTRA_SELLER_LOCATION") ?: "Indonesia"
        sellerRating = intent.getDoubleExtra("EXTRA_SELLER_RATING", 0.0)
        sellerReviews = intent.getIntExtra("EXTRA_SELLER_REVIEWS", 0)
        cartProductIdsToClear = intent.getStringArrayListExtra("EXTRA_CART_ITEM_IDS") ?: arrayListOf()
    }

    private fun bindProductAndSellerUI() {
        tvProductTitle.text = productTitle.ifBlank { "Produk Guppy" }
        val metaParts = listOfNotNull(
            productCategory.takeIf { it.isNotBlank() },
            productStrain.takeIf { it.isNotBlank() && !it.equals("Semua", true) }?.let { "Galur $it" }
        )
        tvProductMeta.text = metaParts.joinToString(" · ")
        val shortU = shortUnit(unitLabel)
        tvUnitBadge.text = shortU.uppercase(Locale.getDefault())
        tvUnitPrice.text = formatCurrency(unitPrice)
        tvUnitSuffix.text = "/ $shortU"
        SafeImageLoader.loadWithFallbackSafely(
            this,
            ivProductImage,
            productImageUrl,
            productImageDataUrl,
            productImageRes,
            256
        )

        val displaySeller = sellerName.ifBlank { "Toko Guppy" }
        tvSellerName.text = displaySeller
        tvSellerInitials.text = computeInitials(displaySeller)
        tvSellerRating.text = String.format(Locale.US, "%.1f", sellerRating)
        tvSellerReviews.text = "($sellerReviews ulasan)"
        updateSellerLocationWithDistance()
    }

    private fun refreshProductFromFirestore() {
        if (productId.isBlank()) return
        FirestoreRepository.fetchProductById(productId) { product ->
            if (isFinishing || isDestroyed || product == null) return@fetchProductById
            productTitle = product.title
            productCategory = product.category
            productStrain = product.strain
            unitLabel = product.unit.ifBlank { unitLabel }
            val livePrice = if (product.priceNumeric > 0L) product.priceNumeric else parsePrice(product.price)
            if (livePrice > 0L) unitPrice = livePrice
            availableStock = product.effectiveStock()
            if (availableStock > 0 && quantity > availableStock) quantity = availableStock
            val prevSellerId = sellerId
            if (sellerId.isBlank()) sellerId = product.sellerId
            if (sellerName.isBlank()) sellerName = product.seller
            if (productImageUrl.isBlank()) productImageUrl = product.photoUris.firstOrNull().orEmpty()
            if (productImageDataUrl.isBlank()) productImageDataUrl = product.photoDataUrls.firstOrNull().orEmpty()
            if (product.imageRes != 0) productImageRes = product.imageRes

            bindProductAndSellerUI()
            updateCalculations()
            if (sellerId.isNotBlank() && (!hasLoadedSellerData || prevSellerId != sellerId)) {
                loadAllSellerCheckoutData()
            } else {
                fetchBiteshipShippingRates()
            }
        }
    }

    private fun loadAllSellerCheckoutData() {
        if (sellerId.isBlank()) return
        hasLoadedSellerData = true
        loadSellerProfile()
        loadSellerCourierSettings()
        loadSellerPaymentMethods()
    }

    private fun loadSellerProfile() {
        if (sellerId.isBlank()) return
        FirestoreRepository.fetchUserProfileById(sellerId) { profile ->
            if (isFinishing || isDestroyed || profile == null) return@fetchUserProfileById
            sellerName = profile.farmName.ifBlank { profile.name.ifBlank { sellerName } }
            sellerLocation = profile.location.ifBlank { sellerLocation }
            sellerRating = profile.rating
            sellerReviews = profile.totalReviewsCount
            if (profile.lat != 0.0 && profile.lng != 0.0) {
                sellerLat = profile.lat
                sellerLng = profile.lng
            }

            tvSellerName.text = sellerName.ifBlank { "Toko Guppy" }
            tvSellerInitials.text = computeInitials(sellerName)
            tvSellerRating.text = String.format(Locale.US, "%.1f", sellerRating)
            tvSellerReviews.text = "($sellerReviews ulasan)"
            if (profile.avatarUri.isNotBlank()) {
                val loaded = SafeImageLoader.loadUriSafely(this, ivSellerAvatar, profile.avatarUri, R.drawable.ic_default_user_avatar, 160)
                ivSellerAvatar.visibility = if (loaded) View.VISIBLE else View.GONE
                tvSellerInitials.visibility = if (loaded) View.GONE else View.VISIBLE
            }
            updateSellerLocationWithDistance()

            if (sellerLat == 0.0 || sellerLng == 0.0) {
                // Fallback to seller's farm or default address pin if profile lat/lng wasn't synced yet
                FirestoreRepository.fetchUserAddresses(sellerId, preferServer = false) { sellerAddresses ->
                    if (isFinishing || isDestroyed) return@fetchUserAddresses
                    val pinAddr = sellerAddresses.firstOrNull {
                        it.lat != 0.0 && it.lng != 0.0 &&
                            (it.label.contains("Gudang", true) || it.label.contains("Farm", true) || it.label.contains("Toko", true))
                    } ?: sellerAddresses.firstOrNull { it.isDefault && it.lat != 0.0 && it.lng != 0.0 }
                        ?: sellerAddresses.firstOrNull { it.lat != 0.0 && it.lng != 0.0 }
                    if (pinAddr != null) {
                        sellerLat = pinAddr.lat
                        sellerLng = pinAddr.lng
                        if (sellerLocation.isBlank() || sellerLocation == "Indonesia") {
                            sellerLocation = pinAddr.fullAddress
                        }
                        updateSellerLocationWithDistance()
                    }
                    fetchBiteshipShippingRates()
                }
            } else {
                fetchBiteshipShippingRates()
            }
        }
    }

    private fun loadSellerCourierSettings() {
        if (sellerId.isBlank()) return
        FirestoreRepository.fetchCourierSettings(sellerId) { settings ->
            if (isFinishing || isDestroyed) return@fetchCourierSettings
            sellerCourierSettings = settings
            val anyEnabled = settings.jntExpressEnabled ||
                settings.jneRegularEnabled ||
                settings.jneYesEnabled ||
                settings.sicepatHaluEnabled ||
                settings.grabInstantEnabled

            courierStates["JNT"]?.enabledBySeller = if (anyEnabled) settings.jntExpressEnabled else true
            courierStates["JNE"]?.enabledBySeller = if (anyEnabled) (settings.jneRegularEnabled || settings.jneYesEnabled) else true
            courierStates["SICEPAT"]?.enabledBySeller = if (anyEnabled) settings.sicepatHaluEnabled else true
            courierStates["INSTANT"]?.enabledBySeller = if (anyEnabled) settings.grabInstantEnabled else true

            val preferredKey = when {
                settings.mainCourier.contains("J&T", true) || settings.mainCourier.contains("JNT", true) -> "JNT"
                settings.mainCourier.contains("JNE", true) -> "JNE"
                settings.mainCourier.contains("SiCepat", true) -> "SICEPAT"
                settings.mainCourier.contains("Grab", true) || settings.mainCourier.contains("GoSend", true) -> "INSTANT"
                else -> "JNT"
            }
            if (courierStates[preferredKey]?.enabledBySeller == true) {
                selectedCourier = preferredKey
            } else {
                selectedCourier = courierStates.entries.firstOrNull { it.value.enabledBySeller }?.key ?: "JNT"
            }

            applyCourierVisibilityAndUI()
        }
    }

    private fun setupAddressSection() {
        btnChangeAddress.setOnClickListener {
            startActivity(Intent(this, ShippingAddressActivity::class.java))
        }
        tvShippingStatusNote.setOnClickListener {
            if (buyerAddress == null || buyerAddress?.lat == 0.0 || buyerAddress?.lng == 0.0) {
                startActivity(Intent(this, ShippingAddressActivity::class.java))
            } else {
                fetchBiteshipShippingRates()
            }
        }
    }

    private fun loadBuyerAddress() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirestoreRepository.fetchUserAddresses(currentUserId, preferServer = true) { addresses ->
            if (isFinishing || isDestroyed) return@fetchUserAddresses
            val defaultAddr = addresses.find { it.isDefault } ?: addresses.firstOrNull()
            val coordsChanged = buyerAddress?.id != defaultAddr?.id ||
                buyerAddress?.lat != defaultAddr?.lat ||
                buyerAddress?.lng != defaultAddr?.lng
            buyerAddress = defaultAddr
            if (defaultAddr != null) {
                tvRecipientNamePhone.text = "${defaultAddr.recipientName} (${defaultAddr.phone})"
                val labelPrefix = if (defaultAddr.label.isNotBlank()) "[${defaultAddr.label}] " else ""
                tvFullAddress.text = "$labelPrefix${defaultAddr.fullAddress}"
                updateSellerLocationWithDistance()
                if (coordsChanged || !shippingLoading && shippingCost == 0L) {
                    fetchBiteshipShippingRates()
                }
            } else {
                tvRecipientNamePhone.text = "Belum ada alamat pengiriman"
                tvFullAddress.text = "Ketuk 'Ubah' untuk menambahkan alamat utama tujuan pengiriman Anda."
                shippingCost = 0L
                tvShippingStatusNote.text = "Tambahkan alamat utama Anda untuk menghitung ongkos kirim Biteship."
                updateCalculations()
            }
        }
    }

    private fun updateSellerLocationWithDistance() {
        val baseLoc = sellerLocation.ifBlank { "Indonesia" }
        val buyer = buyerAddress
        if (buyer != null && buyer.lat != 0.0 && buyer.lng != 0.0 && sellerLat != 0.0 && sellerLng != 0.0) {
            val res = FloatArray(1)
            Location.distanceBetween(buyer.lat, buyer.lng, sellerLat, sellerLng, res)
            val distKm = res[0] / 1000.0
            val distText = if (distKm < 1.0) "${(distKm * 1000).toInt()} m" else String.format(Locale.US, "%.1f km", distKm).replace('.', ',')
            tvSellerLocation.text = "$baseLoc · $distText"
        } else {
            tvSellerLocation.text = baseLoc
        }
    }

    private fun loadSellerPaymentMethods() {
        if (sellerId.isBlank()) {
            tvCheckoutPaymentStatus.visibility = View.VISIBLE
            tvCheckoutPaymentStatus.text = "Data penjual tidak ditemukan."
            layoutSellerPaymentMethods.removeAllViews()
            selectedPaymentMethod = null
            return
        }

        tvCheckoutPaymentStatus.visibility = View.VISIBLE
        tvCheckoutPaymentStatus.text = "Memuat metode pembayaran dari penjual..."

        FirestoreRepository.fetchUserPaymentMethods(sellerId) { methods ->
            if (isFinishing || isDestroyed) return@fetchUserPaymentMethods
            val sorted = methods.distinctBy { it.id }.sortedByDescending { it.isMain }
            sellerPaymentMethods = sorted

            val previousSelectedId = selectedPaymentMethod?.id
            selectedPaymentMethod = sorted.find { it.id == previousSelectedId }
                ?: sorted.find { it.isMain }
                ?: sorted.firstOrNull()

            renderSellerPaymentMethods()
        }
    }

    private fun renderSellerPaymentMethods() {
        layoutSellerPaymentMethods.removeAllViews()
        if (sellerPaymentMethods.isEmpty()) {
            tvCheckoutPaymentStatus.visibility = View.VISIBLE
            tvCheckoutPaymentStatus.text = "Penjual belum mengatur metode pembayaran. Hubungi penjual lewat chat."
            return
        }

        tvCheckoutPaymentStatus.visibility = View.GONE
        val dp = resources.displayMetrics.density
        val primaryColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val blackColor = ContextCompat.getColor(this, R.color.guppy_text_black)
        val grayColor = ContextCompat.getColor(this, R.color.guppy_text_gray)
        val fontBold = ResourcesCompat.getFont(this, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD
        val fontMed = ResourcesCompat.getFont(this, R.font.inter_medium) ?: Typeface.DEFAULT
        val fontReg = ResourcesCompat.getFont(this, R.font.inter_regular) ?: Typeface.DEFAULT

        sellerPaymentMethods.forEachIndexed { index, method ->
            val isSelected = selectedPaymentMethod?.id == method.id
            val isQris = method.type.equals("QRIS", true) || method.providerName.contains("QRIS", true)
            val isBank = !isQris && (method.type.equals("Bank", true) ||
                method.providerName.contains("BCA", true) ||
                method.providerName.contains("BNI", true) ||
                method.providerName.contains("Mandiri", true) ||
                method.providerName.contains("BRI", true) ||
                method.providerName.contains("Bank", true))

            val cardRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
                setBackgroundResource(if (isSelected) R.drawable.bg_variant_card_selected else R.drawable.bg_variant_card_unselected)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (index == 0) (8 * dp).toInt() else (10 * dp).toInt()
                }
            }

            val iconView = ImageView(this).apply {
                setImageResource(
                    when {
                        isQris -> R.drawable.ic_qris
                        isBank -> R.drawable.ic_payment_card
                        else -> R.drawable.ic_ewallet
                    }
                )
                layoutParams = LinearLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt())
            }

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (12 * dp).toInt()
                    marginEnd = (10 * dp).toInt()
                }
            }

            val typeLabel = when {
                isQris -> "QRIS"
                isBank -> "Transfer Bank"
                else -> "E-Wallet"
            }

            val tvTitle = TextView(this).apply {
                text = if (isQris) "QRIS (${method.providerName})" else "${method.providerName} ($typeLabel)"
                textSize = 14f
                typeface = if (isSelected) fontBold else fontMed
                setTextColor(if (isSelected) primaryColor else blackColor)
            }

            val detailSubtitle = if (isQris) {
                val ownerPart = method.accountOwner.ifBlank { sellerName.ifBlank { "Toko Penjual" } }
                "Scan kode QRIS penjual · $ownerPart"
            } else {
                val acc = method.accountNumber.ifBlank { "-" }
                val ownerSuffix = if (method.accountOwner.isNotBlank()) " · a.n. ${method.accountOwner}" else ""
                "$acc$ownerSuffix"
            }

            val tvSub = TextView(this).apply {
                text = detailSubtitle
                textSize = 12f
                typeface = fontReg
                setTextColor(grayColor)
                setPadding(0, (2 * dp).toInt(), 0, 0)
            }

            textCol.addView(tvTitle)
            textCol.addView(tvSub)

            val radioView = ImageView(this).apply {
                setImageResource(if (isSelected) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected)
                layoutParams = LinearLayout.LayoutParams((20 * dp).toInt(), (20 * dp).toInt())
            }

            cardRow.addView(iconView)
            cardRow.addView(textCol)
            cardRow.addView(radioView)

            cardRow.setOnClickListener {
                selectedPaymentMethod = method
                renderSellerPaymentMethods()
            }

            layoutSellerPaymentMethods.addView(cardRow)
        }
    }

    private fun setupStepper() {
        val btnMin = findViewById<ImageView>(R.id.btnCheckoutMin)
        val btnPlus = findViewById<ImageView>(R.id.btnCheckoutPlus)

        btnMin.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateCalculations()
                fetchBiteshipShippingRates()
            }
        }

        btnPlus.setOnClickListener {
            if (quantity < availableStock) {
                quantity++
                updateCalculations()
                fetchBiteshipShippingRates()
            } else {
                Toast.makeText(this, "Maksimal stok tersedia $availableStock.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupShippingOptions() {
        val selectCourier: (String) -> Unit = { key ->
            val state = courierStates[key]
            if (state != null && state.availableFromBiteship && state.price > 0L) {
                selectedCourier = key
                shippingCost = state.price
                applyCourierVisibilityAndUI()
                updateCalculations()
            } else {
                Toast.makeText(this, "Layanan kurir ini belum tersedia untuk rute pengiriman Anda.", Toast.LENGTH_SHORT).show()
            }
        }

        optionShipJNT.setOnClickListener { selectCourier("JNT") }
        optionShipJNE.setOnClickListener { selectCourier("JNE") }
        optionShipSicepat.setOnClickListener { selectCourier("SICEPAT") }
        optionShipInstant.setOnClickListener { selectCourier("INSTANT") }
    }

    private fun applyCourierVisibilityAndUI() {
        val bgSelected = R.drawable.bg_variant_card_selected
        val bgUnselected = R.drawable.bg_variant_card_unselected
        val primaryColor = ContextCompat.getColor(this, R.color.guppy_primary)
        val blackColor = ContextCompat.getColor(this, R.color.guppy_text_black)
        val grayColor = ContextCompat.getColor(this, R.color.guppy_text_gray)

        val rows = listOf(
            Triple("JNT", optionShipJNT, Triple(icRadioShipJNT, tvShipJNTTitle, Pair(tvShipJNTEta, tvShipJNTPrice))),
            Triple("JNE", optionShipJNE, Triple(icRadioShipJNE, tvShipJNETitle, Pair(tvShipJNEEta, tvShipJNEPrice))),
            Triple("SICEPAT", optionShipSicepat, Triple(icRadioShipSicepat, tvShipSicepatTitle, Pair(tvShipSicepatEta, tvShipSicepatPrice))),
            Triple("INSTANT", optionShipInstant, Triple(icRadioShipInstant, tvShipInstantTitle, Pair(tvShipInstantEta, tvShipInstantPrice)))
        )

        rows.forEach { (key, container, widgets) ->
            val state = courierStates[key] ?: return@forEach
            container.visibility = if (state.enabledBySeller) View.VISIBLE else View.GONE
            if (!state.enabledBySeller) return@forEach

            val (radio, titleView, subPair) = widgets
            val (etaView, priceView) = subPair
            val isSelected = (key == selectedCourier && state.availableFromBiteship && state.price > 0L)

            titleView.text = state.title
            etaView.text = when {
                shippingLoading -> "Menghitung estimasi Biteship..."
                state.eta.isNotBlank() -> state.eta
                else -> "Menunggu data rute"
            }
            priceView.text = when {
                shippingLoading -> "Menghitung..."
                state.availableFromBiteship && state.price > 0L -> formatCurrency(state.price)
                else -> "Tidak tersedia untuk rute ini"
            }

            container.alpha = if (!shippingLoading && !state.availableFromBiteship) 0.55f else 1.0f

            if (isSelected) {
                container.setBackgroundResource(bgSelected)
                radio.setImageResource(R.drawable.ic_radio_selected)
                titleView.setTextColor(primaryColor)
                priceView.setTextColor(primaryColor)
            } else {
                container.setBackgroundResource(bgUnselected)
                radio.setImageResource(R.drawable.ic_radio_unselected)
                titleView.setTextColor(blackColor)
                priceView.setTextColor(grayColor)
            }
        }
    }

    private fun updateCalculations() {
        val subtotal = unitPrice * quantity
        val total = subtotal + shippingCost + serviceFee
        val shortU = shortUnit(unitLabel)

        tvQuantity.text = quantity.toString()
        if (unitLabel.contains("pasang", true)) {
            tvQuantitySubtext.text = "$quantity $shortU · Setara ${quantity * 2} ekor"
            tvBottomSubtext.text = "$quantity $shortU · ${quantity * 2} ekor"
        } else {
            tvQuantitySubtext.text = "$quantity $shortU"
            tvBottomSubtext.text = "$quantity $shortU"
        }

        tvSummarySubtotal.text = formatCurrency(subtotal)
        tvSummaryShipping.text = when {
            shippingLoading -> "Menghitung..."
            shippingCost > 0L -> formatCurrency(shippingCost)
            else -> "-"
        }
        tvSummaryServiceFee.text = formatCurrency(serviceFee)
        tvSummaryTotal.text = formatCurrency(total)
        tvBottomTotal.text = formatCurrency(total)
    }

    private fun setupPayButton() {
        val btnPay = findViewById<TextView>(R.id.btnPayNow)
        btnPay.setOnClickListener {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (currentUid.isBlank()) {
                Toast.makeText(this, "Silakan masuk ke akun terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sellerId.isNotBlank() && sellerId == currentUid) {
                Toast.makeText(this, "Anda tidak dapat membeli produk milik sendiri.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val addr = buyerAddress
            if (addr == null) {
                AlertDialog.Builder(this)
                    .setTitle("Alamat Pengiriman Kosong")
                    .setMessage("Tambahkan alamat utama pengiriman terlebih dahulu sebelum melanjutkan pembayaran.")
                    .setPositiveButton("Tambah Alamat") { _, _ ->
                        startActivity(Intent(this, ShippingAddressActivity::class.java))
                    }
                    .setNegativeButton("Batal", null)
                    .show()
                return@setOnClickListener
            }
            if (unitPrice <= 0L) {
                Toast.makeText(this, "Data harga produk sedang dimuat, silakan tunggu sebentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (shippingLoading) {
                Toast.makeText(this, "Ongkos kirim Biteship sedang dihitung, silakan tunggu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (shippingCost <= 0L) {
                Toast.makeText(this, "Tarif ongkos kirim Biteship belum tersedia. Pastikan pin alamat sudah diatur.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val payMethod = selectedPaymentMethod
            if (payMethod == null) {
                Toast.makeText(this, "Pilih metode pembayaran penjual terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = (unitPrice * quantity) + shippingCost + serviceFee
            val activeCourierState = courierStates[selectedCourier]
            val courierText = activeCourierState?.title?.ifBlank { selectedCourier } ?: selectedCourier
            val courierEtaText = activeCourierState?.eta.orEmpty()
            val orderNum = "#GM-${(10000..99999).random()}"
            val resolvedSellerName = sellerName.ifBlank { tvSellerName.text.toString() }
            val deadlineMillis = System.currentTimeMillis() + 86400000L

            val isQris = payMethod.type.equals("QRIS", true) || payMethod.providerName.contains("QRIS", true)
            val resolvedPayType = when {
                isQris -> "QRIS"
                payMethod.type.equals("Bank", true) -> "Bank"
                else -> payMethod.type.ifBlank { "E-Wallet" }
            }
            val displayMethodName = if (isQris) {
                "QRIS (${payMethod.providerName})"
            } else {
                "${payMethod.providerName} ($resolvedPayType)"
            }

            val order = FirestoreRepository.OrderModel(
                id = orderNum,
                userId = currentUid,
                sellerId = sellerId,
                productId = productId,
                productName = productTitle.ifBlank { tvProductTitle.text.toString() },
                sellerName = resolvedSellerName,
                unit = unitLabel,
                unitPrice = unitPrice,
                quantity = quantity,
                totalAmount = total,
                shippingCost = shippingCost,
                serviceFee = serviceFee,
                shippingAddress = addr.fullAddress,
                shippingEta = courierEtaText,
                recipientName = addr.recipientName,
                recipientPhone = addr.phone,
                courier = courierText,
                paymentMethod = displayMethodName,
                paymentType = resolvedPayType,
                paymentProvider = payMethod.providerName,
                paymentAccountNumber = payMethod.accountNumber,
                paymentAccountOwner = payMethod.accountOwner,
                paymentQrisDataUrl = payMethod.qrisDataUrl,
                productImageUrl = productImageUrl,
                productImageDataUrl = productImageDataUrl,
                paymentDeadline = deadlineMillis,
                status = "Menunggu Pembayaran"
            )

            btnPay.isEnabled = false
            btnPay.text = "Memproses..."

            FirestoreRepository.createOrderWithId(order) { success, docId ->
                if (isFinishing || isDestroyed) return@createOrderWithId
                btnPay.isEnabled = true
                btnPay.text = "Bayar sekarang"

                if (!success) {
                    Toast.makeText(this, "Gagal membuat pesanan. Periksa koneksi Anda dan coba lagi.", Toast.LENGTH_SHORT).show()
                    return@createOrderWithId
                }

                if (cartProductIdsToClear.isNotEmpty()) {
                    FirestoreRepository.deleteCartItems(currentUid, cartProductIdsToClear) {}
                }

                val intent = Intent(this, PaymentPendingActivity::class.java).apply {
                    putExtra("EXTRA_ORDER_DOC_ID", docId)
                    putExtra("EXTRA_ORDER_ID", orderNum)
                    putExtra("EXTRA_SELLER_ID", sellerId)
                    putExtra("EXTRA_PRODUCT_ID", productId)
                    putExtra("EXTRA_PRODUCT_NAME", order.productName)
                    putExtra("EXTRA_PRODUCT_IMAGE_URL", productImageUrl)
                    putExtra("EXTRA_PRODUCT_IMAGE_DATA_URL", productImageDataUrl)
                    putExtra("EXTRA_UNIT_PRICE", unitPrice)
                    putExtra("EXTRA_UNIT_LABEL", unitLabel)
                    putExtra("EXTRA_QUANTITY", quantity)
                    putExtra("EXTRA_TOTAL_AMOUNT", total)
                    putExtra("EXTRA_SHIPPING_COST", shippingCost)
                    putExtra("EXTRA_SERVICE_FEE", serviceFee)
                    putExtra("EXTRA_COURIER", courierText)
                    putExtra("EXTRA_SHIPPING_ETA", courierEtaText)
                    putExtra("EXTRA_SELLER_NAME", resolvedSellerName)
                    putExtra("EXTRA_ADDRESS_TEXT", addr.fullAddress)
                    putExtra("EXTRA_PAYMENT_METHOD", displayMethodName)
                    putExtra("EXTRA_PAYMENT_TYPE", resolvedPayType)
                    putExtra("EXTRA_PAYMENT_PROVIDER", payMethod.providerName)
                    putExtra("EXTRA_PAYMENT_ACCOUNT_NUMBER", payMethod.accountNumber)
                    putExtra("EXTRA_PAYMENT_ACCOUNT_OWNER", payMethod.accountOwner)
                    putExtra("EXTRA_PAYMENT_QRIS_DATA_URL", payMethod.qrisDataUrl)
                    putExtra("EXTRA_PAYMENT_DEADLINE", deadlineMillis)
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun fetchBiteshipShippingRates() {
        val buyer = buyerAddress
        if (buyer == null) {
            shippingLoading = false
            shippingCost = 0L
            tvShippingStatusNote.text = "Tambahkan alamat utama Anda untuk menghitung tarif Biteship."
            applyCourierVisibilityAndUI()
            updateCalculations()
            return
        }
        if (buyer.lat == 0.0 || buyer.lng == 0.0) {
            shippingLoading = false
            shippingCost = 0L
            tvShippingStatusNote.text = "Pin lokasi alamat pengiriman Anda belum diatur. Ketuk Ubah untuk memilih titik peta."
            applyCourierVisibilityAndUI()
            updateCalculations()
            return
        }
        if (sellerLat == 0.0 || sellerLng == 0.0) {
            shippingLoading = false
            shippingCost = 0L
            tvShippingStatusNote.text = "Koordinat gudang/alamat penjual belum tersedia untuk kalkulasi Biteship."
            applyCourierVisibilityAndUI()
            updateCalculations()
            return
        }

        val currentSeq = ++ratesRequestSeq
        shippingLoading = true
        tvShippingStatusNote.text = "Menghitung tarif pengiriman Biteship (${500 * quantity} g)..."
        applyCourierVisibilityAndUI()
        updateCalculations()

        val activeCourierCodes = buildList {
            if (courierStates["JNT"]?.enabledBySeller != false) add("jnt")
            if (courierStates["JNE"]?.enabledBySeller != false) add("jne")
            if (courierStates["SICEPAT"]?.enabledBySeller != false) add("sicepat")
            if (courierStates["INSTANT"]?.enabledBySeller != false) {
                add("grab")
                add("gojek")
            }
        }.ifEmpty { listOf("jnt", "jne", "sicepat", "grab", "gojek") }

        val request = RatesRequest(
            originLatitude = sellerLat,
            originLongitude = sellerLng,
            destinationLatitude = buyer.lat,
            destinationLongitude = buyer.lng,
            couriers = activeCourierCodes.joinToString(","),
            items = listOf(
                RateItem(
                    productTitle.ifBlank { "Ikan Guppy" },
                    (unitPrice * quantity).coerceAtLeast(1000L),
                    500 * quantity
                )
            )
        )

        BiteshipClient.instance.getShippingRates(
            authorization = BiteshipClient.API_KEY,
            request = request
        ).enqueue(object : Callback<RatesResponse> {
            override fun onResponse(call: Call<RatesResponse>, response: Response<RatesResponse>) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || currentSeq != ratesRequestSeq) return@runOnUiThread
                    shippingLoading = false

                    if (response.isSuccessful && response.body()?.success == true) {
                        val pricings = response.body()?.pricing ?: emptyList()

                        val jntOption = pricings.filter {
                            it.courierCode.contains("jnt", true) || it.courierName.contains("j&t", true)
                        }.minByOrNull { it.price }

                        val jneOptions = pricings.filter {
                            it.courierCode.contains("jne", true) || it.courierName.contains("jne", true)
                        }
                        val jneOption = when {
                            sellerCourierSettings.jneYesEnabled && !sellerCourierSettings.jneRegularEnabled ->
                                jneOptions.firstOrNull { it.serviceTypeMatches("yes") } ?: jneOptions.minByOrNull { it.price }
                            sellerCourierSettings.jneRegularEnabled && !sellerCourierSettings.jneYesEnabled ->
                                jneOptions.firstOrNull { !it.serviceTypeMatches("yes") } ?: jneOptions.minByOrNull { it.price }
                            else -> jneOptions.minByOrNull { it.price }
                        }

                        val sicepatOption = pricings.filter {
                            it.courierCode.contains("sicepat", true) || it.courierName.contains("sicepat", true)
                        }.minByOrNull { it.price }

                        val instantOption = pricings.filter {
                            it.type?.contains("instant", true) == true ||
                                it.courierCode.contains("gojek", true) ||
                                it.courierCode.contains("grab", true)
                        }.minByOrNull { it.price }

                        updateCourierOption("JNT", jntOption, "J&T Express")
                        updateCourierOption("JNE", jneOption, "JNE Express")
                        updateCourierOption("SICEPAT", sicepatOption, "SiCepat Express")
                        updateCourierOption("INSTANT", instantOption, "GoSend / GrabExpress Instan")

                        val activeEntry = courierStates[selectedCourier]
                        if (activeEntry == null || !activeEntry.enabledBySeller || !activeEntry.availableFromBiteship || activeEntry.price <= 0L) {
                            val fallback = courierStates.entries.firstOrNull {
                                it.value.enabledBySeller && it.value.availableFromBiteship && it.value.price > 0L
                            }
                            if (fallback != null) {
                                selectedCourier = fallback.key
                                shippingCost = fallback.value.price
                            } else {
                                shippingCost = 0L
                            }
                        } else {
                            shippingCost = activeEntry.price
                        }

                        tvShippingStatusNote.text = if (shippingCost > 0L) {
                            "Tarif real-time Biteship (${500 * quantity} g)"
                        } else {
                            "Tidak ada layanan kurir Biteship yang tersedia untuk rute ini."
                        }
                        applyCourierVisibilityAndUI()
                        updateCalculations()
                        Log.d("BiteshipCheckout", "Biteship rates loaded: ${pricings.size} options")
                    } else {
                        handleRatesFailure("Tarif Biteship tidak merespons (${response.code()}). Ketuk untuk coba lagi.")
                    }
                }
            }

            override fun onFailure(call: Call<RatesResponse>, t: Throwable) {
                Log.e("BiteshipCheckout", "Rates call failed: ${t.message}")
                runOnUiThread {
                    if (isFinishing || isDestroyed || currentSeq != ratesRequestSeq) return@runOnUiThread
                    shippingLoading = false
                    handleRatesFailure("Gagal terhubung ke Biteship. Ketuk di sini untuk muat ulang.")
                }
            }
        })
    }

    private fun updateCourierOption(key: String, option: PricingOption?, fallbackTitle: String) {
        val state = courierStates[key] ?: return
        if (option != null && option.price > 0L) {
            state.availableFromBiteship = true
            state.price = option.price
            state.title = buildCourierDisplayTitle(option, fallbackTitle)
            state.eta = option.duration?.takeIf { it.isNotBlank() }?.let { "Estimasi $it" } ?: "Estimasi mengikuti kurir"
        } else {
            state.availableFromBiteship = false
            state.price = 0L
            state.title = fallbackTitle
            state.eta = "Tidak menjangkau rute ini"
        }
    }

    private fun buildCourierDisplayTitle(option: PricingOption, fallbackTitle: String): String {
        val base = option.courierName.ifBlank { fallbackTitle }
        val service = option.courierServiceName.ifBlank { option.courierServiceCode }
        return if (service.isNotBlank()) "$base ($service)" else base
    }

    private fun handleRatesFailure(note: String) {
        courierStates.values.forEach {
            it.availableFromBiteship = false
            it.price = 0L
            it.eta = "Gagal memuat estimasi"
        }
        shippingCost = 0L
        tvShippingStatusNote.text = note
        applyCourierVisibilityAndUI()
        updateCalculations()
    }

    private fun PricingOption.serviceTypeMatches(type: String): Boolean {
        return courierServiceName.contains(type, ignoreCase = true) ||
            courierServiceCode.contains(type, ignoreCase = true)
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
