package com.webitel.mobile_demo_app.chat


data class AuthInfo(
    val host: String,
    val token: String,
    val issuer: String,
    val userName: String,
    val isWebSocket: Boolean
)