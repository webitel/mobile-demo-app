package com.webitel.mobile_demo_app.ui

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_demo_app.data.UsersRepository
import com.webitel.mobile_demo_app.notifications.Notifications
import com.webitel.mobile_sdk.domain.Call
import com.webitel.mobile_sdk.domain.CallState
import com.webitel.mobile_sdk.domain.CallStateListener
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.Code
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.LoginListener
import com.webitel.mobile_sdk.domain.Session
import com.webitel.mobile_sdk.domain.VoiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch



class FragmentViewModel: ViewModel() {

    private val usersRepository = UsersRepository(DemoApp.instance)

    private val portalClient = DemoApp.instance.portalClient

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


    private fun checkCallsAndChat() {
        portalClient.getVoiceClient(object: CallbackListener<VoiceClient> {
            override fun onSuccess(t: VoiceClient) {
                if (t.activeCall != null)  {
                    setUserActivity(UserActivity.CALL)
                    t.activeCall?.addListener(callListener)
                }else {
                    setUserActivity(UserActivity.NONE)
                   // checkChat()
                }
            }

            override fun onError(e: Error) {
                if (e.code != Code.UNAUTHENTICATED) {
                    setUserActivity(UserActivity.UNAVAILABLE)
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                            DemoApp.instance,
                            "${e.code}\n${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    setUserActivity(UserActivity.NONE)
                }
            }
        })
    }


    private fun getCapabilities(callback: (List<String>?) -> Unit) {
        portalClient.getUserSession(object: CallbackListener<Session> {
            override fun onSuccess(t: Session) {
                onResultCapabilities(t, callback)
            }

            override fun onError(e: Error) {
                if (e.code == Code.UNAUTHENTICATED) {
                    userLogin(callback)

                } else {
                    onErrorCapabilities(e, callback)
                    setUserActivity(UserActivity.UNAVAILABLE)
                }
            }
        })
    }


    private fun onErrorCapabilities(e: Error, callback: (List<String>?) -> Unit) {
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


    private fun onResultCapabilities(session: Session, callback: (List<String>?) -> Unit) {
        val scope = arrayListOf<String>()
        if (session.isChatAvailable) scope.add("chat")
        if (session.isVoiceAvailable) scope.add("call")
        viewModelScope.launch(Dispatchers.Main) {
            callback.invoke(scope)
            _isLoading.value = false
        }
    }


    private fun userLogin(callback: (List<String>?) -> Unit) {
        val user = usersRepository.getUser()
        portalClient.userLogin(user, object: LoginListener {
            override fun onLoginFinished(session: Session) {
                onResultCapabilities(session, callback)
            }

            override fun onLogoutFinished() {}

            override fun onError(e: Error) {
                onErrorCapabilities(e, callback)
            }
        })
    }


    private fun setUserActivity(value: UserActivity) {
        viewModelScope.launch(Dispatchers.Main) {
            _userActivity.value = value
            _isLoading.value = false
        }
    }


    private val callListener = object: CallStateListener {
        override fun onCreateCall(call: Call) {}

        override fun onCallStateChanged(call: Call, oldState: List<CallState>) {
            if (call.state.contains(CallState.DISCONNECTED)) {
                Notifications.instance.cancelCallNotification()
                checkActivities()
            }
        }

        override fun onCreateCallFailed(e: Error) {}
    }


    enum class UserActivity {

        /**
         * User has active CALL
         */
        CALL,

        /**
         * User has NO activities
         */
        NONE,

        /**
         * User is UNAVAILABLE
         */
        UNAVAILABLE
    }
}

