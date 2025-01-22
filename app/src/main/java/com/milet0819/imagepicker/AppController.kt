package com.milet0819.imagepicker

import android.app.Application
import com.milet0819.notificationtest.common.utils.ComponentLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppController: Application() {

    companion object {
        lateinit var imagesDirPath: String
        lateinit var videoDirPath: String
        const val IMAGES_DIR = "images"
        const val VIDEOS_DIR = "videos"
    }

    @Inject
    lateinit var componentLogger: ComponentLogger

    override fun onCreate() {
        super.onCreate()

        imagesDirPath = "$cacheDir/$IMAGES_DIR/"
        videoDirPath = "$cacheDir/$VIDEOS_DIR/"

        if (BuildConfig.DEBUG) {
            componentLogger.initialize(this)
        }
    }
}