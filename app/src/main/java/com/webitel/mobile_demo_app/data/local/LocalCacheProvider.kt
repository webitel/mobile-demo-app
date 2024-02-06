package com.webitel.mobile_demo_app.data.local

import android.content.Context
import androidx.lifecycle.lifecycleScope
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.UUID


@Database(entities = [MessageDataItem::class], version = 3, exportSchema = false)
abstract class LocalCacheProvider : RoomDatabase() {

    abstract fun messagesDao(): MessagesDao

    companion object {
        val INSTANCE get() = _instance ?: error("call LocalCacheProvider.createInstance() first")

        private var _instance: LocalCacheProvider? = null

        fun createInstance(context: Context) {
            check(_instance == null) { "LocalCacheProvider singleton instance has been already created" }
            _instance = Room.databaseBuilder(context.applicationContext, LocalCacheProvider::class.java, "database.db")
                .fallbackToDestructiveMigration()
                .build()
            _instance
        }


    }
}