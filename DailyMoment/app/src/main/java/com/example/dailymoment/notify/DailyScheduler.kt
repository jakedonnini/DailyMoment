package com.example.dailymoment.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.dailymoment.data.Prefs
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

object DailyScheduler {
    private const val REQUEST_CODE = 1001

    /** Keep the existing alarm if it's still in the future (app launch, reboot); otherwise pick a new time. */
    fun ensureScheduled(context: Context) {
        val app = context.applicationContext
        val stored = Prefs.nextAlarmAt(app)
        if (stored > System.currentTimeMillis()) setAlarm(app, stored) else scheduleNext(app)
    }

    /** Always pick a fresh random time (after a notification fires or an entry is saved). */
    fun scheduleNext(context: Context) {
        val app = context.applicationContext
        val triggerAt = computeNextTrigger(app)
        Prefs.setNextAlarmAt(app, triggerAt)
        setAlarm(app, triggerAt)
    }

    private fun setAlarm(app: Context, triggerAt: Long) {
        val am = app.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            app, REQUEST_CODE, Intent(app, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun computeNextTrigger(app: Context): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now()
        val startH = Prefs.windowStartHour(app)
        val endH = Prefs.windowEndHour(app)

        val today = LocalDate.now()
        val doneToday = today.toString() == Prefs.lastEntryDay(app) ||
                today.toString() == Prefs.lastNotifiedDay(app)

        var from = maxOf(now.plusMinutes(1), today.atTime(startH, 0))
        var to = today.atTime(endH, 0)

        // Already handled today, or today's window is over -> use tomorrow's window.
        if (doneToday || !from.isBefore(to)) {
            val tomorrow = today.plusDays(1)
            from = tomorrow.atTime(startH, 0)
            to = tomorrow.atTime(endH, 0)
        }

        val windowSeconds = Duration.between(from, to).seconds.coerceAtLeast(1)
        val pick = from.plusSeconds(Random.nextLong(windowSeconds))
        return pick.atZone(zone).toInstant().toEpochMilli()
    }
}
