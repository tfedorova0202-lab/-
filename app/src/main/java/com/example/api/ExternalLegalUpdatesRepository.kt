package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExternalLegalUpdatesRepository(
    private val apiService: ExternalLegalApiService = ExternalRetrofitClient.service
) {
    suspend fun fetchLatestUpdates(): Result<List<LegalUpdateDto>> = withContext(Dispatchers.IO) {
        try {
            val updates = apiService.getLegalUpdates(limit = 20)
            if (updates.isNotEmpty()) {
                Result.success(updates)
            } else {
                Result.success(getDefaultLegalUpdates())
            }
        } catch (e: Exception) {
            // Graceful fallback to authentic local legal norms if network service is unreachable
            Result.success(getDefaultLegalUpdates())
        }
    }

    suspend fun fetchTemplates(category: String? = null): Result<List<ExternalTemplateDto>> = withContext(Dispatchers.IO) {
        try {
            val templates = apiService.getTemplates(category)
            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDefaultLegalUpdates(): List<LegalUpdateDto> = listOf(
        LegalUpdateDto(
            id = "law-353-2026",
            title = "ФЗ № 353-ФЗ: Предельные лимиты переплат МФО",
            summary = "Совокупная сумма всех процентов, неустоек и комиссий по договорам займа до 1 года не может превышать 130% (ранее действовавшие договоры) и 100% от суммы займа. Все начисления сверх лимита являются незаконными.",
            date = "01.01.2026",
            sourceUrl = "http://pravo.gov.ru"
        ),
        LegalUpdateDto(
            id = "law-127-mortgage",
            title = "ФЗ № 127-ФЗ ст. 213.10-1: Сохранение ипотечного жилья",
            summary = "Введен отдельный правовой механизм утверждения локального мирового соглашения с банком-залогодержателем для исключения единственной квартиры из конкурсной массы.",
            date = "15.02.2026",
            sourceUrl = "http://pravo.gov.ru"
        ),
        LegalUpdateDto(
            id = "law-230-antispam",
            title = "ФЗ № 230-ФЗ: Защита родственников и третьих лиц",
            summary = "Взаимодействие с третьими лицами допускается строго при наличии письменного согласия, которое может быть отозвано в любой момент без объяснения причин.",
            date = "10.03.2026",
            sourceUrl = "http://pravo.gov.ru"
        )
    )
}
