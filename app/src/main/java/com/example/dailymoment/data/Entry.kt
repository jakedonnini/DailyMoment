package com.example.dailymoment.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,          // epoch millis
    val photoPath: String,        // absolute path in app-private storage
    val happened: String,         // "What happened today?"
    val learned: String           // "What did you learn?"
)
