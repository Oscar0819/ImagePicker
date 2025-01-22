package com.milet0819.imagepicker.utils

import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat
import com.milet0819.notificationtest.common.utils.logger

fun isGranted(context: Context, permission: String): Boolean {
    val isGranted = ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED
    logger("$permission isGranted=$isGranted")
    return isGranted
}

fun isDenied(context: Context, permission: String): Boolean {
    val isDenied = ContextCompat.checkSelfPermission(context, permission) == PERMISSION_DENIED
    logger("$permission isDenied=$isDenied")
    return isDenied
}