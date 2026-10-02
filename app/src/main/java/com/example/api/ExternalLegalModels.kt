package com.example.api

import kotlinx.serialization.Serializable

@Serializable
data class LegalUpdateDto(
    val id: String,
    val title: String,
    val summary: String,
    val date: String,
    val sourceUrl: String
)

@Serializable
data class ExternalTemplateDto(
    val id: String,
    val title: String,
    val category: String,
    val contentUrl: String,
    val format: String
)