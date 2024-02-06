package com.webitel.mobile_demo_app.ui

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.data.UsersRepository
import com.webitel.mobile_demo_app.data.remote.PortalCustomerService
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.Call
import com.webitel.mobile_sdk.domain.CallState
import com.webitel.mobile_sdk.domain.CallStateListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_demo_app.data.remote.PortalCustomerService.UserActivity
import com.webitel.mobile_demo_app.data.remote.PortalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FragmentViewModel : ViewModel() {

    private val usersRepository = UsersRepository(DemoApp.instance)
    private val portalClient = DemoApp.instance.portalClient2

    private val _userActivity = MutableLiveData(UserActivity.NONE)
    var userActivity: LiveData<UserActivity> = _userActivity

    private val _isLoading = MutableLiveData(false)
    var isLoading: LiveData<Boolean> = _isLoading


    fun checkActivities() {
        viewModelScope.launch(Dispatchers.Main) {
            if (_isLoading.value == true) return@launch
            _isLoading.value = true

            viewModelScope.launch(Dispatchers.IO) {
                checkCallsAndChat()
            }
        }
    }


    fun recheckActivities() {
        checkActivities()
    }


    fun getUserCapabilities(callback: (List<String>?) -> Unit) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            getCapabilities(callback)
        }
    }


    fun userLogout() {
        viewModelScope.launch(Dispatchers.IO) {
            portalClient.logout()
        }
    }


    fun setUser() {
        viewModelScope.launch(Dispatchers.IO) {
            portalClient.setUser(usersRepository.getUser())
        }
    }


    private fun checkCallsAndChat() {
        viewModelScope.launch(Dispatchers.IO) {
            val userActivity = portalClient.getUserActivity()
            setUserActivity(userActivity)
            if (userActivity == UserActivity.CALL) {
                portalClient.setCallListener(callListener)
            }
        }
    }


    private suspend fun getCapabilities(callback: (List<String>?) -> Unit) {
        try {
            val capabilities = portalClient.getUserCapabilities()
            viewModelScope.launch(Dispatchers.Main) {
                _isLoading.value = false
                callback(capabilities)
            }

        } catch (e: PortalException) {
            onErrorCapabilities(e, callback)
        }

    }


    private fun onErrorCapabilities(e: PortalException, callback: (List<String>?) -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            _isLoading.value = false
            callback.invoke(null)
            Toast.makeText(
                DemoApp.instance,
                "${e.code}\n${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun setUserActivity(value: PortalCustomerService.UserActivity) {
        viewModelScope.launch(Dispatchers.Main) {
            _userActivity.value = value
            _isLoading.value = false
        }
    }


    private val callListener = object : CallStateListener {
        override fun onCreateCall(call: Call) {}

        override fun onCallStateChanged(call: Call, oldState: List<CallState>) {
            if (call.state.contains(CallState.DISCONNECTED)) {
                Notifications.instance.cancelCallNotification()
                checkActivities()
            }
        }

        override fun onCreateCallFailed(e: Error) {}
    }
}

