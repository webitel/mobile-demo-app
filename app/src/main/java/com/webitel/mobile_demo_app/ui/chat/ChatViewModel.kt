package com.webitel.mobile_demo_app.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.data.MessagesRepository
import com.webitel.mobile_demo_app.data.local.LocalCacheProvider
import com.webitel.mobile_demo_app.data.local.MessageDataItem
import com.webitel.mobile_demo_app.data.remote.RemoteRepository
import com.webitel.mobile_sdk.domain.Error
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch


class ChatViewModel : ViewModel() {
    private val messagesRepository = MessagesRepository(
        RemoteRepository(),
        LocalCacheProvider.INSTANCE
    )
    val messages: LiveData<List<MessageDataItem>> = messagesRepository.messages

    private val _error = MutableLiveData<Error?>()
    var error: LiveData<Error?> = _error


    fun sendMessage(text: String, attachmentUri: Uri?) {
        val textMessage = text.trim()
        if (textMessage.isEmpty() && attachmentUri == null) {
            Toast.makeText(
                DemoApp.instance,
                "The text cannot be empty", Toast.LENGTH_LONG
            ).show()
            return
        }
        viewModelScope.launch(IO) {
            messagesRepository.sendMessage(textMessage, attachmentUri)
        }
    }


    fun getUpdates() {
        viewModelScope.launch(IO) {
            messagesRepository.loadUpdates()
        }
    }


    fun downloadFile(messageId: Long) {
        viewModelScope.launch(IO) {
            messagesRepository.downloadFile(messageId)
        }
    }
}