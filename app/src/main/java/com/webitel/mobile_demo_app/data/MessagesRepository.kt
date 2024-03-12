package com.webitel.mobile_demo_app.data

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.data.local.LocalCacheProvider
import com.webitel.mobile_demo_app.data.local.MessageDataItem
import com.webitel.mobile_demo_app.data.remote.PortalCustomerService
import com.webitel.mobile_demo_app.data.remote.PortalException
import com.webitel.mobile_demo_app.data.remote.RemoteRepository
import com.webitel.mobile_demo_app.extensions.toData
import com.webitel.mobile_sdk.domain.DialogListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.MediaUploadListener
import com.webitel.mobile_sdk.domain.Message
import com.webitel.mobile_sdk.domain.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID


class MessagesRepository(
    private val remoteRepository: RemoteRepository,
    database: LocalCacheProvider
) {
    private val dao = database.messagesDao()
    val messages = MutableLiveData<List<MessageDataItem>>(arrayListOf())

    init {
        CoroutineScope(Dispatchers.IO).launch {
            refreshMessages()
        }
    }

    suspend fun loadHistory(): Int {
        val id = dao.getOldestMessagesId() ?: 0
        val m = remoteRepository.getHistory(
            PortalCustomerService.Params(limit = 20, offset = id),
            dialogListener
        )

        val md = m.map {
            it.toData()
        }

        saveMessages(md)
        return md.size
    }


    suspend fun loadUpdates(): Int {
        val limitItems = 20
        var sizeReceived = 0
        var sizeSaved = 0

        var id = dao.getNewestMessagesId() ?: 0
        if (id == 0L) return loadHistory()

        do {
            try {
                val m = remoteRepository.getUpdates(
                    PortalCustomerService.Params(limit = limitItems, offset = id),
                    dialogListener
                )

                sizeSaved += m.size
                sizeReceived = m.size
                id = m.lastOrNull()?.id ?: 0

                val md = m.map {
                    it.toData()
                }
                saveMessages(md)
            }catch (e: PortalException) {
                Log.e("loadUpdates", e.message)
            }

        } while (sizeReceived == limitItems)

        return sizeSaved
    }


    suspend fun downloadFile(messageId: Long) {
        val message = dao.getMessageById(messageId)
        if (message?.mediaDownloading == true) {
            return
        }

        val fileId = message?.mediaId
        if (fileId.isNullOrEmpty()) {
            return
        }

        dao.startMediaDownload(messageId)
        refreshMessages()

        remoteRepository.downloadFile(
            fileId = fileId,
            observer = getStreamObserver(
                uuid = message.uuid,
                messageId,
                message.mediaName ?: "unknown",
                message.mediaType ?: "application/octet-stream"
            ),
            listener = dialogListener
        )
    }


    private fun getStreamObserver(
        uuid: String,
        messageId: Long,
        fileName: String,
        mimeType: String
    ): StreamObserver {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val resolver = DemoApp.instance.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        }

        val uri = resolver.insert(collection, contentValues)
        val outputStream = resolver.openOutputStream(uri!!)
        var bytes: Long = 0

        return object : StreamObserver {
            override fun onCompleted() {
                outputStream?.close()
                CoroutineScope(Dispatchers.IO).launch {
                    dao.endMediaDownload(messageId, uri.toString())
                    refreshMessages()
                }
            }

            override fun onError(e: Error) {
                outputStream?.close()
                CoroutineScope(Dispatchers.IO).launch {
                    dao.endMediaDownload(messageId, null)
                    refreshMessages()
                }
                Log.e("downloadFileErr", e.message)
            }

            override fun onNext(value: ByteArray) {
                outputStream?.write(value)
                bytes += value.size
                CoroutineScope(Dispatchers.IO).launch {
                    dao.updateMediaDownloadedBytes(uuid, bytes)
                    refreshMessages()
                }
                Log.e("downloadFileNex", value.size.toString())
            }
        }
    }


    inner class Attachment(
        val stream: InputStream,
        val title: String,
        val type: String,
        val path: String,
        val size: Long
    )


    @SuppressLint("Range")
    private fun getAttachmentInfo(attachmentUri: Uri?): Attachment? {
        if (attachmentUri == null) return null

        val contentResolver = DemoApp.instance.contentResolver
        val inputStream = contentResolver.openInputStream(attachmentUri)
            ?: return null

        val cursor = DemoApp.instance.contentResolver.query(
            attachmentUri, null, null, null, null
        )
        cursor?.moveToFirst()

        val name =
            cursor?.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)) ?: "unknown"
        val size =
            cursor?.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                ?: inputStream.available().toLong()
        val type = contentResolver.getType(attachmentUri) ?: ""

        cursor?.close()

        return Attachment(
            inputStream,
            title = name,
            type = type,
            path = attachmentUri.toString(),
            size
        )
    }

    suspend fun sendMessage(text: String, attachmentUri: Uri? = null) {

        if (text.isEmpty() && attachmentUri == null) {
            Log.e("sendTextMessageError", "text and attachmentUri cannot be empty")
            return
        }

        val uuid = UUID.randomUUID().toString()
        val lastId = dao.getNewestMessagesId()

        val attachment = getAttachmentInfo(attachmentUri)

        val m = MessageDataItem(
            uuid = uuid,
            id = null,
            dialogId = "",
            authorName = "You",
            authorType = "",
            dateCreated = System.currentTimeMillis(),
            body = text,
            isIncoming = false,
            isSent = false,
            attribute = lastId,
            mediaName = attachment?.title,
            mediaType = attachment?.type,
            mediaSize = attachment?.size,
            mediaUri = attachment?.path,
            mediaUploading = !attachment?.path.isNullOrEmpty(),
        )

        saveMessages(listOf(m))

        val options = Message.options()

        if (text.isNotEmpty()) options.withText(text)
        if (attachment != null) {
            options.withMedia(
                stream = attachment.stream,
                fileName = attachment.title,
                mimeType = attachment.type
            )

            options.uploadListener(object : MediaUploadListener {
                override fun onCompleted() {
                    CoroutineScope(Dispatchers.IO).launch {
                        dao.mediaUploadComplete(uuid)
                        refreshMessages()
                    }
                }

                override fun onProgress(bytesSent: Long) {
                    CoroutineScope(Dispatchers.IO).launch {
                        Log.e(
                            "uploadListener",
                            "size - ${attachment.size}; bytesSent - $bytesSent"
                        )
                        dao.updateMediaUploadedBytes(uuid, bytesSent)
                        refreshMessages()
                    }
                }
            })
        }


        try {
            val res = remoteRepository.sendMessage(dialogListener, options)
            res?.let {
                val id = if (res.id == 0L) null else res.id
                setMessageId(uuid, id, res.file?.id)
            }
        } catch (e: PortalException) {
            Log.e("sendTextMessage", "${e.code.name}; ${e.message}")
            dao.updateMessageError(uuid, e.message, e.code.ordinal)
            refreshMessages()
        }
    }

    suspend fun refreshMessages() {
        val m = dao.getAllMessages()
        messages.postValue(m)
    }


    private val dialogListener = object : DialogListener {
        override fun onMessageAdded(message: Message) {
            CoroutineScope(Dispatchers.IO).launch {
                saveMessages(listOf(message.toData()))
            }
        }
    }


    private suspend fun saveMessages(messages: List<MessageDataItem>) {
        try {
            dao.insert(messages)
            refreshMessages()
        } catch (e: Exception) {
            Log.e("saveMessages", e.message.toString())
        }
    }

    private suspend fun setMessageId(uuid: String, id: Long?, fileId: String?) {
        try {
            dao.updateMessageId(uuid, id, fileId, true)
            refreshMessages()
        } catch (e: Exception) {
            Log.e("setMessageId", e.message.toString())
        }
    }
}
