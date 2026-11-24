package com.stiki.marcelloilham.guppymarketidn.menuHome

import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.stiki.marcelloilham.guppymarketidn.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var etChatMessage: EditText
    private lateinit var btnSendMessage: ImageView
    private lateinit var layoutDynamicMessages: LinearLayout
    private lateinit var scrollChatMessages: NestedScrollView

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

        val storeName = intent.getStringExtra("EXTRA_STORE_NAME") ?: "Bekasi Aqua Farm"
        val tvStoreName = findViewById<TextView>(R.id.tvChatStoreName)
        tvStoreName.text = storeName

        initViews()
        setupButtons()
        setupQuickReplies()
    }

    private fun initViews() {
        etChatMessage = findViewById(R.id.etChatMessage)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        layoutDynamicMessages = findViewById(R.id.layoutDynamicChatMessages)
        scrollChatMessages = findViewById(R.id.scrollChatMessages)
    }

    private fun setupButtons() {
        val btnViewStore = findViewById<TextView>(R.id.btnViewStore)
        val btnViewProposalOrder = findViewById<TextView>(R.id.btnViewProposalOrder)

        btnViewStore?.setOnClickListener {
            startActivity(Intent(this, DetailFarmActivity::class.java))
        }

        btnViewProposalOrder?.setOnClickListener {
            startActivity(Intent(this, CheckoutActivity::class.java))
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

        chip1?.setOnClickListener {
            appendAndSendMessage(chip1.text.toString())
        }

        chip2?.setOnClickListener {
            appendAndSendMessage(chip2.text.toString())
        }

        chip3?.setOnClickListener {
            appendAndSendMessage(chip3.text.toString())
        }
    }

    private fun appendAndSendMessage(text: String) {
        etChatMessage.setText(text)
        sendUserMessage()
    }

    private fun sendUserMessage() {
        val messageText = etChatMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        // Create container for outgoing message
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
            gravity = Gravity.END
        }

        val bubbleText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxWidth = maxPx
            setBackgroundResource(R.drawable.bg_chat_bubble_outgoing)
            setPadding(padding14, padding10, padding14, padding10)
            text = messageText
            setTextColor(getColor(R.color.white))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ChatActivity, R.font.inter_regular)
            textSize = 13f
        }

        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormatter.format(Date()) + " ✓"

        val timeText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = margin4
            }
            text = currentTime
            setTextColor(getColor(R.color.guppy_text_gray))
            typeface = androidx.core.content.res.ResourcesCompat.getFont(this@ChatActivity, R.font.inter_regular)
            textSize = 10f
        }

        container.addView(bubbleText)
        container.addView(timeText)

        layoutDynamicMessages.addView(container)

        // Clear input box
        etChatMessage.setText("")

        // Scroll to bottom
        scrollChatMessages.post {
            scrollChatMessages.fullScroll(View.FOCUS_DOWN)
        }
    }
}
