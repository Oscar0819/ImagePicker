package com.milet0819.imagepicker.utils

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog

inline fun ComponentActivity.showListOptionAlertDialog(title: String,
                                     options: Array<String>,
                                     crossinline callback: (Int) -> Unit) {
    AlertDialog.Builder(this).apply {
        setTitle(title)
        setItems(options) { dialog, which ->
            callback(which)
        }
    }.show()
}