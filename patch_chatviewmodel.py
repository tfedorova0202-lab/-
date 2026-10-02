import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    text = f.read()

# Add imports for Database and Application if needed
imports = """import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.AppDatabase
import com.example.api.LegalAssistantRepository"""

text = text.replace('import androidx.lifecycle.ViewModel', imports)

# Change ChatViewModel to AndroidViewModel
text = text.replace('class ChatViewModel : ViewModel() {', 'class ChatViewModel(application: Application) : AndroidViewModel(application) {')

# Add repository instantiation inside ChatViewModel
repo_init = """    private val repository = LegalAssistantRepository(
        AppDatabase.getDatabase(application).queryHistoryDao()
    )"""

text = text.replace('val isLoading: StateFlow<Boolean> = _isLoading', 'val isLoading: StateFlow<Boolean> = _isLoading\n' + repo_init)

# Use repository inside sendMessage
old_try_block = """                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val contents = _messages.value.map { msg ->
                    val apiRole = if (msg.role == "user") "user" else "model"
                    Content(
                        role = apiRole,
                        parts = listOf(Part(text = msg.text))
                    )
                }
                
                val basePrompt = "Ты — ведущий юрист-практик с 15-летним опытом в сфере защиты прав должников. Отвечай кратко, профессионально и по делу. Успокаивай клиента, предлагай законные решения."
                val promptWithKnowledge = if (localAnswer != null) {
                    "$basePrompt\\n\\nИспользуй эту выдержку из локальной базы ФЗ-127 для точного ответа: $localAnswer"
                } else {
                    basePrompt
                }
                
                val systemInstruction = Content(
                    role = "user",
                    parts = listOf(Part(text = promptWithKnowledge))
                )
                
                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = systemInstruction,
                    tools = listOf(Tool(googleSearch = GoogleSearch()))
                )
                
                val response = RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                val replyText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Извините, не смог сформировать ответ."
                
                _messages.value = _messages.value + ChatMessage("model", replyText)"""

new_try_block = """                val contents = _messages.value.map { msg ->
                    val apiRole = if (msg.role == "user") "user" else "model"
                    Content(
                        role = apiRole,
                        parts = listOf(Part(text = msg.text))
                    )
                }
                
                val basePrompt = "Ты — ведущий юрист-практик с 15-летним опытом в сфере защиты прав должников. Отвечай кратко, профессионально и по делу. Успокаивай клиента, предлагай законные решения."
                val promptWithKnowledge = if (localAnswer != null) {
                    "$basePrompt\\n\\nИспользуй эту выдержку из локальной базы ФЗ-127 для точного ответа: $localAnswer"
                } else {
                    basePrompt
                }
                
                val replyText = repository.sendGeneralChatMessage(contents, promptWithKnowledge)
                
                _messages.value = _messages.value + ChatMessage("model", replyText)
                
                // Save to history offline
                repository.saveQueryToHistory(text, replyText)"""

text = text.replace(old_try_block, new_try_block)

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
    f.write(text)
print("Patched ChatViewModel.kt")
