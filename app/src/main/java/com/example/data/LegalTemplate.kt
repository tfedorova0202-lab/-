package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "legal_templates")
data class LegalTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val content: String
)
