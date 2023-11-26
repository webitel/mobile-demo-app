package com.webitel.mobile_demo_app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.ui.MainActivity


class Notifications private constructor() {
    private val callsChannelId: String = "calls_channel_w"
    private val chatChannelId: String = DemoApp.instance
        .resources.getString(R.string.default_notification_channel_id)
    private var notificationManager: NotificationManager =
        DemoApp.instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
    private val callsChannel = NotificationChannel(
        callsChannelId,
        "Webitel Calls",
        NotificationManager.IMPORTANCE_LOW
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private val chatChannel = NotificationChannel(
        chatChannelId,
        "Webitel Chat",
        NotificationManager.IMPORTANCE_HIGH
    )


    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createCallsChannel()
            createChatChannel()
        }
    }


    fun showCallNotification(context: Context) {
        notificationManager.notify(callNotificationId, outgoingCallNotification(context))
    }


    fun showMessageNotification(context: Context, title: String, body: String) {
        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            messageNotification(context, title, body)
        )
    }


    fun cancelCallNotification() {
        notificationManager.cancel(callNotificationId)
    }


    private fun messageNotification(context: Context, title: String, body: String): Notification {
        return NotificationCompat.Builder(context, chatChannelId)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createCallsChannel() {
        if (!notificationManager.notificationChannels.contains(callsChannel)) {
            callsChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            callsChannel.setSound(null, null)
            notificationManager.createNotificationChannel(callsChannel)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChatChannel() {
        if (!notificationManager.notificationChannels.contains(chatChannel)) {
            chatChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            chatChannel.setSound(null, null)
            notificationManager.createNotificationChannel(chatChannel)
        }
    }


    private val callNotificationId = 1
    private fun outgoingCallNotification(context: Context): Notification {

        val activeContent = getActiveCallContent(context)

        val containerPendingIntent = getContainerPendingIntent(context, callNotificationId)
        val hangupPendingIntent = getHangupPendingIntent(context, callNotificationId)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val incomingCaller = Person.Builder()
                .setName("Webitel")
                .setUri("service")
                .setImportant(true)
                .build()
            Notification.Builder(context, callsChannelId)
                .setContentIntent(containerPendingIntent)
                .setFullScreenIntent(containerPendingIntent, false)
                .setSmallIcon(R.drawable.ic_baseline_call_24)
                .setStyle(
                    Notification.CallStyle.forOngoingCall(incomingCaller, hangupPendingIntent)
                )
                .addPerson(incomingCaller)
                .build()

        } else {
            NotificationCompat.Builder(context, callsChannelId)
                .setSmallIcon(R.drawable.ic_baseline_call_24)
                .setContentTitle("service")
                .setContentText(activeContent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setContentIntent(containerPendingIntent)
                .setFullScreenIntent(containerPendingIntent, true)
                .build()
        }
    }


    private fun getContainerPendingIntent(context: Context, id: Int): PendingIntent {
        val containerIntent = Intent(context, MainActivity::class.java)
        containerIntent.action = "OPEN_CALL_DETAIL_FROM_NOTIFY"
        containerIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        return PendingIntent.getActivity(
            context, id,
            containerIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    private fun getHangupPendingIntent(context: Context, id: Int): PendingIntent {
        val hangupIntent = Intent(context, MainActivity::class.java)
        hangupIntent.action = "ACTION_HANGUP_CALL_FROM_NOTIFY"
        return PendingIntent.getActivity(
            context, id,
            hangupIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    private fun getActiveCallContent(context: Context): String {
        return try {
            context.resources.getString(R.string.call_state_active)
        } catch (e: Exception) {
            "Webitel call"
        }
    }


    companion object {
        val instance = Notifications()
    }
}