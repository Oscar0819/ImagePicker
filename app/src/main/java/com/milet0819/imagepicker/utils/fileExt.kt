package com.milet0819.imagepicker.utils

import androidx.activity.ComponentActivity
import com.milet0819.notificationtest.common.utils.logger
import java.io.File

/**
 * 비어 있지 않은 디렉토리를 삭제하려면, 먼저 디렉토리 안의 모든 파일 및 하위 디렉토리를 삭제해야 합니다.
 */
fun ComponentActivity.clearDir(dir: File) {
    if (dir.exists()) {
        if (dir.isDirectory) {
            val child = dir.listFiles()
            child?.forEach { childFile ->
                val isDeleted = childFile.delete()
                if (isDeleted) {
                    logger("Delete Success")
                } else {
                    logger("Delete Failed")
                }
            }
        }
    }
}