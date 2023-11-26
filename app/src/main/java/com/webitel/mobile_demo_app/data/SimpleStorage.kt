package com.webitel.mobile_demo_app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.webitel.mobile_sdk.domain.User

class SimpleStorage(context: Context) {

    private val STORE_KEY_USER_DATA = "simple_user"
    private val SHARED_PREFS = "simple_store"

    private var gson: Gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        SHARED_PREFS,
        Context.MODE_PRIVATE
    )
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()


    fun getUser(): User? {
        val json =  sharedPreferences.getString(STORE_KEY_USER_DATA, null)
        if (!json.isNullOrEmpty()) {
            return gson.fromJson(json, User::class.java)
        }
        return null
    }


    fun saveUser(user: User) {
        editor.putString(STORE_KEY_USER_DATA, gson.toJson(user)).commit()
    }
}