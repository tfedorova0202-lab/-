package com.example

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FirestoreManager
import com.example.data.Debt
import com.example.data.DebtDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DebtCalculatorViewModel(
    private val dao: DebtDao,
    private val firestoreManager: FirestoreManager? = null
) : ViewModel() {

    // 1. Получаем живой поток из базы данных (Room как офлайн-кеш)
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

    // 3. Сохранение или обновление долга (Ядро расчетов План/Факт + синхронизация с облаком)
    fun saveDebt(
        id: String?,
        creditorName: String,
        contractDate: String,
        factAmount: Double,
        penaltyAmount: Double,
        insuranceAmount: Double,
        isMortgage: Boolean,
        deadlineDate: String = "",
        deadlineType: String = ""
    ) {
        // Юридическая логика расчета: срезаем незаконные накрутки и страховки
        var currentPlan = factAmount - penaltyAmount - insuranceAmount
        if (currentPlan < 0) currentPlan = 0.0 // Защита от отрицательного долга
        
        val finalId = id ?: UUID.randomUUID().toString()
        val legalBasisText = if (penaltyAmount > 0 && insuranceAmount > 0) {
            "ст. 333 ГК РФ, ст. 958 ГК РФ"
        } else if (penaltyAmount > 0) {
            "ст. 333 ГК РФ"
        } else if (insuranceAmount > 0) {
            "ст. 958 ГК РФ, 353-ФЗ"
        } else {
            "ст. 809 ГК РФ"
        }
        
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
            deadlineType = deadlineType,
            legalBasis = legalBasisText
        )
        
        viewModelScope.launch {
            dao.insertDebt(debt) // Сохраняем надежно в локальный Room
            firestoreManager?.let { fs ->
                try {
                    fs.saveDebt(debt) // Синхронизируем в облако Firestore, если авторизован
                } catch (e: Exception) {
                    Log.w(TAG, "Cloud sync skipped: ${e.message}")
                }
            }
        }
    }

    // 4. Удаление долга
    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            dao.deleteDebt(debt)
            firestoreManager?.let { fs ->
                try {
                    fs.deleteDebt(debt.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Cloud delete skipped: ${e.message}")
                }
            }
        }
    }

    // 5. Синхронизация с Firestore
    fun syncWithFirestore() {
        viewModelScope.launch {
            firestoreManager?.let { fs ->
                val cloudDebtsResult = fs.getDebts()
                if (cloudDebtsResult.isSuccess) {
                    val cloudDebts = cloudDebtsResult.getOrDefault(emptyList())
                    for (cloudDebt in cloudDebts) {
                        dao.insertDebt(cloudDebt)
                    }
                }
            }
        }
    }

    // 6. Очистка реестра
    fun clearAllDebts() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    // 7. Загрузка демонстрационных данных для макетов приложений и презентаций
    fun loadMockupPresentationData() {
        viewModelScope.launch {
            val sampleDebts = listOf(
                Debt(
                    id = "mockup-sber",
                    creditorName = "ПАО Сбербанк (Кредит №49201)",
                    contractDate = "12.04.2023",
                    factAmount = 380000.0,
                    penaltyAmount = 65000.0,
                    insuranceAmount = 28000.0,
                    planAmount = 287000.0,
                    status = "В работе (Претензия)",
                    isMortgage = false,
                    deadlineDate = "30.09.2026",
                    deadlineType = "Период охлаждения",
                    legalBasis = "ст. 333 ГК РФ, ст. 958 ГК РФ"
                ),
                Debt(
                    id = "mockup-tbank",
                    creditorName = "АО Т-Банк (Карта *8412)",
                    contractDate = "18.09.2023",
                    factAmount = 145000.0,
                    penaltyAmount = 32000.0,
                    insuranceAmount = 9000.0,
                    planAmount = 104000.0,
                    status = "В работе (ст. 333 ГК РФ)",
                    isMortgage = false,
                    legalBasis = "ст. 333 ГК РФ"
                ),
                Debt(
                    id = "mockup-zaimer",
                    creditorName = "МФК Займер (Онлайн-заем №8812)",
                    contractDate = "05.02.2024",
                    factAmount = 42000.0,
                    penaltyAmount = 19000.0,
                    insuranceAmount = 0.0,
                    planAmount = 23000.0,
                    status = "Судебный приказ",
                    isMortgage = false,
                    deadlineDate = "29.09.2026",
                    deadlineType = "10 дней (ст. 128 ГПК)",
                    legalBasis = "ч. 23 ст. 5 ФЗ № 353-ФЗ"
                ),
                Debt(
                    id = "mockup-vtb-mortgage",
                    creditorName = "Банк ВТБ (Ипотека №55102)",
                    contractDate = "14.11.2021",
                    factAmount = 2400000.0,
                    penaltyAmount = 0.0,
                    insuranceAmount = 0.0,
                    planAmount = 2400000.0,
                    status = "Ипотека (Иммунитет)",
                    isMortgage = true,
                    legalBasis = "ст. 213.10-1 ФЗ № 127-ФЗ"
                )
            )
            for (debt in sampleDebts) {
                dao.insertDebt(debt)
            }
        }
    }

    companion object {
        private const val TAG = "DebtCalculatorVM"
    }
}
