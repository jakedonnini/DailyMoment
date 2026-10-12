package com.example.dailymoment

import android.app.Application
import com.example.dailymoment.notify.DailyScheduler
import com.example.dailymoment.notify.NotificationHelper

class MomentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        DailyScheduler.ensureScheduled(this)
    }
}
