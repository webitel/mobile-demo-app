package com.webitel.mobile_demo_app.ui.chat

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.ChatClient
import com.webitel.mobile_sdk.domain.Message
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.Dialog
import com.webitel.mobile_sdk.domain.DialogListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.MessageCallbackListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<ArrayList<Message>>(arrayListOf())
    val messages: LiveData<ArrayList<Message>> = _messages

    private val _error = MutableLiveData<Error?>()
    var error: LiveData<Error?> = _error

    private var isShownDialog = false

    private val portalClient = DemoApp.instance.portalClient
    private var client: ChatClient? = null
    private var dialog: Dialog? = null
    private var isFetching = false


    fun getChatClient() {
        portalClient.getChatClient(object : CallbackListener<ChatClient> {
            override fun onSuccess(t: ChatClient) {
                client = t
                viewModelScope.launch(Dispatchers.Main) {
                    _error.postValue(null)
                }
                getServiceDialog(t)
            }

            override fun onError(e: Error) {
                viewModelScope.launch(Dispatchers.Main) {
                    _error.postValue(e)
                }
                client = null
            }
        })
    }


    fun sendMessage(text: String) {
        if (text.trim().isEmpty()) {
            Toast.makeText(
                DemoApp.instance,
                "The text cannot be empty", Toast.LENGTH_LONG
            ).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            dialog?.sendMessage(
                Message
                    .options()
                    .withText(text.trim()),
                object : MessageCallbackListener {
                    override fun onSend(m: Message) {
                        viewModelScope.launch(Dispatchers.Main) {
                            _messages.value!!.add(m)
                            _messages.postValue(_messages.value)
                        }
                    }

                    override fun onSent(m: Message) {
                        updateMessageByReqId(m)
                    }

                    override fun onError(e: Error) {
                        viewModelScope.launch(Dispatchers.Main) {
                            _messages.postValue(_messages.value)
                        }
                    }
                }
            )
        }
    }


    fun getUpdates() {
        dialog?.let {
            viewModelScope.launch(Dispatchers.IO) {
                getUpdates(it)
            }
        }
    }


    fun subscribeListener() {
        dialog?.addListener(chatListener)
    }


    fun unsubscribeListener() {
        dialog?.removeListener(chatListener)
    }


    fun setActiveDialog(value: Boolean) {
        isShownDialog = value
    }


    override fun onCleared() {
        unsubscribeListener()
        super.onCleared()
    }


    private fun getServiceDialog(c: ChatClient) {
        c.getServiceDialog(object : CallbackListener<Dialog> {
            override fun onSuccess(t: Dialog) {
                dialog = t
                dialog?.addListener(chatListener)
                getHistory(t)
            }

            override fun onError(e: Error) {
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        DemoApp.instance,
                        e.message, Toast.LENGTH_LONG
                    ).show()
                }
            }
        })
    }


    private fun getHistory(d: Dialog) {
        if (isFetching) return
        isFetching = true
        d.getHistory(callback = object : CallbackListener<List<Message>>{
            override fun onSuccess(t: List<Message>) {
                val reversedList = t.reversed()
                viewModelScope.launch(Dispatchers.Main) {
                    _messages.value?.clear()
                    _messages.value?.addAll(reversedList)
                    _messages.postValue(_messages.value)
                    isFetching = false
                }
            }

            override fun onError(e: Error) {
                isFetching = false
                Log.e("getHistory", e.message)
            }
        })
    }


    private fun getUpdates(d: Dialog) {
        if (isFetching) return
        isFetching = true
        d.getUpdates(object : CallbackListener<List<Message>>{
            override fun onSuccess(t: List<Message>) {
                viewModelScope.launch(Dispatchers.Main) {
                    _messages.value?.addAll(t)
                    _messages.postValue(_messages.value)
                    isFetching = false
                }
            }

            override fun onError(e: Error) {
                isFetching = false
                Log.e("getUpdates", e.message)
            }
        })
    }


    private fun updateMessageByReqId(m: Message) {
        viewModelScope.launch(Dispatchers.Main) {
            val s = _messages.value?.firstOrNull { it.id == m.id }
            if (s == null) {
                _messages.value?.add(m)
            }
            _messages.postValue(_messages.value)
        }
    }


    private val chatListener: DialogListener = object : DialogListener {
        override fun onMessageAdded(message: Message) {

            viewModelScope.launch(Dispatchers.Main) {
                _messages.value!!.add(message)
                _messages.postValue(_messages.value)
            }

            if (!isShownDialog) {
                Notifications.instance.showMessageNotification(
                    DemoApp.instance,
                    title = "Service",
                    body = message.text ?: "File"
                )
            }
        }
    }
}