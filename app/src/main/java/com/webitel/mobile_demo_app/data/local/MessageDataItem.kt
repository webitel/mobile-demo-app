package com.webitel.mobile_demo_app.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "message_table", primaryKeys = ["uuid"], indices = [Index(value = ["id"],unique = true)])
data class MessageDataItem(
    override val uuid: String,
    override val id: Long?,
    override val dialogId: String,
    override val authorName: String,
    override val authorType: String,
    override val dateCreated: Long,
    override val body: String?,
    override val isIncoming: Boolean,
    override val isSent: Boolean,
    override val attribute: Long? = null,
    override val mediaName: String? = null,
    override val mediaId: String? = null,
    override val mediaType: String? = null,
    override val mediaSize: Long? = null,
    override val mediaUri: String? = null,
    override val mediaDownloadedBytes: Long? = null,
    override val mediaDownloading: Boolean? = null,
    override val mediaUploading: Boolean? = null,
    override val mediaUploadedBytes: Long? = null,
    override val errorCode: Int? = null,
    override val errorMessage: String? = null
): MessageItem


interface MessageItem {
    val uuid: String
    val id: Long?
    val dialogId: String
    val authorName: String
    val authorType: String
    val dateCreated: Long
    val body: String?
    val isIncoming: Boolean
    val isSent: Boolean
    val attribute: Long?
    val mediaId: String?
    val mediaName: String?
    val mediaType: String?
    val mediaSize: Long?
    val mediaUri: String?
    val mediaDownloadedBytes: Long?
    val mediaDownloading: Boolean?
    val mediaUploading: Boolean?
    val mediaUploadedBytes: Long?
    val errorCode: Int?
    val errorMessage: String?
}
