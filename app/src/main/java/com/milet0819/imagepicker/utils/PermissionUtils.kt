package com.milet0819.imagepicker.utils

import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat

// TODO MODIFY
object PermissionUtils {
    fun isGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED
    }
}