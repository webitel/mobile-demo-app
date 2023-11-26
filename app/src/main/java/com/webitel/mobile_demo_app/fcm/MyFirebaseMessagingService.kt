package com.webitel.mobile_demo_app.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.RegisterResult


class MyFirebaseMessagingService: FirebaseMessagingService() {


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
        DemoApp.instance.portalClient.registerFCMToken(
            token, object : CallbackListener<RegisterResult> {
            override fun onError(e: Error) {
                Log.e("registerFCMErr", e.message)
            }
            override fun onSuccess(t: RegisterResult) {}
        })
    }
}