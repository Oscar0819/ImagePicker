package com.milet0819.notificationtest.common.utils

import android.util.Log
import com.milet0819.notificationtest.BuildConfig
import kotlin.coroutines.coroutineContext

fun logger(message: Any) {
    if (BuildConfig.DEBUG) {
        Log.e("Milet0819 LOG", "${getStackTrace()} \n $message")
    }
}

private fun getStackTrace(): String? {
    var logInfo = "Log Info"
    try {
        val elements = Throwable().stackTrace
        val className = elements[2].className

        logInfo = "$className"
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return logInfo
}