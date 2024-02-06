package com.webitel.mobile_demo_app.app

import android.app.Application
import com.webitel.mobile_demo_app.data.local.LocalCacheProvider
import com.webitel.mobile_demo_app.data.remote.PortalCustomerService
import com.webitel.mobile_sdk.domain.PortalClient


class DemoApp: Application()  {

    // Webitel (Client: Portal App) token issued
    private val PORTAL_CLIENT = "dsfjjeijnddkjfngjdfgndfnkgnJnnindis"
    // Webitel Customer Portal service host address
    private val ADDRESS = "grpcs://demo.webitel.me:443"

    lateinit var portalClient2: PortalCustomerService
        private set


    override fun onCreate() {
        super.onCreate()

        LocalCacheProvider.createInstance(this)
        portalClient2 = PortalCustomerService(
            this,
            token = PORTAL_CLIENT,
            address = ADDRESS
        )
        instance = this
    }


    companion object {
        lateinit var instance: DemoApp
            private set
    }
}