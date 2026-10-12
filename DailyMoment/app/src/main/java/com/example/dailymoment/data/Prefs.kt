package com.example.dailymoment.data

import android.content.Context

/** Tiny SharedPreferences wrapper. Window hours are defaults for now; add a settings screen later. */
object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("moment_prefs", Context.MODE_PRIVATE)

    fun windowStartHour(c: Context) = sp(c).getInt("window_start", 9)
    fun windowEndHour(c: Context) = sp(c).getInt("window_end", 21)

    fun lastEntryDay(c: Context): String? = sp(c).getString("last_entry_day", null)
    fun setLastEntryDay(c: Context, day: String) = sp(c).edit().putString("last_entry_day", day).apply()

    fun lastNotifiedDay(c: Context): String? = sp(c).getString("last_notified_day", null)
    fun setLastNotifiedDay(c: Context, day: String) = sp(c).edit().putString("last_notified_day", day).apply()

    fun nextAlarmAt(c: Context): Long = sp(c).getLong("next_alarm_at", 0L)
    fun setNextAlarmAt(c: Context, t: Long) = sp(c).edit().putLong("next_alarm_at", t).apply()
}
