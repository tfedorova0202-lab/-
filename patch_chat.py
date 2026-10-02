import re

with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'r') as f:
    text = f.read()

old_block = """                // 1. Проверяем локальную базу знаний
                val localAnswer = com.example.data.KnowledgeBase.findAnswer(text)
                if (localAnswer != null) {
                    _messages.value = _messages.value + ChatMessage("model", localAnswer)
                } else {
                    // 2. Если в локальной базе ответа нет, идем в сеть к Gemini
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    
                    val contents = _messages.value.map { msg ->
                        val apiRole = if (msg.role == "user") "user" else "model"
                        Content(
                            role = apiRole,
                            parts = listOf(Part(text = msg.text))
                        )
                    }
                    
                    val systemInstruction = Content(
                        role = "user",
                        parts = listOf(Part(text = "Ты — ведущий юрист-практик с 15-летним опытом в сфере защиты прав должников. Отвечай кратко, профессионально и по делу. Успокаивай клиента, предлагай законные решения."))
                    )
                    
                    val request = GenerateContentRequest(
                        contents = contents,
                        systemInstruction = systemInstruction
                    )
                    
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val replyText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Извините, не смог сформировать ответ."
                    
                    _messages.value = _messages.value + ChatMessage("model", replyText)
                }"""

new_block = """                // 1. Проверяем локальную базу знаний
                val localAnswer = com.example.data.KnowledgeBase.findAnswer(text)
                
                // 2. Идем в сеть к Gemini, обогащая промпт локальными данными
                val apiKey = BuildConfig.GEMINI_API_KEY
                
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
                    systemInstruction = systemInstruction
                )
                
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val replyText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Извините, не смог сформировать ответ."
                
                _messages.value = _messages.value + ChatMessage("model", replyText)"""

if old_block in text:
    text = text.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/ChatViewModel.kt', 'w') as f:
        f.write(text)
    print("Patched successfully")
else:
    print("Old block not found!")
