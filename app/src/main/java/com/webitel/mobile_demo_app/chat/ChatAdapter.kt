package com.webitel.mobile_demo_app.chat

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.webitel.mobile_demo_app.R
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.Date
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible


internal class ChatAdapter(
    private val onDownloadFile: (UiMessage) -> Unit,
    private val onOpenMedia: (String, Uri) -> Unit
) : ListAdapter<UiMessage, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SENT -> SentMessageViewHolder(
                inflater.inflate(R.layout.item_container_sent_message, parent, false),
                onOpenMedia
            )
            else -> ReceivedMessageViewHolder(
                inflater.inflate(R.layout.item_container_received_message, parent, false),
                onOpenMedia
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.lastOrNull() is MessageChangePayload.Percent) {
            val message = getItem(position)
            (holder as? BaseMessageViewHolder)?.updateProgress(message)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).message?.isIncoming == true)
            VIEW_TYPE_RECEIVED else VIEW_TYPE_SENT


    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }
}


internal abstract class BaseMessageViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    protected val text: TextView = itemView.findViewById(R.id.textMessage)
    protected val time: TextView = itemView.findViewById(R.id.time)

    protected val fileItem = FileItem(
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

    abstract fun bind(message: UiMessage)

    open fun updateProgress(message: UiMessage) {
        if (message.fileState == FileState.Transferring) {
            fileItem.showProgress(message.progress)
        }
    }

    protected fun bindText(message: UiMessage) {
        text.text = message.textUi
        text.isVisible = message.textUi.isNotEmpty()
        time.text = formatTime(message.message?.sentAt ?: 0)
    }

    @SuppressLint("SimpleDateFormat")
    private fun formatTime(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm")
        return try {

            val netDate = Date(timeStamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            e.toString()
        }
    }
}


internal class SentMessageViewHolder(itemView: View, private val onOpenMedia: (String, Uri) -> Unit) :
    BaseMessageViewHolder(itemView) {

    private val readImg: ImageView = itemView.findViewById(R.id.daneImageView)
    private val errorImg: ImageView = itemView.findViewById(R.id.cancelImageView)
    private val errorText: TextView = itemView.findViewById(R.id.errorText)
    private val sendingProgress: ProgressBar = itemView.findViewById(R.id.sendProgressBar)

    override fun bind(message: UiMessage) {
        bindText(message)

        when (message.state) {
            MessageState.SENT -> showSent()
            MessageState.ERROR -> showError(message.error)
            MessageState.SENDING -> showSending()
        }

        fileItem.bind(message, onOpenMedia)
    }

    private fun showSent() {
        readImg.isVisible = true
        sendingProgress.isGone = true
        errorImg.isGone = true
        errorText.isGone = true
        time.isVisible = true
    }

    private fun showSending() {
        sendingProgress.isVisible = true
        readImg.isGone = true
        errorImg.isGone = true
        time.isGone = true
    }

    private fun showError(error: String?) {
        errorImg.isVisible = true
        errorText.isVisible = true
        errorText.text = error.orEmpty()
        readImg.isGone = true
        sendingProgress.isGone = true
        time.isGone = true
    }
}


internal class ReceivedMessageViewHolder(itemView: View, private val onOpenMedia: (String, Uri) -> Unit) :
    BaseMessageViewHolder(itemView) {

    private val avatar: ImageView = itemView.findViewById(R.id.avatarImageView)
    private val author: TextView = itemView.findViewById(R.id.author)

    override fun bind(message: UiMessage) {
        bindText(message)

        avatar.setImageResource(
            if (message.message?.from?.type == "bot")
                R.drawable.ic_droid else R.drawable.ic_agent_avatar
        )
        author.text = message.message?.from?.name

        fileItem.bind(message, onOpenMedia)
    }
}


internal fun FileItem.bind(message: UiMessage,
                           onOpenMedia: (mimeType: String, uri: Uri) -> Unit) {
    if (!message.isFile) {
        fileContainer.isGone = true
        return
    }

    fileContainer.isVisible = true
    fileTitle.text = message.fileTitle
    fileSize.text = message.fileSize.toReadableSize()

    when {
        message.fileState == FileState.Transferring ->
            showProgress(message.progress)

        message.localFilePath.isNullOrEmpty() -> {
            loadController.visibility = View.VISIBLE
            loadPercent.visibility = View.GONE
            loadProgressBar.visibility = View.GONE
            fileInfoContainer.visibility = View.VISIBLE
            downloadBtn.setOnClickListener {
                //onDownloadFile(message)
            }
        }

        else -> {
            try {
                val mUri = message.localFilePath.toUri()

                if (message.mimeType.startsWith("image/")) {
                    fileImg.visibility = View.VISIBLE
                    loadController.visibility = View.GONE
                    fileInfoContainer.visibility = View.GONE

                    Glide.with(fileImg.context)
                        .load(mUri)
                        .into(fileImg)

                } else {
                    downloadBtn.visibility = View.VISIBLE
                    loadController.visibility = View.VISIBLE
                    loadPercent.visibility = View.GONE
                    loadProgressBar.visibility = View.GONE
                    fileInfoContainer.visibility = View.VISIBLE

                    Glide.with(fileImg.context)
                        .load(R.drawable.ic_attach_file)
                        .into(downloadBtn)
                }

                fileImg.setOnClickListener {
                    onOpenMedia(message.mimeType, mUri)
                }
            } catch (e: Exception) {
                Log.e("setImageURI", e.message.toString())
            }
        }
    }
}


@SuppressLint("DefaultLocale")
private fun Long.toReadableSize(): String? {
    var bytes = this
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


internal fun FileItem.showProgress(progress: Int) {
    loadController.isVisible = true
    loadPercent.isVisible = true
    loadProgressBar.isVisible = true
    downloadBtn.isGone = true
    loadPercent.text = "$progress%"
}


class FileItem(
    val fileImg: ImageView,
    var loadProgressBar: ProgressBar,
    var downloadBtn: ImageView,
    var loadPercent: TextView,
    var fileTitle: TextView,
    var fileSize: TextView,
    var fileContainer: ConstraintLayout,
    var fileInfoContainer: LinearLayout,
    var loadController: RelativeLayout
)


private class MessageDiffCallback : DiffUtil.ItemCallback<UiMessage>() {
    override fun areItemsTheSame(
        oldItem: UiMessage,
        newItem: UiMessage): Boolean {
        return oldItem.requestId == newItem.requestId
    }


    override fun areContentsTheSame(
        oldItem: UiMessage,
        newItem: UiMessage): Boolean {
        return oldItem == newItem
    }


    override fun getChangePayload(oldItem: UiMessage, newItem: UiMessage): Any? {
        val progress = oldItem.progress
        val progressNew = newItem.progress
        return when {
            progress != progressNew -> {
                MessageChangePayload.Percent(newItem)
            }
            else -> null
        }
    }
}


private sealed interface MessageChangePayload {
    data class Percent(val message: UiMessage) : MessageChangePayload
}