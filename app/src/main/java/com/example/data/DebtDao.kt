package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    // Получение всех долгов с автоматическим обновлением (Flow)
    @Query("SELECT * FROM debts")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsList(): List<Debt>

    // Добавление или замена записи при конфликте ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    // Обновление существующей записи
    @Update
    suspend fun updateDebt(debt: Debt)

    // Удаление записи по объекту
    @Delete
    suspend fun deleteDebt(debt: Debt)

    // Удаление записи по ID
    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun deleteDebtById(debtId: String)
    
    // Очистка всей таблицы (например, при полном списании или банкротстве)
    @Query("DELETE FROM debts")
    suspend fun clearAll()
}
