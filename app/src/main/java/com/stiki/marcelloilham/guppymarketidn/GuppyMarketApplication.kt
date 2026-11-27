package com.stiki.marcelloilham.guppymarketidn

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.stiki.marcelloilham.guppymarketidn.util.AuctionNotificationHelper
import com.stiki.marcelloilham.guppymarketidn.util.ChatNotificationHelper

class GuppyMarketApplication : Application() {

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            ChatNotificationHelper.startListening(this, user.uid)
            ChatNotificationHelper.syncFcmToken(user.uid)
            AuctionNotificationHelper.startListening(this, user.uid)
        } else {
            ChatNotificationHelper.stopListening()
            AuctionNotificationHelper.stopListening()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ChatNotificationHelper.ensureChannel(this)
        AuctionNotificationHelper.ensureChannel(this)
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }
}
