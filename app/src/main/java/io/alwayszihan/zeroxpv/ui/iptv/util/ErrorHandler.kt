package io.alwayszihan.zeroxpv.ui.iptv.util

import android.util.Log

object ErrorHandler {

    private const val TAG = "ZeroXPV"

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, "[$tag] $message", throwable)
        } else {
            Log.e(TAG, "[$tag] $message")
        }
    }
}
