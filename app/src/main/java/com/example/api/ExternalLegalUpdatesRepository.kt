package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExternalLegalUpdatesRepository(
    private val apiServiceProvider: () -> ExternalLegalApiService = { ExternalRetrofitClient.service }
) {
    suspend fun fetchLatestUpdates(): Result<List<LegalUpdateDto>> = withContext(Dispatchers.IO) {
        if (!ExternalRetrofitClient.isConfigured()) {
            return@withContext Result.success(getDefaultLegalUpdates())
        }
        try {
            val updates = apiServiceProvider().getLegalUpdates(limit = 20)
            if (updates.isNotEmpty()) {
                Result.success(updates)
            } else {
                Result.success(getDefaultLegalUpdates())
            }
        } catch (e: Exception) {
            Result.success(getDefaultLegalUpdates())
        }
    }

    suspend fun fetchTemplates(category: String? = null): Result<List<ExternalTemplateDto>> = withContext(Dispatchers.IO) {
        if (!ExternalRetrofitClient.isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Сервер обновлений шаблонов не настроен; используются локальные шаблоны."))
        }
        try {
            val templates = apiServiceProvider().getTemplates(category)
            Result.success(templates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getDefaultLegalUpdates(): List<LegalUpdateDto> = listOf(
        LegalUpdateDto(
            id = "law-353-2026",
            title = "ФЗ № 353-ФЗ (ред. ФЗ № 545-ФЗ): Предельные лимиты переплат МФО",
            summary = "По договорам потребительского займа сроком до 1 года, заключённым с 01.04.2026, предельная сумма всех процентов, неустоек и платных услуг ограничена 100% от суммы займа (для договоров с 01.07.2023 по 31.03.2026 — 130%, ранее — 150%).",
            date = "01.04.2026",
            sourceUrl = "https://pravo.gov.ru"
        ),
        LegalUpdateDto(
            id = "law-127-mortgage",
            title = "ФЗ № 127-ФЗ ст. 213.10-1: Сохранение ипотечного жилья",
            summary = "Действует механизм утверждения судом отдельного мирового соглашения с залоговым кредитором (при отсутствии просрочки или согласии банка / участии третьего лица) для сохранения единственного ипотечного жилья в процедуре банкротства.",
            date = "08.09.2024",
            sourceUrl = "https://pravo.gov.ru"
        ),
        LegalUpdateDto(
            id = "law-230-antispam",
            title = "ФЗ № 230-ФЗ: Защита родственников и третьих лиц",
            summary = "Взаимодействие с третьими лицами допускается строго при наличии письменного согласия должника и невыраженного несогласия третьего лица; отзыв согласия (ч. 7 ст. 4) подается в любой момент.",
            date = "01.02.2024",
            sourceUrl = "https://pravo.gov.ru"
        )
    )
}
