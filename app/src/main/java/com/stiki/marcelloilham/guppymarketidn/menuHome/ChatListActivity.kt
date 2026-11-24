package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stiki.marcelloilham.guppymarketidn.R

class ChatListActivity : AppCompatActivity() {

    data class ChatRoom(
        val storeName: String,
        val avatarText: String,
        val avatarBgColorHex: String,
        val lastMessage: String,
        val time: String,
        val unreadCount: Int = 0
    )

    private lateinit var etSearch: EditText
    private lateinit var recycler: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var chatListAdapter: ChatListAdapter

    private var activeFilter: String = "ALL"
    private var activeQuery: String = ""

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

        setupFilterChips()
        setupChatList()

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

    private fun setupFilterChips() {
        val chipAll = findViewById<TextView>(R.id.chipChatAll)
        val chipUnread = findViewById<TextView>(R.id.chipChatUnread)
        val chipOrder = findViewById<TextView>(R.id.chipChatOrder)

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

    private fun setupChatList() {
        val rooms = listOf(
            ChatRoom("Bekasi Aqua Farm", "BA", "#D85C42", "Sehat kak, sudah dikarantina 3 hari dan dikemas...", "09:18", 1),
            ChatRoom("Jakarta Guppy House", "JG", "#00897B", "Pesanan #JP8841 telah dikirim via JNE Express", "Kemarin", 0),
            ChatRoom("CS GuppyMarket IDN", "CS", "#1565C0", "Halo Marcello, ada yang bisa CS bantu terkait transaksi?", "12 Okt", 0)
        )

        chatListAdapter = ChatListAdapter(this, rooms) { room ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("EXTRA_STORE_NAME", room.storeName)
            }
            startActivity(intent)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = chatListAdapter

        updateEmptyState()
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
        private val rawList: List<ChatRoom>,
        private val onClick: (ChatRoom) -> Unit
    ) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

        private var filteredList = rawList.toMutableList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvAvatar: TextView = view.findViewById(R.id.tvChatAvatarText)
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
            holder.tvAvatar.text = room.avatarText
            holder.tvAvatar.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.transparent)
            holder.tvAvatar.setBackgroundColor(android.graphics.Color.parseColor(room.avatarBgColorHex))

            holder.tvStoreName.text = room.storeName
            holder.tvLastMessage.text = room.lastMessage
            holder.tvTime.text = room.time

            if (room.unreadCount > 0) {
                holder.tvUnreadBadge.visibility = View.VISIBLE
                holder.tvUnreadBadge.text = room.unreadCount.toString()
            } else {
                holder.tvUnreadBadge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onClick(room) }
        }

        override fun getItemCount(): Int = filteredList.size

        fun filter(query: String, filterCode: String) {
            val result = rawList.filter { room ->
                val matchesQuery = query.isEmpty() ||
                    room.storeName.lowercase().contains(query.lowercase()) ||
                    room.lastMessage.lowercase().contains(query.lowercase())

                val matchesFilter = when (filterCode) {
                    "UNREAD" -> room.unreadCount > 0
                    "ORDER" -> room.lastMessage.contains("Pesanan", true) || room.lastMessage.contains("JP8841", true)
                    else -> true
                }

                matchesQuery && matchesFilter
            }

            filteredList = result.toMutableList()
            notifyDataSetChanged()
        }
    }
}
