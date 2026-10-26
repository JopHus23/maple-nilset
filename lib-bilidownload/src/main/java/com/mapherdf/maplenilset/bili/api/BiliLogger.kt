package com.mapherdf.maplenilset.bili.api

import android.util.Log

object BiliLogger {
    @Volatile
    var isEnabled: Boolean = false

    fun d(tag: String, message: String) {
        if (isEnabled) Log.d(tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) Log.w(tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) Log.e(tag, message, throwable)
    }
}