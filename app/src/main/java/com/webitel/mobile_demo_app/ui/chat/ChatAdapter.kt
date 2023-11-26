package com.webitel.mobile_demo_app.ui.chat

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.webitel.mobile_demo_app.R
import com.webitel.mobile_sdk.domain.Message
import java.text.SimpleDateFormat
import java.util.Date

class ChatAdapter(
    private var messages: ArrayList<Message>
    ): RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    class SentMessageViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var text: TextView
        var time: TextView
        var errImg: ImageView
        var readImg: ImageView
        var errorText: TextView
        var sendProgressBar: ProgressBar

        init {
            time = itemView.findViewById(R.id.time)
            text = itemView.findViewById(R.id.textMessage)
            errorText = itemView.findViewById(R.id.errorText)
            readImg = itemView.findViewById(R.id.daneImageView)
            errImg = itemView.findViewById(R.id.cancelImageView)
            sendProgressBar = itemView.findViewById(R.id.sendProgressBar)
        }
    }


    fun setMessages(messages: ArrayList<Message>) {
        this.messages = messages
    }


    class ReceivedMessageViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var text: TextView
        var time: TextView
        val avatar: ImageView
        val author: TextView

        init {
            text = itemView.findViewById(R.id.textMessage)
            time = itemView.findViewById(R.id.time)
            avatar = itemView.findViewById(R.id.avatarImageView)
            author = itemView.findViewById(R.id.author)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val sm = SentMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_container_sent_message, parent, false)
            )
            sm.setIsRecyclable(false)
            sm
        } else {
            val rm = ReceivedMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_container_received_message, parent, false)
            )
            rm.setIsRecyclable(false)
            rm
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            VIEW_TYPE_SENT -> {
                val sentHolder = holder as SentMessageViewHolder
                sentHolder.text.text = "${messages[position].text}"

                if (messages[position].sentAt > 0) {
                    sentHolder.readImg.visibility = View.VISIBLE
                    sentHolder.sendProgressBar.visibility = View.GONE
                    sentHolder.errImg.visibility = View.GONE
                    sentHolder.time.visibility = View.VISIBLE
                    sentHolder.time.text = getDateTime(messages[position].sentAt)

                } else if (messages[position].error != null) {
                    val e = "--> code: ${messages[position].error?.code}\n"+
                        "--> description: ${messages[position].error?.message}"

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
            }

            VIEW_TYPE_RECEIVED -> {
                val avatar = if (messages[position].from.type == "bot")
                    R.drawable.ic_droid
                else R.drawable.ic_agent_avatar

                val receivedHolder = holder as ReceivedMessageViewHolder
                receivedHolder.text.text = messages[position].text
                receivedHolder.time.text = getDateTime(messages[position].sentAt)
                receivedHolder.avatar.setImageResource(avatar)
                receivedHolder.author.text = messages[position].from.name
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if ((messages[position].isIncoming)) { VIEW_TYPE_RECEIVED }
        else VIEW_TYPE_SENT
    }


    override fun getItemCount(): Int {
        return messages.size
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    @SuppressLint("SimpleDateFormat")
    fun getDateTime(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm")
        return try {

            val netDate = Date(timeStamp)
            sdf.format(netDate)
        } catch (e: Exception) {
            e.toString()
        }
    }


    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}