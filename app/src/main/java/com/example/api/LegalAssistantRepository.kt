package com.example.api

import com.example.BuildConfig
import com.example.data.QueryHistory
import com.example.data.QueryHistoryDao
import kotlinx.coroutines.flow.Flow

class LegalAssistantRepository(private val queryHistoryDao: QueryHistoryDao) {
    private val apiKey = BuildConfig.GEMINI_API_KEY
    
    val chatHistory: Flow<List<QueryHistory>> = queryHistoryDao.getAllHistory()
    
    // For general chat and multi-turn (uses gemini-3.5-flash)
    suspend fun sendGeneralChatMessage(
        messages: List<Content>,
        systemPrompt: String
    ): String {
        val request = GenerateContentRequest(
            contents = messages,
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt))),
            tools = listOf(Tool(googleSearch = GoogleSearch()))
        )
        return executeCall("gemini-3.5-flash", request)
    }

    // For complex legal analysis (uses gemini-3.1-pro-preview)
    suspend fun analyzeComplexDocument(
        text: String,
        systemPrompt: String
    ): String {
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt)))
        )
        return executeCall("gemini-3.1-pro-preview", request)
    }
    
    // For fast categorization or simple tasks (uses gemini-3.1-flash-lite)
    suspend fun quickCategorize(
        text: String
    ): String {
         val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text))))
        )
        return executeCall("gemini-3.1-flash-lite", request)
    }

    private suspend fun executeCall(model: String, request: GenerateContentRequest): String {
        val response = RetrofitClient.service.generateContent(model, apiKey, request)
        return response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
            ?: throw Exception("Empty response from AI")
    }
    
    suspend fun saveQueryToHistory(query: String, response: String) {
        queryHistoryDao.insertHistory(QueryHistory(query = query, response = response))
    }
    
    suspend fun clearHistory() {
        queryHistoryDao.clearHistory()
    }
}
