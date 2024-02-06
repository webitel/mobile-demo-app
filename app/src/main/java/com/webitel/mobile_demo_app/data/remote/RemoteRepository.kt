package com.webitel.mobile_demo_app.data.remote

import com.webitel.mobile_demo_app.app.DemoApp
import com.webitel.mobile_sdk.domain.DialogListener
import com.webitel.mobile_sdk.domain.Message
import com.webitel.mobile_sdk.domain.StreamObserver


class RemoteRepository {

    private val portalClient = DemoApp.instance.portalClient2


    suspend fun getHistory(
        params: PortalCustomerService.Params,
        listener: DialogListener
    ): List<Message> {
        return portalClient.getHistory(params, listener)
    }


    suspend fun getUpdates(
        params: PortalCustomerService.Params,
        listener: DialogListener
    ): List<Message> {
        return portalClient.getUpdates(params, listener)
    }


    suspend fun sendMessage(
        listener: DialogListener,
        options: Message.options
    ): Message? {
        return portalClient.sendMessage(listener, options)
    }

    suspend fun downloadFile(
        fileId: String,
        observer: StreamObserver,
        listener: DialogListener,
    ) {
        return portalClient.downloadFile(fileId, observer, listener)
    }
}