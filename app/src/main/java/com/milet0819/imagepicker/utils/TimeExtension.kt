package com.milet0819.notificationtest.common.utils

import java.text.SimpleDateFormat
import java.util.Locale

fun getCurrentTime(): String {
    val currentTime = System.currentTimeMillis()

    val sdf = SimpleDateFormat("현재시각은 yyyy-MM-dd hh:mm:ss.SSS ", Locale.KOREA)
    return sdf.format(currentTime)
}