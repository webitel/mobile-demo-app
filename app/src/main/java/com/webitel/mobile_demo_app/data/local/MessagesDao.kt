package com.webitel.mobile_demo_app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessagesDao {

    @Query("SELECT * FROM message_table ORDER BY CASE WHEN `id` IS NULL THEN attribute ELSE `id` END")
    suspend fun getAllMessages(): List<MessageDataItem>

    @Query("SELECT COUNT(id) FROM message_table")
    suspend fun getCountMessages(): Int

    @Query("SELECT MAX(id) FROM message_table")
    suspend fun getNewestMessagesId(): Long?
    @Query("SELECT MIN(id) FROM message_table WHERE id > 0")
    suspend fun getOldestMessagesId(): Long?

    @Query("SELECT * FROM message_table WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageDataItem?

    @Query("UPDATE message_table SET body = :text WHERE uuid = :uuid")
    suspend fun updateMessageBody(uuid: String, text: String)

    @Query("UPDATE message_table SET mediaDownloading = 1 WHERE id = :id")
    suspend fun startMediaDownload(id: Long)

    @Query("UPDATE message_table SET mediaDownloading = 0, mediaUri = :location WHERE id = :id")
    suspend fun endMediaDownload(id: Long, location: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: List<MessageDataItem>)

    @Query("UPDATE message_table SET id = :messageId, mediaId = :fileId, isSent = :isSent WHERE uuid = :uuid")
    suspend fun updateMessageId(uuid: String, messageId: Long?, fileId: String?, isSent: Boolean)

    @Query("UPDATE message_table SET mediaUploading = 0 WHERE uuid = :uuid")
    suspend fun  mediaUploadComplete(uuid: String)
    @Query("UPDATE message_table SET mediaUploading = 1 WHERE uuid = :uuid")
    suspend fun startMediaUpload(uuid: String)

    @Query("UPDATE message_table SET mediaUploadedBytes = :mediaUploadedBytes  WHERE uuid = :uuid")
    suspend fun updateMediaUploadedBytes(uuid: String, mediaUploadedBytes: Long)
    @Query("UPDATE message_table SET mediaDownloadedBytes = :downloadedBytes WHERE uuid = :uuid")
    suspend fun updateMediaDownloadedBytes(uuid: String, downloadedBytes: Long)

    @Query("UPDATE message_table SET mediaUri = :location WHERE uuid = :uuid")
    suspend fun updateMediaLocation(uuid: String, location: String)

    @Query("UPDATE message_table SET errorMessage = :errorMessage, errorCode = :errorCode WHERE uuid = :uuid")
    suspend fun updateMessageError(uuid: String, errorMessage: String?, errorCode: Int?)
}