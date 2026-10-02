package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "query_history")
data class QueryHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val response: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
