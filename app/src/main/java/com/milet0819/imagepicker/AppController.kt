package com.milet0819.imagepicker

import android.app.Application
import com.milet0819.notificationtest.common.utils.ComponentLogger
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppController: Application() {

    @Inject
    lateinit var componentLogger: ComponentLogger

    override fun onCreate() {
        super.onCreate()

        componentLogger.initialize(this)
    }
}