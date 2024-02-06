package com.webitel.mobile_demo_app.data.remote

import com.webitel.mobile_sdk.domain.Code
import com.webitel.mobile_sdk.domain.Error

class PortalException(
    private val error: Error
) : Exception() {
    val code: Code
        get() = error.code

    override val message: String
        get() = error.message

    override fun toString() = error.toString()
}