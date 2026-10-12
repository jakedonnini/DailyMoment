package com.example.dailymoment.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailymoment.data.AppDatabase
import com.example.dailymoment.data.Entry
import com.example.dailymoment.data.Prefs
import com.example.dailymoment.notify.DailyScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EntryViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).entryDao()

    val entries: StateFlow<List<Entry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val streak: StateFlow<Int> = entries.map { computeStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun save(photoPath: String, happened: String, learned: String, onDone: () -> Unit) {
        viewModelScope.launch {
            dao.insert(
                Entry(
                    createdAt = System.currentTimeMillis(),
                    photoPath = photoPath,
                    happened = happened.trim(),
                    learned = learned.trim()
                )
            )
            val app = getApplication<Application>()
            Prefs.setLastEntryDay(app, LocalDate.now().toString())
            DailyScheduler.scheduleNext(app) // today is done -> next random time is tomorrow
            onDone()
        }
    }

    private fun computeStreak(list: List<Entry>): Int {
        val zone = ZoneId.systemDefault()
        val days = list.map { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate() }.toSet()
        var d = LocalDate.now()
        if (d !in days) d = d.minusDays(1) // today not posted yet doesn't break the streak
        var n = 0
        while (d in days) { n++; d = d.minusDays(1) }
        return n
    }
}
