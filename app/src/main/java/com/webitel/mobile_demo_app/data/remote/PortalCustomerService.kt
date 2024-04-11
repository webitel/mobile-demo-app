package com.webitel.mobile_demo_app.data.remote

import android.app.Application
import android.util.Log
import com.webitel.mobile_sdk.domain.Call
import com.webitel.mobile_sdk.domain.CallStateListener
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.ChatClient
import com.webitel.mobile_sdk.domain.Code
import com.webitel.mobile_sdk.domain.ConnectListener
import com.webitel.mobile_sdk.domain.ConnectState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class PortalCustomerService(application: Application, token: String, address: String) {
    private var user: User? = null
    private var _portalClient: PortalClient
    private var _chatClient: ChatClient? = null
    private var _serviceDialog: Dialog? = null

    private val useJWT = false
    private val jwtList = arrayListOf(
        "eyJhbGciOiJSUzI1NiIsImtpZCI6Im1YRjdVdzhMb2JOWERUQUVrbVh1ZFEiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJodHRwczovL2FwaS5wb3MuZmhsLmxvY2FsIiwiZXhwIjoxNzEwMjQ2NjYwLCJpYXQiOjE3MTAyNDY1NDUsImlpZCI6Imp3dF9tb2JpbGVfZGVtb19jdXN0b21lcl9mYWtlX2lkIiwiaXNzIjoiaHR0cHM6Ly9kZXYud2ViaXRlbC5jb20vcG9ydGFsIiwianRpIjoiIiwibmFtZSI6IkpvaG4gRG9lIChKV1Q6RGVtbykiLCJuYmYiOjE3MTAyNDY2MDAsInBsdGYiOiJOQVRJVkUiLCJyb2xlIjoiY2xpZW50Iiwic2NvcGUiOlsib2ZmbGluZV9hY2Nlc3MiLCJvcGVuaWQiLCJwb3MtY2xpZW50IiwicG9zLWNsaWVudC11bnZlcmlmaWVkIl0sInN1YiI6IjVkZDY5MzM5NTcwNTNhOTBiYTdkYTBhMTM2MjQ0MzNmIiwidWlkIjoiY2Q2Njk3ZjctOTg4ZS00MmU3LWFhYWYtYWNhMWI5MmU0ZWI5In0.dTULWh8NC7Nj26zoAs3lnqOnMso2xq7bJQIMume0zkcFD73ljCU2dDdmq9N2M7dzc_IlfkdRFL-Z6AZDBwTOe_uvxDyLCbyEmTHA2jaQFu9cnp15UPOvFy6aSlP9jWOtcdlhMAv8BE7DyvL2tnNOFGQBVzZLx4bQ7va85g2Jbbg4YIfwnp2Z9vPL0ag8-Q2IP619fBvcFjYXjRnX5IpYrf_cUsUirJe7hasekXe37UCvAQ0o2MnTqmXx2aYuTa9ArkbYYNvMMAcjUHE4ZNQTwk3UvvnMs8wqWhHSXfQ0UpQICJ-TVWC2KmTxbfRq8IklxnvMHZDkb6MWZm1zPkBSNQ",
        "eyJhbGciOiJSUzI1NiIsImtpZCI6Im1YRjdVdzhMb2JOWERUQUVrbVh1ZFEiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJodHRwczovL2FwaS5wb3MuZmhsLmxvY2FsIiwiZXhwIjoxNzEwMjQ2NzIwLCJpYXQiOjE3MTAyNDY1NDUsImlpZCI6Imp3dF9tb2JpbGVfZGVtb19jdXN0b21lcl9mYWtlX2lkIiwiaXNzIjoiaHR0cHM6Ly9kZXYud2ViaXRlbC5jb20vcG9ydGFsIiwianRpIjoiIiwibmFtZSI6IkpvaG4gRG9lIChKV1Q6RGVtbykiLCJuYmYiOjE3MTAyNDY2NTUsInBsdGYiOiJOQVRJVkUiLCJyb2xlIjoiY2xpZW50Iiwic2NvcGUiOlsib2ZmbGluZV9hY2Nlc3MiLCJvcGVuaWQiLCJwb3MtY2xpZW50IiwicG9zLWNsaWVudC11bnZlcmlmaWVkIl0sInN1YiI6IjVkZDY5MzM5NTcwNTNhOTBiYTdkYTBhMTM2MjQ0MzNmIiwidWlkIjoiY2Q2Njk3ZjctOTg4ZS00MmU3LWFhYWYtYWNhMWI5MmU0ZWI5In0.cc-mUorF5TU2UpF8zRlkonIFC5uTBY9N2Ia0skXebkwJhJGCuIuRfLdoMw-PjzyFqLXACLRxzFTHktwy9WvyPM6DDs4KolhUZoXE9awQXnQO2nkbnrmRBNI3hZ4U-sykrZLMLZ9D75yDB7Okw6yI6VkdWi_oBwsXDzBDNzu_q4vuV3ZZGKw0CAEUt8NUqwoUUC9aeEw-Un80bm9pdqP27ki0cNvignEhkQ3Js0ITQeS4FDI43w0mC2ovIY-zdJoYzDIxxHiVsijXq-X7ye0yAczic46jhiEj67LpDbUA0vrN1aKSJo0d_jsYHWWwLdOCTMrFBSUPrJcRziTCzAmJ1A",
        "eyJhbGciOiJSUzI1NiIsImtpZCI6Im1YRjdVdzhMb2JOWERUQUVrbVh1ZFEiLCJ0eXAiOiJKV1QifQ.eyJhdWQiOiJodHRwczovL2FwaS5wb3MuZmhsLmxvY2FsIiwiZXhwIjoxNzEwMjQ2NzgwLCJpYXQiOjE3MTAyNDY1NDUsImlpZCI6Imp3dF9tb2JpbGVfZGVtb19jdXN0b21lcl9mYWtlX2lkIiwiaXNzIjoiaHR0cHM6Ly9kZXYud2ViaXRlbC5jb20vcG9ydGFsIiwianRpIjoiIiwibmFtZSI6IkpvaG4gRG9lIChKV1Q6RGVtbykiLCJuYmYiOjE3MTAyNDY3MTUsInBsdGYiOiJOQVRJVkUiLCJyb2xlIjoiY2xpZW50Iiwic2NvcGUiOlsib2ZmbGluZV9hY2Nlc3MiLCJvcGVuaWQiLCJwb3MtY2xpZW50IiwicG9zLWNsaWVudC11bnZlcmlmaWVkIl0sInN1YiI6IjVkZDY5MzM5NTcwNTNhOTBiYTdkYTBhMTM2MjQ0MzNmIiwidWlkIjoiY2Q2Njk3ZjctOTg4ZS00MmU3LWFhYWYtYWNhMWI5MmU0ZWI5In0.W1Gc03RK2MkVI0jSDRPzBJmgg4Qoc6Z9PeGm6uDbv_BnE92yB387yac0gafpMnsLIMaXN4dLXjCZln6ByYIVAg19uPtQMJv90M819YTdyFIvuT7nl8KD2HSoVrVP_NEk_9Dct7EG8ojtQZ-19anuZADICHjWYF7kfv3aKtF4xA681IxSi_j60OIOii8ldcyyqlAahfWCACR6oEXFNaH2uxXRu5TzZ409edDnf65VsvsE3oV_Jv_lbBgWDtLMN6gm7mvJHDt1-JB-Ft9ivpiFvkr9AH4zS_-ZFFh6aH0wWOOgwoSY4IfreYaX2BxYYOfyJI4_BPYg-lp-v-d7Z_Zpyw"
    )


    init {
        _portalClient = createClient(application, token, address)
    }


    fun addConnectListener(listener: ConnectListener) {
        CoroutineScope(Dispatchers.IO).launch {
            _portalClient.addConnectListener(listener)
        }
    }


    fun removeConnectListener(listener: ConnectListener) {
        CoroutineScope(Dispatchers.IO).launch {
            _portalClient.removeConnectListener(listener)
        }
    }


    fun openConnect() {
        CoroutineScope(Dispatchers.IO).launch {
            _portalClient.openConnect()
        }
    }


    fun getConnectState(): ConnectState {
        return _portalClient.getConnectState()
    }


    suspend fun registerFCMToken(token: String) {
        return suspendCoroutine { continuation ->
            _portalClient.registerDevice(
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
        if (useJWT) {
            setJWT(callback)

        } else {
            userLogin(callback)
        }
    }


    private fun userLogin(callback: (Session?, Error?) -> Unit) {
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


    private fun setJWT(callback: (Session?, Error?) -> Unit) {
        val token = generateJWT()
        if (!token.isNullOrEmpty()) {
            _portalClient.setAccessToken(token, object : CallbackListener<Session> {
                override fun onError(e: Error) {
                    callback(null, e)
                }

                override fun onSuccess(t: Session) {
                    callback(t, null)
                }
            })

        } else {
            callback(
                null,
                Error(
                    "List with JWT is empty",
                    Code.NOT_FOUND
                )
            )
        }
    }


    private fun generateJWT(): String? {
        return jwtList.removeFirstOrNull()
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

