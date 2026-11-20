package com.example.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object ExternalRetrofitClient {
    // Production backend URL must be supplied via secure configuration; dev URLs are prohibited in release builds.
    private const val PRODUCTION_BASE_URL = ""

    fun isConfigured(): Boolean = PRODUCTION_BASE_URL.startsWith("https://")

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: ExternalLegalApiService by lazy {
        check(isConfigured()) {
            "Production legal updates backend URL is not configured (dev endpoints are disabled)."
        }
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(PRODUCTION_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(ExternalLegalApiService::class.java)
    }
}