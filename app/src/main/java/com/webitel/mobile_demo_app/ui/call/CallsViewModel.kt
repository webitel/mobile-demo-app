package com.webitel.mobile_demo_app.ui.call

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.Call
import com.webitel.mobile_sdk.domain.CallState
import com.webitel.mobile_sdk.domain.CallStateListener
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.VoiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class CallsViewModel : ViewModel() {

    private val _error = MutableLiveData<Error?>()
    var error: LiveData<Error?> = _error
    private val _call = MutableLiveData<Call?>()
    var callLive: LiveData<Call?> = _call

    private val portalClient = DemoApp.instance.portalClient


    fun makeCall() {
        viewModelScope.launch(Dispatchers.IO) {
            portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onSuccess(t: VoiceClient) {
                    makeCall(t)
                }

                override fun onError(e: Error) {
                    _error.postValue(e)
                }
            })
        }
    }


    fun muteCall() {
        _call.value?.toggleMute()
    }


    fun holdCall() {
        _call.value?.toggleHold()
    }


    fun toggleLoudspeaker() {
        _call.value?.toggleLoudspeaker()
    }


    fun disconnectCall() {
        _call.value?.disconnect()
    }


    fun clearData() {
        viewModelScope.launch(Dispatchers.Main) {
            _error.value = null
            _call.value = null
        }
    }


    private fun makeCall(v: VoiceClient) {
        v.makeCall(callListener)
    }


    private val callListener = object : CallStateListener {
        override fun onCreateCall(call: Call) {
            Notifications.instance.showCallNotification(DemoApp.instance)
            _call.postValue(call)
        }

        override fun onCallStateChanged(call: Call, oldState: List<CallState>) {
            _call.postValue(call)
        }

        override fun onCreateCallFailed(e: Error) {
            _error.postValue(e)
        }
    }
}