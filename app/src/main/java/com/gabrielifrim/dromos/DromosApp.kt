package com.gabrielifrim.dromos

import android.app.Application
import com.gabrielifrim.dromos.core.tracking.TrackingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DromosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TrackingService.createChannel(this)
    }
}
