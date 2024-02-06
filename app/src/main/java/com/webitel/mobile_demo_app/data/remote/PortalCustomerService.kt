package com.webitel.mobile_demo_app.data.remote

import android.app.Application
import android.util.Log
import com.webitel.mobile_sdk.domain.Call
import com.webitel.mobile_sdk.domain.CallStateListener
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.ChatClient
import com.webitel.mobile_sdk.domain.Code
import com.webitel.mobile_sdk.domain.Dialog
import com.webitel.mobile_sdk.domain.DialogListener
import com.webitel.mobile_sdk.domain.LoginListener
import com.webitel.mobile_sdk.domain.Session
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.HistoryRequest
import com.webitel.mobile_sdk.domain.Message
import com.webitel.mobile_sdk.domain.MessageCallbackListener
import com.webitel.mobile_sdk.domain.PortalClient
import com.webitel.mobile_sdk.domain.RegisterResult
import com.webitel.mobile_sdk.domain.StreamObserver
import com.webitel.mobile_sdk.domain.User
import com.webitel.mobile_sdk.domain.VoiceClient
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class PortalCustomerService(application: Application, token: String, address: String) {
    private var user: User? = null
    private var _portalClient: PortalClient
    private var _chatClient: ChatClient? = null
    private var _serviceDialog: Dialog? = null


    init {
        _portalClient = createClient(application, token, address)
    }


    suspend fun registerFCMToken(token: String) {
        return suspendCoroutine { continuation ->
            _portalClient.registerFCMToken(
                token, object : CallbackListener<RegisterResult> {
                    override fun onError(e: Error) {
                        continuation.resumeWithException(
                            PortalException(e)
                        )
                    }

                    override fun onSuccess(t: RegisterResult) {
                        continuation.resume(Unit)
                    }
                })
        }
    }

    suspend fun logout() {
        return suspendCoroutine { continuation ->
            _portalClient.userLogout(object : LoginListener {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onLoginFinished(session: Session) {}

                override fun onLogoutFinished() {
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun getUserActivity(): UserActivity {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onSuccess(t: VoiceClient) {
                    if (t.activeCall != null) {
                        continuation.resume(UserActivity.CALL)
                        // t.activeCall?.addListener(callListener)
                    } else {
                        continuation.resume(UserActivity.NONE)
                    }
                }

                override fun onError(e: Error) {
                    if (e.code != Code.UNAUTHENTICATED) {
                        continuation.resume(UserActivity.UNAVAILABLE)
                        Log.e("getUserActivity", e.message)
                    } else {
                        continuation.resume(UserActivity.NONE)
                    }
                }
            })
        }
    }


    private fun onResultCapabilities(session: Session): List<String> {
        val scope = arrayListOf<String>()
        if (session.isChatAvailable) scope.add("chat")
        if (session.isVoiceAvailable) scope.add("call")
        return scope
    }

    suspend fun getUserCapabilities(): List<String> {
        return suspendCoroutine { continuation ->

            _portalClient.getUserSession(object : CallbackListener<Session> {
                override fun onSuccess(t: Session) {
                    continuation.resume(
                        onResultCapabilities(t)
                    )
                }

                override fun onError(e: Error) {
                    if (e.code == Code.UNAUTHENTICATED) {
                        login { session, err ->
                            if (err != null)
                                continuation.resumeWithException(
                                    PortalException(err)
                                )
                            else {
                                continuation.resume(
                                    onResultCapabilities(session!!)
                                )
                            }
                        }

                    } else {
                        continuation.resumeWithException(
                            PortalException(e)
                        )
                    }
                }
            })
        }
    }


    suspend fun setCallListener(listener: CallStateListener) {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onSuccess(t: VoiceClient) {
                    t.activeCall?.addListener(listener)
                    continuation.resume(Unit)
                }

                override fun onError(e: Error) {
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun getUpdates(params: Params, listener: DialogListener): List<Message> {
        return suspendCoroutine { continuation ->
            if (user == null) {
                continuation.resumeWithException(
                    PortalException(
                        Error(
                            "you need to call setUser first",
                            Code.NOT_FOUND
                        )
                    )
                )
            }

            if (_chatClient == null || _serviceDialog == null) {
                initChatControllerAndLogin(listener) { err ->
                    if (err != null) {
                        continuation.resumeWithException(
                            PortalException(err)
                        )
                    } else
                        getUpdatesAndLoginWrap(params, listener, continuation)

                }
            } else
                getUpdatesAndLoginWrap(params, listener, continuation)

        }
    }


    suspend fun getHistory(params: Params, listener: DialogListener): List<Message> {
        return suspendCoroutine { continuation ->
            if (user == null) {
                continuation.resumeWithException(
                    PortalException(
                        Error(
                            "you need to call setUser first",
                            Code.NOT_FOUND
                        )
                    )
                )
            }

            if (_chatClient == null || _serviceDialog == null) {
                initChatControllerAndLogin(listener) { err ->
                    if (err != null) {
                        continuation.resumeWithException(
                            PortalException(err)
                        )
                    } else
                        getHistoryAndLoginWrap(params, listener, continuation)

                }
            } else
                getHistoryAndLoginWrap(params, listener, continuation)

        }
    }


    suspend fun downloadFile(
        fileId: String,
        observer: StreamObserver,
        listener: DialogListener,
    ) {
        return suspendCoroutine { continuation ->
            if (user == null) {
                continuation.resumeWithException(
                    PortalException(
                        Error(
                            "you need to call setUser first",
                            Code.NOT_FOUND
                        )
                    )
                )
            }

            if (_chatClient == null || _serviceDialog == null) {
                initChatControllerAndLogin(listener) { err ->
                    if (err != null) {
                        continuation.resumeWithException(
                            PortalException(err)
                        )
                    } else
                        _serviceDialog?.downloadFile(fileId, observer)
                }
            } else
                _serviceDialog?.downloadFile(fileId, observer)
        }
    }


    suspend fun sendMessage(
        listener: DialogListener,
        options: Message.options
    ): Message? {
        return suspendCoroutine { continuation ->
            if (user == null) {
                continuation.resumeWithException(
                    PortalException(
                        Error(
                            "you need to call setUser first",
                            Code.NOT_FOUND
                        )
                    )
                )
            }

            if (_chatClient == null || _serviceDialog == null) {
                initChatControllerAndLogin(listener) { err ->
                    if (err != null) {
                        continuation.resumeWithException(
                            PortalException(err)
                        )
                    } else
                        sendAndLoginWrap(listener, options, continuation)
                }
            } else
                sendAndLoginWrap(listener, options, continuation)
        }
    }


    suspend fun makeCall(listener: CallStateListener) {
        return suspendCoroutine { continuation ->
            if (user == null) {
                continuation.resumeWithException(
                    PortalException(
                        Error(
                            "you need to call setUser first",
                            Code.NOT_FOUND
                        )
                    )
                )
            }

            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {

                override fun onError(e: Error) {
                    if (e.code == Code.UNAUTHENTICATED) {
                        login { _, err ->
                            if (err != null) {
                                continuation.resumeWithException(
                                    PortalException(err)
                                )
                                return@login
                            }

                            makeCallAganWrap(listener, continuation)
                        }
                    } else {
                        continuation.resumeWithException(
                            PortalException(e)
                        )
                    }
                }

                override fun onSuccess(t: VoiceClient) {
                    t.makeCall(listener)
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun getActiveCall(): Call? {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onSuccess(t: VoiceClient) {
                    continuation.resume(t.activeCall)
                }
            })
        }
    }


    suspend fun disconnectCall() {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onSuccess(t: VoiceClient) {
                    if (t.activeCall == null) {
                        continuation.resumeWithException(
                            PortalException(
                                Error("active call not found", Code.NOT_FOUND)
                            )
                        )
                        return
                    }
                    t.activeCall?.disconnect()
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun sendDtmf(dtmf: String) {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onSuccess(t: VoiceClient) {
                    if (t.activeCall == null) {
                        continuation.resumeWithException(
                            PortalException(
                                Error("active call not found", Code.NOT_FOUND)
                            )
                        )
                        return
                    }
                    t.activeCall?.sendDigits(dtmf)
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun toggleHoldCall() {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onSuccess(t: VoiceClient) {
                    if (t.activeCall == null) {
                        continuation.resumeWithException(
                            PortalException(
                                Error("active call not found", Code.NOT_FOUND)
                            )
                        )
                        return
                    }
                    t.activeCall?.toggleHold()
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun toggleLoudSpeaker() {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onSuccess(t: VoiceClient) {
                    if (t.activeCall == null) {
                        continuation.resumeWithException(
                            PortalException(
                                Error("active call not found", Code.NOT_FOUND)
                            )
                        )
                        return
                    }
                    t.activeCall?.toggleLoudspeaker()
                    continuation.resume(Unit)
                }
            })
        }
    }


    suspend fun toggleMuteCall() {
        return suspendCoroutine { continuation ->
            _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
                override fun onError(e: Error) {
                    continuation.resumeWithException(
                        PortalException(e)
                    )
                }

                override fun onSuccess(t: VoiceClient) {
                    if (t.activeCall == null) {
                        continuation.resumeWithException(
                            PortalException(
                                Error("active call not found", Code.NOT_FOUND)
                            )
                        )
                        return
                    }
                    t.activeCall?.toggleMute()
                    continuation.resume(Unit)
                }
            })
        }
    }


    fun setUser(u: User) {
        user = u
    }


    private fun sendAndLoginWrap(
        listener: DialogListener,
        options: Message.options,
        continuation: Continuation<Message?>
    ) {
        sendAndLogin(listener, options) { error, message ->
            if (error != null) {
                continuation.resumeWithException(
                    PortalException(
                        error
                    )
                )
            } else {
                continuation.resume(message)
            }
        }
    }

    private fun getHistoryAndLoginWrap(
        params: Params,
        listener: DialogListener,
        continuation: Continuation<List<Message>>
    ) {
        getHistoryAndLogin(params, listener) { error, messages ->
            if (error != null) {
                continuation.resumeWithException(
                    PortalException(
                        error
                    )
                )
            } else {
                continuation.resume(messages ?: listOf())
            }
        }
    }


    private fun getUpdatesAndLoginWrap(
        params: Params,
        listener: DialogListener,
        continuation: Continuation<List<Message>>
    ) {
        getUpdatesAndLogin(params, listener) { error, messages ->
            if (error != null) {
                continuation.resumeWithException(
                    PortalException(
                        error
                    )
                )
            } else {
                continuation.resume(messages ?: listOf())
            }
        }
    }


    private fun getUpdatesAndLogin(
        params: Params,
        listener: DialogListener,
        callback: (Error?, List<Message>?) -> Unit
    ) {
        val s = object : CallbackListener<List<Message>> {
            override fun onError(e: Error) {
                if (e.code == Code.UNAUTHENTICATED) {
                    initChatControllerAndLogin(listener) { err ->
                        if (err != null) {
                            callback(err, null)
                        } else
                            getUpdates(callback)
                    }
                } else {
                    callback(e, null)
                }
            }

            override fun onSuccess(t: List<Message>) {
                callback(null, t)
            }
        }

        if (params.offset > 0 || params.limit > 0) {
            _serviceDialog?.getUpdates(
                createHistoryRequest(params), s
            )

        } else {
            _serviceDialog?.getUpdates(s)
        }
    }


    private fun getUpdates(callback: (Error?, List<Message>?) -> Unit) {
        _serviceDialog?.getUpdates(object : CallbackListener<List<Message>> {
            override fun onError(e: Error) {
                callback(e, null)
            }

            override fun onSuccess(t: List<Message>) {
                callback(null, t)
            }
        })
    }


    private fun getHistoryAndLogin(
        params: Params,
        listener: DialogListener,
        callback: (Error?, List<Message>?) -> Unit
    ) {
        val s = object : CallbackListener<List<Message>> {
            override fun onError(e: Error) {
                if (e.code == Code.UNAUTHENTICATED) {
                    initChatControllerAndLogin(listener) { err ->
                        if (err != null) {
                            callback(err, null)
                        } else
                            getHistory(callback)
                    }
                } else {
                    callback(e, null)
                }
            }

            override fun onSuccess(t: List<Message>) {
                callback(null, t)
            }
        }

        if (params.offset > 0 || params.limit > 0) {
            _serviceDialog?.getHistory(
                createHistoryRequest(params), s
            )

        } else {
            _serviceDialog?.getHistory(s)
        }
    }


    private fun createHistoryRequest(params: Params): HistoryRequest {
        val builder = HistoryRequest.Builder()

        if (params.offset > 0) builder.offset(params.offset)
        if (params.limit > 0) builder.limit(params.limit)

        return builder.build()
    }


    private fun getHistory(callback: (Error?, List<Message>?) -> Unit) {
        _serviceDialog?.getHistory(object : CallbackListener<List<Message>> {
            override fun onError(e: Error) {
                callback(e, null)
            }

            override fun onSuccess(t: List<Message>) {
                callback(null, t)
            }
        })
    }


    private fun sendAndLogin(
        listener: DialogListener,
        message: Message.options,
        callback: (Error?, Message?) -> Unit
    ) {
        _serviceDialog?.sendMessage(message = message, object : MessageCallbackListener {
            override fun onError(e: Error) {
                if (e.code == Code.UNAUTHENTICATED) {
                    initChatControllerAndLogin(listener) { err ->
                        if (err != null) {
                            callback(err, null)
                        } else {
                            send(message, callback)
                        }
                    }
                } else {
                    callback(e, null)
                }
            }

            override fun onSend(m: Message) {}

            override fun onSent(m: Message) {
                callback(null, m)
            }
        })
    }


    private fun send(
        message: Message.options,
        callback: (Error?, Message?) -> Unit
    ) {
        _serviceDialog?.sendMessage(message = message, object : MessageCallbackListener {
            override fun onError(e: Error) {
                callback(e, null)
            }

            override fun onSend(m: Message) {}

            override fun onSent(m: Message) {
                callback(null, m)
            }
        })
    }


    private fun initChatControllerAndLogin(
        listener: DialogListener,
        callback: (Error?) -> Unit
    ) {
        _portalClient.getChatClient(object : CallbackListener<ChatClient> {
            override fun onError(e: Error) {
                if (e.code == Code.UNAUTHENTICATED) {
                    login { _, err ->
                        if (err != null) {
                            callback(err)
                            return@login
                        }

                        initChatController(listener, callback)
                    }
                } else {
                    callback(e)
                }
            }

            override fun onSuccess(t: ChatClient) {
                _chatClient = t
                findServiceDialog(listener, t, callback)
            }
        })
    }


    private fun initChatController(
        listener: DialogListener,
        callback: (Error?) -> Unit
    ) {
        _portalClient.getChatClient(object : CallbackListener<ChatClient> {
            override fun onError(e: Error) {
                callback(e)
            }

            override fun onSuccess(t: ChatClient) {
                _chatClient = t
                findServiceDialog(listener, t, callback)
            }
        })
    }


    private fun findServiceDialog(
        listener: DialogListener,
        t: ChatClient,
        callback: (Error?) -> Unit
    ) {
        t.getServiceDialog(object : CallbackListener<Dialog> {
            override fun onError(e: Error) {
                callback(e)
            }

            override fun onSuccess(t: Dialog) {
                t.addListener(listener)
                _serviceDialog = t
                callback(null)
            }
        })
    }


    private fun createClient(
        application: Application,
        token: String,
        address: String
    ): PortalClient {
        return PortalClient.Builder(
            application = application,
            address = address,
            token = token
        )
            .build()
    }


    private fun makeCallAganWrap(listener: CallStateListener, continuation: Continuation<Unit>) {
        makeCallAgan(listener) {
            if (it != null) {
                continuation.resumeWithException(
                    PortalException(it)
                )
            } else
                continuation.resume(Unit)
        }
    }


    private fun makeCallAgan(listener: CallStateListener, callback: (Error?) -> Unit) {
        _portalClient.getVoiceClient(object : CallbackListener<VoiceClient> {
            override fun onError(e: Error) {
                callback(e)
            }

            override fun onSuccess(t: VoiceClient) {
                t.makeCall(listener)
                callback(null)
            }
        })
    }


    private fun login(callback: (Session?, Error?) -> Unit) {
        val u = user
        if (u != null) {
            _portalClient.userLogin(u, object : LoginListener {

                override fun onError(e: Error) {
                    callback(null, e)
                }

                override fun onLoginFinished(session: Session) {
                    callback(session, null)
                }

                override fun onLogoutFinished() {}
            })
        } else {
            callback(
                null,
                Error(
                    "User not found. You need to call setUser first",
                    Code.NOT_FOUND
                )
            )
        }
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

    data class Params(val limit: Int, val offset: Long)
}

