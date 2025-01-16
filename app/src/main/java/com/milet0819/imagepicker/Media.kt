package com.milet0819.imagepicker

import android.net.Uri

data class Media(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimetype: String,
)
