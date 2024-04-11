package com.webitel.mobile_demo_app.extensions

import com.webitel.mobile_demo_app.data.local.MessageDataItem
import com.webitel.mobile_sdk.domain.Message
import java.util.UUID

fun Message.toData(): MessageDataItem {
    return MessageDataItem(
        uuid = this.sendId.ifEmpty { UUID.randomUUID().toString() },
        id = this.id,
        dialogId = "",
        authorName = this.from.name,
        authorType = this.from.type,
        dateCreated = this.sentAt,
        body = this.text,
        isIncoming = this.isIncoming,
        isSent = true,
        mediaId = this.file?.id,
        mediaName = this.file?.fileName,
        mediaType = this.file?.type,
        mediaSize = this.file?.size,
        mediaDownloading = false,
        mediaUploading = false,
        errorCode = this.error?.code?.ordinal ?: 0,
        errorMessage = this.error?.message
    )
}