package com.stiki.marcelloilham.guppymarketidn.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stiki.marcelloilham.guppymarketidn.util.AuctionNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.ChatNotificationHelper

class GuppyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            ChatNotificationHelper.saveFcmToken(uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val title = remoteMessage.notification?.title ?: data["title"] ?: "Notifikasi GuppyMarket"
        val body = remoteMessage.notification?.body ?: data["body"] ?: data["message"] ?: ""
        if (body.isBlank()) return

        val auctionId = data["auctionId"].orEmpty()
        val type = data["type"].orEmpty()
        if (auctionId.isNotBlank() || type.startsWith("AUCTION", ignoreCase = true)) {
            AuctionNotificationHelper.showFcmAuctionNotification(
                context = applicationContext,
                title = title,
                message = body,
                auctionId = auctionId
            )
            return
        }

        val roomId = data["roomId"].orEmpty()
        val storeId = data["storeId"].orEmpty()
        val storeName = data["storeName"].orEmpty()
        val storeLocation = data["storeLocation"].orEmpty()
        val buyerId = data["buyerId"].orEmpty()
        val buyerName = data["buyerName"].orEmpty()
        val isCs = data["isCs"] == "true"

        ChatNotificationHelper.showFcmChatNotification(
            context = applicationContext,
            title = title,
            message = body,
            roomId = roomId,
            storeId = storeId,
            storeName = storeName,
            storeLocation = storeLocation,
            buyerId = buyerId,
            buyerName = buyerName,
            isCs = isCs
        )
    }
}
