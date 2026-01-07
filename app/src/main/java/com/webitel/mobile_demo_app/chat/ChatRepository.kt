package com.webitel.mobile_demo_app.chat

import android.app.Application
import android.util.Log
import com.webitel.mobile_sdk.domain.CallbackListener
import com.webitel.mobile_sdk.domain.CancellationToken
import com.webitel.mobile_sdk.domain.Code
import com.webitel.mobile_sdk.domain.ConnectListener
import com.webitel.mobile_sdk.domain.Dialog
import com.webitel.mobile_sdk.domain.DialogListener
import com.webitel.mobile_sdk.domain.DownloadListener
import com.webitel.mobile_sdk.domain.Error
import com.webitel.mobile_sdk.domain.FileTransferRequest
import com.webitel.mobile_sdk.domain.HistoryRequest
import com.webitel.mobile_sdk.domain.LogLevel
import com.webitel.mobile_sdk.domain.LoginListener
import com.webitel.mobile_sdk.domain.Message
import com.webitel.mobile_sdk.domain.MessageCallbackListener
import com.webitel.mobile_sdk.domain.PortalClient
import com.webitel.mobile_sdk.domain.RegisterResult
import com.webitel.mobile_sdk.domain.Session
import com.webitel.mobile_sdk.domain.UploadResult
import com.webitel.mobile_sdk.domain.User
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class ChatRepository(val dialogListener: DialogListener, val connectListener: ConnectListener) {
    private var portal: PortalClient? = null
    private var dialog: Dialog? = null
    private var user: User? = null


    suspend fun sendMessage(options: Message.options): Message {
        val dialog = findDialog()
        return sendMessageToDialog(dialog, options)
    }


    suspend fun getHistory(historyRequest: HistoryRequest): List<Message> {
        val dialog = findDialog()
        return getHistoryFromDialog(dialog, historyRequest)
    }


    suspend fun getUpdates(historyRequest: HistoryRequest): List<Message> {
        val dialog = findDialog()
        return getUpdatesFromDialog(dialog, historyRequest)
    }


    suspend fun uploadFile(request: FileTransferRequest): UploadResult {
        val dialog = findDialog()
        return uploadFileToDialog(dialog, request)
    }


    suspend fun downloadFile(fileId: String, listener: DownloadListener, offset: Long = 0): CancellationToken {
        val dialog = findDialog()
        return dialog.downloadFile(fileId, offset, listener)
    }


    suspend fun registerFCM(token: String) {
        val portal = requirePortal()
        suspendCancellableCoroutine { cont ->
            portal.registerDevice(token, object : CallbackListener<RegisterResult> {
                override fun onError(e: Error) {
                    cont.resumeWithException(e)
                }

                override fun onSuccess(t: RegisterResult) {
                    cont.resume(Unit)
                }
            })
        }
    }


    suspend fun logout() {
        val portal = requirePortal()

        suspendCancellableCoroutine { cont ->
            portal.userLogout(object : LoginListener {
                override fun onError(e: Error) {
                    cont.resumeWithException(e)
                }
                override fun onLoginFinished(session: Session) {}
                override fun onLogoutFinished() {
                    cont.resume(Unit)
                }
            })
        }
    }


    suspend fun initPortalClient(application: Application, authInfo: AuthInfo) {
        if (portal != null) {
            try {
                logout()
                portal?.removeConnectListener(connectListener)
            }catch (e: Throwable) {
                Log.w(TAG, "Logout failed in initPortalClient()", e)
            }
        }

        dialog?.removeListener(dialogListener)
        dialog = null

        val client = PortalClient.Builder(
            application = application,
            address = authInfo.host,
            token = authInfo.token
        )
            .logLevel(LogLevel.DEBUG)

        if (authInfo.isWebSocket) {
            client.useWebSocket()
        }

        portal = client.build()

        portal?.addConnectListener(connectListener)

        user = buildUser(authInfo)
    }


    suspend fun updateUser(authInfo: AuthInfo) {
        if (portal != null) {
            try {
                logout()
            }catch (e: Throwable){
                Log.w(TAG, "Logout failed in updateUser()", e)
            }
        }

        dialog?.removeListener(dialogListener)
        dialog = null

        user = buildUser(authInfo)
    }


    private suspend fun uploadFileToDialog(
        dialog: Dialog,
        request: FileTransferRequest
    ): UploadResult =
        suspendCancellableCoroutine { cont ->

            val token = dialog.uploadFile(
                request,
                object : CallbackListener<UploadResult> {

                    override fun onSuccess(t: UploadResult) {
                        cont.resume(t)
                    }

                    override fun onError(e: Error) {
                        cont.resumeWithException(e)
                    }
                }
            )
            cont.invokeOnCancellation {
                token.cancel()
            }
        }


    private fun requirePortal(): PortalClient =
        portal ?: throw Error(
            message = "PortalClient not initialized. Please set connection config first",
            code = Code.UNAVAILABLE
        )

    private fun requireUser(): User =
        user ?: error("User not initialized. Please set connection config first")


    private suspend fun getHistoryFromDialog(dialog: Dialog, historyRequest: HistoryRequest): List<Message> =
        suspendCancellableCoroutine { cont ->
            dialog.getHistory(historyRequest, object : CallbackListener<List<Message>> {
                override fun onError(e: Error) {
                    cont.resumeWithException(e)
                }

                override fun onSuccess(t: List<Message>) {
                    cont.resume(t)
                }
            })
        }


    private suspend fun getUpdatesFromDialog(dialog: Dialog, historyRequest: HistoryRequest): List<Message> =
        suspendCancellableCoroutine { cont ->
            dialog.getUpdates(historyRequest, object : CallbackListener<List<Message>> {
                override fun onError(e: Error) {
                    cont.resumeWithException(e)
                }

                override fun onSuccess(t: List<Message>) {
                    cont.resume(t)
                }
            })
        }


    private suspend fun login() {
        val portal = requirePortal()

        suspendCancellableCoroutine { cont ->
            portal.userLogin(requireUser(), object : LoginListener {
                override fun onError(e: Error) {
                    cont.resumeWithException(e)
                }
                override fun onLoginFinished(session: Session) {
                    cont.resume(Unit)
                }
                override fun onLogoutFinished() {}
            })
        }
    }



    private suspend fun sendMessageToDialog(dialog: Dialog, options: Message.options) : Message =
        suspendCancellableCoroutine { cont ->
            dialog.sendMessage(options, object : MessageCallbackListener {
                override fun onError(e: Error) {
                    if (e.code == Code.UNAUTHENTICATED) {
                        this@ChatRepository.dialog = null
                    }
                    cont.resumeWithException(e)
                }

                override fun onSent(m: Message) {
                    cont.resume(m)
                }
            })
        }


    private suspend fun findDialog(): Dialog {
        dialog?.let { return it }

        try {
            return getServiceDialog()
        } catch (e: Error) {
            if (e.code == Code.UNAUTHENTICATED) {
                login()
                return getServiceDialog()
            }
            throw e
        }
    }


    private suspend fun getServiceDialog(): Dialog {
        val portal = requirePortal()

        return suspendCancellableCoroutine { cont ->
            portal.chatClient.getServiceDialog(object : CallbackListener<Dialog> {

                override fun onSuccess(t: Dialog) {
                    t.addListener(dialogListener)
                    dialog = t
                    cont.resume(t)
                }

                override fun onError(e: Error) {
                    cont.resumeWithException(e)
                }
            })
        }
    }


    private fun buildUser(authInfo: AuthInfo):  User {
        return User.Builder(
            iss = authInfo.issuer,
            sub = authInfo.userName, // use userName as ID only in demo version!
            name = authInfo.userName
        ).build()
    }


    companion object {
        const val TAG = "ChatRepository"
    }
}