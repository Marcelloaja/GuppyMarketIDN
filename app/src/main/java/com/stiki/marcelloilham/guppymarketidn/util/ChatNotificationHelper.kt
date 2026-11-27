package com.stiki.marcelloilham.guppymarketidn.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.stiki.marcelloilham.guppymarketidn.MainActivity
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuHome.ChatActivity
import com.stiki.marcelloilham.guppymarketidn.menuHome.ChatListActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Listens for real-time incoming Firestore chat messages while authenticated and shows
 * Android system notifications (plus handles FCM payload fallback and token registration).
 */
object ChatNotificationHelper {

    const val CHANNEL_ID = "guppymarket_chat_channel"
    private const val CHANNEL_NAME = "Pesan & Balasan Chat"
    private const val CHANNEL_DESC = "Notifikasi saat ada pesan baru atau balasan chat toko/CS"

    @Volatile
    var activeChatRoomId: String? = null

    private var listenerUid: String? = null
    private var chatRoomsListener: ListenerRegistration? = null
    private var isInitialSnapshot: Boolean = true
    private val lastSeenTimeByRoom = ConcurrentHashMap<String, Long>()
    private val lastNotifiedKeyByRoom = ConcurrentHashMap<String, String>()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableLights(true)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun startListening(context: Context, uid: String) {
        if (uid.isBlank()) return
        if (listenerUid == uid && chatRoomsListener != null) return

        stopListening()
        ensureChannel(context.applicationContext)

        listenerUid = uid
        isInitialSnapshot = true
        lastSeenTimeByRoom.clear()
        lastNotifiedKeyByRoom.clear()

        val appCtx = context.applicationContext
        chatRoomsListener = FirestoreRepository.listenUserChatRooms(uid) { rooms ->
            if (isInitialSnapshot) {
                isInitialSnapshot = false
                for (room in rooms) {
                    lastSeenTimeByRoom[room.id] = room.lastMessageTime
                }
                return@listenUserChatRooms
            }

            for (room in rooms) {
                val prevSeen = lastSeenTimeByRoom[room.id] ?: 0L
                val isNewer = room.lastMessageTime > prevSeen
                val unreadCount = room.unreadCounts[uid] ?: 0L
                val isFromOther = room.lastSenderId.isNotBlank() && room.lastSenderId != uid
                val isRoomCurrentlyOpen = !activeChatRoomId.isNullOrBlank() && activeChatRoomId == room.id

                if (isNewer) {
                    lastSeenTimeByRoom[room.id] = room.lastMessageTime
                }

                if (isNewer && isFromOther && unreadCount > 0L && !isRoomCurrentlyOpen && room.lastMessage.isNotBlank()) {
                    val notifyKey = "${room.lastMessageTime}_${room.lastSenderId}"
                    if (lastNotifiedKeyByRoom[room.id] != notifyKey) {
                        lastNotifiedKeyByRoom[room.id] = notifyKey
                        showIncomingChatNotification(appCtx, uid, room)
                    }
                }
            }
        }
    }

    fun stopListening() {
        chatRoomsListener?.remove()
        chatRoomsListener = null
        listenerUid = null
        isInitialSnapshot = true
        lastSeenTimeByRoom.clear()
        lastNotifiedKeyByRoom.clear()
    }

    fun syncFcmToken(uid: String) {
        if (uid.isBlank()) return
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        saveFcmToken(uid, token)
                    }
                }
        } catch (_: Throwable) {
        }
    }

    fun saveFcmToken(uid: String, token: String) {
        if (uid.isBlank() || token.isBlank()) return
        FirebaseFirestore.getInstance()
            .collection("user")
            .document(uid)
            .set(mapOf("fcm_token" to token), SetOptions.merge())
    }

    fun cancelRoomNotification(context: Context, roomId: String?) {
        if (roomId.isNullOrBlank()) return
        try {
            NotificationManagerCompat.from(context).cancel(roomId.hashCode())
        } catch (_: Throwable) {
        }
    }

    fun showIncomingChatNotification(
        context: Context,
        currentUid: String,
        room: FirestoreRepository.ChatRoomModel
    ) {
        val isCs = room.isCsSupport || room.storeId == FirestoreRepository.CS_STORE_ID
        val isCurrentUserSeller = !isCs && currentUid == room.storeId
        val senderTitle = when {
            isCs -> FirestoreRepository.CS_STORE_NAME
            isCurrentUserSeller -> room.buyerName.ifBlank { "Pembeli GuppyMarket" }
            else -> room.storeName.ifBlank { "Toko GuppyMarket" }
        }

        val chatIntent = Intent(context, ChatActivity::class.java).apply {
            putExtra("EXTRA_CHAT_ROOM_ID", room.id)
            putExtra("EXTRA_STORE_ID", room.storeId)
            putExtra("EXTRA_STORE_NAME", room.storeName)
            putExtra("EXTRA_STORE_LOCATION", room.storeLocation)
            putExtra("EXTRA_BUYER_ID", room.buyerId)
            putExtra("EXTRA_BUYER_NAME", room.buyerName)
            putExtra("EXTRA_IS_CS", isCs)
        }

        showNotificationWithIntent(
            context = context,
            notificationId = room.id.hashCode(),
            title = senderTitle,
            message = room.lastMessage,
            targetChatIntent = chatIntent
        )
    }

    fun showFcmChatNotification(
        context: Context,
        title: String,
        message: String,
        roomId: String,
        storeId: String,
        storeName: String,
        storeLocation: String,
        buyerId: String,
        buyerName: String,
        isCs: Boolean
    ) {
        if (!activeChatRoomId.isNullOrBlank() && activeChatRoomId == roomId) return

        val targetIntent = if (roomId.isNotBlank() || storeId.isNotBlank()) {
            Intent(context, ChatActivity::class.java).apply {
                putExtra("EXTRA_CHAT_ROOM_ID", roomId)
                putExtra("EXTRA_STORE_ID", storeId)
                putExtra("EXTRA_STORE_NAME", storeName)
                putExtra("EXTRA_STORE_LOCATION", storeLocation)
                putExtra("EXTRA_BUYER_ID", buyerId)
                putExtra("EXTRA_BUYER_NAME", buyerName)
                putExtra("EXTRA_IS_CS", isCs)
            }
        } else {
            Intent(context, ChatListActivity::class.java)
        }

        val notifId = if (roomId.isNotBlank()) roomId.hashCode() else System.currentTimeMillis().toInt()
        showNotificationWithIntent(
            context = context,
            notificationId = notifId,
            title = title.ifBlank { "Pesan Baru GuppyMarket" },
            message = message,
            targetChatIntent = targetIntent
        )
    }

    private fun showNotificationWithIntent(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        targetChatIntent: Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addNextIntent(Intent(context, MainActivity::class.java))
            addNextIntent(Intent(context, ChatListActivity::class.java))
            addNextIntent(targetChatIntent)
        }
        val pendingIntent = stackBuilder.getPendingIntent(
            notificationId,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }
}
