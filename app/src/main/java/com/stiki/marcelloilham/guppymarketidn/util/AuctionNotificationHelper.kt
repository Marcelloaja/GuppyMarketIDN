package com.stiki.marcelloilham.guppymarketidn.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.ListenerRegistration
import com.stiki.marcelloilham.guppymarketidn.MainActivity
import com.stiki.marcelloilham.guppymarketidn.R
import com.stiki.marcelloilham.guppymarketidn.menuAuction.DetailAuctionActivity
import com.stiki.marcelloilham.guppymarketidn.repository.FirestoreRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * Listens for live auctions where the authenticated user is a bidder (`participantIds`)
 * and triggers high-priority notifications for:
 * 1. Outbid ("Posisi Lelang Tersingkirkan") when another bidder overtakes them.
 * 2. Auction Won ("Pemenang Lelang") when the auction finishes and they hold the highest bid.
 */
object AuctionNotificationHelper {

    const val CHANNEL_ID = "guppymarket_auction_channel"
    private const val CHANNEL_NAME = "Status Lelang & Penawaran"
    private const val CHANNEL_DESC = "Notifikasi saat penawaran tersalip atau memenangkan lelang"
    private const val PREFS_NAME = "auction_notif_prefs"

    @Volatile
    var activeAuctionDetailId: String? = null

    private var listenerUid: String? = null
    private var listenerContext: Context? = null
    private var auctionListener: ListenerRegistration? = null
    private var isInitialSnapshot: Boolean = true

    private val wasHighestBidderByAuction = ConcurrentHashMap<String, Boolean>()
    private val lastNotifiedOutbidKey = ConcurrentHashMap<String, String>()
    private val knownAuctionsById = ConcurrentHashMap<String, FirestoreRepository.AuctionModel>()

    private val expiryHandler = Handler(Looper.getMainLooper())
    private val expiryRunnable = Runnable { evaluateAuctionStates() }

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
        if (listenerUid == uid && auctionListener != null) return

        stopListening()
        val appCtx = context.applicationContext
        listenerContext = appCtx
        listenerUid = uid
        ensureChannel(appCtx)

        isInitialSnapshot = true
        wasHighestBidderByAuction.clear()
        lastNotifiedOutbidKey.clear()
        knownAuctionsById.clear()

