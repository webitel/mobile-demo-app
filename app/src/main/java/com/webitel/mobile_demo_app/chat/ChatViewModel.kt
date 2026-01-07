package com.webitel.mobile_demo_app.chat

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webitel.mobile_sdk.domain.ConnectListener
import com.webitel.mobile_sdk.domain.ConnectState
import com.webitel.mobile_sdk.domain.DialogListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.FileTransferRequest
import com.webitel.mobile_sdk.domain.HistoryRequest
import com.webitel.mobile_sdk.domain.Message
import com.webitel.mobile_sdk.domain.UploadListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID


class ChatViewModel(
    private val application: Application
) : AndroidViewModel(application), DialogListener, ConnectListener {
    private val authStorage: AuthStorage = AuthStorage(application)
    private val fileCache: FileCache = PreferencesFileCache(application)
    private val chatRepository = ChatRepository(this, this)
    private var authInfo: AuthInfo? = null

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _connectState = MutableStateFlow<ConnectState>(ConnectState.None)
    val connectState = _connectState.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents = _errorEvents.asSharedFlow()


    override fun onMessageAdded(message: Message) {
        updateItem(message)
    }


    override fun onStateChanged(
        from: ConnectState,
        to: ConnectState
    ) {
        _connectState.value = to
    }


    fun sendMessage(text: String) {
        val requestId = UUID.randomUUID().toString()
        _messages.update { list ->
            list + UiMessage(
                requestId = requestId,
                text = text
            )
        }

        viewModelScope.launch {
            try {
                chatRepository.sendMessage(
                    Message
                        .options()
                        .sendId(requestId)
                        .withText(text)
                )
            } catch (e: Error) {
                updateItemWithError(requestId, e.message)
            }
        }
    }


    fun sendMediaMessage(uri: Uri) {
        val attachment = try {
            getAttachmentInfo(application, uri)
        } catch (e: Exception) {
            viewModelScope.launch {
                _errorEvents.emit(e.message.toString())
            }
            return
        }

        val requestId = UUID.randomUUID().toString()
        _messages.update { list ->
            list + UiMessage(
                requestId = requestId,
                localFilePath = uri.toString(),
                fileState = FileState.Transferring,
                sendingFile = attachment
            )
        }
        val transferRequest = FileTransferRequest
            .Builder(
                attachment.stream,
                attachment.title,
                attachment.type
            )
            .transferListener(getNewUploadListener(requestId))
            .build()
        viewModelScope.launch {
            try {
                val result = chatRepository.uploadFile(transferRequest)
                fileCache.put(result.file.id, uri.toString())
                chatRepository.sendMessage(
                    Message
                        .options()
                        .sendId(requestId)
                        .withFile(result.file)
                )

            }catch (e: Error) {
                updateItemWithError(requestId, e.message)
            }
        }
    }


    fun getAuthInfo(): AuthInfo? = authInfo


    fun updateAuthInfo(newAuth: AuthInfo) {
        val old = authInfo
        if (old == newAuth) {
            Log.d(TAG, "updateAuthInfo: auth not changed")
            return
        }

        authInfo = newAuth

        val connectionChanged =
            old?.host != newAuth.host ||
                    old.token != newAuth.token ||
                    old.isWebSocket != newAuth.isWebSocket

        viewModelScope.launch {
            try {
                if (connectionChanged) {
                    chatRepository.initPortalClient(application, newAuth)
                } else {
                    chatRepository.updateUser(newAuth)
                }
                clearList()
                getHistory()
            } catch (e: Error) {
                viewModelScope.launch {
                    _errorEvents.emit(e.message)
                }
                Log.e(TAG, "updateAuthInfo failed", e)
            } catch (e: Exception) {
                viewModelScope.launch {
                    _errorEvents.emit(e.message.toString())
                }
                Log.e(TAG, "updateAuthInfo failed", e)
            }
            authStorage.saveAuthInfo(newAuth)
        }
    }


    private fun getNewUploadListener(requestId: String): UploadListener {
        return object : UploadListener {
            override fun onCanceled() {
                updateItemWithCanceled(requestId)
            }

            override fun onProgress(size: Long) {
                updateItemWithProgress(requestId, size)
            }

            override fun onStarted(pid: String) {
                updateItemWithUploadId(requestId, pid)
            }
        }
    }


    private fun updateItemWithProgress(requestId: String, uploaded: Long) {
        _messages.update { list ->
            var changed = false

            val updated = list.map { item ->
                if (item.requestId != requestId) return@map item

                val newProgress = ((uploaded * 100) / item.fileSize).toInt()
                val oldProgress = item.progress

                if (
                    newProgress > oldProgress &&
                    newProgress % 5 == 0
                ) {
                    changed = true
                    item.copy(progress = newProgress)
                } else {
                    item
                }
            }

            if (changed) updated else list
        }
    }


    private fun updateItemWithUploadId(requestId: String, uploadId: String) {
        _messages.update { list ->
            var changed = false

            val updated = list.map { item ->
                if (item.requestId == requestId) {
                    changed = true
                    item.copy(uploadId = uploadId)
                } else item
            }

            if (changed) updated else list
        }
    }


    private fun updateItemWithCanceled(requestId: String) {
        _messages.update { list ->
            var changed = false

            val updated = list.map { item ->
                if (item.requestId == requestId) {
                    changed = true
                    item.copy(fileState = FileState.Failed, error = "Canceled")
                } else item
            }

            if (changed) updated else list
        }
    }


    private fun updateItemWithError(requestId: String, errorMessage: String) {
        _messages.update { list ->
            var changed = false

            val updated = list.map { item ->
                if (item.requestId == requestId) {
                    changed = true
                    item.copy(error = errorMessage)
                } else item
            }

            if (changed) updated else list
        }
    }


    private fun clearList() {
        _messages.update { list ->
            if (list.isEmpty()) list else listOf()
        }
    }


    private suspend fun getHistory() {
        val request = HistoryRequest.Builder().build()
        val messages = chatRepository.getHistory(request)
        updateList(messages.reversed())
    }


    private fun updateList(messages: List<Message>) {
        _messages.update { list ->
            messages.map { item ->
                val filePath = if (!item.file?.id.isNullOrEmpty()) {
                    fileCache.get(item.file!!.id)
                } else { null }
                UiMessage(message = item, localFilePath = filePath)
            }
        }
    }


    private fun updateItem(message: Message) {
        _messages.update { list ->

            val index = when {
                message.sendId.isNotBlank() ->
                    list.indexOfFirst { it.requestId == message.sendId }
                        .takeIf { it != -1 }
                        ?: list.indexOfFirst { it.message?.id == message.id }

                else ->
                    list.indexOfFirst { it.message?.id == message.id }
            }

            if (index != -1) {
                list.mapIndexed { i, item ->
                    if (i == index) item.copy(message = message,
                        fileState = FileState.Idle)
                    else item
                }
            } else list + UiMessage(message = message)

        }
    }


    companion object {
        const val TAG = "ChatViewModel"
    }


    init {
        authInfo = authStorage.getAuthInfo()

        viewModelScope.launch {
            authInfo?.let {
                try {
                    chatRepository.initPortalClient(application, it)
                    getHistory()
                }catch (e: Error) {
                    Log.e(TAG, e.message)
                    viewModelScope.launch {
                        _errorEvents.emit(e.message)
                    }
                } catch (e: Exception) {
                    viewModelScope.launch {
                        _errorEvents.emit(e.message.toString())
                    }
                    Log.e(TAG, "updateAuthInfo failed", e)
                }
            }
        }
    }


    @SuppressLint("Range")
    private fun getAttachmentInfo(context: Context, attachmentUri: Uri): Attachment {

        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(attachmentUri)
            ?: throw Exception("inputStream is null")

        val cursor = context.contentResolver.query(
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


    inner class Attachment(
        val stream: InputStream,
        val title: String,
        val type: String,
        val path: String,
        val size: Long
    )
}