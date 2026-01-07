package com.webitel.mobile_demo_app.chat

import android.content.Context
import android.content.SharedPreferences

class AuthStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HOST = "key_host"
        private const val KEY_TOKEN = "key_token"
        private const val KEY_ISSUER = "key_issuer"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_IS_WEBSOCKET = "key_is_websocket"
    }


    fun saveAuthInfo(info: AuthInfo) {
        prefs.edit()
            .putString(KEY_HOST, info.host)
            .putString(KEY_TOKEN, info.token)
            .putString(KEY_ISSUER, info.issuer)
            .putString(KEY_USER_NAME, info.userName)
            .putBoolean(KEY_IS_WEBSOCKET, info.isWebSocket)
            .apply()
    }


    fun getAuthInfo(): AuthInfo? {
        val host = prefs.getString(KEY_HOST, null)
        val token = prefs.getString(KEY_TOKEN, null)
        val issuer = prefs.getString(KEY_ISSUER, null)
        val userName = prefs.getString(KEY_USER_NAME, null)
        val isWebsocket = prefs.getBoolean(KEY_IS_WEBSOCKET, false)

        return if (host != null && token != null && issuer != null && userName != null) {
            AuthInfo(host, token, issuer, userName, isWebsocket)
        } else null
    }


    fun clear() {
        prefs.edit().clear().apply()
    }


    fun getHost(): String? = prefs.getString(KEY_HOST, null)
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getIssuer(): String? = prefs.getString(KEY_ISSUER, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
}