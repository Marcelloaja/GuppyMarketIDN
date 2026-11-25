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

    data class AddressModel(
        val id: String = "",
        val label: String = "Rumah",
        val recipientName: String = "Marcello Aja",
        val phone: String = "0812-3456-7890",
        val fullAddress: String = "Jl. Raya Cibubur No. 14, RT 03/RW 05, Bekasi Selatan, Kota Bekasi, Jawa Barat 17147",
        @field:JvmField
        val isDefault: Boolean = false,
        val notesForCourier: String = ""
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
        val withdrawalAccount: String = ""
    )

    data class PaymentMethodModel(
        val id: String = "",
        val type: String = "Bank", // "Bank" or "E-Wallet"
        val providerName: String = "BCA",
        val accountNumber: String = "",
        val accountOwner: String = "",
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

    // FETCH USER ADDRESSES (PURE REAL DATA)
    fun fetchUserAddresses(userId: String, onResult: (List<AddressModel>) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        val colRef = db.collection("user").document(currentUid).collection("addresses")

        colRef.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(AddressModel::class.java)
                val realList = list.filterNot { it.id == "addr1" || it.id == "addr2" }
                onResult(realList)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // FETCH COURIER SETTINGS
    fun fetchCourierSettings(userId: String, onResult: (CourierSettingsModel) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        db.collection("user").document(currentUid).collection("settings").document("couriers")
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
        val currentUid = auth.currentUser?.uid ?: userId
        db.collection("user").document(currentUid).collection("settings").document("couriers")
            .set(settings)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // FETCH PAYMENT SETTINGS
    fun fetchPaymentSettings(userId: String, onResult: (PaymentSettingsModel) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        db.collection("user").document(currentUid).collection("settings").document("payments")
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
        val currentUid = auth.currentUser?.uid ?: userId
        db.collection("user").document(currentUid).collection("settings").document("payments")
            .set(settings)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // FETCH REAL USER PAYMENT METHODS
    fun fetchUserPaymentMethods(userId: String, onResult: (List<PaymentMethodModel>) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: userId
        db.collection("user").document(currentUid).collection("payment_methods")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.toObjects(PaymentMethodModel::class.java)
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
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // SAVE OR UPDATE ADDRESS TO FIRESTORE
    fun addUserAddress(userId: String, address: AddressModel, onComplete: (Boolean) -> Unit) {
        val docRef = if (address.id.isNotEmpty()) {
            db.collection("user").document(userId).collection("addresses").document(address.id)
        } else {
            db.collection("user").document(userId).collection("addresses").document()
        }
        val finalAddr = if (address.id.isEmpty()) address.copy(id = docRef.id) else address

        docRef.set(finalAddr)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // DELETE ADDRESS FROM FIRESTORE
    fun deleteUserAddress(userId: String, addressId: String, onComplete: (Boolean) -> Unit) {
        db.collection("user").document(userId).collection("addresses").document(addressId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
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
