package com.milet0819.notificationtest.common.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import dagger.Component

inline fun ComponentActivity.registerForActivityResult(
    crossinline callback: (ActivityResult) -> Unit
): ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult  ->
        callback(activityResult)
    }

inline fun Fragment.registerForActivityResult(
    crossinline callback: (ActivityResult) -> Unit
): ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            callback(activityResult)
        }
    }

inline fun ComponentActivity.requestPermission(
    crossinline callback: (Boolean) -> Unit
): ActivityResultLauncher<String> =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        callback(isGranted)
    }

inline fun ComponentActivity.requestPermissions(
    crossinline callback: (Map<String, Boolean>) -> Unit
): ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        callback(results)
    }

inline fun ComponentActivity.takeCamera(
    crossinline callback: (Boolean) -> Unit
): ActivityResultLauncher<Uri> =
    registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
        callback(result)
    }

inline fun ComponentActivity.captureVideo(
    crossinline callback: (Boolean) -> Unit
): ActivityResultLauncher<Uri> =
    registerForActivityResult(ActivityResultContracts.CaptureVideo()) { result ->
        callback(result)
    }