        auctionListener = FirestoreRepository.listenUserParticipatedAuctions(uid) { auctions ->
            val currentTime = System.currentTimeMillis()
            val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            knownAuctionsById.clear()
            for (auction in auctions) {
                knownAuctionsById[auction.id] = auction
            }

            if (isInitialSnapshot) {
                isInitialSnapshot = false
                for (auction in auctions) {
                    val isHighest = auction.highestBidderId == uid
                    wasHighestBidderByAuction[auction.id] = isHighest
                    if (auction.endTimeMillis in 1L..currentTime && isHighest) {
                        val wonKey = "won_${uid}_${auction.id}"
                        // Mark older finished wins silently if already > 5 mins old so reopening app won't replay old wins
                        if (!prefs.getBoolean(wonKey, false) && (currentTime - auction.endTimeMillis) > 5 * 60_000L) {
                            prefs.edit().putBoolean(wonKey, true).apply()
                        }
                    }
                }
            }

            evaluateAuctionStates()
        }
    }

    fun stopListening() {
        expiryHandler.removeCallbacks(expiryRunnable)
        auctionListener?.remove()
        auctionListener = null
        listenerUid = null
        listenerContext = null
        isInitialSnapshot = true
        wasHighestBidderByAuction.clear()
        lastNotifiedOutbidKey.clear()
        knownAuctionsById.clear()
    }

    fun onUserPlacedBid(context: Context, uid: String, auctionId: String) {
        if (uid.isBlank() || auctionId.isBlank()) return
        wasHighestBidderByAuction[auctionId] = true
        cancelAuctionNotification(context, auctionId)
        FirestoreRepository.ensureAuctionParticipant(auctionId, uid)
    }

    fun cancelAuctionNotification(context: Context, auctionId: String?) {
        if (auctionId.isNullOrBlank()) return
        try {
            NotificationManagerCompat.from(context).cancel(("outbid_$auctionId").hashCode())
        } catch (_: Throwable) {
        }
    }

    private fun evaluateAuctionStates() {
        val ctx = listenerContext ?: return
        val uid = listenerUid ?: return
        val now = System.currentTimeMillis()
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var nearestEndMillis: Long = Long.MAX_VALUE

        for (auction in knownAuctionsById.values) {
            if (auction.sellerId == uid) continue

            val isUserHighest = auction.highestBidderId == uid
            val wasUserHighest = wasHighestBidderByAuction[auction.id] == true
            val isLive = auction.endTimeMillis > now
            val isFinished = auction.endTimeMillis in 1L..now

            if (isLive) {
                if (wasUserHighest && !isUserHighest && auction.highestBidderId.isNotBlank()) {
                    val outbidKey = "${auction.currentBidNumeric}_${auction.highestBidderId}"
                    if (lastNotifiedOutbidKey[auction.id] != outbidKey) {
                        lastNotifiedOutbidKey[auction.id] = outbidKey
                        if (activeAuctionDetailId != auction.id) {
                            val overtaker = auction.highestBidderName.ifBlank { "penawar lain" }
                            showAuctionNotification(
                                context = ctx,
                                notificationId = ("outbid_${auction.id}").hashCode(),
                                title = "Posisi Lelang Tersingkirkan!",
                                message = "Penawaranmu pada '${auction.title}' disalip $overtaker (${auction.currentBid}). Pasang bid lagi!",
                                auction = auction
                            )
                        }
                    }
                }
                wasHighestBidderByAuction[auction.id] = isUserHighest
                if (isUserHighest && auction.endTimeMillis < nearestEndMillis) {
                    nearestEndMillis = auction.endTimeMillis
                }
            } else if (isFinished) {
                if (isUserHighest) {
                    val wonKey = "won_${uid}_${auction.id}"
                    if (!prefs.getBoolean(wonKey, false)) {
                        prefs.edit().putBoolean(wonKey, true).apply()
                        showAuctionNotification(
                            context = ctx,
                            notificationId = ("won_${auction.id}").hashCode(),
                            title = "Selamat! Anda Pemenang Lelang 🏆",
                            message = "Anda memenangkan '${auction.title}' dengan tawaran akhir ${auction.currentBid}.",
                            auction = auction
                        )
                    }
                }
                wasHighestBidderByAuction[auction.id] = isUserHighest
            }
        }

        expiryHandler.removeCallbacks(expiryRunnable)
        if (nearestEndMillis != Long.MAX_VALUE) {
            val delayMs = (nearestEndMillis - now + 300L).coerceIn(500L, 60 * 60 * 1000L)
            expiryHandler.postDelayed(expiryRunnable, delayMs)
        }
    }

    fun showFcmAuctionNotification(
        context: Context,
        title: String,
        message: String,
        auctionId: String
    ) {
        val dummyModel = FirestoreRepository.AuctionModel(id = auctionId)
        val notifId = if (auctionId.isNotBlank()) ("fcm_auc_$auctionId").hashCode() else System.currentTimeMillis().toInt()
        showAuctionNotification(
            context = context,
            notificationId = notifId,
            title = title.ifBlank { "Update Lelang GuppyMarket" },
            message = message,
            auction = dummyModel
        )
    }

    private fun showAuctionNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        auction: FirestoreRepository.AuctionModel
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val detailIntent = Intent(context, DetailAuctionActivity::class.java).apply {
            putExtra("EXTRA_AUCTION_ID", auction.id)
            putExtra("EXTRA_AUCTION_SELLER_ID", auction.sellerId)
            putExtra("auction_title", auction.title.ifBlank { "Detail Lelang" })
            putExtra("EXTRA_AUCTION_SELLER", auction.seller)
            putExtra("EXTRA_AUCTION_LOCATION", auction.location)
            putExtra("EXTRA_AUCTION_PRICE", auction.currentBid)
            putExtra("EXTRA_AUCTION_PRICE_NUMERIC", auction.currentBidNumeric)
            putExtra("EXTRA_AUCTION_BIDS_COUNT", auction.bidsCount)
            putExtra("EXTRA_AUCTION_HIGHEST_BIDDER", auction.highestBidderName)
            putExtra("EXTRA_AUCTION_INCREMENT", auction.bidIncrementNumeric)
            putExtra("EXTRA_AUCTION_GRADE", auction.grade)
            putExtra("EXTRA_AUCTION_IMAGE", auction.imageRes)
            putExtra("EXTRA_AUCTION_END_TIME_MILLIS", auction.endTimeMillis)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "BidFragment")
        }

        val stackBuilder = TaskStackBuilder.create(context).apply {
            addNextIntent(mainIntent)
            addNextIntent(detailIntent)
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
            .setCategory(NotificationCompat.CATEGORY_STATUS)
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
