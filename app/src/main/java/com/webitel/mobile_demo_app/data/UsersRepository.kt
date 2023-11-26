package com.webitel.mobile_demo_app.data

import android.content.Context
import com.webitel.mobile_sdk.domain.User


class UsersRepository(context: Context) {

    private val iss = "https://example.org/kolobok"
    private val storage = SimpleStorage(context)


    fun getUser(): User {
        val user = storage.getUser()
        if (user == null) {
            val newUser = generateUser()
            storage.saveUser(newUser)
            return newUser
        }
        return user
    }


    private fun generateUser(): User {
        return User.Builder(
            iss = iss,
            sub = randomString(15),
            name = randomString(5)
        ).build()
    }


    private fun randomString(l: Int): String {
        val alphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789"
        val s = StringBuilder()
        var i = 0
        while (i < l) {
            s.append(
                alphaNumericString.elementAt(
                    (alphaNumericString.indices).random()
                )
            )
            i++
        }

        return s.toString()
    }
}