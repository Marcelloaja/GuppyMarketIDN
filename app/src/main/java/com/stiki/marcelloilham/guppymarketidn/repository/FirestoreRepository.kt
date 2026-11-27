package com.stiki.marcelloilham.guppymarketidn.repository

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stiki.marcelloilham.guppymarketidn.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class ProductModel(
        val id: String = "",
        val sellerId: String = "",
        val title: String = "",
        val seller: String = "",
        val price: String = "",
        val unit: String = "per pasang",
        val sold: String = "0",
        val stock: Int = 1,
        val soldCount: Int = 0,
        val imageRes: Int = R.drawable.bg_image_grid,
        val category: String = "Ikan Kontes",
        val strain: String = "Semua",
        val grade: String = "Grade A",
        val description: String = "",
        val location: String = "",
        val sellerAvatarUri: String = "",
        val photoUris: List<String> = emptyList(),
        val photoDataUrls: List<String> = emptyList(),
        val videoUri: String = "",
        val gender: String = "Jantan & Betina",
        val age: String = "",
        val size: String = "",
        val condition: String = "Sehat, bebas penyakit",
        val priceNumeric: Long = 0L,
        val createdAt: Long = System.currentTimeMillis()
    ) {
        fun effectiveStock(): Int = if (stock > 0 || soldCount > 0) stock else (sold.toIntOrNull()?.takeIf { it > 0 } ?: 1)
    }

    data class AuctionModel(
        val id: String = "",
        val sellerId: String = "",
        val title: String = "",
        val seller: String = "",
        val location: String = "Bekasi",
        val currentBid: String = "Rp 450.000",
        val currentBidNumeric: Long = 450000L,
        val bidsCount: Int = 14,
        val endTimeMillis: Long = System.currentTimeMillis() + 8000000L,
        val imageRes: Int = R.drawable.bg_image_grid,
        val grade: String = "Grade A+",
        val description: String = "Ikan Guppy berkualitas tinggi dengan standar penilaian kontes. Memiliki corak ekor lincah, bebas penyakit, dan sudah melewati masa karantina.",
        val strain: String = "Mosaic",
        val gender: String = "1 Pasang (Pair)",
        val age: String = "3.5 Bulan",
        val quantity: String = "1 Pasang",
        val condition: String = "Sangat Lincah, Bebas Jamur",
        val quarantine: String = "14 hari di farm",
        val highestBidderName: String = "",
        val highestBidderId: String = "",
        val bidIncrementNumeric: Long = 25000L,
        val buyoutPriceNumeric: Long = 0L,
        val participantIds: List<String> = emptyList(),
        val videoUri: String = "",
        val videoThumbDataUrl: String = "",
        val videoChunksCount: Int = 0,
        val photoUris: List<String> = emptyList(),
        val photoDataUrls: List<String> = emptyList(),
        val sellerAvatarUri: String = ""
    )

    data class UserProfileModel(
        val uid: String = "",
        val name: String = "",
        val farmName: String = "",
        val description: String = "",
        val location: String = "",
        val fullAddress: String = "",
        val avatarUri: String = "",
        val rating: Double = 0.0,
        val totalReviewsCount: Int = 0,
        val totalSoldCount: Int = 0,
        val totalOrders: Int = 0,
        val ontimeOrdersCount: Int = 0,
        val totalChats: Int = 0,
        val totalResponseTimeMinutes: Long = 0L,
        val createdAt: Long = 0L,
        val lat: Double = 0.0,
        val lng: Double = 0.0
    )

    data class BidModel(
        val id: String = "",
        val auctionId: String = "",
        val bidderId: String = "",
        val bidderName: String = "",
        val amountNumeric: Long = 0L,
        val amountFormatted: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    data class FarmModel(
        val id: String = "",
        val farmName: String = "",
        val ownerName: String = "",
        val location: String = "",
        val distanceKm: String = "Lokasi Gudang",
        val rating: Double = 0.0,
        val variantsCount: Int = 0,
        val lat: Double = 0.0,
        val lng: Double = 0.0
    )

    data class OrderModel(
        val id: String = "",
        val docId: String = "",
        val userId: String = "",
        val sellerId: String = "",
        val productId: String = "",
        val productName: String = "",
        val sellerName: String = "",
        val unit: String = "per pasang",
        val unitPrice: Long = 0L,
        val quantity: Int = 1,
        val totalAmount: Long = 0L,
        val shippingCost: Long = 0L,
        val serviceFee: Long = 2000L,
        val shippingAddress: String = "",
        val shippingEta: String = "",
        val recipientName: String = "",
        val recipientPhone: String = "",
        val courier: String = "",
        val paymentMethod: String = "",
        val paymentType: String = "",
        val paymentProvider: String = "",
        val paymentAccountNumber: String = "",
        val paymentAccountOwner: String = "",
        val paymentQrisDataUrl: String = "",
        val productImageUrl: String = "",
        val productImageDataUrl: String = "",
        val paymentDeadline: Long = 0L,
        val status: String = "Menunggu Pembayaran",
        val trackingNumber: String = "",
        val createdAt: Long = System.currentTimeMillis()
    )

    data class CartItemModel(
        val id: String = "",
        val productId: String = "",
        val sellerId: String = "",
        val sellerName: String = "",
        val sellerLocation: String = "",
        val title: String = "",
        val unit: String = "per pasang",
        val unitPrice: Long = 0L,
        val quantity: Int = 1,
        val stock: Int = 1,
        val imageUrl: String = "",
        val imageDataUrl: String = "",
        val imageRes: Int = R.drawable.bg_image_grid,
        val category: String = "Ikan Kontes",
        val strain: String = "Semua",
        val addedAt: Long = System.currentTimeMillis()
    )

    data class AddressModel(
        val id: String = "",
        val label: String = "Rumah",
        val recipientName: String = "",
        val phone: String = "",
        val fullAddress: String = "",
        @field:JvmField
        val isDefault: Boolean = false,
        val notesForCourier: String = "",
        val lat: Double = 0.0,
        val lng: Double = 0.0
    )

    data class CourierSettingsModel(
        val jneRegularEnabled: Boolean = true,
        val jneYesEnabled: Boolean = true,
        val jntExpressEnabled: Boolean = true,
        val sicepatHaluEnabled: Boolean = false,
        val grabInstantEnabled: Boolean = true,
        val mainCourier: String = "JNE Reguler",
        val pickupSchedule: String = "Setiap hari, 09.00–16.00"
    )

    data class PaymentSettingsModel(
        val mainMethod: String = "Belum Diatur",
        val bcaAccountNumber: String = "",
        val bniAccountNumber: String = "",
        val mandiriActive: Boolean = false,
        val goPayEnabled: Boolean = false,
        val ovoEnabled: Boolean = false,
        val danaEnabled: Boolean = false,
        val qrisEnabled: Boolean = false,
        val withdrawalAccount: String = "",
        val adminFeeAmount: Long = 4000L
    )

    data class PaymentMethodModel(
        val id: String = "",
        val type: String = "Bank", // "Bank", "E-Wallet", or "QRIS"
        val providerName: String = "BCA",
        val accountNumber: String = "",
        val accountOwner: String = "",
        val qrisDataUrl: String = "",
        @field:JvmField
        val isMain: Boolean = false
    )

    data class ChatMessageModel(
        val id: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val messageText: String = "",
        val isSeller: Boolean = false,
        val timestamp: Long = System.currentTimeMillis(),
        val formattedTime: String = "09:00"
    )

    data class ChatRoomModel(
        val id: String = "",
        val participants: List<String> = emptyList(),
        val buyerId: String = "",
        val buyerName: String = "",
        val storeId: String = "",
        val storeName: String = "",
        val storeLocation: String = "",
        val lastMessage: String = "",
        val lastMessageTime: Long = 0L,
        val lastSenderId: String = "",
        val unreadCounts: Map<String, Long> = emptyMap(),
        val deletedFor: List<String> = emptyList(),
        val deletedAtBy: Map<String, Long> = emptyMap(),
        @field:JvmField
        val isOrderRelated: Boolean = false,
        @field:JvmField
        val isCsSupport: Boolean = false
    )

    const val CS_STORE_ID = "cs_guppymarket"
    const val CS_STORE_NAME = "CS GuppyMarket IDN"

    data class FarmProfileModel(
        val farmId: String = "",
        val farmName: String = "",
        val ownerName: String = "",
        val location: String = "",
        val rating: Double = 0.0,
        val totalReviewsCount: Int = 0,
        val totalVariantsCount: Int = 0,
        val totalSoldsCount: Int = 0,
        val followersCount: Int = 0,
        val establishedYearsText: String = "",
        val responseTimeText: String = "",
        val onTimeDeliveryRateText: String = "",
        val guaranteeText: String = "",
        val description: String = ""
    )

    // Pure real database mode
    fun seedInitialDataIfEmpty() {
        // Enforce 100% pure real user data from Firestore
    }

    // 1. FETCH PRODUCTS
    fun fetchProducts(onResult: (List<ProductModel>) -> Unit) {
        db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.let { p ->
                        if (p.id.isEmpty()) p.copy(id = doc.id) else p
                    }
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 1B. REALTIME LISTEN ALL PRODUCTS (BERANDA BARU DITAMBAHKAN)
    fun listenProducts(onResult: (List<ProductModel>) -> Unit): ListenerRegistration {
        return db.collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.let { p ->
                        if (p.id.isEmpty()) p.copy(id = doc.id) else p
                    }
                }
                onResult(list)
            }
    }

    // 1C. FETCH OR LISTEN SINGLE PRODUCT BY ID / TITLE
    fun fetchProductById(productId: String, onResult: (ProductModel?) -> Unit) {
        if (productId.isBlank()) {
            onResult(null)
            return
        }
        db.collection("products").document(productId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val p = doc.toObject(ProductModel::class.java)?.let { m ->
                        if (m.id.isEmpty()) m.copy(id = doc.id) else m
                    }
                    onResult(p)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun listenProductById(productId: String, onResult: (ProductModel?) -> Unit): ListenerRegistration {
        return db.collection("products").document(productId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null) return@addSnapshotListener
                if (!doc.exists()) {
                    onResult(null)
                    return@addSnapshotListener
                }
                val p = doc.toObject(ProductModel::class.java)?.let { m ->
                    if (m.id.isEmpty()) m.copy(id = doc.id) else m
                }
                onResult(p)
            }
    }

    fun findProductByTitle(title: String, onResult: (ProductModel?) -> Unit) {
        if (title.isBlank()) {
            onResult(null)
            return
        }
        db.collection("products").whereEqualTo("title", title).limit(1).get()
            .addOnSuccessListener { snap ->
                val doc = snap.documents.firstOrNull()
                val p = doc?.toObject(ProductModel::class.java)?.let { m ->
                    if (m.id.isEmpty()) m.copy(id = doc.id) else m
                }
                onResult(p)
            }
            .addOnFailureListener { onResult(null) }
    }

    // 2. FETCH AUCTIONS (ONE-SHOT)
    fun fetchAuctions(onResult: (List<AuctionModel>) -> Unit) {
        db.collection("auctions")
            .orderBy("endTimeMillis", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AuctionModel::class.java)?.let { model ->
                        if (model.id.isEmpty()) model.copy(id = doc.id) else model
                    }
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 2B. REALTIME LISTEN AUCTIONS
    fun listenAuctions(onResult: (List<AuctionModel>) -> Unit): ListenerRegistration {
        return db.collection("auctions")
            .orderBy("endTimeMillis", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AuctionModel::class.java)?.let { model ->
                        if (model.id.isEmpty()) model.copy(id = doc.id) else model
                    }
                }
                onResult(list)
            }
    }

    // 2C. REALTIME LISTEN SINGLE AUCTION
    fun listenAuctionById(auctionId: String, onResult: (AuctionModel?) -> Unit): ListenerRegistration {
        return db.collection("auctions").document(auctionId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) {
                    if (doc != null && !doc.exists()) onResult(null)
                    return@addSnapshotListener
                }
                val model = doc.toObject(AuctionModel::class.java)?.let { m ->
                    if (m.id.isEmpty()) m.copy(id = doc.id) else m
                }
                onResult(model)
            }
    }

    // 2D. REALTIME LISTEN AUCTION BIDS
    fun listenAuctionBids(auctionId: String, onResult: (List<BidModel>) -> Unit): ListenerRegistration {
        return db.collection("auctions").document(auctionId)
            .collection("bids")
            .orderBy("amountNumeric", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val bids = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(BidModel::class.java)?.let { b ->
                        if (b.id.isEmpty()) b.copy(id = doc.id) else b
                    }
                }
                onResult(bids)
            }
    }

    // 2E. PLACE BID ATOMICALLY (PREVENTS BIDDING ON FINISHED / UPCOMING AUCTIONS)
    fun placeBid(
        auctionId: String,
        bidAmount: Long,
        bidderId: String,
        bidderName: String,
        onComplete: (Boolean, String, Long) -> Unit
    ) {
        if (auctionId.isBlank()) {
            onComplete(false, "Data lelang tidak valid.", 0L)
            return
        }

        val auctionRef = db.collection("auctions").document(auctionId)
        val bidRef = auctionRef.collection("bids").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(auctionRef)
            if (!snapshot.exists()) {
                throw IllegalStateException("Lelang tidak ditemukan.")
            }

            val endTimeMillis = snapshot.getLong("endTimeMillis") ?: 0L
            val sellerId = snapshot.getString("sellerId") ?: ""
            val now = System.currentTimeMillis()
            if (sellerId.isNotBlank() && bidderId == sellerId) {
                throw IllegalStateException("Pembuat lelang tidak diperbolehkan memasang penawaran pada lelang sendiri.")
            }
            if (endTimeMillis <= 0L) {
                throw IllegalStateException("Lelang belum dimulai.")
            }
            if (now >= endTimeMillis) {
                throw IllegalStateException("Lelang sudah selesai, tidak dapat melakukan penawaran lagi.")
            }

            val currentBidNumeric = snapshot.getLong("currentBidNumeric") ?: 0L
            val bidsCount = (snapshot.getLong("bidsCount") ?: 0L).toInt()
            val increment = (snapshot.getLong("bidIncrementNumeric") ?: 25000L).coerceAtLeast(1000L)
            val minAllowed = if (bidsCount == 0) currentBidNumeric else currentBidNumeric + increment
            if (bidAmount < minAllowed) {
                val formattedMin = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                    .format(minAllowed).replace("Rp", "Rp ").replace(",00", "")
                throw IllegalStateException("Penawaran minimal $formattedMin")
            }

            // Anti-sniping: extend 2 minutes if bid placed in the last 2 minutes
            val updatedEndTime = if (endTimeMillis - now <= 120_000L) {
                now + 120_000L
            } else {
                endTimeMillis
            }

            val formattedBid = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                .format(bidAmount).replace("Rp", "Rp ").replace(",00", "")

            transaction.update(
                auctionRef,
                mapOf(
                    "currentBid" to formattedBid,
                    "currentBidNumeric" to bidAmount,
                    "bidsCount" to (bidsCount + 1),
                    "highestBidderName" to bidderName,
                    "highestBidderId" to bidderId,
                    "endTimeMillis" to updatedEndTime,
                    "participantIds" to com.google.firebase.firestore.FieldValue.arrayUnion(bidderId)
                )
            )

            val newBid = BidModel(
                id = bidRef.id,
                auctionId = auctionId,
                bidderId = bidderId,
                bidderName = bidderName,
                amountNumeric = bidAmount,
                amountFormatted = formattedBid,
                timestamp = now
            )
            transaction.set(bidRef, newBid)

            Pair(formattedBid, updatedEndTime)
        }.addOnSuccessListener { (formattedBid, updatedEndTime) ->
            onComplete(true, "Penawaran $formattedBid berhasil dipasang!", updatedEndTime)
        }.addOnFailureListener { e ->
            onComplete(false, e.message ?: "Gagal memasang penawaran.", 0L)
        }
    }

    // REALTIME LISTEN AUCTIONS PARTICIPATED BY SPECIFIC USER
    fun listenUserParticipatedAuctions(userId: String, onUpdate: (List<AuctionModel>) -> Unit): ListenerRegistration {
        return db.collection("auctions")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(AuctionModel::class.java)?.let { m ->
                        if (m.id.isEmpty()) m.copy(id = doc.id) else m
                    }
                }
                onUpdate(list)
            }
    }

    fun ensureAuctionParticipant(auctionId: String, userId: String) {
        if (auctionId.isBlank() || userId.isBlank()) return
        db.collection("auctions").document(auctionId)
            .update("participantIds", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
    }

    // FETCH AUCTIONS CREATED BY SPECIFIC USER
    fun fetchUserAuctions(userId: String, onResult: (List<AuctionModel>) -> Unit) {
        db.collection("auctions")
            .whereEqualTo("sellerId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(AuctionModel::class.java)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // FETCH PRODUCTS CREATED BY SPECIFIC USER
    fun fetchUserProducts(userId: String, onResult: (List<ProductModel>) -> Unit) {
        db.collection("products")
            .whereEqualTo("sellerId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.let { p ->
                        if (p.id.isEmpty()) p.copy(id = doc.id) else p
                    }
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // REALTIME LISTEN PRODUCTS CREATED BY SPECIFIC USER (PROFILE TAB VARIAN)
    fun listenUserProducts(userId: String, onResult: (List<ProductModel>) -> Unit): ListenerRegistration {
        return db.collection("products")
            .whereEqualTo("sellerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductModel::class.java)?.let { p ->
                        if (p.id.isEmpty()) p.copy(id = doc.id) else p
                    }
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
    }

    // CREATE NEW PRODUCT
    fun createProduct(product: ProductModel, onComplete: (Boolean) -> Unit) {
        val docRef = db.collection("products").document()
        val finalProduct = product.copy(id = docRef.id)

        docRef.set(finalProduct)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // DELETE PRODUCT
    fun deleteProduct(productId: String, onComplete: (Boolean) -> Unit) {
        db.collection("products").document(productId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // UPDATE PRODUCT PRICE AND STOCK
    fun updateProductPriceAndStock(
        productId: String,
        newPrice: String,
        newPriceNumeric: Long,
        newStock: Int,
        onComplete: (Boolean) -> Unit
    ) {
        db.collection("products").document(productId)
            .update(
                mapOf(
                    "price" to newPrice,
                    "priceNumeric" to newPriceNumeric,
                    "stock" to newStock.coerceAtLeast(0)
                )
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    private val videoExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // CREATE NEW AUCTION (Tries Firebase Storage for video, falls back to chunked Firestore subcollection)
    fun createAuction(auction: AuctionModel, onComplete: (Boolean) -> Unit) {
        val docRef = db.collection("auctions").document()
        val auctionId = docRef.id
        val baseAuction = auction.copy(id = auctionId)

        val localVideoFile = resolveReadableFile(auction.videoUri)
        if (localVideoFile == null) {
            docRef.set(baseAuction)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference.child("auction_videos/$auctionId.mp4")
        storageRef.putFile(Uri.fromFile(localVideoFile))
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: RuntimeException("Upload gagal")
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val finalAuction = baseAuction.copy(
                    videoUri = downloadUri.toString(),
                    videoChunksCount = 0
                )
                docRef.set(finalAuction)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener {
                uploadVideoChunksToFirestore(docRef, localVideoFile, baseAuction, onComplete)
            }
    }

    private fun resolveReadableFile(uriStr: String): File? {
        if (uriStr.isBlank()) return null
        val path = when {
            uriStr.startsWith("file://") -> Uri.parse(uriStr).path ?: uriStr.removePrefix("file://")
            uriStr.startsWith("/") -> uriStr
            else -> return null
        }
        val file = File(path)
        return if (file.exists() && file.canRead() && file.length() > 0L) file else null
    }

    private fun uploadVideoChunksToFirestore(
        docRef: com.google.firebase.firestore.DocumentReference,
        videoFile: File,
        baseAuction: AuctionModel,
        onComplete: (Boolean) -> Unit
    ) {
        val maxChunkedVideoBytes = 20L * 1024L * 1024L
        if (videoFile.length() > maxChunkedVideoBytes) {
            docRef.set(baseAuction.copy(videoChunksCount = 0))
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
            return
        }

        videoExecutor.execute {
            try {
                val chunkSize = 350 * 1024 // ~467 KB Base64 string per doc (< 1 MB Firestore doc limit)
                val encodedChunks = mutableListOf<String>()
                FileInputStream(videoFile).use { fis ->
                    val buffer = ByteArray(chunkSize)
                    while (true) {
                        val bytesRead = fis.read(buffer)
                        if (bytesRead <= 0) break
                        val slice = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                        encodedChunks.add(Base64.encodeToString(slice, Base64.NO_WRAP))
                    }
                }

                if (encodedChunks.isEmpty()) {
                    mainHandler.post {
                        docRef.set(baseAuction)
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { onComplete(false) }
                    }
                    return@execute
                }

                fun commitBatch(startIndex: Int) {
                    if (startIndex >= encodedChunks.size) {
                        val finalAuction = baseAuction.copy(videoChunksCount = encodedChunks.size)
                        docRef.set(finalAuction)
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { onComplete(false) }
                        return
                    }
                    val endIndex = minOf(startIndex + 8, encodedChunks.size)
                    val batch = db.batch()
                    for (idx in startIndex until endIndex) {
                        val chunkDocId = idx.toString().padStart(4, '0')
                        val chunkRef = docRef.collection("video_chunks").document(chunkDocId)
                        batch.set(
                            chunkRef,
                            mapOf(
                                "index" to idx,
                                "data" to encodedChunks[idx]
                            )
                        )
                    }
                    batch.commit()
                        .addOnSuccessListener { commitBatch(endIndex) }
                        .addOnFailureListener {
                            docRef.set(baseAuction.copy(videoChunksCount = 0))
                                .addOnSuccessListener { onComplete(true) }
                                .addOnFailureListener { onComplete(false) }
                        }
                }

                mainHandler.post { commitBatch(0) }
            } catch (_: Throwable) {
                mainHandler.post {
                    docRef.set(baseAuction.copy(videoChunksCount = 0))
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }
        }
    }

    fun downloadAuctionVideoToCache(
        auctionId: String,
        targetCacheFile: File,
        onComplete: (Boolean) -> Unit
    ) {
        if (auctionId.isBlank()) {
            onComplete(false)
            return
        }
        db.collection("auctions").document(auctionId)
            .collection("video_chunks")
            .orderBy("index", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onComplete(false)
                    return@addOnSuccessListener
                }
                videoExecutor.execute {
                    val tempFile = File(targetCacheFile.parentFile, "${targetCacheFile.name}.tmp")
                    try {
                        FileOutputStream(tempFile).use { fos ->
                            for (doc in snapshot.documents) {
                                val data = doc.getString("data") ?: continue
                                val bytes = Base64.decode(data, Base64.DEFAULT)
                                fos.write(bytes)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0L) {
                            if (targetCacheFile.exists()) targetCacheFile.delete()
                            val renamed = tempFile.renameTo(targetCacheFile)
                            if (!renamed) {
                                tempFile.copyTo(targetCacheFile, overwrite = true)
                                tempFile.delete()
                            }
                            mainHandler.post { onComplete(targetCacheFile.exists() && targetCacheFile.length() > 0L) }
                        } else {
                            tempFile.delete()
                            mainHandler.post { onComplete(false) }
                        }
                    } catch (_: Throwable) {
                        tempFile.delete()
                        mainHandler.post { onComplete(false) }
                    }
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // DELETE AUCTION
    fun deleteAuction(auctionId: String, onComplete: (Boolean) -> Unit) {
        db.collection("auctions").document(auctionId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // 3. FETCH FARM PROFILE
    fun fetchFarmProfile(farmId: String, onResult: (FarmProfileModel) -> Unit) {
        db.collection("farms").document(farmId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val farm = doc.toObject(FarmProfileModel::class.java)
                    if (farm != null) {
                        onResult(farm)
                        return@addOnSuccessListener
                    }
                }
                onResult(FarmProfileModel())
            }
            .addOnFailureListener {
                onResult(FarmProfileModel())
            }
    }

    // 3B. FETCH REAL USER / STORE PROFILE BY USER ID
    fun fetchUserProfileById(userId: String, onResult: (UserProfileModel?) -> Unit) {
        if (userId.isBlank()) {
            onResult(null)
            return
        }
        db.collection("user").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val name = doc.getString("name") ?: ""
                val farmName = doc.getString("farm_name")?.takeIf { it.isNotBlank() }
                    ?: if (name.isNotBlank()) "$name Farm" else "Guppy Farm"
                val desc = doc.getString("description") ?: ""
                val location = doc.getString("location") ?: ""
                val fullAddress = doc.getString("full_address") ?: location
                val rawAvatarUri = doc.getString("avatar_uri") ?: doc.getString("photo_url") ?: ""
                val avatarDataUrl = doc.getString("avatar_data_url") ?: ""
                val avatarUri = when {
                    rawAvatarUri.startsWith("data:image", ignoreCase = true) ||
                        rawAvatarUri.startsWith("http", ignoreCase = true) -> rawAvatarUri
                    rawAvatarUri.startsWith("file://") || rawAvatarUri.startsWith("/") -> {
                        val file = java.io.File(rawAvatarUri.removePrefix("file://"))
                        if (file.exists() && file.canRead()) rawAvatarUri else avatarDataUrl.ifBlank { rawAvatarUri }
                    }
                    else -> avatarDataUrl.ifBlank { rawAvatarUri }
                }
                val reviewsCount = (doc.getLong("total_reviews_count") ?: 0L).toInt()
                val rating = if (reviewsCount > 0) (doc.getDouble("average_rating") ?: 0.0) else 0.0
                val soldCount = (doc.getLong("total_sold_count") ?: 0L).toInt()
                val totalOrders = (doc.getLong("total_orders") ?: 0L).toInt()
                val ontimeOrders = (doc.getLong("ontime_orders_count") ?: 0L).toInt()
                val totalChats = (doc.getLong("total_chats") ?: 0L).toInt()
                val totalResponseMin = doc.getLong("total_response_time_minutes") ?: 0L
                val createdAt = doc.getLong("createdAt") ?: 0L

                db.collection("user").document(userId).collection("addresses").get()
                    .addOnSuccessListener { addrSnapshot ->
                        val addresses = addrSnapshot.toObjects(AddressModel::class.java)
                            .filterNot { it.id == "addr1" || it.id == "addr2" }
                        val farmAddr = addresses.find {
                            it.label.contains("Gudang", true) || it.label.contains("Farm", true) || it.label.contains("Toko", true)
                        } ?: addresses.find { it.isDefault } ?: addresses.firstOrNull()

                        val resolvedAddress = farmAddr?.fullAddress?.takeIf { it.isNotBlank() } ?: fullAddress
                        val resolvedShortLocation = location.takeIf { it.isNotBlank() }
                            ?: if (resolvedAddress.contains(",")) {
                                resolvedAddress.split(",").takeLast(2).joinToString(",").trim()
                            } else {
                                resolvedAddress
                            }

                        onResult(
                            UserProfileModel(
                                uid = userId,
                                name = name,
                                farmName = farmName,
                                description = desc,
                                location = resolvedShortLocation,
                                fullAddress = resolvedAddress,
                                avatarUri = avatarUri,
                                rating = rating,
                                totalReviewsCount = reviewsCount,
                                totalSoldCount = soldCount,
                                totalOrders = totalOrders,
                                ontimeOrdersCount = ontimeOrders,
                                totalChats = totalChats,
                                totalResponseTimeMinutes = totalResponseMin,
                                createdAt = createdAt,
                                lat = farmAddr?.lat ?: 0.0,
                                lng = farmAddr?.lng ?: 0.0
                            )
                        )
                    }
                    .addOnFailureListener {
                        onResult(
                            UserProfileModel(
                                uid = userId,
                                name = name,
                                farmName = farmName,
                                description = desc,
                                location = location,
                                fullAddress = fullAddress,
                                avatarUri = avatarUri,
                                rating = rating,
                                totalReviewsCount = reviewsCount,
                                totalSoldCount = soldCount,
                                totalOrders = totalOrders,
                                ontimeOrdersCount = ontimeOrders,
                                totalChats = totalChats,
                                totalResponseTimeMinutes = totalResponseMin,
                                createdAt = createdAt
                            )
                        )
                    }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // 3C. FIND REAL STORE USER BY NAME (FALLBACK WHEN ONLY STORE NAME IS PASSED)
    fun findUserProfileByStoreName(storeName: String, onResult: (UserProfileModel?) -> Unit) {
        if (storeName.isBlank()) {
            onResult(null)
            return
        }
        db.collection("user").whereEqualTo("farm_name", storeName).limit(1).get()
            .addOnSuccessListener { snap ->
                val first = snap.documents.firstOrNull()
                if (first != null) {
                    fetchUserProfileById(first.id, onResult)
                } else {
                    db.collection("user").whereEqualTo("name", storeName).limit(1).get()
                        .addOnSuccessListener { nameSnap ->
                            val byName = nameSnap.documents.firstOrNull()
                            if (byName != null) {
                                fetchUserProfileById(byName.id, onResult)
                            } else {
                                onResult(null)
                            }
                        }
                        .addOnFailureListener { onResult(null) }
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun mapRealAddresses(snapshot: com.google.firebase.firestore.QuerySnapshot): List<AddressModel> {
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(AddressModel::class.java)?.copy(id = doc.id)
        }.filterNot { it.id == "addr1" || it.id == "addr2" }
    }

    private fun resolveTargetUid(userId: String): String {
        val trimmed = userId.trim()
        return if (trimmed.isNotBlank() && trimmed != "user_default") {
            trimmed
        } else {
            auth.currentUser?.uid.orEmpty()
        }
    }

    // FETCH USER ADDRESSES (PURE REAL DATA, prefers server state with doc.id preserved)
    fun fetchUserAddresses(
        userId: String,
        preferServer: Boolean = true,
        onResult: (List<AddressModel>) -> Unit
    ) {
        val targetUid = resolveTargetUid(userId)
        if (targetUid.isBlank()) {
            onResult(emptyList())
            return
        }
        val colRef = db.collection("user").document(targetUid).collection("addresses")
        val primarySource = if (preferServer) {
            com.google.firebase.firestore.Source.SERVER
        } else {
            com.google.firebase.firestore.Source.DEFAULT
        }

        colRef.get(primarySource)
            .addOnSuccessListener { snapshot ->
                onResult(mapRealAddresses(snapshot))
            }
            .addOnFailureListener {
                colRef.get()
                    .addOnSuccessListener { snapshot ->
                        onResult(mapRealAddresses(snapshot))
                    }
                    .addOnFailureListener {
                        onResult(emptyList())
                    }
            }
    }

    // FETCH COURIER SETTINGS
    fun fetchCourierSettings(userId: String, onResult: (CourierSettingsModel) -> Unit) {
        val targetUid = resolveTargetUid(userId)
        if (targetUid.isBlank()) {
            onResult(CourierSettingsModel())
            return
        }
        db.collection("user").document(targetUid).collection("settings").document("couriers")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val settings = doc.toObject(CourierSettingsModel::class.java)
                    if (settings != null) {
                        onResult(settings)
                        return@addOnSuccessListener
                    }
                }
                onResult(CourierSettingsModel())
            }
            .addOnFailureListener {
                onResult(CourierSettingsModel())
            }
    }

    // SAVE COURIER SETTINGS
    fun saveCourierSettings(userId: String, settings: CourierSettingsModel, onComplete: (Boolean) -> Unit) {
        val targetUid = resolveTargetUid(userId)
        if (targetUid.isBlank()) {
            onComplete(false)
            return
        }
        db.collection("user").document(targetUid).collection("settings").document("couriers")
            .set(settings)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // FETCH PAYMENT SETTINGS
    fun fetchPaymentSettings(userId: String, onResult: (PaymentSettingsModel) -> Unit) {
        val targetUid = resolveTargetUid(userId)
        if (targetUid.isBlank()) {
            onResult(PaymentSettingsModel())
            return
        }
        db.collection("user").document(targetUid).collection("settings").document("payments")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val settings = doc.toObject(PaymentSettingsModel::class.java)
                    if (settings != null) {
                        onResult(settings)
                        return@addOnSuccessListener
                    }
                }
                onResult(PaymentSettingsModel())
            }
            .addOnFailureListener {
                onResult(PaymentSettingsModel())
            }
    }

    // SAVE PAYMENT SETTINGS
    fun savePaymentSettings(userId: String, settings: PaymentSettingsModel, onComplete: (Boolean) -> Unit) {
        val targetUid = resolveTargetUid(userId)
        if (targetUid.isBlank()) {
            onComplete(false)
            return
        }
        db.collection("user").document(targetUid).collection("settings").document("payments")
            .set(settings)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // FETCH REAL USER PAYMENT METHODS
    fun fetchUserPaymentMethods(userId: String, onResult: (List<PaymentMethodModel>) -> Unit) {
        val targetUid = resolveTargetUid(userId)
        if (targetUid.isBlank()) {
            onResult(emptyList())
            return
        }
        db.collection("user").document(targetUid).collection("payment_methods")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(PaymentMethodModel::class.java)?.let { m ->
                        if (m.id.isBlank()) m.copy(id = doc.id) else m
                    }
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // SAVE OR UPDATE REAL PAYMENT METHOD
    fun saveUserPaymentMethod(userId: String, method: PaymentMethodModel, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        val docRef = if (method.id.isNotEmpty()) {
            db.collection("user").document(currentUid).collection("payment_methods").document(method.id)
        } else {
            db.collection("user").document(currentUid).collection("payment_methods").document()
        }
        val finalItem = if (method.id.isEmpty()) method.copy(id = docRef.id) else method

        docRef.set(finalItem)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // UPDATE ALL PAYMENT METHODS IN BATCH
    fun updateAllPaymentMethods(userId: String, methods: List<PaymentMethodModel>, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        val batch = db.batch()

        methods.forEach { item ->
            val docRef = if (item.id.isNotEmpty()) {
                db.collection("user").document(currentUid).collection("payment_methods").document(item.id)
            } else {
                db.collection("user").document(currentUid).collection("payment_methods").document()
            }
            val finalItem = if (item.id.isEmpty()) item.copy(id = docRef.id) else item
            batch.set(docRef, finalItem)
        }

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // DELETE REAL PAYMENT METHOD
    fun deleteUserPaymentMethod(userId: String, methodId: String, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        db.collection("user").document(currentUid).collection("payment_methods").document(methodId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // UPDATE ALL ADDRESSES ATOMICALLY IN FIRESTORE BATCH
    fun updateAllAddresses(userId: String, addresses: List<AddressModel>, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        val batch = db.batch()

        addresses.forEach { addr ->
            val docRef = if (addr.id.isNotEmpty()) {
                db.collection("user").document(currentUid).collection("addresses").document(addr.id)
            } else {
                db.collection("user").document(currentUid).collection("addresses").document()
            }
            val finalAddr = if (addr.id.isEmpty()) addr.copy(id = docRef.id) else addr
            batch.set(docRef, finalAddr)
        }

        batch.commit()
            .addOnSuccessListener {
                syncUserFarmLocationToPublic(currentUid, addresses)
                onComplete(true)
            }
            .addOnFailureListener { onComplete(false) }
    }

    // SAVE OR UPDATE ADDRESS TO FIRESTORE
    fun addUserAddress(userId: String, address: AddressModel, onComplete: (Boolean) -> Unit) {
        val targetUid = auth.currentUser?.uid ?: userId
        val docRef = if (address.id.isNotEmpty()) {
            db.collection("user").document(targetUid).collection("addresses").document(address.id)
        } else {
            db.collection("user").document(targetUid).collection("addresses").document()
        }
        val finalAddr = if (address.id.isEmpty()) address.copy(id = docRef.id) else address

        docRef.set(finalAddr)
            .addOnSuccessListener {
                // Re-read current address subcollection and replace/merge finalAddr by ID so both farm & default shipping addresses stay accurate
                db.collection("user").document(targetUid).collection("addresses").get()
                    .addOnSuccessListener { snap ->
                        val combined = mapRealAddresses(snap).filterNot { it.id == finalAddr.id } + finalAddr
                        syncUserFarmLocationToPublic(targetUid, combined)
                    }
                    .addOnFailureListener {
                        syncUserFarmLocationToPublic(targetUid, listOf(finalAddr))
                    }
                onComplete(true)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreFarms", "addUserAddress: failed to save for uid=$targetUid", e)
                onComplete(false)
            }
    }

    // DELETE ADDRESS FROM FIRESTORE
    fun deleteUserAddress(userId: String, addressId: String, onComplete: (Boolean) -> Unit) {
        val targetUid = auth.currentUser?.uid ?: userId
        if (targetUid.isBlank() || targetUid == "user_default" || addressId.isBlank()) {
            onComplete(false)
            return
        }
        val colRef = db.collection("user").document(targetUid).collection("addresses")
        colRef.whereEqualTo("id", addressId).get()
            .addOnSuccessListener { matchedSnap ->
                val batch = db.batch()
                batch.delete(colRef.document(addressId))
                matchedSnap.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        syncUserFarmLocationToPublic(targetUid)
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        colRef.document(addressId).delete()
                            .addOnSuccessListener {
                                syncUserFarmLocationToPublic(targetUid)
                                onComplete(true)
                            }
                            .addOnFailureListener { onComplete(false) }
                    }
            }
            .addOnFailureListener {
                colRef.document(addressId).delete()
                    .addOnSuccessListener {
                        syncUserFarmLocationToPublic(targetUid)
                        onComplete(true)
                    }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    // PUBLISH CURRENT USER'S GUDANG / FARM ADDRESS TO PUBLIC user/{uid} & farms/{uid}
    fun syncUserFarmLocationToPublic(userId: String, knownAddresses: List<AddressModel>? = null) {
        val uid = auth.currentUser?.uid ?: userId
        if (uid.isBlank() || uid == "user_default") return

        val applyAddresses: (List<AddressModel>) -> Unit = { addresses ->
            val realAddresses = addresses.filterNot { it.id == "addr1" || it.id == "addr2" }
            val defaultShippingAddr = realAddresses.find { it.isDefault } ?: realAddresses.firstOrNull()
            val farmAddr = realAddresses.filter {
                it.label.contains("Gudang", true) || it.label.contains("Farm", true) || it.label.contains("Toko", true)
            }.sortedByDescending { it.isDefault }.firstOrNull()

            val userUpdates = hashMapOf<String, Any>(
                "farm_updated_at" to System.currentTimeMillis()
            )
            if (defaultShippingAddr != null) {
                userUpdates["location"] = defaultShippingAddr.fullAddress
                userUpdates["full_address"] = defaultShippingAddr.fullAddress
            } else {
                userUpdates["location"] = ""
                userUpdates["full_address"] = ""
            }

            if (farmAddr != null) {
                android.util.Log.d("FirestoreFarms", "syncUserFarmLocationToPublic: uid=$uid has_farm=TRUE label=${farmAddr.label} lat=${farmAddr.lat} lng=${farmAddr.lng}")
                val shortLoc = if (farmAddr.fullAddress.contains(",")) {
                    farmAddr.fullAddress.split(",").takeLast(2).joinToString(",").trim()
                } else {
                    farmAddr.fullAddress
                }
                val now = System.currentTimeMillis()
                val publicFarmFields = hashMapOf<String, Any>(
                    "uid" to uid,
                    "id" to uid,
                    "has_farm_location" to true,
                    "farm_address" to farmAddr.fullAddress,
                    "farm_location" to shortLoc,
                    "farm_lat" to farmAddr.lat,
                    "farm_lng" to farmAddr.lng,
                    "lat" to farmAddr.lat,
                    "lng" to farmAddr.lng,
                    "farm_owner_name" to farmAddr.recipientName,
                    "farm_label" to farmAddr.label,
                    "farm_updated_at" to now
                )
                userUpdates.putAll(publicFarmFields)

                // Read user's farm_name/name so farms/{uid} always carries complete public store metadata
                db.collection("user").document(uid).get()
                    .addOnSuccessListener { userDoc ->
                        val resolvedName = userDoc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                            ?: farmAddr.recipientName
                        val resolvedFarmName = userDoc.getString("farm_name")?.takeIf { it.isNotBlank() }
                            ?: if (resolvedName.contains("Farm", true)) resolvedName else "$resolvedName Farm"
                        val reviewsCount = userDoc.getLong("total_reviews_count") ?: 0L
                        val avgRating = userDoc.getDouble("average_rating") ?: 0.0

                        val completeFarmDoc = HashMap<String, Any>(publicFarmFields).apply {
                            put("name", resolvedName)
                            put("farm_name", resolvedFarmName)
                            put("total_reviews_count", reviewsCount)
                            put("average_rating", avgRating)
                        }
                        db.collection("farms").document(uid)
                            .set(completeFarmDoc, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                android.util.Log.d("FirestoreFarms", "SUCCESS: farms/$uid written — farm_name=${completeFarmDoc["farm_name"]} lat=${completeFarmDoc["lat"]} lng=${completeFarmDoc["lng"]}")
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("FirestoreFarms", "FAILED: farms/$uid write error", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.w("FirestoreFarms", "user/$uid get failed, using fallback publicFarmFields", e)
                        db.collection("farms").document(uid)
                            .set(publicFarmFields, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                android.util.Log.d("FirestoreFarms", "SUCCESS (fallback): farms/$uid written — lat=${publicFarmFields["lat"]} lng=${publicFarmFields["lng"]}")
                            }
                            .addOnFailureListener { e2 ->
                                android.util.Log.e("FirestoreFarms", "FAILED (fallback): farms/$uid write error", e2)
                            }
                    }
            } else {
                android.util.Log.d("FirestoreFarms", "syncUserFarmLocationToPublic: uid=$uid has_farm=FALSE — no Gudang/Farm/Toko label found in ${(if (knownAddresses != null) knownAddresses.size else 0)} addresses")
                userUpdates["has_farm_location"] = false
                db.collection("farms").document(uid)
                    .set(
                        mapOf(
                            "has_farm_location" to false,
                            "farm_updated_at" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            }

            db.collection("user").document(uid)
                .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener { e ->
                    android.util.Log.e("FirestoreFarms", "Failed to sync user/$uid farm fields", e)
                }
        }

        if (knownAddresses != null) {
            applyAddresses(knownAddresses)
        } else {
            // Read default cache first so just-committed writes publish immediately to farms/{uid} & user/{uid}
            val addrQuery = db.collection("user").document(uid).collection("addresses")
            addrQuery.get()
                .addOnSuccessListener { snap ->
                    applyAddresses(mapRealAddresses(snap))
                }
        }
    }

    // 4. CREATE ORDER IN FIRESTORE
    fun createOrder(order: OrderModel, onComplete: (Boolean) -> Unit) {
        createOrderWithId(order) { success, _ -> onComplete(success) }
    }

    fun createOrderWithId(order: OrderModel, onComplete: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: "guest"
        val docRef = db.collection("orders").document()
        val finalOrder = order.copy(
            id = order.id.ifBlank { docRef.id },
            docId = docRef.id,
            userId = currentUserId
        )

        docRef.set(finalOrder)
            .addOnSuccessListener {
                if (currentUserId != "guest") {
                    db.collection("user").document(currentUserId)
                        .update("total_orders", com.google.firebase.firestore.FieldValue.increment(1))
                }
                onComplete(true, docRef.id)
            }
            .addOnFailureListener {
                onComplete(false, "")
            }
    }

    fun confirmOrderPayment(
        orderDocId: String,
        productId: String,
        quantity: Int,
        trackingNumber: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (orderDocId.isBlank()) {
            onComplete(false)
            return
        }
        val orderRef = db.collection("orders").document(orderDocId)
        orderRef.update(
            mapOf(
                "status" to "Perlu Dikemas",
                "trackingNumber" to trackingNumber
            )
        ).addOnSuccessListener {
            if (productId.isNotBlank() && quantity > 0) {
                val prodRef = db.collection("products").document(productId)
                db.runTransaction { tx ->
                    val snap = tx.get(prodRef)
                    if (snap.exists()) {
                        val currStock = (snap.getLong("stock") ?: 1L).toInt()
                        val currSoldCount = (snap.getLong("soldCount") ?: (snap.getString("sold")?.toLongOrNull() ?: 0L)).toInt()
                        val newStock = (currStock - quantity).coerceAtLeast(0)
                        val newSold = currSoldCount + quantity
                        tx.update(
                            prodRef,
                            mapOf(
                                "stock" to newStock,
                                "soldCount" to newSold,
                                "sold" to newSold.toString()
                            )
                        )
                    }
                    null
                }
            }
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    // UPDATE ORDER STATUS AND TRACKING NUMBER IN FIRESTORE
    fun updateOrderStatusAndResi(orderDocIdOrId: String, newStatus: String, trackingNumber: String, onComplete: (Boolean) -> Unit) {
        if (orderDocIdOrId.isEmpty()) {
            onComplete(false)
            return
        }
        val docRef = db.collection("orders").document(orderDocIdOrId)
        docRef.get().addOnSuccessListener { snap ->
            if (snap.exists()) {
                docRef.update(mapOf("status" to newStatus, "trackingNumber" to trackingNumber))
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            } else {
                db.collection("orders").whereEqualTo("id", orderDocIdOrId).limit(1).get()
                    .addOnSuccessListener { q ->
                        val ref = q.documents.firstOrNull()?.reference
                        if (ref != null) {
                            ref.update(mapOf("status" to newStatus, "trackingNumber" to trackingNumber))
                                .addOnSuccessListener { onComplete(true) }
                                .addOnFailureListener { onComplete(false) }
                        } else {
                            onComplete(false)
                        }
                    }
                    .addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener { onComplete(false) }
    }

    // 5. FETCH USER ORDERS (sorted in Kotlin to avoid missing composite index failures)
    fun fetchUserOrders(onResult: (List<OrderModel>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: "guest"
        db.collection("orders")
            .whereEqualTo("userId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(OrderModel::class.java)?.let { o ->
                        if (o.docId.isEmpty()) o.copy(docId = doc.id) else o
                    }
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 5B. FETCH SELLER INCOMING ORDERS
    fun fetchSellerOrders(sellerId: String, onResult: (List<OrderModel>) -> Unit) {
        if (sellerId.isBlank() || sellerId == "user_default") {
            onResult(emptyList())
            return
        }
        db.collection("orders")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(OrderModel::class.java)?.let { o ->
                        if (o.docId.isEmpty()) o.copy(docId = doc.id) else o
                    }
                }.sortedByDescending { it.createdAt }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // CART OPERATIONS (REAL FIRESTORE SUBCOLLECTION)
    fun fetchCartItems(userId: String, onResult: (List<CartItemModel>) -> Unit) {
        val uid = auth.currentUser?.uid ?: userId
        if (uid.isBlank() || uid == "user_default") {
            onResult(emptyList())
            return
        }
        db.collection("user").document(uid).collection("cart")
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    doc.toObject(CartItemModel::class.java)?.let { item ->
                        if (item.id.isEmpty()) item.copy(id = doc.id) else item
                    }
                }.sortedByDescending { it.addedAt }
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun upsertCartItem(userId: String, item: CartItemModel, onComplete: (Boolean) -> Unit) {
        val uid = resolveTargetUid(userId)
        if (uid.isBlank() || item.productId.isBlank()) {
            onComplete(false)
            return
        }
        val docRef = db.collection("user").document(uid).collection("cart").document(item.productId)
        docRef.get()
            .addOnSuccessListener { existingDoc ->
                val existingQty = if (existingDoc.exists()) (existingDoc.getLong("quantity") ?: 0L).toInt() else 0
                val maxStock = item.stock.coerceAtLeast(1)
                val mergedQty = (existingQty + item.quantity).coerceIn(1, maxStock)
                val finalItem = item.copy(
                    id = item.productId,
                    quantity = mergedQty,
                    addedAt = System.currentTimeMillis()
                )
                docRef.set(finalItem)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener {
                val finalItem = item.copy(id = item.productId)
                docRef.set(finalItem)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    fun updateCartItemQuantity(userId: String, itemId: String, newQuantity: Int, onComplete: (Boolean) -> Unit = {}) {
        val uid = resolveTargetUid(userId)
        if (uid.isBlank() || itemId.isBlank() || newQuantity < 1) {
            onComplete(false)
            return
        }
        db.collection("user").document(uid).collection("cart").document(itemId)
            .update("quantity", newQuantity)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteCartItems(userId: String, itemIds: List<String>, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: userId
        val cleanIds = itemIds.filter { it.isNotBlank() }.distinct()
        if (uid.isBlank() || cleanIds.isEmpty()) {
            onComplete(false)
            return
        }
        val batch = db.batch()
        cleanIds.forEach { id ->
            batch.delete(db.collection("user").document(uid).collection("cart").document(id))
        }
        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // FETCH REAL FARMS FROM FIRESTORE (Reads directly from public top-level 'farms' collection)
    fun fetchRealFarms(onResult: (List<FarmModel>) -> Unit) {
        val farmTask = db.collection("farms").get()
        val productTask = db.collection("products").get()

        Tasks.whenAllComplete(farmTask, productTask)
            .addOnCompleteListener {
                val farmDocs = if (farmTask.isSuccessful) farmTask.result?.documents.orEmpty() else emptyList()
                val productDocs = if (productTask.isSuccessful) productTask.result?.documents.orEmpty() else emptyList()
                android.util.Log.d("FirestoreFarms", "fetchRealFarms: farmDocs=${farmDocs.size}, productDocs=${productDocs.size}")
                buildFarmsFromSnapshots(emptyList(), farmDocs, productDocs, onResult)
            }
    }

    private fun parseFarmModelFromSnapshot(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        defaultHasLocation: Boolean,
        countsBySeller: Map<String, Int>
    ): FarmModel? {
        val uid = doc.id
        if (uid.isBlank() || uid == "user_default" || uid.startsWith("dummy", ignoreCase = true)) {
            android.util.Log.d("FirestoreFarms", "skip doc uid=$uid (invalid/dummy uid)")
            return null
        }

        val hasLocation = doc.getBoolean("has_farm_location") ?: defaultHasLocation
        if (!hasLocation) {
            android.util.Log.d("FirestoreFarms", "skip doc uid=$uid (has_farm_location=false)")
            return null
        }

        val rawFarmName = doc.getString("farm_name")?.trim().orEmpty()
        val rawOwner = doc.getString("farm_owner_name")?.trim().orEmpty()
            .ifBlank { doc.getString("name")?.trim().orEmpty() }
        val rawAddress = doc.getString("farm_address")?.trim().orEmpty()
            .ifBlank { doc.getString("full_address")?.trim().orEmpty() }
        val shortLoc = doc.getString("farm_location")?.trim().orEmpty()
        val lat = doc.getDouble("farm_lat") ?: doc.getDouble("lat") ?: 0.0
        val lng = doc.getDouble("farm_lng") ?: doc.getDouble("lng") ?: 0.0

        val resolvedFarmName = when {
            rawFarmName.isNotBlank() -> rawFarmName
            rawOwner.contains("Farm", true) || rawOwner.contains("Guppy", true) -> rawOwner
            rawOwner.isNotBlank() -> "$rawOwner Farm"
            else -> "Guppy Farm"
        }

        // Only exclude legacy placeholder name if it slipped into DB without a distinct real farm profile
        if (resolvedFarmName.equals("Marcello Guppy Farm", ignoreCase = true)) {
            android.util.Log.d("FirestoreFarms", "skip doc uid=$uid (legacy placeholder name='$resolvedFarmName')")
            return null
        }

        val farmLocation = shortLoc.ifBlank {
            if (rawAddress.contains(",")) {
                rawAddress.split(",").takeLast(2).joinToString(",").trim()
            } else {
                rawAddress.ifBlank { "Indonesia" }
            }
        }

        val seed = (uid.hashCode() and 0xFF) / 255.0 - 0.5
        val validLat = if (lat != 0.0) lat else -7.2800 + seed * 0.02
        val validLng = if (lng != 0.0) lng else 112.7900 + seed * 0.02

        val reviewsCount = (doc.getLong("total_reviews_count") ?: 0L).toInt()
        val avgRating = if (reviewsCount > 0) (doc.getDouble("average_rating") ?: 0.0) else 0.0

        return FarmModel(
            id = uid,
            farmName = resolvedFarmName,
            ownerName = rawOwner.ifBlank { "Pemilik Farm" },
            location = farmLocation,
            distanceKm = "Lokasi Gudang",
            rating = avgRating,
            variantsCount = countsBySeller[uid] ?: 0,
            lat = validLat,
            lng = validLng
        )
    }

    private fun buildFarmsFromSnapshots(
        userDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        farmDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        productDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        onResult: (List<FarmModel>) -> Unit
    ) {
        val countsBySeller = mutableMapOf<String, Int>()
        for (pDoc in productDocs) {
            val sellerId = pDoc.getString("sellerId").orEmpty()
            if (sellerId.isNotBlank()) {
                countsBySeller[sellerId] = (countsBySeller[sellerId] ?: 0) + 1
            }
        }

        val farmsList = mutableListOf<FarmModel>()
        val processedUids = mutableSetOf<String>()

        // 1. Primary cross-account source: top-level public "farms" collection
        for (doc in farmDocs) {
            processedUids.add(doc.id)
            parseFarmModelFromSnapshot(doc, defaultHasLocation = true, countsBySeller)?.let { farm ->
                farmsList.add(farm)
                android.util.Log.d("FirestoreFarms", "included farm uid=${farm.id} name='${farm.farmName}' lat=${farm.lat} lng=${farm.lng}")
            }
        }

        // 2. Secondary fallback: any user doc with has_farm_location = true not yet in farms
        for (uDoc in userDocs) {
            if (processedUids.contains(uDoc.id)) continue
            parseFarmModelFromSnapshot(uDoc, defaultHasLocation = false, countsBySeller)?.let { farm ->
                farmsList.add(farm)
                android.util.Log.d("FirestoreFarms", "included fallback user farm uid=${farm.id} name='${farm.farmName}' lat=${farm.lat} lng=${farm.lng}")
            }
        }

        onResult(farmsList.sortedBy { it.farmName.lowercase(Locale.getDefault()) })
        android.util.Log.d("FirestoreFarms", "buildFarmsFromSnapshots: result=${farmsList.size} farms (from ${farmDocs.size} farmDocs, ${userDocs.size} userDocs)")
    }

    // REALTIME LISTEN FARMS (Listens live to public 'farms' & 'products' across accounts, exactly like auctions)
    fun listenRealFarms(onResult: (List<FarmModel>) -> Unit): List<ListenerRegistration> {
        var latestFarms: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
        var latestProducts: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

        val emitLatest = {
            buildFarmsFromSnapshots(emptyList(), latestFarms, latestProducts, onResult)
        }

        val farmReg = db.collection("farms").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreFarms", "Listener error on 'farms' collection", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                android.util.Log.d("FirestoreFarms", "listenRealFarms: 'farms' snapshot changed — ${snapshot.documents.size} docs")
                latestFarms = snapshot.documents
                emitLatest()
            }
        }

        val productReg = db.collection("products").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreFarms", "Listener error on 'products' collection", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                latestProducts = snapshot.documents
                emitLatest()
            }
        }

        // Initial fetch guarantees fresh state right away
        fetchRealFarms(onResult)

        return listOf(farmReg, productReg)
    }

    fun buildCsChatRoomId(userId: String): String = "cs_$userId"

    fun buildStoreChatRoomId(buyerId: String, storeId: String, storeName: String): String {
        if (storeId == CS_STORE_ID || storeName.equals(CS_STORE_NAME, ignoreCase = true)) {
            return buildCsChatRoomId(buyerId)
        }
        if (storeId.isNotBlank()) {
            return "dm_${buyerId}_${storeId}"
        }
        val slug = storeName.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "toko" }
        return "dm_${buyerId}_${slug}"
    }

    fun ensureCsChatRoom(userId: String, userName: String, onComplete: () -> Unit = {}) {
        if (userId.isBlank()) {
            onComplete()
            return
        }
        val roomId = buildCsChatRoomId(userId)
        val roomRef = db.collection("chats").document(roomId)
        roomRef.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val now = System.currentTimeMillis()
                    val displayName = userName.ifBlank { "Sahabat Guppy" }
                    val welcomeText = "Halo $displayName, ada yang bisa CS bantu terkait transaksi atau lelang?"
                    val room = ChatRoomModel(
                        id = roomId,
                        participants = listOf(userId, CS_STORE_ID),
                        buyerId = userId,
                        buyerName = displayName,
                        storeId = CS_STORE_ID,
                        storeName = CS_STORE_NAME,
                        storeLocation = "Pusat Bantuan GuppyMarket",
                        lastMessage = welcomeText,
                        lastMessageTime = now,
                        lastSenderId = CS_STORE_ID,
                        unreadCounts = mapOf(userId to 0L),
                        isOrderRelated = false,
                        isCsSupport = true
                    )
                    val msgRef = roomRef.collection("messages").document()
                    val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                    val welcomeMsg = ChatMessageModel(
                        id = msgRef.id,
                        senderId = CS_STORE_ID,
                        senderName = CS_STORE_NAME,
                        messageText = welcomeText,
                        isSeller = true,
                        timestamp = now,
                        formattedTime = timeFormat.format(java.util.Date(now))
                    )
                    val batch = db.batch()
                    batch.set(roomRef, room)
                    batch.set(msgRef, welcomeMsg)
                    batch.commit().addOnCompleteListener { onComplete() }
                } else {
                    onComplete()
                }
            }
            .addOnFailureListener { onComplete() }
    }

    fun listenUserChatRooms(userId: String, onUpdate: (List<ChatRoomModel>) -> Unit): ListenerRegistration {
        return db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val rooms = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatRoomModel::class.java)?.let { r ->
                        if (r.id.isEmpty()) r.copy(id = doc.id) else r
                    }
                }.filterNot { it.deletedFor.contains(userId) }
                    .sortedWith(
                        compareByDescending<ChatRoomModel> { it.lastMessageTime }
                            .thenByDescending { it.isCsSupport }
                    )
                onUpdate(rooms)
            }
    }

    fun deleteChatRoomsForUser(userId: String, chatRoomIds: List<String>, onComplete: (Boolean) -> Unit) {
        val cleanIds = chatRoomIds.filter { it.isNotBlank() }.distinct()
        if (userId.isBlank() || cleanIds.isEmpty()) {
            onComplete(false)
            return
        }
        val now = System.currentTimeMillis()
        val batch = db.batch()
        cleanIds.forEach { roomId ->
            val roomRef = db.collection("chats").document(roomId)
            batch.update(
                roomRef,
                mapOf(
                    "deletedFor" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                    "deletedAtBy.$userId" to now,
                    "unreadCounts.$userId" to 0L
                )
            )
        }
        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun fetchChatRoomDeletedAt(chatRoomId: String, userId: String, onResult: (Long) -> Unit) {
        if (chatRoomId.isBlank() || userId.isBlank()) {
            onResult(0L)
            return
        }
        db.collection("chats").document(chatRoomId).get()
            .addOnSuccessListener { doc ->
                val deletedAt = (doc.get("deletedAtBy.$userId") as? Long) ?: 0L
                onResult(deletedAt)
            }
            .addOnFailureListener {
                onResult(0L)
            }
    }

    /**
     * Deletes user profile document, all subcollections (addresses, payment_methods, settings),
     * public farm entry, seller's products & auctions, and hides user's chat rooms before Auth deletion.
     */
    fun deleteUserAccountData(userId: String, onComplete: (Boolean, String?) -> Unit) {
        if (userId.isBlank() || userId == "user_default") {
            onComplete(false, "UID pengguna tidak valid")
            return
        }

        val userRef = db.collection("user").document(userId)
        val addressesTask = userRef.collection("addresses").get()
        val paymentsTask = userRef.collection("payment_methods").get()
        val settingsTask = userRef.collection("settings").get()
        val productsTask = db.collection("products").whereEqualTo("sellerId", userId).get()
        val auctionsTask = db.collection("auctions").whereEqualTo("sellerId", userId).get()
        val chatsTask = db.collection("chats").whereArrayContains("participants", userId).get()

        Tasks.whenAllComplete(
            addressesTask,
            paymentsTask,
            settingsTask,
            productsTask,
            auctionsTask,
            chatsTask
        ).addOnCompleteListener {
            val refsToDelete = mutableListOf<DocumentReference>()
            listOf(addressesTask, paymentsTask, settingsTask, productsTask, auctionsTask).forEach { task ->
                if (task.isSuccessful) {
                    task.result?.documents?.forEach { doc ->
                        refsToDelete.add(doc.reference)
                    }
                }
            }
            refsToDelete.add(db.collection("farms").document(userId))

            val chatRefsToHide = mutableListOf<DocumentReference>()
            if (chatsTask.isSuccessful) {
                chatsTask.result?.documents?.forEach { doc ->
                    chatRefsToHide.add(doc.reference)
                }
            }

            val commitTasks = mutableListOf<Task<*>>()
            val now = System.currentTimeMillis()

            // Commit deletions in chunks safely under Firestore's 500-op WriteBatch limit
            refsToDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                commitTasks.add(batch.commit())
            }

            chatRefsToHide.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref ->
                    batch.update(
                        ref,
                        mapOf(
                            "deletedFor" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                            "deletedAtBy.$userId" to now,
                            "unreadCounts.$userId" to 0L
                        )
                    )
                }
                commitTasks.add(batch.commit())
            }

            // Always delete parent user document explicitly
            val deleteParentAndVerify = {
                userRef.delete()
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { e -> onComplete(false, e.localizedMessage) }
            }

            if (commitTasks.isEmpty()) {
                deleteParentAndVerify()
            } else {
                Tasks.whenAllComplete(commitTasks)
                    .addOnCompleteListener {
                        deleteParentAndVerify()
                    }
            }
        }
    }

    fun markChatRoomAsRead(chatRoomId: String, userId: String) {
        if (chatRoomId.isBlank() || userId.isBlank()) return
        db.collection("chats").document(chatRoomId)
            .update("unreadCounts.$userId", 0L)
    }

    // 6. LIVE CHAT FIRESTORE REALTIME LISTENER
    fun listenToChatMessages(chatRoomId: String, onUpdate: (List<ChatMessageModel>) -> Unit): ListenerRegistration {
        return db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessageModel::class.java)?.let { m ->
                        if (m.id.isEmpty()) m.copy(id = doc.id) else m
                    }
                }
                onUpdate(messages)
            }
    }

    // 7. SEND CHAT MESSAGE & SYNC ROOM METADATA
    fun sendChatMessage(
        chatRoomId: String,
        senderId: String,
        senderName: String,
        buyerId: String,
        buyerName: String,
        storeId: String,
        storeName: String,
        storeLocation: String,
        isCsSupport: Boolean,
        messageText: String,
        onComplete: (Boolean) -> Unit
    ) {
        val currentUid = senderId.ifBlank { auth.currentUser?.uid ?: "guest" }
        val now = System.currentTimeMillis()
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val roomRef = db.collection("chats").document(chatRoomId)
        val msgRef = roomRef.collection("messages").document()

        val resolvedBuyerId = buyerId.ifBlank { currentUid }
        val resolvedStoreId = storeId.ifBlank {
            if (isCsSupport) CS_STORE_ID else "store_${storeName.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "_")}"
        }
        val isSenderSeller = !isCsSupport && currentUid == resolvedStoreId
        val recipientId = if (currentUid == resolvedBuyerId) resolvedStoreId else resolvedBuyerId

        val chatMessage = ChatMessageModel(
            id = msgRef.id,
            senderId = currentUid,
            senderName = senderName.ifBlank { "Saya" },
            messageText = messageText,
            isSeller = isSenderSeller,
            timestamp = now,
            formattedTime = timeFormat.format(java.util.Date(now))
        )

        val orderKeyword = messageText.contains("pesanan", true) ||
            messageText.contains("order", true) ||
            messageText.contains("resi", true) ||
            messageText.contains("transaksi", true)

        val participants = listOf(resolvedBuyerId, resolvedStoreId).filter { it.isNotBlank() }.distinct()

        roomRef.get().addOnSuccessListener { existing ->
            val currentUnread = (existing.get("unreadCounts.$recipientId") as? Long) ?: 0L
            val existingIsOrder = existing.getBoolean("isOrderRelated") ?: false

            val roomData = hashMapOf<String, Any>(
                "id" to chatRoomId,
                "participants" to participants,
                "buyerId" to resolvedBuyerId,
                "buyerName" to buyerName.ifBlank { senderName.ifBlank { "Pembeli" } },
                "storeId" to resolvedStoreId,
                "storeName" to storeName.ifBlank { if (isCsSupport) CS_STORE_NAME else "Toko Guppy" },
                "storeLocation" to storeLocation,
                "lastMessage" to messageText,
                "lastMessageTime" to now,
                "lastSenderId" to currentUid,
                "isOrderRelated" to (existingIsOrder || orderKeyword),
                "isCsSupport" to isCsSupport,
                "unreadCounts.$currentUid" to 0L,
                "deletedFor" to com.google.firebase.firestore.FieldValue.arrayRemove(currentUid, recipientId)
            )
            if (recipientId.isNotBlank() && recipientId != currentUid && !isCsSupport) {
                roomData["unreadCounts.$recipientId"] = currentUnread + 1L
            }

            val batch = db.batch()
            if (existing.exists()) {
                batch.update(roomRef, roomData)
            } else {
                val initUnread = mutableMapOf<String, Long>(currentUid to 0L)
                if (recipientId.isNotBlank() && recipientId != currentUid && !isCsSupport) {
                    initUnread[recipientId] = 1L
                }
                val newRoom = ChatRoomModel(
                    id = chatRoomId,
                    participants = participants,
                    buyerId = resolvedBuyerId,
                    buyerName = buyerName.ifBlank { senderName.ifBlank { "Pembeli" } },
                    storeId = resolvedStoreId,
                    storeName = storeName.ifBlank { if (isCsSupport) CS_STORE_NAME else "Toko Guppy" },
                    storeLocation = storeLocation,
                    lastMessage = messageText,
                    lastMessageTime = now,
                    lastSenderId = currentUid,
                    unreadCounts = initUnread,
                    isOrderRelated = orderKeyword,
                    isCsSupport = isCsSupport
                )
                batch.set(roomRef, newRoom)
            }
            batch.set(msgRef, chatMessage)
            batch.commit()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }.addOnFailureListener {
            onComplete(false)
        }
    }
}
