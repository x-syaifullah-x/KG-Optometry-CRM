package com.lizpostudio.kgoptometrycrm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class Messaging : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        println("onNewToken($token)")
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        println(notification?.body)
        super.onMessageReceived(message)
    }
}