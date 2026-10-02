package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.LegalAssistantRepository
import com.example.api.Part
import com.example.data.AppDatabase
import com.example.data.KnowledgeBase
import com.example.utils.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val role: String, val text: String)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val userPrefs = UserPreferences(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            "model", 
            "Здравствуйте! Я ИИ-ассистент по правовым вопросам. Обращаю внимание: мои ответы носят ознакомительный характер и не заменяют консультацию специалиста. Пожалуйста, не передавайте мне полные паспортные данные и CVV-коды карт.\n\nЯ помогу вам пересчитать законный долг, составить план действий и проанализировать документы.\n\n" +

            "Какой у вас вопрос? Вы также можете использовать макрокоманды: /ПЛАН_ФАКТ, /АНТИКОЛЛЕКТОР, /ПРИВАТНОСТЬ, /СНИЖЕНИЕ_СТОИМОСТИ, /ИПОТЕКА_ЩИТ"
        )
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _customApiKey = MutableStateFlow(userPrefs.getCustomGeminiApiKey())
    val customApiKey: StateFlow<String> = _customApiKey

    private val database = AppDatabase.getDatabase(application)
    private val repository = LegalAssistantRepository(
        queryHistoryDao = database.queryHistoryDao(),
        messageDao = database.messageDao(),
        customApiKeyProvider = { userPrefs.getCustomGeminiApiKey() }
    )

    init {
        loadSavedMessages()
    }

    private fun loadSavedMessages() {
        viewModelScope.launch {
            try {
                val saved = repository.getAllSavedMessages()
                if (saved.isNotEmpty()) {
                    _messages.value = saved.map {
                        ChatMessage(role = if (it.isUser) "user" else "model", text = it.text)
                    }
                }
            } catch (e: Exception) {
                // Если база пустая или первая загрузка, оставляем дефолтное приветствие
            }
        }
    }

    private fun persistMessage(role: String, text: String) {
        viewModelScope.launch {
            try {
                repository.saveChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    text = text,
                    isUser = (role == "user"),
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                repository.clearChatMessages()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val resetMsg = ChatMessage(
                "model",
                "История диалога успешно очищена согласно протоколу приватности («Инкогнито»).\n\n" +
                "Какой у вас вопрос? Вы также можете использовать макрокоманды: /ПЛАН_ФАКТ, /АНТИКОЛЛЕКТОР, /ПРИВАТНОСТЬ, /СНИЖЕНИЕ_СТОИМОСТИ, /ИПОТЕКА_ЩИТ"
            )
            _messages.value = listOf(resetMsg)
        }
    }

    fun saveApiKey(newKey: String) {
        userPrefs.saveCustomGeminiApiKey(newKey)
        _customApiKey.value = newKey.trim()
        val note = if (newKey.isNotBlank()) {
            "API-ключ Gemini успешно сохранен в настройках приложения. Теперь ответы формируются с использованием нейросети."
        } else {
            "API-ключ сброшен. Чат переведен в автономный экспертный режим правовой базы знаний."
        }
        _messages.value = _messages.value + ChatMessage("model", note)
    }

    fun hasCustomKey(): Boolean = userPrefs.getCustomGeminiApiKey().isNotBlank()

    fun sendDocument(base64Data: String, mimeType: String, fileName: String) {
        val userMsg = ChatMessage("user", "[Файл: $fileName] Пожалуйста, проведи правовой анализ этого документа, найди суммы долга (факт, пени, страховки) и занеси их в реестр.")
        _messages.value = _messages.value + userMsg
        persistMessage("user", userMsg.text)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val basePrompt = """
                    Ты — ИИ-ассистент, выполняющий роль юриста-аналитика. ВНИМАНИЕ: твои ответы носят ознакомительный характер, не заменяют консультацию специалиста и не являются офертой. Избегай генерации вредного контента и нарушения законов. Пользователь прикрепил документ ($fileName).
                    Проанализируй его содержание.
                    
                    ВЕРНИ ОТВЕТ СТРОГО В ТАКОМ ФОРМАТЕ:
                    1. Выводы: Краткий ответ по документу.
                    2. Аргументы: Правовая позиция с обоснованием.
                    3. Риски: Возможные негативные последствия.
                    4. Источники: Точные ссылки на нормы права.
                    5. Пошаговый план действий: Четкая инструкция, что делать дальше.

                    ЕСЛИ В ДОКУМЕНТЕ ЕСТЬ ДАННЫЕ О ДОЛГАХ (требования банков, МФО, коллекторов, судебные приказы):
                    В КОНЦЕ ответа обязательно добавь блок JSON, обернутый в ```json ... ``` с массивом `extracted_debts`.
                    
                    ```json
                    {
                      "extracted_debts": [
                        {
                          "creditorName": "Название кредитора (например, ООО МФК Быстроденьги)",
                          "contractDate": "Номер и дата договора",
                          "factAmount": 15000.00,
                          "penaltyAmount": 5000.00,
                          "insuranceAmount": 0.0,
                          "planAmount": 10000.00,
                          "status": "Анализ документа",
                          "isMortgage": false
                        }
                      ]
                    }
                    ```
                    Рассчитай `planAmount` как `factAmount - penaltyAmount - insuranceAmount`.
                """.trimIndent()

                var replyText = repository.analyzeDocumentWithFile(
                    text = "Проведи правовой анализ этого документа. Извлеки все суммы и долги.",
                    base64Data = base64Data,
                    mimeType = mimeType,
                    systemPrompt = basePrompt
                )

                // Парсинг JSON блока с долгами
                val jsonRegex = Regex("```json\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
                val matchResult = jsonRegex.find(replyText)
                if (matchResult != null) {
                    val jsonString = matchResult.groupValues[1]
                    replyText = replyText.replace(matchResult.value, "").trim()
                    try {
                        val jsonObject = org.json.JSONObject(jsonString)
                        val extractedDebts = jsonObject.optJSONArray("extracted_debts")
                        if (extractedDebts != null) {
                            val db = AppDatabase.getDatabase(getApplication()).debtDao()
                            var addedCount = 0
                            for (i in 0 until extractedDebts.length()) {
                                val debtObj = extractedDebts.getJSONObject(i)
                                val debtId = java.util.UUID.randomUUID().toString()
                                val debt = com.example.data.Debt(
                                    id = debtId,
                                    creditorName = debtObj.optString("creditorName", "Неизвестно"),
                                    contractDate = debtObj.optString("contractDate", ""),
                                    factAmount = debtObj.optDouble("factAmount", 0.0),
                                    penaltyAmount = debtObj.optDouble("penaltyAmount", 0.0),
                                    insuranceAmount = debtObj.optDouble("insuranceAmount", 0.0),
                                    planAmount = debtObj.optDouble("planAmount", 0.0),
                                    status = debtObj.optString("status", "Анализ документа"),
                                    isMortgage = debtObj.optBoolean("isMortgage", false)
                                )
                                db.insertDebt(debt)
                                addedCount++
                            }
                            if (addedCount > 0) {
                                replyText += "\n\n✅ **Успешно извлечено и добавлено в Реестр задолженностей:** $addedCount записей."
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                _messages.value = _messages.value + ChatMessage("model", replyText)
                persistMessage("model", replyText)
                repository.saveQueryToHistory("[Файл: $fileName]", replyText)
            } catch (e: Exception) {
                val errText = "Ошибка при обработке файла: ${e.message}"
                _messages.value = _messages.value + ChatMessage("model", errText)
                persistMessage("model", errText)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        val trimmed = text.trim()
        if (trimmed.equals("/ОЧИСТИТЬ", ignoreCase = true) || trimmed.equals("/CLEAR", ignoreCase = true)) {
            clearChatHistory()
            return
        }

        val userMsg = ChatMessage("user", trimmed)
        _messages.value = _messages.value + userMsg
        persistMessage("user", trimmed)
        
        // Быстрая обработка макрокоманд
        val macroResponse = handleMacroCommand(trimmed)
        if (macroResponse != null) {
            _messages.value = _messages.value + ChatMessage("model", macroResponse)
            persistMessage("model", macroResponse)
            viewModelScope.launch {
                repository.saveQueryToHistory(trimmed, macroResponse)
            }
            return
        }

        val debtDao = AppDatabase.getDatabase(getApplication()).debtDao()

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Проверяем локальную базу знаний
                val localAnswer = KnowledgeBase.findAnswer(trimmed)
                
                // 2. Формируем контекст для Gemini с обязательным чередованием (user/model)
                val history = _messages.value
                val firstUserIdx = history.indexOfFirst { it.role == "user" }
                val contents = if (firstUserIdx != -1) {
                    val rawList = history.subList(firstUserIdx, history.size)
                    val mergedList = mutableListOf<Content>()
                    var currentRole = "user"
                    var currentText = StringBuilder()
                    
                    for (msg in rawList) {
                        val msgRole = if (msg.role == "user") "user" else "model"
                        if (msgRole == currentRole) {
                            if (currentText.isNotEmpty()) currentText.append("\n\n")
                            currentText.append(msg.text)
                        } else {
                            mergedList.add(Content(role = currentRole, parts = listOf(Part(text = currentText.toString()))))
                            currentRole = msgRole
                            currentText = java.lang.StringBuilder(msg.text)
                        }
                    }
                    if (currentText.isNotEmpty()) {
                        mergedList.add(Content(role = currentRole, parts = listOf(Part(text = currentText.toString()))))
                    }
                    mergedList
                } else {
                    listOf(Content(role = "user", parts = listOf(Part(text = trimmed))))
                }
                
                val basePrompt = """
                    Ты — ИИ-ассистент, выполняющий роль юриста-аналитика. ВНИМАНИЕ: твои ответы носят ознакомительный характер, не заменяют консультацию специалиста и не являются офертой. Избегай генерации вредного контента и нарушения законов.с 15-летним опытом в сфере защиты прав должников, банковского права, разбора кредитных историй (БКИ) и сохранения имущества.
                    Твоя цель — защитить клиента, объяснить сложную ситуацию простым языком, снять тревожность и дать четкий, пошаговый план действий.
                    
                    ВАЖНОЕ ПРАВИЛО ПАМЯТИ И ДИАГНОСТИКИ:
                    - Ты НИКОГДА не забываешь все ранее предоставленные факты, цифры и написанное в диалоге, не теряешь ход мысли.
                    - Ты постоянно проводишь глубокий анализ и диагностику работы приложения (реестра, документов) в фоновом режиме, выявляя скрытые риски или нестыковки в предоставленных клиентом данных.
                    
                    ПРАВИЛА И СТРУКТУРА ПРАВОВОГО РАЗБОРА СИТУАЦИИ (Рабочий вопрос / Анализ документа):
                    Отвечай СТРОГО по фактам, опираясь на действующее законодательство и судебную практику. Ничего не выдумывать (никаких галлюцинаций).
                    Работай как единый профессиональный механизм с приложением. Если предлагаешь составить документ, напомни, что в приложении есть генератор готовых шаблонов. Если заносишь долг в базу, скажи, что он добавлен в Реестр задолженностей.
                    Если пользователь задает сложный правовой вопрос или просит проанализировать документ (договор, судебный приказ, претензию), твой ответ ДОЛЖЕН иметь четкую профессиональную структуру:
                    1. Выводы: Краткий ответ по существу ситуации.
                    2. Аргументы: Правовая позиция с пошаговым обоснованием.
                    3. Ключевые риски: На что обратить внимание, процессуальные сроки, возможные негативные последствия.
                    4. Источники: Точные ссылки на нормы права (статьи, законы) и судебную практику, на которых основан ответ (чтобы их можно было легко проверить).
                    5. Пошаговый план действий: Четкая инструкция, что делать дальше (какие заявления писать, куда отправлять, какие функции приложения использовать).
                    
                    Обязательно ссылайся на точные статьи законов, например:
                    - ч. 7 ст. 4 ФЗ № 230-ФЗ (отзыв согласия на звонки близким);
                    - ст. 8 ФЗ № 230-ФЗ (полный отказ от взаимодействия);
                    - ст. 128, 129 ГПК РФ (отмена судебного приказа за 10 дней);
                    - ст. 333 ГК РФ (снижение неустоек и штрафов);
                    - ч. 23 ст. 5 ФЗ № 353-ФЗ (лимит переплаты по МФО не более 130%);
                    - ст. 958 ГК РФ (возврат страховки);
                    - ст. 446 ГПК РФ и ст. 213.10-1 ФЗ № 127-ФЗ (сохранение единственного и ипотечного жилья).
                    
                    ВАЖНОЕ ПРАВИЛО ИЗВЛЕЧЕНИЯ ДАННЫХ (ФОРМИРОВАНИЕ БАЗЫ КРЕДИТОВ):
                    Если пользователь загрузил текст документа (например, кредитный договор, выписку из БКИ, претензию или судебный приказ), из которого можно извлечь конкретные задолженности (кредитора, суммы), ты ОБЯЗАН провести глубокий анализ и в самом конце своего ответа добавить скрытый JSON-блок (и ничего кроме JSON внутри этих скобок), содержащий извлеченные долги. Приложение перехватит этот блок и автоматически пополнит Реестр задолженностей, не теряя при этом информации.
                    Формат JSON (строго):
                    ```json
                    {
                      "extracted_debts": [
                        {
                          "id": "генерация_уникального_id",
                          "creditorName": "Название кредитора (банка, МФО)",
                          "contractDate": "Дата договора или пусто",
                          "factAmount": 15000.0,
                          "penaltyAmount": 5000.0,
                          "insuranceAmount": 0.0,
                          "planAmount": 10000.0,
                          "status": "Активный",
                          "isMortgage": false
                        }
                      ]
                    }
                    ```
                    Рассчитай `planAmount` как `factAmount - penaltyAmount - insuranceAmount` (или применив ст. 333 ГК РФ, ч.23 ст. 5 353-ФЗ).
                    Добавляй этот блок ТОЛЬКО если в тексте реально есть данные о долгах. Если ты добавляешь этот блок, в самом текстовом ответе скажи пользователю, что данные успешно извлечены и добавлены в Реестр задолженностей.

                    В конце каждого ответа предлагай 2–3 подходящие макрокоманды (/ПЛАН_ФАКТ, /АУДИТ_ДОК, /ПРИВАТНОСТЬ, /СНИЖЕНИЕ_СТОИМОСТИ, /ИПОТЕКА_ЩИТ).
                """.trimIndent()

                val promptWithKnowledge = if (localAnswer != null) {
                    "$basePrompt\n\n[Нормативная справка из базы]: $localAnswer"
                } else {
                    basePrompt
                }
                
                var replyText = repository.sendGeneralChatMessage(contents, promptWithKnowledge)
                
                // Парсинг JSON блока с долгами
                val jsonRegex = Regex("```json\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
                val matchResult = jsonRegex.find(replyText)
                if (matchResult != null) {
                    val jsonString = matchResult.groupValues[1]
                    replyText = replyText.replace(matchResult.value, "").trim()
                    try {
                        val jsonObject = org.json.JSONObject(jsonString)
                        val extractedDebts = jsonObject.optJSONArray("extracted_debts")
                        if (extractedDebts != null) {
                            for (i in 0 until extractedDebts.length()) {
                                val debtObj = extractedDebts.getJSONObject(i)
                                val debtId = debtObj.optString("id").takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString()
                                val debt = com.example.data.Debt(
                                    id = debtId,
                                    creditorName = debtObj.optString("creditorName", "Неизвестно"),
                                    contractDate = debtObj.optString("contractDate", ""),
                                    factAmount = debtObj.optDouble("factAmount", 0.0),
                                    penaltyAmount = debtObj.optDouble("penaltyAmount", 0.0),
                                    insuranceAmount = debtObj.optDouble("insuranceAmount", 0.0),
                                    planAmount = debtObj.optDouble("planAmount", 0.0),
                                    status = debtObj.optString("status", "Активный"),
                                    isMortgage = debtObj.optBoolean("isMortgage", false)
                                )
                                debtDao.insertDebt(debt)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                _messages.value = _messages.value + ChatMessage("model", replyText)
                persistMessage("model", replyText)
                repository.saveQueryToHistory(trimmed, replyText)
            } catch (e: Exception) {
                // Если произошел сбой сети или API-ключа, выдаем экспертный ответ из правовой базы
                val fallbackAnswer = KnowledgeBase.findAnswer(trimmed)
                val responseText = if (fallbackAnswer != null) {
                    "Правовой анализ ситуации (Автономная юридическая база):\n\n" +
                    "$fallbackAnswer\n\n" +
                    "Рекомендованные шаги:\n" +
                    "1. Зафиксируйте дату требования или получения судебного акта.\n" +
                    "2. Сформируйте процессуальный документ во вкладке «Шаблоны».\n" +
                    "3. Подключите Госуслуги (Госпочту) для контроля корреспонденции.\n\n" +
                    "Рекомендованные команды:\n• /ПЛАН_ФАКТ — сравнительный аудит долга\n• /ПРИВАТНОСТЬ — защита семьи от звонков\n• /АНТИКОЛЛЕКТОР — жалоба в ФССП и отзыв согласия"
                } else {
                    "Разбор вопроса дежурным юристом:\n\n" +
                    "По закону (ст. 333 ГК РФ и 230-ФЗ) заемщик имеет полное право оспорить незаконные штрафы, завышенные пени и прекратить звонки близким людям.\n\n" +
                    "План действий:\n" +
                    "• Шаг 1: Если вынесен судебный приказ — отмените его в 10-дневный срок по ст. 128 ГПК РФ (готовый бланк есть в разделе «Шаблоны»).\n" +
                    "• Шаг 2: Направьте кредиторам отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ № 230-ФЗ).\n" +
                    "• Шаг 3: Проверьте начисления по кредиту — сумма неустоек подлежит снижению по ст. 333 ГК РФ до ставки ЦБ.\n\n" +
                    "(Примечание: Для подключения расширенных ответов онлайн-нейросети вы можете указать свой Gemini API ключ через иконку ключа вверху экрана).\n\n" +
                    "Рекомендованные команды:\n• /ПЛАН_ФАКТ\n• /АНТИКОЛЛЕКТОР\n• /СНИЖЕНИЕ_СТОИМОСТИ"
                }
                _messages.value = _messages.value + ChatMessage("model", responseText)
                persistMessage("model", responseText)
                repository.saveQueryToHistory(trimmed, responseText)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleMacroCommand(command: String): String? {
        return when (command.uppercase().trim()) {
            "/MENU", "МЕНЮ", "/ПОМОЩЬ" -> """
                Реестр доступных макрокоманд правовой защиты:
                • /ПЛАН_ФАКТ — расчет реального законного долга со срезом штрафов (ст. 333 ГК РФ)
                • /БКИ_АНАЛИЗ — аудит кредитной истории и выявление фантомных долгов
                • /ПРИВАТНОСТЬ — инструкция «Инкогнито»: перенос почты на Заказное.Почта.ру
                • /АНТИКОЛЛЕКТОР — алгоритм прекращения звонков близким и жалоб в ФССП
                • /СНИЖЕНИЕ_СТОИМОСТИ — аудит переплат по МФО (ч. 23 ст. 5 ФЗ № 353-ФЗ) и страховок
                • /ИПОТЕКА_ЩИТ — сохранение ипотечного жилья по ст. 213.10-1 ФЗ № 127-ФЗ
                • /АУДИТ_ДОК — профессиональный правовой анализ документа или ситуации с выводами, рисками и источниками
                • /ПРОЦЕСС_ДОКУМЕНТ — переход к генерации заявлений в разделе «Шаблоны»
            """.trimIndent()

            "/АНТИКОЛЛЕКТОР" -> """
                Инструкция «Антиколлектор» (ФЗ № 230-ФЗ):
                1. Звонки родственникам, друзьям и коллегам:
                   Подайте «Заявление об отзыве согласия на взаимодействие с третьими лицами» (ч. 7 ст. 4 ФЗ № 230-ФЗ). Подается в ЛЮБОЙ МОМЕНТ просрочки. После получения кредиторы не имеют права звонить никому, кроме вас.
                2. Полный отказ от взаимодействия (ст. 8 ФЗ № 230-ФЗ):
                   Подается строго через 4 месяца со дня просрочки. Запрещает любые звонки и встречи, взаимодействие только письмами.
                3. Нарушение лимитов (звонки чаще 2 раз в неделю, ночные звонки):
                   Жалоба подается в Управление ФССП вашего региона и в интернет-приемную Банка России с приложением детализации звонков и аудиозаписей.

                Рекомендованные команды:
                • /ПРИВАТНОСТЬ
                • /ПРОЦЕСС_ДОКУМЕНТ
            """.trimIndent()

            "/ПРИВАТНОСТЬ" -> """
                Протокол приватности «Инкогнито»:
                1. Исключение почтовых утечек:
                   Подключите сервис «Электронные заказные письма» (Заказное.Почта.ру) и Госпочту на Госуслугах. Письма от судов и приставов перестанут приходить в бумажном виде в почтовый ящик по месту жительства.
                2. Защита работодателя и родственников:
                   Немедленно направьте кредиторам отзыв согласия по ч. 7 ст. 4 ФЗ № 230-ФЗ.
                3. Судебный мониторинг:
                   Раз в 10 дней проверяйте картотеку мирового судебного участка по месту регистрации, чтобы не пропустить 10-дневный срок на отмену судебного приказа (ст. 128 ГПК РФ).

                Рекомендованные команды:
                • /АНТИКОЛЛЕКТОР
                • /ПЛАН_ФАКТ
            """.trimIndent()

            "/ПЛАН_ФАКТ", "/РЕЕСТР_ДОЛГОВ" -> """
                Система расчета «План / Факт»:
                • ФАКТ — суммы требований, заявляемые банками и МФО с учетом неустоек и навязанных комиссий.
                • ШТРАФЫ К ОТМЕНЕ — несоразмерные пени (ст. 333 ГК РФ), страховые премии (ст. 958 ГК РФ) и превышение 130% лимита по МФО (ч. 23 ст. 5 ФЗ № 353-ФЗ).
                • ПЛАН — законный очищенный долг, подлежащий реальной выплате или реструктуризации.

                Перейдите во вкладку «Реестр долгов», чтобы внести ваши обязательства или сформировать сводную таблицу в PDF/CSV.

                Рекомендованные команды:
                • /СНИЖЕНИЕ_СТОИМОСТИ
                • /БКИ_АНАЛИЗ
            """.trimIndent()

            "/ИПОТЕКА_ЩИТ" -> """
                Защита единственного ипотечного жилья (ст. 213.10-1 ФЗ № 127-ФЗ):
                1. С 2024 года действует механизм сохранения единственного ипотечного жилья при банкротстве гражданина.
                2. Должник имеет право заключить отдельное мировое соглашение или утвердить локальный план реструктуризации с залоговым банком.
                3. Ипотека продолжает выплачиваться в обычном графике (в том числе третьими лицами), а все остальные потребительские кредиты, микрозаймы и штрафы списываются судом.

                Рекомендованные команды:
                • /ПЛАН_ФАКТ
                • /MENU
            """.trimIndent()

            "/СНИЖЕНИЕ_СТОИМОСТИ" -> """
                Снижение стоимости и срез переплат:
                1. Снижение пени и неустоек: ст. 333 ГК РФ (заявляется при поступлении иска или в возражении).
                2. Лимит переплаты МФО: ч. 23 ст. 5 ФЗ № 353-ФЗ (проценты и штрафы не могут превышать 1.3 размера займа).
                3. Возврат страховок: ст. 958 ГК РФ (период охлаждения 30 дней либо возврат части премии при досрочном закрытии кредита).
                4. Оспаривание навязанных платных подписок и юридических консультаций: ст. 16 ЗоЗП РФ.

                Рекомендованные команды:
                • /ПЛАН_ФАКТ
                • /ПРОЦЕСС_ДОКУМЕНТ
            """.trimIndent()

            "/ПРАВО", "/СТРАТЕГИИ", "/ЗАКОНЫ" -> """
                Правовая база и 5 законных вариантов решения долговых вопросов (2026):
                
                1. Досудебная оптимизация: срез пени по ст. 333 ГК РФ, лимит 130% по ч. 23 ст. 5 ФЗ № 353-ФЗ, отзыв согласия на контакт с близкими по ч. 7 ст. 4 ФЗ № 230-ФЗ.
                2. Судебная защита: отмена судебного приказа за 10 дней (ст. 128 ГПК РФ), применение срока давности 3 года (ст. 196 ГК РФ), срез штрафов в суде до ставки ЦБ.
                3. Исполнительное производство: сохранение прожиточного минимума (ст. 69 ФЗ № 229-ФЗ), судебная рассрочка (ст. 203 ГПК РФ), иммунитет жилья (ст. 446 ГПК РФ).
                4. Внесудебное списание через МФЦ: ст. 223.2 ФЗ № 127-ФЗ (от 25 тыс. до 1 млн руб. бесплатно для пенсионеров, пособий или закрытом ИП).
                5. Защита единственной ипотеки: ст. 213.10-1 ФЗ № 127-ФЗ (мировое соглашение по квартире с сохранением жилья и списанием остальных кредитов).
                
                Вы также можете открыть интерактивный справочник в разделе «Инструменты» -> «Правовая база и 5 стратегий».

                Рекомендованные команды:
                • /ПЛАН_ФАКТ
                • /АНТИКОЛЛЕКТОР
                • /ПРИВАТНОСТЬ
            """.trimIndent()

            "/ПРОЦЕСС_ДОКУМЕНТ" -> """
                Генератор процессуальных документов:
                В приложении доступно 10 готовых официальных шаблонов:
                1. Отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ-230)
                2. Полный отказ от взаимодействия (ст. 8 ФЗ-230)
                3. Возражения на судебный приказ (ст. 128 ГПК РФ)
                4. Претензия на возврат страховки (ст. 958 ГК РФ)
                5. Оспаривание займа по безденежности (ст. 812 ГК РФ)
                6. Заявление о сохранении прожиточного минимума (ФЗ-229)
                7. Заявление о рассрочке решения суда (ст. 203 ГПК РФ)
                8. Ходатайство о сроке давности 3 года и ст. 333 ГК РФ
                9. Мировое соглашение по ипотеке (ст. 213.10-1 ФЗ-127)
                10. Заявление на кредитные каникулы и реструктуризацию
                
                Перейдите во вкладку «Инструменты» -> «Юридические шаблоны» для скачивания PDF.

                Рекомендованные команды:
                • /ПЛАН_ФАКТ
                • /ПРАВО
            """.trimIndent()

            "/БКИ_АНАЛИЗ" -> """
                Аудит кредитной истории (БКИ):
                1. Закажите бесплатный отчет через портал Госуслуги (2 раза в год в каждом БКИ бесплатно: НБКИ, ОКБ, Скоринг Бюро).
                2. Загрузите PDF-отчет во вкладке «Инструменты» -> «Анализ БКИ».
                3. Алгоритм выявит: фантомные долги, задвоенные договоры, незакрытые лимиты по картам и начисленные штрафы.
                
                Рекомендованные команды:
                • /ПЛАН_ФАКТ
                • /СНИЖЕНИЕ_СТОИМОСТИ
            """.trimIndent()

            "/АУДИТ_ДОК" -> """
                Модуль анализа правовых документов активирован.
                
                Скопируйте и отправьте текст вашего документа (договора займа, судебного приказа, постановления пристава или претензии).
                
                Нейросеть проведет глубокий анализ по строгому профессиональному стандарту и подготовит:
                1. Краткие выводы по существу.
                2. Правовые аргументы на базе законов РФ.
                3. Ключевые риски и процессуальные сроки (например, 10 дней на отмену приказа).
                4. Источники и статьи законов, чтобы вы могли лично проверить аргументацию.
                
                Ожидаю текст документа для разбора...
            """.trimIndent()

            else -> null
        }
    }
}

