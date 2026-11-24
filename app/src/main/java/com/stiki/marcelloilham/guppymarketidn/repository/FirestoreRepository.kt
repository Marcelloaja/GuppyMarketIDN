package com.stiki.marcelloilham.guppymarketidn.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stiki.marcelloilham.guppymarketidn.R

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    data class ProductModel(
        val id: String = "",
        val sellerId: String = "",
        val title: String = "",
        val seller: String = "",
        val price: String = "",
        val unit: String = "per ekor",
        val sold: String = "0",
        val imageRes: Int = R.drawable.bg_image_grid,
        val category: String = "Kontes",
        val strain: String = "Semua",
        val priceNumeric: Long = 0L,
        val createdAt: Long = System.currentTimeMillis()
    )

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
        val quarantine: String = "14 hari di farm"
    )

    data class OrderModel(
        val id: String = "",
        val userId: String = "",
        val productName: String = "",
        val sellerName: String = "Bekasi Aqua Farm",
        val quantity: Int = 1,
        val totalAmount: Long = 0L,
        val shippingCost: Long = 0L,
        val courier: String = "Kurir Instan",
        val paymentMethod: String = "Transfer Bank (VA)",
        val status: String = "Dikirim",
        val trackingNumber: String = "JNE Express JP8841",
        val createdAt: Long = System.currentTimeMillis()
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

    data class FarmProfileModel(
        val farmId: String = "farm_jakarta",
        val farmName: String = "Jakarta Guppy House",
        val ownerName: String = "Marcello Aja",
        val location: String = "Jakarta Selatan · 2,4 km",
        val rating: Double = 4.9,
        val totalReviewsCount: Int = 186,
        val totalVariantsCount: Int = 18,
        val totalSoldsCount: Int = 1240,
        val followersCount: Int = 2400,
        val establishedYearsText: String = "4 thn",
        val responseTimeText: String = "10 menit",
        val onTimeDeliveryRateText: String = "98%",
        val guaranteeText: String = "AMAN",
        val description: String = "Spesialis guppy kontes berkualitas tinggi sejak 2021. Fokus pada pemurnian galur murni (pure line) dengan standar penilaian nasional. Melayani pengiriman ke seluruh Indonesia dengan garansi ikan hidup sampai tujuan."
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
                val list = snapshot.toObjects(ProductModel::class.java)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 2. FETCH AUCTIONS
    fun fetchAuctions(onResult: (List<AuctionModel>) -> Unit) {
        db.collection("auctions")
            .orderBy("endTimeMillis", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(AuctionModel::class.java)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
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
                val list = snapshot.toObjects(ProductModel::class.java)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
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
    fun updateProductPriceAndStock(productId: String, newPrice: String, newStock: String, onComplete: (Boolean) -> Unit) {
        db.collection("products").document(productId)
            .update(
                mapOf(
                    "price" to newPrice,
                    "sold" to newStock
                )
            )
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // CREATE NEW AUCTION
    fun createAuction(auction: AuctionModel, onComplete: (Boolean) -> Unit) {
        val docRef = db.collection("auctions").document()
        val finalAuction = auction.copy(id = docRef.id)

        docRef.set(finalAuction)
            .addOnSuccessListener {
                onComplete(true)
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

    // 4. CREATE ORDER IN FIRESTORE
    fun createOrder(order: OrderModel, onComplete: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: "guest"
        val orderWithUser = order.copy(userId = currentUserId)
        val docRef = db.collection("orders").document()
        val finalOrder = orderWithUser.copy(id = docRef.id)

        docRef.set(finalOrder)
            .addOnSuccessListener {
                if (currentUserId != "guest") {
                    db.collection("user").document(currentUserId)
                        .update("total_orders", com.google.firebase.firestore.FieldValue.increment(1))
                }
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // 5. FETCH USER ORDERS
    fun fetchUserOrders(onResult: (List<OrderModel>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: "guest"
        db.collection("orders")
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(OrderModel::class.java)
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // 6. LIVE CHAT FIRESTORE REALTIME LISTENER
    fun listenToChatMessages(chatRoomId: String, onUpdate: (List<ChatMessageModel>) -> Unit) {
        db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val messages = snapshot.toObjects(ChatMessageModel::class.java)
                onUpdate(messages)
            }
    }

    // 7. SEND CHAT MESSAGE
    fun sendChatMessage(chatRoomId: String, messageText: String, onComplete: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: "buyer1"
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val msgDoc = db.collection("chats")
            .document(chatRoomId)
            .collection("messages")
            .document()

        val chatMessage = ChatMessageModel(
            id = msgDoc.id,
            senderId = currentUserId,
            senderName = "Saya",
            messageText = messageText,
            isSeller = false,
            timestamp = System.currentTimeMillis(),
            formattedTime = timeFormat.format(java.util.Date())
        )

        msgDoc.set(chatMessage)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}
