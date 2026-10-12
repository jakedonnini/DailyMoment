package com.example.dailymoment.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailymoment.data.Prefs
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.show(context)
        Prefs.setLastNotifiedDay(context, LocalDate.now().toString())
        DailyScheduler.scheduleNext(context) // queue up tomorrow's random time
    }
}
