package com.webitel.mobile_demo_app.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.notifications.Notifications
import kotlinx.coroutines.runBlocking


class MyFirebaseMessagingService : FirebaseMessagingService() {


    override fun onNewToken(token: String) {
        sendRegistrationToServer(token)
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            val text = it.body
            if (!text.isNullOrEmpty()) {
                Notifications.instance.showMessageNotification(
                    DemoApp.instance, it.title ?: "Service", text
                )
            }
        }
    }


    private fun sendRegistrationToServer(token: String) {
        runBlocking {
            try {
                DemoApp.instance.portalClient2.registerFCMToken(token)
            } catch (e: Exception) {
                Log.e("sendRegistration", e.message.toString())
            }
        }
    }
}