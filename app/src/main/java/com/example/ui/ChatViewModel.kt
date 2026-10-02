package com.example.ui

import com.example.api.Tool
import com.example.api.GoogleSearch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.AppDatabase
import com.example.api.LegalAssistantRepository
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("model", "Здравствуйте! Я ваш ИИ-помощник по защите прав заемщиков. Я могу проанализировать вашу ситуацию, рассказать о 230-ФЗ, помочь с банкротством или отбиться от коллекторов. Какой у вас вопрос?")
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val repository = LegalAssistantRepository(
        AppDatabase.getDatabase(application).queryHistoryDao()
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val userMsg = ChatMessage("user", text)
        _messages.value = _messages.value + userMsg
        
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Проверяем локальную базу знаний
                val localAnswer = com.example.data.KnowledgeBase.findAnswer(text)
                
                // 2. Идем в сеть к Gemini, обогащая промпт локальными данными
                val contents = _messages.value.map { msg ->
                    val apiRole = if (msg.role == "user") "user" else "model"
                    Content(
                        role = apiRole,
                        parts = listOf(Part(text = msg.text))
                    )
                }
                
                val basePrompt = "Ты — ведущий юрист-практик с 15-летним опытом в сфере защиты прав должников. Отвечай кратко, профессионально и по делу. Успокаивай клиента, предлагай законные решения."
                val promptWithKnowledge = if (localAnswer != null) {
                    "$basePrompt\n\nИспользуй эту выдержку из локальной базы ФЗ-127 для точного ответа: $localAnswer"
                } else {
                    basePrompt
                }
                
                val replyText = repository.sendGeneralChatMessage(contents, promptWithKnowledge)
                
                _messages.value = _messages.value + ChatMessage("model", replyText)
                
                // Save to history offline
                repository.saveQueryToHistory(text, replyText)
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("model", "Ошибка соединения: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
