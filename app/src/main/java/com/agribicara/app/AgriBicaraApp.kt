package com.agribicara.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class AgriBicaraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Build rilis: tree yang meneruskan WARN/ERROR ke Crashlytics
        // ditambahkan di Fase 8. Jangan log data pribadi di rilis.
    }
}
