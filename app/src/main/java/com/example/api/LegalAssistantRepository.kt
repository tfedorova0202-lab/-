package com.example.api

import com.example.api.SafetySetting

import com.example.BuildConfig
import com.example.data.MessageDao
import com.example.data.MessageEntity
import com.example.data.QueryHistory
import com.example.data.QueryHistoryDao
import kotlinx.coroutines.flow.Flow

class LegalAssistantRepository(
    private val queryHistoryDao: QueryHistoryDao,
    private val messageDao: MessageDao? = null,
    private val customApiKeyProvider: (() -> String)? = null
) {
    suspend fun getAllSavedMessages(): List<MessageEntity> {
        return messageDao?.getAllMessages() ?: emptyList()
    }

    suspend fun saveChatMessage(id: String, text: String, isUser: Boolean, timestamp: Long = System.currentTimeMillis()) {
        messageDao?.insertMessage(MessageEntity(id = id, text = text, isUser = isUser, timestamp = timestamp))
    }

    suspend fun clearChatMessages() {
        messageDao?.clearHistory()
    }
    private fun getActiveApiKey(): String {
        val custom = customApiKeyProvider?.invoke()?.trim()
        if (!custom.isNullOrBlank()) {
            return custom
        }
        val buildKey = ""
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY" && buildKey != "dummy_key") {
            return buildKey
        }
        return ""
    }
    
    val chatHistory: Flow<List<QueryHistory>> = queryHistoryDao.getAllHistory()
    
    // For general chat and multi-turn (uses gemini-3.5-flash with proper multi-turn turn alternating)
    suspend fun sendGeneralChatMessage(
        messages: List<Content>,
        systemPrompt: String
    ): String {
        // Gemini API strict rule: contents MUST start with 'user' role and alternate turns!
        // We drop any leading 'model' greeting messages to prevent 400 Bad Request
        val firstUserIndex = messages.indexOfFirst { it.role == "user" }
        val validContents = if (firstUserIndex != -1) {
            messages.subList(firstUserIndex, messages.size)
        } else {
            messages
        }

        
        
        val safetySettings = listOf(
            SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "BLOCK_MEDIUM_AND_ABOVE")
        )

        val request = GenerateContentRequest(
            safetySettings = safetySettings,
            contents = if (validContents.isEmpty()) messages else validContents,
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt + "\n\nCRITICAL POLICY: DO NOT provide instructions on illegal activities. Remind the user to not share PII. All outputs are for informational purposes only.")))
        )
        return try {
            executeCall("gemini-3.5-flash", request)
        } catch (e: Exception) {
            // Fallback to gemini-2.5-flash if 3.5 is unavailable or throttled
            try {
                executeCall("gemini-2.5-flash", request)
            } catch (fallbackError: Exception) {
                throw e
            }
        }
    }

    // For complex legal analysis (uses gemini-3.1-pro-preview)
    suspend fun analyzeDocumentWithFile(
        text: String,
        base64Data: String,
        mimeType: String,
        systemPrompt: String
    ): String {
        
        
        val safetySettings = listOf(
            SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "BLOCK_MEDIUM_AND_ABOVE")
        )

        val request = GenerateContentRequest(
            safetySettings = safetySettings,
            contents = listOf(Content(role = "user", parts = listOf(
                Part(text = text),
                Part(inlineData = InlineData(mimeType = mimeType, data = base64Data))
            ))),
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt + "\n\nCRITICAL POLICY: DO NOT provide instructions on illegal activities. Remind the user to not share PII. All outputs are for informational purposes only.")))
        )
        return executeCall("gemini-3.1-pro-preview", request)
    }

    suspend fun analyzeComplexDocument(
        text: String,
        systemPrompt: String
    ): String {
        
        
        val safetySettings = listOf(
            SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "BLOCK_MEDIUM_AND_ABOVE")
        )

        val request = GenerateContentRequest(
            safetySettings = safetySettings,
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text)))),
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt + "\n\nCRITICAL POLICY: DO NOT provide instructions on illegal activities. Remind the user to not share PII. All outputs are for informational purposes only.")))
        )
        return executeCall("gemini-3.1-pro-preview", request)
    }
    
    // For fast categorization or simple tasks (uses gemini-3.1-flash-lite-preview)
    suspend fun quickCategorize(
        text: String
    ): String {
         
        
        val safetySettings = listOf(
            SafetySetting(category = "HARM_CATEGORY_HARASSMENT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_HATE_SPEECH", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_SEXUALLY_EXPLICIT", threshold = "BLOCK_MEDIUM_AND_ABOVE"),
            SafetySetting(category = "HARM_CATEGORY_DANGEROUS_CONTENT", threshold = "BLOCK_MEDIUM_AND_ABOVE")
        )

        val request = GenerateContentRequest(
            safetySettings = safetySettings,
            contents = listOf(Content(role = "user", parts = listOf(Part(text = text))))
        )
        return executeCall("gemini-3.1-flash-lite-preview", request)
    }

    private suspend fun executeCall(model: String, request: GenerateContentRequest): String {
        val key = getActiveApiKey()
        if (key.isBlank()) {
            throw Exception("API-ключ Gemini не настроен. Нажмите на значок ключа вверху экрана или в Профиле, чтобы указать ваш ключ, либо используйте офлайн-консультант.")
        }
        val response = RetrofitClient.service.generateContent(model, key, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
            ?: throw Exception("Не удалось получить ответ от ИИ (пустой ответ или блокировка фильтром безопасности)")
    }
    
    suspend fun saveQueryToHistory(query: String, response: String) {
        queryHistoryDao.insertHistory(QueryHistory(query = query, response = response))
    }
    
    suspend fun clearHistory() {
        queryHistoryDao.clearHistory()
    }
}
