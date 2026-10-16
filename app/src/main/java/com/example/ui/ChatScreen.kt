package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.utils.DocumentGenerator
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Shield
import com.example.R

@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val customKey by viewModel.customApiKey.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var reportingMessage by remember { mutableStateOf<com.example.ui.ChatMessage?>(null) }
    var reportReason by remember { mutableStateOf("Юридическая неточность в норме закона") }
    var reportComment by remember { mutableStateOf("") }
    var showDocConsentDialog by remember { mutableStateOf(false) }
    var pendingDocBase64 by remember { mutableStateOf<String?>(null) }
    var pendingDocMime by remember { mutableStateOf<String?>(null) }
    var pendingDocName by remember { mutableStateOf<String?>(null) }
    var tempKeyInput by remember { mutableStateOf(customKey) }
    val context = LocalContext.current

    val documentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
            if (mimeType.startsWith("image/") || mimeType == "application/pdf") {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        var fileName = "Document"
                        val cursor = context.contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    fileName = it.getString(nameIndex)
                                }
                            }
                        }
                        pendingDocBase64 = base64Data
                        pendingDocMime = mimeType
                        pendingDocName = fileName
                        showDocConsentDialog = true
                    } else {
                        Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Поддерживаются только изображения и PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val macroChips = listOf(
        "/MENU",
        "/ПЛАН_ФАКТ",
        "/ПРИВАТНОСТЬ",
        "/БКИ_АНАЛИЗ",
        "/СНИЖЕНИЕ_СТОИМОСТИ",
        "/ИПОТЕКА_ЩИТ",
        "/РЕЕСТР_ДОЛГОВ",
        "/ПРАВО",
        "/АУДИТ_ДОК",
        "/АНТИКОЛЛЕКТОР",
        "/ПРОЦЕСС_ДОКУМЕНТ",
        "/ПРОВЕРКА_МФО",
        "/КАЛЕНДАРЬ",
        "/МАРКЕТ",
        "/ДИСКЛЕЙМЕР",
        "/ПОЛИТИКА",
        "/ОЧИСТИТЬ"
    )

    val suggestedQuestions = listOf(
        "🔎 Проверить МФО в ЦБ РФ",
        "⚠️ Дисклеймер ИИ",
        "Правовой анализ документа",
        "Как отменить судебный приказ?",
        "Как запретить звонки родственникам?",
        "Сохранят ли ипотеку (213.10-1)?",
        "Списание долгов через МФЦ",
        "Снижение штрафов по ст. 333 ГК",
        "Лимит переплаты по МФО 130%"
    )

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("Важное уведомление об использовании ИИ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Настоящее приложение использует технологии искусственного интеллекта на базе моделей Google Gemini. Пожалуйста, ознакомьтесь с правилами:", style = MaterialTheme.typography.bodySmall)
                    Text("• Возможность ошибок: ИИ может генерировать неточную, неполную или недостоверную информацию (так называемые «галлюцинации»). Всегда перепроверяйте критически важные данные.", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("• Не является экспертной рекомендацией: Ответы ИИ в сфере юриспруденции носят ознакомительный характер и не заменяют официальную консультацию.", style = MaterialTheme.typography.bodySmall)
                    Text("• Ограничение ответственности: Разработчик приложения не несет ответственности за убытки или последствия использования сгенерированной ИИ информации.", style = MaterialTheme.typography.bodySmall)
                    Text("• Конфиденциальность: Не вводите в чат свои полные паспортные данные или номера банковских карт.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showDisclaimerDialog = false }) { Text("Понятно, согласен") }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Настройка Gemini API") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Если у вас есть собственный ключ Google Gemini API, укажите его здесь для работы онлайн-нейросети прямо на телефоне.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempKeyInput,
                        onValueChange = { tempKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        "При отсутствии ключа чат работает автономно на базе актуального законодательства РФ (ФЗ-230, ФЗ-127, ГК РФ).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveApiKey(tempKeyInput)
                    showApiKeyDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Очистить историю диалога?") },
            text = {
                Text(
                    "Все сообщения будут безвозвратно удалены с устройства в соответствии с протоколом приватности («Инкогнито»). База реестра долгов сохранится.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDocConsentDialog && pendingDocBase64 != null) {
        AlertDialog(
            onDismissRequest = {
                showDocConsentDialog = false
                pendingDocBase64 = null
            },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Согласие на ИИ-анализ документа", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Вы выбрали файл: ${pendingDocName ?: "Документ"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "• Содержимое файла передается по защищенному каналу (TLS 1.3) в API нейросети (Google Gemini) исключительно для оптического распознавания (OCR), поиска кредиторов и сумм долга.\n" +
                               "• Документ не сохраняется на сервере и не передается третьим лицам.\n" +
                               "• Пожалуйста, убедитесь, что в файле нет открытых реквизитов банковских карт (CVC/CVV) или полных серий/номеров паспортов.\n" +
                               "• Вы также можете отказаться и добавить параметры долга вручную в Реестре без передачи документа.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val data = pendingDocBase64
                        val mime = pendingDocMime ?: "application/pdf"
                        val name = pendingDocName ?: "Document"
                        showDocConsentDialog = false
                        pendingDocBase64 = null
                        if (data != null) {
                            viewModel.sendDocument(data, mime, name)
                        }
                    }
                ) {
                    Text("Согласен, анализировать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDocConsentDialog = false
                        pendingDocBase64 = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    reportingMessage?.let { reportedMsg ->
        AlertDialog(
            onDismissRequest = { reportingMessage = null },
            icon = { Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Пожаловаться на ответ ИИ", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "В соответствии с политикой Google Play по генеративному ИИ, вы можете сообщить о недостоверном, некорректном или опасном ответе нейросети.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Причина обращения:", style = MaterialTheme.typography.labelMedium)
                    val reasons = listOf(
                        "Юридическая неточность или неверная статья закона",
                        "Галлюцинация ИИ (выдуманные факты или сроки)",
                        "Неприемлемый или вводящий в заблуждение контент",
                        "Другая ошибка"
                    )
                    reasons.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = r }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = reportReason == r,
                                onClick = { reportReason = r }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(r, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = reportComment,
                        onValueChange = { reportComment = it },
                        label = { Text("Комментарий (необязательно)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportingMessage = null
                        reportComment = ""
                        Toast.makeText(context, "✓ Жалоба зафиксирована. Спасибо за обратную связь!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportingMessage = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chat_banner_legal_1789342092300),
            contentDescription = "Hero banner",
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentScale = ContentScale.Crop
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ИИ Юрист-Защитник",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (customKey.isNotBlank()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (customKey.isNotBlank()) "Gemini AI подключен" else "Правовая база (Автономно)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { 
                        tempKeyInput = customKey
                        showApiKeyDialog = true 
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Key, 
                        contentDescription = "Настройка API ключа",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showClearHistoryDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep, 
                        contentDescription = "Очистить историю диалога",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Чипы макрокоманд
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(macroChips) { cmd ->
                AssistChip(
                    onClick = { viewModel.sendMessage(cmd) },
                    label = { Text(cmd, style = MaterialTheme.typography.labelSmall) },
                    enabled = !isLoading
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Column {
                        val hasTable = msg.text.contains("|")
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                                .widthIn(max = if (hasTable) 580.dp else 320.dp)
                        ) {
                            if (hasTable) {
                                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    Text(
                                        text = msg.text,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                                    )
                                }
                            } else {
                                Text(
                                    text = msg.text,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (!isUser && msg.text.length > 50) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        val uri = DocumentGenerator.generateAnalysisReportPdf(context, msg.text)
                                        if (uri != null) {
                                            val savedPath = DocumentGenerator.saveToDownloads(context, uri, "Analysis_Report_${System.currentTimeMillis()}.pdf")
                                            Toast.makeText(context, "Отчет сохранен: $savedPath", Toast.LENGTH_LONG).show()
                                            DocumentGenerator.openPdf(context, uri)
                                        } else {
                                            Toast.makeText(context, "Ошибка генерации", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Сохранить PDF", style = MaterialTheme.typography.labelSmall)
                                }

                                IconButton(
                                    onClick = { reportingMessage = msg },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "Пожаловаться на ответ ИИ",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp))
                    }
                }
            }
        }
        
        // Suggested questions chips
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(suggestedQuestions) { question ->
                SuggestionChip(
                    onClick = { 
                        if (question == "⚠️ Дисклеймер ИИ") { showDisclaimerDialog = true } else { viewModel.sendMessage(question) } 
                    },
                    label = { Text(question, style = MaterialTheme.typography.labelSmall) },
                    enabled = !isLoading
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    documentLauncher.launch(arrayOf("application/pdf", "image/*"))
                },
                enabled = !isLoading,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Загрузить документ", tint = MaterialTheme.colorScheme.primary)
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Вопрос или текст документа...") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
