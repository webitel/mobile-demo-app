package com.webitel.mobile_demo_app.ui.chat

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_demo_app.data.local.MessageDataItem
import com.webitel.mobile_demo_app.data.local.MessageItem
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.Date


internal class ChatAdapter(
    private val onDownloadFile: (message: MessageItem) -> Unit,
    private val onOpenMedia: (mimeType: String, uri: Uri) -> Unit
) : ListAdapter<MessageDataItem, RecyclerView.ViewHolder>(MessageDiffCallback()) {


    class SentMessageViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var text: TextView
        var time: TextView
        var errImg: ImageView
        var readImg: ImageView
        var errorText: TextView
        var sendProgressBar: ProgressBar
        val fileItem: FileItem

        init {
            time = itemView.findViewById(R.id.time)
            text = itemView.findViewById(R.id.textMessage)
            errorText = itemView.findViewById(R.id.errorText)
            readImg = itemView.findViewById(R.id.daneImageView)
            errImg = itemView.findViewById(R.id.cancelImageView)
            sendProgressBar = itemView.findViewById(R.id.sendProgressBar)
            fileItem = FileItem(
                fileImg = itemView.findViewById(R.id.fileImageView),
                fileContainer = itemView.findViewById(R.id.fileContainer),
                fileInfoContainer = itemView.findViewById(R.id.fileInfoContainer),
                fileTitle = itemView.findViewById(R.id.fileTitle),
                fileSize = itemView.findViewById(R.id.fileSize),
                loadController = itemView.findViewById(R.id.loadController),
                loadPercent = itemView.findViewById(R.id.loadPercent),
                loadProgressBar = itemView.findViewById(R.id.loadProgressBar),
                downloadBtn = itemView.findViewById(R.id.downloadBtn)
            )
        }
    }


    class ReceivedMessageViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var text: TextView
        var time: TextView
        val avatar: ImageView
        val author: TextView
        val fileItem: FileItem

        init {
            text = itemView.findViewById(R.id.textMessage)
            time = itemView.findViewById(R.id.time)
            avatar = itemView.findViewById(R.id.avatarImageView)
            author = itemView.findViewById(R.id.author)
            fileItem = FileItem(
                fileImg = itemView.findViewById(R.id.fileImageView),
                fileContainer = itemView.findViewById(R.id.fileContainer),
                fileInfoContainer = itemView.findViewById(R.id.fileInfoContainer),
                fileTitle = itemView.findViewById(R.id.fileTitle),
                fileSize = itemView.findViewById(R.id.fileSize),
                loadController = itemView.findViewById(R.id.loadController),
                loadPercent = itemView.findViewById(R.id.loadPercent),
                loadProgressBar = itemView.findViewById(R.id.loadProgressBar),
                downloadBtn = itemView.findViewById(R.id.downloadBtn)
            )
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val sm = SentMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_container_sent_message, parent, false)
            )
            sm
        } else {
            val rm = ReceivedMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_container_received_message, parent, false)
            )
            rm
        }
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        when (payloads.lastOrNull()) {
            is MessageChangePayload.Percent -> {
                val message = getItem(position)
                when (holder.itemViewType) {
                    VIEW_TYPE_SENT -> {
                        holder as SentMessageViewHolder
                        serPercent(holder.fileItem, message)
                    }

                    VIEW_TYPE_RECEIVED -> {
                        holder as ReceivedMessageViewHolder
                        serPercent(holder.fileItem, message)
                    }
                }
            }

            else -> onBindViewHolder(holder, position)
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_SENT -> {
                val m = getItem(position)
                val sentHolder = holder as SentMessageViewHolder
                sentHolder.text.text = "${m.body}"
                sentHolder.text.visibility = if (m.body.isNullOrEmpty())
                    View.GONE else View.VISIBLE
                if (m.isSent) {
                    sentHolder.readImg.visibility = View.VISIBLE
                    sentHolder.sendProgressBar.visibility = View.GONE
                    sentHolder.errImg.visibility = View.GONE
                    sentHolder.time.visibility = View.VISIBLE
                    sentHolder.time.text = getDateTime(m.dateCreated)

                } else if (m.errorMessage != null) {
                    val e = "--> code: ${m.errorCode}\n" +
                            "--> description: ${m.errorMessage}"

                    sentHolder.errImg.visibility = View.VISIBLE
                    sentHolder.errorText.visibility = View.VISIBLE
                    sentHolder.errorText.text = e
                    sentHolder.readImg.visibility = View.GONE
                    sentHolder.sendProgressBar.visibility = View.GONE
                    sentHolder.time.visibility = View.GONE

                } else {
                    sentHolder.sendProgressBar.visibility = View.VISIBLE
                    sentHolder.readImg.visibility = View.GONE
                    sentHolder.errImg.visibility = View.GONE
                    sentHolder.time.visibility = View.GONE
                }
                setupFileUi(sentHolder.fileItem, m)
            }

            VIEW_TYPE_RECEIVED -> {
                val m = getItem(position)
                val avatar = if (m.authorType == "bot")
                    R.drawable.ic_droid
                else R.drawable.ic_agent_avatar

                val receivedHolder = holder as ReceivedMessageViewHolder
                receivedHolder.text.visibility = if (m.body.isNullOrEmpty())
                    View.GONE else View.VISIBLE
                receivedHolder.text.text = m.body
                receivedHolder.time.text = getDateTime(m.dateCreated)
                receivedHolder.avatar.setImageResource(avatar)
                receivedHolder.author.text = m.authorName

                setupFileUi(receivedHolder.fileItem, m)
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        val m = getItem(position)
        return if ((m.isIncoming)) {
            VIEW_TYPE_RECEIVED
        } else VIEW_TYPE_SENT
    }


    override fun getItemCount(): Int {
        return currentList.size
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    fun getNewestMessage(): MessageDataItem? {
        return currentList.lastOrNull()
    }


    @SuppressLint("DefaultLocale")
    private fun humanReadableByteCountSI(bytes: Long): String? {
        var bytes = bytes
        if (-1000 < bytes && bytes < 1000) {
            return "$bytes B"
        }
        val ci = StringCharacterIterator("kMGTPE")
        while (bytes <= -999950 || bytes >= 999950) {
            bytes /= 1000
            ci.next()
        }
        return java.lang.String.format("%.1f %cB", bytes / 1000.0, ci.current())
    }


    @SuppressLint("SimpleDateFormat")
    private fun getDateTime(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm")
        return try {

            val netDate = Date(timeStamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            e.toString()
        }
    }


    private fun setupFileUi(fileItem: FileItem, message: MessageDataItem) {
        if (message.mediaName.isNullOrEmpty()) {
            fileItem.fileContainer.visibility = View.GONE
            return
        }

        fileItem.fileContainer.visibility = View.VISIBLE

        if (message.mediaUploading == true
            || message.mediaDownloading == true
        ) {
            fileItem.downloadBtn.visibility = View.GONE
            fileItem.fileImg.visibility = View.GONE
            fileItem.loadController.visibility = View.VISIBLE
            fileItem.fileInfoContainer.visibility = View.VISIBLE
            fileItem.loadPercent.visibility = View.VISIBLE
            fileItem.loadProgressBar.visibility = View.VISIBLE
            serPercent(fileItem, message)
            fileItem.fileSize.text = humanReadableByteCountSI(
                message.mediaSize ?: 0
            )
            fileItem.fileTitle.text = message.mediaName
            return
        }

        if (message.mediaUri.isNullOrEmpty()) {
            fileItem.downloadBtn.visibility = View.VISIBLE
            fileItem.loadController.visibility = View.VISIBLE
            fileItem.loadPercent.visibility = View.GONE
            fileItem.loadProgressBar.visibility = View.GONE
            fileItem.fileInfoContainer.visibility = View.VISIBLE
            fileItem.downloadBtn.setOnClickListener {
                onDownloadFile(message)
            }
            fileItem.fileSize.text = humanReadableByteCountSI(
                message.mediaSize ?: 0
            )
            fileItem.fileTitle.text = message.mediaName
        } else {
            try {
                val mUri = Uri.parse(message.mediaUri)
                val mimeType = message.mediaType ?: ""

                if (mimeType.startsWith("image/")) {
                    fileItem.fileImg.visibility = View.VISIBLE
                    fileItem.loadController.visibility = View.GONE
                    fileItem.fileInfoContainer.visibility = View.GONE

                    Glide.with(fileItem.fileImg.context)
                        .load(mUri)
                        .into(fileItem.fileImg)

                } else {
                    fileItem.downloadBtn.visibility = View.VISIBLE
                    fileItem.loadController.visibility = View.VISIBLE
                    fileItem.loadPercent.visibility = View.GONE
                    fileItem.loadProgressBar.visibility = View.GONE
                    fileItem.fileInfoContainer.visibility = View.VISIBLE
                    fileItem.downloadBtn.setOnClickListener {
                        onOpenMedia(mimeType, mUri)
                    }
                    fileItem.fileSize.text = humanReadableByteCountSI(
                        message.mediaSize ?: 0
                    )
                    fileItem.fileTitle.text = message.mediaName

                    Glide.with(fileItem.fileImg.context)
                        .load(R.drawable.ic_attach_file)
                        .into(fileItem.downloadBtn)
                }

                fileItem.fileImg.setOnClickListener {
                    onOpenMedia(mimeType, mUri)
                }
            } catch (e: Exception) {
                Log.e("setImageURI", e.message.toString())
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun serPercent(fileItem: FileItem, message: MessageDataItem) {
        if (message.mediaUploading == true) {
            fileItem.loadPercent.visibility = View.VISIBLE
            fileItem.loadProgressBar.visibility = View.VISIBLE
            fileItem.downloadBtn.visibility = View.GONE
            fileItem.loadPercent.text = "${
                ((message.mediaUploadedBytes
                    ?: 0) * 100 / (message.mediaSize ?: 0))
            } %"
        } else if (message.mediaDownloading == true) {
            fileItem.loadPercent.visibility = View.VISIBLE
            fileItem.loadProgressBar.visibility = View.VISIBLE
            fileItem.downloadBtn.visibility = View.GONE
            fileItem.loadPercent.text = "${
                ((message.mediaDownloadedBytes
                    ?: 0) * 100 / (message.mediaSize ?: 0))
            } %"
        } else {
            fileItem.loadPercent.visibility = View.GONE
            fileItem.loadProgressBar.visibility = View.GONE
            fileItem.downloadBtn.visibility = View.VISIBLE
        }
    }


    class FileItem(
        val fileImg: ImageView,
        var loadProgressBar: ProgressBar,
        var downloadBtn: ImageView,
        var loadPercent: TextView,
        var fileTitle: TextView,
        var fileSize: TextView,
        var fileContainer: LinearLayout,
        var fileInfoContainer: LinearLayout,
        var loadController: RelativeLayout
    )

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }
}


private class MessageDiffCallback : DiffUtil.ItemCallback<MessageDataItem>() {
    override fun areItemsTheSame(
        oldItem: MessageDataItem,
        newItem: MessageDataItem): Boolean {
        return oldItem.uuid == newItem.uuid
    }


    override fun areContentsTheSame(
        oldItem: MessageDataItem,
        newItem: MessageDataItem): Boolean {

        val body = oldItem.body
        val bodyNew = newItem.body
        val isSent = oldItem.isSent
        val isSentNew = newItem.isSent
        val err = oldItem.errorMessage
        val errNew = newItem.errorMessage
        val mediaUploadedBytes = oldItem.mediaUploadedBytes
        val mediaUploadedBytesNew = newItem.mediaUploadedBytes
        val mediaUploading = oldItem.mediaUploading
        val mediaUploadingNew = newItem.mediaUploading
        val mediaDownloading = oldItem.mediaDownloading
        val mediaDownloadingNew = newItem.mediaDownloading
        val mediaDownloadedBytes = oldItem.mediaDownloadedBytes
        val mediaDownloadedBytesNew = newItem.mediaDownloadedBytes
        val mediaUri = oldItem.mediaUri
        val mediaUriNew = newItem.mediaUri

        return body == bodyNew
                && isSent == isSentNew
                && err == errNew
                && mediaUploadedBytes == mediaUploadedBytesNew
                && mediaUploading == mediaUploadingNew
                && mediaDownloadedBytes == mediaDownloadedBytesNew
                && mediaUri == mediaUriNew
                && mediaDownloading == mediaDownloadingNew
    }


    override fun getChangePayload(oldItem: MessageDataItem, newItem: MessageDataItem): Any? {
        val mediaUploadedBytes = oldItem.mediaUploadedBytes
        val mediaUploadedBytesNew = newItem.mediaUploadedBytes
        val mediaDownloadedBytes = oldItem.mediaDownloadedBytes
        val mediaDownloadedBytesNew = newItem.mediaDownloadedBytes
        return when {
            mediaUploadedBytes != mediaUploadedBytesNew -> {
                MessageChangePayload.Percent(newItem)
            }

            mediaDownloadedBytes != mediaDownloadedBytesNew -> {
                MessageChangePayload.Percent(newItem)
            }

            else -> null
        }
    }
}


private sealed interface MessageChangePayload {
    data class Percent(val message: MessageDataItem) : MessageChangePayload
}