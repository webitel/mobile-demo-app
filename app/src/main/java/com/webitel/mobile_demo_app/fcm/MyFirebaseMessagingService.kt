package com.webitel.mobile_demo_app.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {}
    override fun onMessageReceived(remoteMessage: RemoteMessage) {}
}