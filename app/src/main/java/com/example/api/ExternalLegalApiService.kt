package com.example.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ExternalLegalApiService {
    @GET("v1/legal-updates")
    suspend fun getLegalUpdates(
        @Query("limit") limit: Int = 20
    ): List<LegalUpdateDto>

    @GET("v1/templates")
    suspend fun getTemplates(
        @Query("category") category: String? = null
    ): List<ExternalTemplateDto>
}