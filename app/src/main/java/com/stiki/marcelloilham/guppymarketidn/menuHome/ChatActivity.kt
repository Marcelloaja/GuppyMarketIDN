package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.ChatNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var etChatMessage: EditText
    private lateinit var btnSendMessage: ImageView
    private lateinit var layoutDynamicMessages: LinearLayout
    private lateinit var scrollChatMessages: NestedScrollView
    private var tvChatEmptyState: TextView? = null

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var messagesListener: ListenerRegistration? = null

    private var currentUid: String = ""
    private var currentUserName: String = "Saya"
    private var chatRoomId: String = ""
    private var storeId: String = ""
    private var storeName: String = FirestoreRepository.CS_STORE_NAME
    private var storeLocation: String = ""
    private var buyerId: String = ""
    private var buyerName: String = ""
    private var isCsSupport: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val topBar = findViewById<View>(R.id.topBarChat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBar.updatePadding(
                top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
            )
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackChat)
        btnBack.setOnClickListener { finish() }

        initViews()
        resolveChatContext()
        bindHeaderAndStoreCard()
        bindAttachedProductIfPresent()
        setupButtons()
        setupQuickReplies()
    }

    override fun onStart() {
        super.onStart()
        setupRealtimeChatListener()
    }

    override fun onResume() {
        super.onResume()
        ChatNotificationHelper.activeChatRoomId = chatRoomId
        ChatNotificationHelper.cancelRoomNotification(this, chatRoomId)
    }

    override fun onPause() {
        super.onPause()
        if (ChatNotificationHelper.activeChatRoomId == chatRoomId) {
            ChatNotificationHelper.activeChatRoomId = null
        }
    }

    override fun onStop() {
        super.onStop()
        messagesListener?.remove()
        messagesListener = null
    }

    private fun initViews() {
        etChatMessage = findViewById(R.id.etChatMessage)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        layoutDynamicMessages = findViewById(R.id.layoutDynamicChatMessages)
        scrollChatMessages = findViewById(R.id.scrollChatMessages)
        tvChatEmptyState = findViewById(R.id.tvChatEmptyState)
    }

    private fun resolveChatContext() {
        val currentUser = firebaseAuth.currentUser
        currentUid = currentUser?.uid ?: "guest"

        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        currentUserName = prefs.getString("name", null)?.takeIf { it.isNotBlank() }
            ?: currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")
            ?: "Pengguna GuppyMarket"

        storeId = intent.getStringExtra("EXTRA_STORE_ID") ?: ""
        storeName = intent.getStringExtra("EXTRA_STORE_NAME") ?: FirestoreRepository.CS_STORE_NAME
        storeLocation = intent.getStringExtra("EXTRA_STORE_LOCATION") ?: ""
        isCsSupport = intent.getBooleanExtra("EXTRA_IS_CS", false) ||
            storeId == FirestoreRepository.CS_STORE_ID ||
            storeName.equals(FirestoreRepository.CS_STORE_NAME, ignoreCase = true)

        buyerId = intent.getStringExtra("EXTRA_BUYER_ID")?.takeIf { it.isNotBlank() } ?: currentUid
        buyerName = intent.getStringExtra("EXTRA_BUYER_NAME")?.takeIf { it.isNotBlank() } ?: currentUserName

        val passedRoomId = intent.getStringExtra("EXTRA_CHAT_ROOM_ID") ?: ""
        chatRoomId = if (passedRoomId.isNotBlank()) {
            passedRoomId
        } else if (isCsSupport) {
            FirestoreRepository.buildCsChatRoomId(currentUid)
        } else {
            FirestoreRepository.buildStoreChatRoomId(buyerId, storeId, storeName)
        }

        if (isCsSupport && currentUser != null) {
            FirestoreRepository.ensureCsChatRoom(currentUid, currentUserName)
        }
    }

    private fun bindHeaderAndStoreCard() {
        val isCurrentUserSeller = !isCsSupport && currentUid == storeId
        val partnerDisplayName = when {
            isCsSupport -> FirestoreRepository.CS_STORE_NAME
            isCurrentUserSeller -> buyerName.ifBlank { "Pembeli" }
            else -> storeName.ifBlank { "Toko Guppy" }
        }

        val tvHeaderName = findViewById<TextView>(R.id.tvChatStoreName)
        val tvResponseSub = findViewById<TextView>(R.id.tvChatResponseSub)
        val tvCardAvatar = findViewById<TextView>(R.id.tvChatStoreAvatar)
        val tvCardName = findViewById<TextView>(R.id.tvChatStoreCardName)
        val tvCardSub = findViewById<TextView>(R.id.tvChatStoreSubLabel)
        val tvVerifiedTag = findViewById<TextView>(R.id.tvChatVerifiedTag)
        val ivVerified = findViewById<ImageView>(R.id.ivChatVerifiedBadge)
        val btnViewStore = findViewById<TextView>(R.id.btnViewStore)

        tvHeaderName.text = partnerDisplayName
        tvCardName?.text = partnerDisplayName

        val initials = if (isCsSupport) {
            "CS"
        } else {
            partnerDisplayName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifEmpty { "GM" }
        }
        tvCardAvatar?.text = initials
        tvCardAvatar?.backgroundTintList = ColorStateList.valueOf(
            Color.parseColor(if (isCsSupport) "#1565C0" else "#00897B")
        )

        when {
            isCsSupport -> {
                tvResponseSub?.text = " · Layanan Resmi 24 Jam"
                tvCardSub?.text = "Pusat Bantuan & Resolusi Transaksi"
                tvVerifiedTag?.text = "OFFICIAL"
                ivVerified?.visibility = View.VISIBLE
                btnViewStore?.visibility = View.GONE
            }
            isCurrentUserSeller -> {
                tvResponseSub?.text = " · Pembeli GuppyMarket"
                tvCardSub?.text = "Percakapan pesanan & stok toko Anda"
                tvVerifiedTag?.visibility = View.GONE
                ivVerified?.visibility = View.GONE
                btnViewStore?.visibility = View.GONE
            }
            else -> {
                tvResponseSub?.text = " · Terhubung real-time"
                tvCardSub?.text = storeLocation.ifBlank { "Farm Guppy Terdaftar" }
                tvVerifiedTag?.text = "VERIFIED"
                ivVerified?.visibility = View.VISIBLE
                btnViewStore?.visibility = View.VISIBLE
            }
        }

        val partnerUid = when {
            isCsSupport -> ""
            isCurrentUserSeller -> buyerId
            else -> storeId
        }
        val cardStoreAvatarImg = findViewById<View>(R.id.cardChatStoreAvatarImage)
        val ivStoreAvatarImg = findViewById<ImageView>(R.id.ivChatStoreAvatarImage)
        cardStoreAvatarImg?.visibility = View.GONE

        if (partnerUid.isNotBlank() && ivStoreAvatarImg != null && cardStoreAvatarImg != null) {
            FirestoreRepository.fetchUserProfileById(partnerUid) { profile ->
                if (isFinishing || isDestroyed || profile == null) return@fetchUserProfileById
                val updatedName = if (isCurrentUserSeller) {
                    profile.name.ifBlank { partnerDisplayName }
                } else {
                    profile.farmName.ifBlank { profile.name.ifBlank { partnerDisplayName } }
                }
                tvHeaderName.text = updatedName
                tvCardName?.text = updatedName
                if (!isCurrentUserSeller && profile.location.isNotBlank()) {
                    storeLocation = profile.location
                    tvCardSub?.text = profile.location
                }
                if (profile.avatarUri.isNotBlank()) {
                    val loaded = SafeImageLoader.loadUriSafely(
                        this,
                        ivStoreAvatarImg,
                        profile.avatarUri,
                        R.drawable.ic_default_user_avatar,
                        128
                    )
                    cardStoreAvatarImg.visibility = if (loaded) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun bindAttachedProductIfPresent() {
        val cardProduct = findViewById<View>(R.id.cardAttachedProduct)
        val productName = intent.getStringExtra("EXTRA_PRODUCT_NAME")
        val productPrice = intent.getStringExtra("EXTRA_PRODUCT_PRICE")

        if (!productName.isNullOrBlank()) {
            cardProduct?.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvAttachedProductTitle)?.text = productName
            findViewById<TextView>(R.id.tvAttachedProductPrice)?.text = productPrice ?: ""
        } else {
            cardProduct?.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        val btnViewStore = findViewById<TextView>(R.id.btnViewStore)

        btnViewStore?.setOnClickListener {
            val intent = Intent(this, DetailFarmActivity::class.java).apply {
                putExtra("farm_id", storeId)
                putExtra("farm_title", storeName)
                putExtra("farm_location", storeLocation)
            }
            startActivity(intent)
        }

        btnSendMessage.setOnClickListener {
            sendUserMessage()
        }

        etChatMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserMessage()
                true
            } else false
        }
    }

    private fun setupQuickReplies() {
        val chip1 = findViewById<TextView>(R.id.chipQuickReply1)
        val chip2 = findViewById<TextView>(R.id.chipQuickReply2)
        val chip3 = findViewById<TextView>(R.id.chipQuickReply3)

        if (isCsSupport) {
            chip1?.text = "Status pesanan saya"
            chip2?.text = "Kendala pembayaran"
            chip3?.text = "Bantuan lelang"
        } else {
            chip1?.text = "Stok masih tersedia?"
            chip2?.text = "Garansi sehat 24 jam"
            chip3?.text = "Bisa kirim hari ini?"
        }

        chip1?.setOnClickListener { appendAndSendMessage(chip1.text.toString()) }
        chip2?.setOnClickListener { appendAndSendMessage(chip2.text.toString()) }
        chip3?.setOnClickListener { appendAndSendMessage(chip3.text.toString()) }
    }

    private fun appendAndSendMessage(text: String) {
        etChatMessage.setText(text)
        sendUserMessage()
    }

    private fun setupRealtimeChatListener() {
        if (chatRoomId.isBlank()) return
        FirestoreRepository.markChatRoomAsRead(chatRoomId, currentUid)

        FirestoreRepository.fetchChatRoomDeletedAt(chatRoomId, currentUid) { deletedAfterMillis ->
            if (isFinishing || isDestroyed) return@fetchChatRoomDeletedAt
            messagesListener?.remove()
            messagesListener = FirestoreRepository.listenToChatMessages(chatRoomId) { allMessages ->
                if (isFinishing || isDestroyed) return@listenToChatMessages

                val messages = if (deletedAfterMillis > 0L) {
                    allMessages.filter { it.timestamp > deletedAfterMillis }
                } else {
                    allMessages
                }

                layoutDynamicMessages.removeAllViews()
                if (messages.isEmpty()) {
                    tvChatEmptyState?.visibility = View.VISIBLE
                } else {
                    tvChatEmptyState?.visibility = View.GONE
                    for (msg in messages) {
                        val isOutgoing = msg.senderId == currentUid
                        val timeLabel = if (isOutgoing) "${msg.formattedTime} ✓" else msg.formattedTime
                        appendMessageToUI(msg.messageText, timeLabel, isOutgoing)
                    }
                    FirestoreRepository.markChatRoomAsRead(chatRoomId, currentUid)
                }

                scrollChatMessages.post {
                    scrollChatMessages.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun sendUserMessage() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu untuk mengirim pesan.", Toast.LENGTH_SHORT).show()
            return
        }

        val messageText = etChatMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        etChatMessage.setText("")

        FirestoreRepository.sendChatMessage(
            chatRoomId = chatRoomId,
            senderId = currentUid,
            senderName = currentUserName,
            buyerId = buyerId,
            buyerName = buyerName,
            storeId = storeId,
            storeName = storeName,
            storeLocation = storeLocation,
            isCsSupport = isCsSupport,
            messageText = messageText
        ) { success ->
            if (!success && !isFinishing && !isDestroyed) {
                Toast.makeText(this, "Gagal mengirim pesan, periksa koneksi.", Toast.LENGTH_SHORT).show()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                appendMessageToUI(messageText, timeFormat.format(Date()), true)
            }
        }
    }

    private fun appendMessageToUI(msgText: String, timeTextStr: String, isOutgoing: Boolean) {
        val density = resources.displayMetrics.density
        val margin12 = (12 * density).toInt()
        val margin4 = (4 * density).toInt()
        val padding14 = (14 * density).toInt()
        val padding10 = (10 * density).toInt()
        val maxPx = (280 * density).toInt()

        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = margin12
            }
            orientation = LinearLayout.VERTICAL
            gravity = if (isOutgoing) Gravity.END else Gravity.START
        }

        val bubbleText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxWidth = maxPx
            setBackgroundResource(if (isOutgoing) R.drawable.bg_chat_bubble_outgoing else R.drawable.bg_chat_bubble_incoming)
            setPadding(padding14, padding10, padding14, padding10)
            text = msgText
            setTextColor(
                ContextCompat.getColor(
                    this@ChatActivity,
                    if (isOutgoing) R.color.white else R.color.guppy_text_black
                )
            )
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ChatActivity, R.font.inter_regular)
            textSize = 13f
        }

        val timeText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = margin4
            }
            text = timeTextStr
            setTextColor(ContextCompat.getColor(this@ChatActivity, R.color.guppy_text_gray))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ChatActivity, R.font.inter_regular)
            textSize = 10f
        }

        container.addView(bubbleText)
        container.addView(timeText)

        layoutDynamicMessages.addView(container)

        scrollChatMessages.post {
            scrollChatMessages.fullScroll(View.FOCUS_DOWN)
        }
    }
}
