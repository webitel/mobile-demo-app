package com.webitel.mobile_demo_app.chat

import com.webitel.mobile_sdk.domain.Message


data class UiMessage(
    val message: Message? = null,
    val requestId: String? = null,
    val text: String? = null,
    val fileState: FileState = FileState.Idle,
    val progress: Int = 0,
    val uploadId: String? = null,
    val localFilePath: String? = null,
    val sendingFile: ChatViewModel.Attachment? = null,
    val error: String? = null
) {
    val textUi: String
        get() {
            return message?.text ?: text.orEmpty()
        }

    val state: MessageState
        get() {
            return if (message != null) MessageState.SENT
            else if (error != null) MessageState.ERROR
            else MessageState.SENDING
        }

    val isFile: Boolean
        get() {
            return !localFilePath.isNullOrEmpty() || !message?.file?.id.isNullOrEmpty()
        }

    val mimeType: String
        get() {
            return message?.file?.type ?: sendingFile?.type ?: ""
        }

    val fileTitle: String
        get() {
            return message?.file?.fileName ?: sendingFile?.title ?: ""
        }

    val fileSize: Long
        get() {
            return message?.file?.size ?: sendingFile?.size ?: 0
        }

}


enum class MessageState {
    SENDING,
    SENT,
    ERROR
}


sealed class FileState {
    data object Idle : FileState()
    data object Transferring : FileState()
    data object Failed : FileState()
}
