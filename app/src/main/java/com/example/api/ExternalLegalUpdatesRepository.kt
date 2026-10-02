package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExternalLegalUpdatesRepository(
    private val apiService: ExternalLegalApiService = ExternalRetrofitClient.service
) {
    suspend fun fetchLatestUpdates(): Result<List<LegalUpdateDto>> = withContext(Dispatchers.IO) {
        try {
            val updates = apiService.getLegalUpdates(limit = 20)
            Result.success(updates)
        } catch (e: Exception) {
            Result.failure(e)
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
}