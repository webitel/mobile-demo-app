package com.webitel.mobile_demo_app.chat

interface FileCache {

    fun put(fileId: String, localPath: String)

    fun get(fileId: String): String?

    fun remove(fileId: String)

    fun clear()
}