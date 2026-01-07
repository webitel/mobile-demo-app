package com.webitel.mobile_demo_app.chat

import android.content.Context
import androidx.core.net.toUri


class PreferencesFileCache(
    private val context: Context
) : FileCache {

    private val prefs = context.getSharedPreferences(
        "file_cache",
        Context.MODE_PRIVATE
    )

    override fun put(fileId: String, localPath: String) {
        prefs.edit()
            .putString(fileId, localPath)
            .apply()
    }

    override fun get(fileId: String): String? {
        val uriString = prefs.getString(fileId, null) ?: return null
        val uri = uriString.toUri()

        val exists = try {
            context.contentResolver.openInputStream(uri)?.close()
            true
        } catch (e: Exception) {
            false
        }

        return if (exists) uriString else {
            prefs.edit().remove(fileId).apply()
            null
        }
    }

    override fun remove(fileId: String) {
        prefs.edit()
            .remove(fileId)
            .apply()
    }

    override fun clear() {
        prefs.edit()
            .clear()
            .apply()
    }
}