package com.milet0819.imagepicker.imagepicker

import android.net.Uri

data class Media(
    val uri: Uri,
    val name: String,
    val size: Long,
    val duration: Int = 0,
    val mimeType: String,
)
