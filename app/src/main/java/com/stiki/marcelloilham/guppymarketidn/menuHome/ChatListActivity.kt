package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import com.stiki.marcelloilham.guppymarketidn.util.ChatNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.SafeImageLoader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatListActivity : AppCompatActivity() {

    data class ChatRoomUI(
        val roomId: String,
        val partnerUid: String,
        val partnerName: String,
        val partnerAvatarUri: String,
        val avatarText: String,
        val avatarBgColorHex: String,
        val lastMessage: String,
        val time: String,
        val unreadCount: Int,
        val isOrderRelated: Boolean,
        val isCsSupport: Boolean,
        val rawRoom: FirestoreRepository.ChatRoomModel
    )

    private lateinit var etSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var chatListAdapter: ChatListAdapter

    private lateinit var chipAll: TextView
    private lateinit var chipUnread: TextView
    private lateinit var chipOrder: TextView

    private lateinit var bottomBarSelection: View
    private lateinit var tvSelectedChatCount: TextView
    private lateinit var btnSelectAllChats: TextView
    private lateinit var btnCancelChatSelection: TextView
    private lateinit var btnDeleteSelectedChats: TextView

    private var activeFilter: String = "ALL"
    private var activeQuery: String = ""

    private val firebaseAuth = FirebaseAuth.getInstance()
    private var chatRoomsListener: ListenerRegistration? = null
    private val profileCache = mutableMapOf<String, FirestoreRepository.UserProfileModel?>()
    private val fetchingProfileUids = mutableSetOf<String>()
    private var latestRooms: List<FirestoreRepository.ChatRoomModel> = emptyList()
    private val selectedRoomIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        val topBar = findViewById<View>(R.id.topBarChatList)
        topBar?.let {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                it.updatePadding(
                    top = systemBars.top + (8 * resources.displayMetrics.density).toInt()
                )
                insets
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBackChatList)
        btnBack.setOnClickListener { finish() }

        etSearch = findViewById(R.id.etSearchChatList)
        recycler = findViewById(R.id.recyclerChatList)
        tvEmpty = findViewById(R.id.tvEmptyChatList)

        chipAll = findViewById(R.id.chipChatAll)
        chipUnread = findViewById(R.id.chipChatUnread)
        chipOrder = findViewById(R.id.chipChatOrder)

        bottomBarSelection = findViewById(R.id.bottomBarChatSelection)
        tvSelectedChatCount = findViewById(R.id.tvSelectedChatCount)
        btnSelectAllChats = findViewById(R.id.btnSelectAllChats)
        btnCancelChatSelection = findViewById(R.id.btnCancelChatSelection)
        btnDeleteSelectedChats = findViewById(R.id.btnDeleteSelectedChats)

        setupFilterChips()
        setupSelectionControls()
        initChatRecycler()
        attachSwipeToDelete()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (selectedRoomIds.isNotEmpty()) {
                    exitSelectionMode()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                activeQuery = s.toString().trim()
                chatListAdapter.filter(activeQuery, activeFilter)
                updateEmptyState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        startRealtimeChatList()
    }

    override fun onStop() {
        super.onStop()
        chatRoomsListener?.remove()
        chatRoomsListener = null
    }

    private fun setupFilterChips() {
        val chips = listOf(chipAll, chipUnread, chipOrder)

        fun selectFilter(selected: TextView, filterCode: String) {
            activeFilter = filterCode

            chips.forEach { chip ->
                if (chip == selected) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_primary))
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_outline)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.guppy_text_black))
                }
            }

            chatListAdapter.filter(activeQuery, activeFilter)
            updateEmptyState()
        }

        chipAll.setOnClickListener { selectFilter(chipAll, "ALL") }
        chipUnread.setOnClickListener { selectFilter(chipUnread, "UNREAD") }
        chipOrder.setOnClickListener { selectFilter(chipOrder, "ORDER") }
    }

    private fun initChatRecycler() {
        chatListAdapter = ChatListAdapter(
            context = this,
            rawList = emptyList(),
            selectedIds = selectedRoomIds,
            onClick = { item ->
                if (selectedRoomIds.isNotEmpty()) {
                    toggleRoomSelection(item.roomId)
                } else {
                    val raw = item.rawRoom
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("EXTRA_CHAT_ROOM_ID", raw.id)
                        putExtra("EXTRA_STORE_ID", raw.storeId)
                        putExtra("EXTRA_STORE_NAME", raw.storeName)
                        putExtra("EXTRA_STORE_LOCATION", raw.storeLocation)
                        putExtra("EXTRA_BUYER_ID", raw.buyerId)
                        putExtra("EXTRA_BUYER_NAME", raw.buyerName)
                        putExtra("EXTRA_IS_CS", item.isCsSupport)
                    }
                    startActivity(intent)
                }
            },
            onLongClick = { item, view ->
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                toggleRoomSelection(item.roomId)
            }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = chatListAdapter
        updateEmptyState()
    }

    private fun setupSelectionControls() {
        btnCancelChatSelection.setOnClickListener { exitSelectionMode() }

        btnSelectAllChats.setOnClickListener {
            val visibleRooms = chatListAdapter.getVisibleItems()
            if (visibleRooms.isEmpty()) return@setOnClickListener
            val allSelected = visibleRooms.all { selectedRoomIds.contains(it.roomId) }
            if (allSelected) {
                selectedRoomIds.clear()
            } else {
                visibleRooms.forEach { selectedRoomIds.add(it.roomId) }
            }
            updateSelectionBarUI()
            chatListAdapter.notifyDataSetChanged()
        }

        btnDeleteSelectedChats.setOnClickListener {
            val roomsToDelete = chatListAdapter.getVisibleItems()
                .filter { selectedRoomIds.contains(it.roomId) }
            if (roomsToDelete.isNotEmpty()) {
                confirmDeleteChatRooms(roomsToDelete, restorePosition = null)
            }
        }
    }

    private fun toggleRoomSelection(roomId: String) {
        if (roomId.isBlank()) return
        if (!selectedRoomIds.add(roomId)) {
            selectedRoomIds.remove(roomId)
        }
        updateSelectionBarUI()
        chatListAdapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        selectedRoomIds.clear()
        updateSelectionBarUI()
        if (::chatListAdapter.isInitialized) {
            chatListAdapter.notifyDataSetChanged()
        }
    }

    private fun updateSelectionBarUI() {
        if (selectedRoomIds.isEmpty()) {
            bottomBarSelection.visibility = View.GONE
            return
        }
        bottomBarSelection.visibility = View.VISIBLE
        tvSelectedChatCount.text = "${selectedRoomIds.size} chat dipilih"
        val visibleRooms = chatListAdapter.getVisibleItems()
        val allSelected = visibleRooms.isNotEmpty() && visibleRooms.all { selectedRoomIds.contains(it.roomId) }
        btnSelectAllChats.text = if (allSelected) "Batal pilih semua" else "Pilih semua"
    }

    private fun attachSwipeToDelete() {
        val density = resources.displayMetrics.density
        val cornerRadiusPx = 16f * density
        val iconSizePx = (22f * density).toInt()
        val horizontalMarginPx = (20f * density).toInt()
        val readyThreshold = 0.65f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12.5f * density
            isFakeBoldText = true
        }
        val trashDrawable = ContextCompat.getDrawable(this, R.drawable.ic_trash_delete)?.mutate()
        val readyHapticHolders = mutableSetOf<RecyclerView.ViewHolder>()

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                // Disable swipe while multi-select mode is active
                return if (selectedRoomIds.isNotEmpty()) 0 else super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = readyThreshold

            override fun getSwipeEscapeVelocity(defaultValue: Float): Float = defaultValue * 2.5f

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    recycler.parent?.requestDisallowInterceptTouchEvent(true)
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                readyHapticHolders.remove(viewHolder)
                recycler.parent?.requestDisallowInterceptTouchEvent(false)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX != 0f) {
                    val width = itemView.width.toFloat().coerceAtLeast(1f)
                    val swipeProgress = kotlin.math.abs(dX) / width
                    val isReadyToDelete = swipeProgress >= readyThreshold

                    if (isReadyToDelete && isCurrentlyActive && readyHapticHolders.add(viewHolder)) {
                        itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    } else if (!isReadyToDelete) {
                        readyHapticHolders.remove(viewHolder)
                    }

                    val rect = RectF(
                        itemView.left.toFloat(),
                        itemView.top.toFloat(),
                        itemView.right.toFloat(),
                        itemView.bottom.toFloat()
                    )

                    if (isReadyToDelete) {
                        bgPaint.color = Color.parseColor("#E53935")
                        c.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)
                        textPaint.color = Color.WHITE
                        trashDrawable?.setTint(Color.WHITE)
                    } else {
                        bgPaint.color = Color.parseColor("#FFF3E0")
                        c.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, bgPaint)
                        borderPaint.color = Color.parseColor("#FB8C00")
                        c.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, borderPaint)
                        textPaint.color = Color.parseColor("#E65100")
                        trashDrawable?.setTint(Color.parseColor("#E65100"))
                    }

                    val labelText = if (isReadyToDelete) "Lepas untuk hapus" else "Usap lebih jauh..."
                    val centerY = (itemView.top + itemView.bottom) / 2
                    val iconTop = centerY - iconSizePx / 2
                    val iconBottom = iconTop + iconSizePx
                    val textWidth = textPaint.measureText(labelText)
                    val iconTextGap = (8f * density).toInt()
                    val baselineOffset = (textPaint.descent() + textPaint.ascent()) / 2f
                    val textY = centerY - baselineOffset

                    if (dX > 0) {
                        val iconLeft = itemView.left + horizontalMarginPx
                        val iconRight = iconLeft + iconSizePx
                        trashDrawable?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        trashDrawable?.draw(c)
                        if (kotlin.math.abs(dX) > iconSizePx + horizontalMarginPx + textWidth * 0.4f) {
                            c.drawText(labelText, (iconRight + iconTextGap).toFloat(), textY, textPaint)
                        }
                    } else {
                        val iconRight = itemView.right - horizontalMarginPx
                        val iconLeft = iconRight - iconSizePx
                        trashDrawable?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        trashDrawable?.draw(c)
                        if (kotlin.math.abs(dX) > iconSizePx + horizontalMarginPx + textWidth * 0.4f) {
                            c.drawText(labelText, iconLeft - iconTextGap - textWidth, textY, textPaint)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val room = chatListAdapter.getItemAt(pos)
                if (room == null) {
                    chatListAdapter.notifyItemChanged(pos)
                    return
                }
                confirmDeleteChatRooms(listOf(room), restorePosition = pos)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(recycler)
    }

    private fun confirmDeleteChatRooms(roomsToDelete: List<ChatRoomUI>, restorePosition: Int?) {
        val currentUid = firebaseAuth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            restorePosition?.let { chatListAdapter.notifyItemChanged(it) }
            Toast.makeText(this, "Silakan login terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        val count = roomsToDelete.size
        val title = if (count == 1) {
            "Hapus chat dengan ${roomsToDelete[0].partnerName}?"
        } else {
            "Hapus $count percakapan dipilih?"
        }
        val message = if (count == 1) {
            "Percakapan ini akan dihapus dari daftar chat Anda. Riwayat lama tidak akan tampil lagi."
        } else {
            "$count percakapan terpilih akan dihapus dari daftar chat Anda. Riwayat lama tidak akan tampil lagi."
        }

        var confirmed = false
        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Batal") { d, _ ->
                d.dismiss()
            }
            .setPositiveButton("Ya, Hapus", null)
            .setOnDismissListener {
                if (!confirmed && restorePosition != null) {
                    chatListAdapter.notifyItemChanged(restorePosition)
                }
            }
            .create()

        dialog.setOnShowListener {
            val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            btnPositive.setTextColor(Color.parseColor("#E53935"))

            btnPositive.setOnClickListener {
                btnPositive.isEnabled = false
                btnNegative.isEnabled = false
                btnPositive.text = "Menghapus..."

                val roomIds = roomsToDelete.map { it.roomId }
                FirestoreRepository.deleteChatRoomsForUser(currentUid, roomIds) { success ->
                    if (isFinishing || isDestroyed) return@deleteChatRoomsForUser
                    if (success) {
                        confirmed = true
                        roomIds.forEach { ChatNotificationHelper.cancelRoomNotification(this, it) }
                        if (selectedRoomIds.isNotEmpty()) {
                            selectedRoomIds.removeAll(roomIds.toSet())
                            updateSelectionBarUI()
                        }
                        dialog.dismiss()
                        val successMsg = if (count == 1) {
                            "Chat dengan ${roomsToDelete[0].partnerName} dihapus"
                        } else {
                            "$count chat berhasil dihapus"
                        }
                        Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        btnPositive.isEnabled = true
                        btnNegative.isEnabled = true
                        btnPositive.text = "Ya, Hapus"
                        Toast.makeText(this, "Gagal menghapus chat. Coba lagi.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun startRealtimeChatList() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            tvEmpty.text = "Silakan login untuk melihat pesan & chat toko"
            chatListAdapter.updateList(emptyList(), activeQuery, activeFilter)
            updateEmptyState()
            return
        }

        val currentUid = currentUser.uid
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("name", null)?.takeIf { it.isNotBlank() }
            ?: currentUser.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser.email?.substringBefore("@")
            ?: "Sahabat Guppy"

        // Ensure official CS GuppyMarket IDN room exists for this authenticated user
        FirestoreRepository.ensureCsChatRoom(currentUid, userName)

        chatRoomsListener?.remove()
        chatRoomsListener = FirestoreRepository.listenUserChatRooms(currentUid) { rooms ->
            if (isFinishing || isDestroyed) return@listenUserChatRooms
            latestRooms = rooms
            renderChatRooms(currentUid, rooms)
        }
    }

    private fun renderChatRooms(currentUid: String, rooms: List<FirestoreRepository.ChatRoomModel>) {
        val palette = listOf("#00897B", "#D85C42", "#5D4037", "#7B1FA2", "#0097A7")
        val uiRooms = rooms.mapIndexed { idx, room ->
            val isCs = room.isCsSupport || room.storeId == FirestoreRepository.CS_STORE_ID
            val isCurrentUserSeller = !isCs && currentUid == room.storeId
            val partnerUid = when {
                isCs -> ""
                isCurrentUserSeller -> room.buyerId
                else -> room.storeId
            }

            if (partnerUid.isNotBlank() && !profileCache.containsKey(partnerUid) && !fetchingProfileUids.contains(partnerUid)) {
                fetchingProfileUids.add(partnerUid)
                FirestoreRepository.fetchUserProfileById(partnerUid) { profile ->
                    fetchingProfileUids.remove(partnerUid)
                    profileCache[partnerUid] = profile
                    if (!isFinishing && !isDestroyed) {
                        renderChatRooms(currentUid, latestRooms)
                    }
                }
            }

            val cachedProfile = if (partnerUid.isNotBlank()) profileCache[partnerUid] else null
            val partnerName = when {
                isCs -> FirestoreRepository.CS_STORE_NAME
                isCurrentUserSeller -> cachedProfile?.name?.takeIf { it.isNotBlank() }
                    ?: room.buyerName.ifBlank { "Pembeli" }
                else -> cachedProfile?.farmName?.takeIf { it.isNotBlank() }
                    ?: cachedProfile?.name?.takeIf { it.isNotBlank() }
                    ?: room.storeName.ifBlank { "Toko Guppy" }
            }
            val partnerAvatar = cachedProfile?.avatarUri.orEmpty()

            val initials = if (isCs) {
                "CS"
            } else {
                partnerName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "GM" }
            }

            val bgColorHex = if (isCs) "#1565C0" else palette[idx % palette.size]
            val unread = (room.unreadCounts[currentUid] ?: 0L).toInt()

            ChatRoomUI(
                roomId = room.id,
                partnerUid = partnerUid,
                partnerName = partnerName,
                partnerAvatarUri = partnerAvatar,
                avatarText = initials,
                avatarBgColorHex = bgColorHex,
                lastMessage = room.lastMessage.ifBlank { "Belum ada pesan" },
                time = formatChatTime(room.lastMessageTime),
                unreadCount = unread,
                isOrderRelated = room.isOrderRelated,
                isCsSupport = isCs,
                rawRoom = room
            )
        }

        val visibleIds = uiRooms.map { it.roomId }.toSet()
        if (selectedRoomIds.retainAll(visibleIds)) {
            updateSelectionBarUI()
        }

        val unreadRoomsCount = uiRooms.count { it.unreadCount > 0 }
        chipAll.text = "Semua (${uiRooms.size})"
        chipUnread.text = "Belum Dibaca ($unreadRoomsCount)"

        chatListAdapter.updateList(uiRooms, activeQuery, activeFilter)
        updateSelectionBarUI()
        updateEmptyState()
    }

    private fun formatChatTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val calMsg = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance()
        val sameDay = calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calMsg.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
        return if (sameDay) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            SimpleDateFormat("dd MMM", Locale("in", "ID")).format(Date(timestamp))
        }
    }

    private fun updateEmptyState() {
        if (chatListAdapter.itemCount == 0) {
            recycler.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }
    }

    class ChatListAdapter(
        private val context: Context,
        private var rawList: List<ChatRoomUI>,
        private val selectedIds: Set<String>,
        private val onClick: (ChatRoomUI) -> Unit,
        private val onLongClick: (ChatRoomUI, View) -> Unit
    ) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

        private var filteredList = rawList.toMutableList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val cardItem: MaterialCardView? = view as? MaterialCardView
            val ivSelectIndicator: ImageView? = view.findViewById(R.id.ivChatSelectIndicator)
            val tvAvatar: TextView = view.findViewById(R.id.tvChatAvatarText)
            val cardAvatarImage: View? = view.findViewById(R.id.cardChatAvatarImage)
            val ivAvatarImage: ImageView? = view.findViewById(R.id.ivChatAvatarImage)
            val tvStoreName: TextView = view.findViewById(R.id.tvChatStoreName)
            val tvLastMessage: TextView = view.findViewById(R.id.tvChatLastMessage)
            val tvTime: TextView = view.findViewById(R.id.tvChatTime)
            val tvUnreadBadge: TextView = view.findViewById(R.id.tvUnreadBadge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val room = filteredList[position]
            val isSelectionMode = selectedIds.isNotEmpty()
            val isSelected = selectedIds.contains(room.roomId)

            holder.ivSelectIndicator?.let { iv ->
                if (isSelectionMode) {
                    iv.visibility = View.VISIBLE
                    iv.setImageResource(
                        if (isSelected) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                    )
                } else {
                    iv.visibility = View.GONE
                }
            }

            holder.cardItem?.let { card ->
                if (isSelected) {
                    card.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    card.strokeColor = Color.parseColor("#E53935")
                    card.strokeWidth = (1.5f * context.resources.displayMetrics.density).toInt()
                } else {
                    card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
                    card.strokeColor = ContextCompat.getColor(context, R.color.guppy_stroke)
                    card.strokeWidth = (1f * context.resources.displayMetrics.density).toInt()
                }
            }

            holder.tvAvatar.text = room.avatarText
            holder.tvAvatar.setBackgroundResource(R.drawable.bg_circle_image)
            holder.tvAvatar.backgroundTintList = ColorStateList.valueOf(Color.parseColor(room.avatarBgColorHex))

            if (room.partnerAvatarUri.isNotBlank() && holder.ivAvatarImage != null && holder.cardAvatarImage != null) {
                val loaded = SafeImageLoader.loadUriSafely(
                    context,
                    holder.ivAvatarImage,
                    room.partnerAvatarUri,
                    R.drawable.ic_default_user_avatar,
                    128
                )
                holder.cardAvatarImage.visibility = if (loaded) View.VISIBLE else View.GONE
            } else {
                holder.cardAvatarImage?.visibility = View.GONE
            }

            holder.tvStoreName.text = room.partnerName
            holder.tvLastMessage.text = room.lastMessage
            holder.tvTime.text = room.time

            if (room.unreadCount > 0) {
                holder.tvUnreadBadge.visibility = View.VISIBLE
                holder.tvUnreadBadge.text = room.unreadCount.toString()
            } else {
                holder.tvUnreadBadge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onClick(room) }
            holder.itemView.setOnLongClickListener { view ->
                onLongClick(room, view)
                true
            }
        }

        override fun getItemCount(): Int = filteredList.size

        fun getItemAt(position: Int): ChatRoomUI? = filteredList.getOrNull(position)

        fun getVisibleItems(): List<ChatRoomUI> = filteredList.toList()

        fun updateList(newList: List<ChatRoomUI>, query: String, filterCode: String) {
            rawList = newList
            filter(query, filterCode)
        }

        fun filter(query: String, filterCode: String) {
            val result = rawList.filter { room ->
                val matchesQuery = query.isEmpty() ||
                    room.partnerName.lowercase().contains(query.lowercase()) ||
                    room.lastMessage.lowercase().contains(query.lowercase())

                val matchesFilter = when (filterCode) {
                    "UNREAD" -> room.unreadCount > 0
                    "ORDER" -> room.isOrderRelated ||
                        room.lastMessage.contains("Pesanan", true) ||
                        room.lastMessage.contains("transaksi", true)
                    else -> true
                }

                matchesQuery && matchesFilter
            }

            filteredList = result.toMutableList()
            notifyDataSetChanged()
        }
    }
}
