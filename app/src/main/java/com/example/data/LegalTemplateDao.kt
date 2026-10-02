package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LegalTemplateDao {
    @Query("SELECT * FROM legal_templates ORDER BY category, title")
    fun getAllTemplates(): Flow<List<LegalTemplate>>

    @Query("SELECT * FROM legal_templates WHERE category = :category ORDER BY title")
    fun getTemplatesByCategory(category: String): Flow<List<LegalTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: LegalTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<LegalTemplate>)
}
