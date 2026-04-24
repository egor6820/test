package com.example.tossday

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        com.example.tossday.notifications.NotificationHelper.createChannel(this)
    }
}
