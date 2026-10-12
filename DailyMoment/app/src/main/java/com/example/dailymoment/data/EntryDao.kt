package com.example.dailymoment.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Entry>>

    @Insert
    suspend fun insert(entry: Entry): Long

    @Delete
    suspend fun delete(entry: Entry)
}
