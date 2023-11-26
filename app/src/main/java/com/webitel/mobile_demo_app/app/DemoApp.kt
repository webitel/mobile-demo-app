package com.webitel.mobile_demo_app.app

import android.app.Application
import com.webitel.mobile_sdk.domain.PortalClient


class DemoApp: Application()  {

    // Webitel (Client: Portal App) token issued
    private val PORTAL_CLIENT = "dsfjjeijnddkjfngjdfgndfnkgnJnnindis"
    // Webitel Customer Portal service host address
    private val ADDRESS = "grpcs://demo.webitel.me:443"

    lateinit var portalClient: PortalClient
        private set


    override fun onCreate() {
        super.onCreate()

        instance = this
        portalClient = PortalClient
            .Builder(
                application = this,
                address = ADDRESS,
                token = PORTAL_CLIENT
            )
            .build()
    }


    companion object {
        lateinit var instance: DemoApp
            private set
    }
}