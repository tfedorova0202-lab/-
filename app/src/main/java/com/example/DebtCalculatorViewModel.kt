package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Debt
import com.example.data.DebtDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DebtCalculatorViewModel(private val dao: DebtDao) : ViewModel() {

    // 1. Получаем живой поток из базы данных. Если закроем приложение - данные останутся.
    val debts = dao.getAllDebts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 2. Автоматический расчет ИТОГО для Факт, План и Сумм к отмене
    val totalFact = debts.map { list -> list.sumOf { it.factAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPlan = debts.map { list -> list.sumOf { it.planAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalPenalty = debts.map { list -> list.sumOf { it.penaltyAmount + it.insuranceAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 3. Сохранение или обновление долга (Ядро расчетов План/Факт)
    fun saveDebt(id: String?, creditorName: String, contractDate: String, factAmount: Double, penaltyAmount: Double, insuranceAmount: Double, isMortgage: Boolean, deadlineDate: String = "", deadlineType: String = "") {
        // Юридическая логика расчета: срезаем незаконные накрутки и страховки
        var currentPlan = factAmount - penaltyAmount - insuranceAmount
        if (currentPlan < 0) currentPlan = 0.0 // Защита от отрицательного долга
        
        val finalId = id ?: UUID.randomUUID().toString()
        
        val debt = Debt(
            id = finalId,
            creditorName = creditorName,
            contractDate = contractDate,
            factAmount = factAmount,
            penaltyAmount = penaltyAmount,
            insuranceAmount = insuranceAmount,
            planAmount = currentPlan,
            status = if (isMortgage) "Ипотека (Иммунитет)" else "В работе",
            isMortgage = isMortgage,
            deadlineDate = deadlineDate,
            deadlineType = deadlineType
        )
        
        viewModelScope.launch {
            dao.insertDebt(debt) // Сохраняем надежно в Room
        }
    }

    // 4. Удаление долга
    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            dao.deleteDebt(debt)
        }
    }
}
