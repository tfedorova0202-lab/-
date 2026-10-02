package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QueryHistoryDao {
    @Query("SELECT * FROM query_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<QueryHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: QueryHistory)

    @Query("DELETE FROM query_history")
    suspend fun clearHistory()
}
