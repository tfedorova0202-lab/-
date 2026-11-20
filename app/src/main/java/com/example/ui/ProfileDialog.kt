package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.UserPreferences
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.FirestoreManager
import com.example.api.LegalAssistantRepository
import com.example.data.AppDatabase
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Storefront
import android.content.Intent
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.Debt

@Composable
fun ProfileDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit,
    onLoadMockupData: () -> Unit = {},
    onClearAllDebts: () -> Unit = {},
    onFullDataReset: () -> Unit = {},
    onOpenStorePublishingClick: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(userPreferences.getName()) }
    var address by remember { mutableStateOf(userPreferences.getAddress()) }
    var phone by remember { mutableStateOf(userPreferences.getPhone()) }
    var hasExistingPin by remember { mutableStateOf(userPreferences.hasPin()) }
    var newPin by remember { mutableStateOf("") }
    var disablePinRequested by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var serverPingStatus by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var mockupStatus by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showFinancialDisclaimerDialog by remember { mutableStateOf(false) }
    var showDeleteAllDataConfirmDialog by remember { mutableStateOf(false) }

    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        LegalAssistantRepository(
            queryHistoryDao = db.queryHistoryDao(),
            messageDao = db.messageDao(),
            customApiKeyProvider = { userPreferences.getCustomGeminiApiKey() }
        )
    }

    AlertDialog(
        properties = SecureUiHelper.dialogProperties(context),
        onDismissRequest = onDismiss,
        title = { Text("Профиль и Серверы", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Блок состояния серверов
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Подключение к серверам", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = "• ИИ-сервер Gemini: ${if (repository.isServerConfigured()) "Ключ задан (прямой HTTPS-запрос)" else "Ключ не задан (офлайн-база знаний)"}\n• Транспорт: HTTPS/TLS (cleartext отключён)\n• Облачная БД: Firestore (только при входе через Google)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isPinging = true
                                        serverPingStatus = "Проверка отклика сервера..."
                                        val res = repository.pingServer()
                                        serverPingStatus = if (res.isSuccess) {
                                            "✓ " + (res.getOrNull() ?: "Сервер доступен")
                                        } else {
                                            val err = res.exceptionOrNull()?.message ?: "нет связи с generativelanguage.googleapis.com"
                                            "✕ Сервер ИИ недоступен: $err. Работает локальная база знаний."
                                        }
                                        isPinging = false
                                    }
                                },
                                enabled = !isPinging,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isPinging) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Проверить связь с сервером")
                            }
                        }
                        if (serverPingStatus != null) {
                            Text(
                                text = serverPingStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Блок макетов приложений (Showcase Mockup Mode)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Размещение в макетах приложений", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = "Автоматическая загрузка эталонных кейсов для создания скриншотов и презентационных макетов стора.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onLoadMockupData()
                                    mockupStatus = "✓ Данные для макетов загружены в реестр"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Загрузить макеты")
                            }
                            OutlinedButton(
                                onClick = {
                                    onClearAllDebts()
                                    mockupStatus = "✓ Реестр очищен"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Очистить")
                            }
                        }
                        if (mockupStatus != null) {
                            Text(
                                text = mockupStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Резервное копирование и экспорт реестра (JSON)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Резервная копия реестра (JSON)", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = "Экспортировать реестр в открытом формате JSON (без шифрования; передавайте только по доверенному каналу).",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            val allDebts = db.debtDao().getAllDebtsList()
                                            val jsonArray = JSONArray()
                                            allDebts.forEach { debt ->
                                                val obj = JSONObject().apply {
                                                    put("id", debt.id)
                                                    put("creditorName", debt.creditorName)
                                                    put("contractDate", debt.contractDate)
                                                    put("factAmount", debt.factAmount)
                                                    put("penaltyAmount", debt.penaltyAmount)
                                                    put("insuranceAmount", debt.insuranceAmount)
                                                    put("planAmount", debt.planAmount)
                                                    put("isMortgage", debt.isMortgage)
                                                    put("status", debt.status)
                                                    put("deadlineDate", debt.deadlineDate)
                                                    put("deadlineType", debt.deadlineType)
                                                }
                                                jsonArray.put(obj)
                                            }
                                            val jsonString = jsonArray.toString(2)
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Резервная копия реестра задолженностей")
                                                putExtra(Intent.EXTRA_TEXT, jsonString)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Экспорт резервной копии"))
                                            backupStatus = "✓ Реестр экспортирован (${allDebts.size} записей)"
                                        } catch (e: Exception) {
                                            backupStatus = "Ошибка экспорта: ${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Экспорт", fontSize = 12.sp)
                            }
                        }
                        if (backupStatus != null) {
                            Text(
                                text = backupStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Данные для процессуальных документов", 
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ФИО") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес регистрации/проживания") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Контактный телефон") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Приватность (Защита ПИН-кодом — хэш PBKDF2)", 
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            newPin = it
                            disablePinRequested = false
                        }
                    },
                    label = {
                        Text(
                            if (hasExistingPin && !disablePinRequested) "Новый 4-значный ПИН (установлен)"
                            else "4-значный ПИН-код (4 цифры)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                )
                if (hasExistingPin && !disablePinRequested) {
                    TextButton(
                        onClick = {
                            newPin = ""
                            disablePinRequested = true
                        }
                    ) {
                        Text("Отключить ПИН-код при сохранении", color = MaterialTheme.colorScheme.error)
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Учетная запись", 
                    style = MaterialTheme.typography.titleSmall
                )

                val authIdentifier by authViewModel.authIdentifier.collectAsState()
                val authType by authViewModel.authType.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (authIdentifier.isNotBlank()) "Авторизован: $authIdentifier" else "Режим «Инкогнито» (Офлайн-защита)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (authType.isNotBlank()) {
                            Text(
                                text = "Тип входа: ${
                                    when (authType) {
                                        "EMAIL" -> "Почта"
                                        "PHONE" -> "Телефон (SMS)"
                                        "GOOGLE" -> "Google Account"
                                        else -> "Офлайн"
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                var syncStatus by remember { mutableStateOf("") }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        scope.launch {
                            syncStatus = "Синхронизация..."
                            val firestore = FirestoreManager(context)
                            try {
                                val debts = db.debtDao().getAllDebtsList()
                                var successCount = 0
                                var lastError: String? = null
                                for (debt in debts) {
                                    val res = firestore.saveDebt(debt)
                                    if (res.isSuccess) {
                                        successCount++
                                    } else {
                                        lastError = res.exceptionOrNull()?.message
                                    }
                                }
                                syncStatus = when {
                                    debts.isEmpty() -> "Реестр пуст — синхронизация не требуется"
                                    successCount == debts.size -> "✓ Синхронизировано с Firestore: $successCount из ${debts.size}"
                                    else -> "⚠ Синхронизировано $successCount из ${debts.size}. ${lastError ?: "Войдите через Google и проверьте сеть"}"
                                }
                            } catch (e: Exception) {
                                syncStatus = "⚠ Ошибка синхронизации: ${e.message ?: "нет подключения"}"
                            }
                        }
                    }) {
                        Text("Синхронизировать")
                    }

                    Button(
                        onClick = { 
                            authViewModel.signOut() 
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Выйти")
                    }
                }
                if (syncStatus.isNotEmpty()) {
                    Text(text = syncStatus, style = MaterialTheme.typography.bodySmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Юридический статус и соответствие требованиям магазинов (Google Play / RuStore)
                Text(
                    text = "Правовой статус и соответствие сторам", 
                    style = MaterialTheme.typography.titleSmall
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Информационно-правовой статус", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = "Сервис не является МФО/банком, не выдает займы и кредиты. Все расчеты носят справочно-правовой характер.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showFinancialDisclaimerDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Дисклеймер", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { showPrivacyPolicyDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Политика 152-ФЗ", fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = onOpenStorePublishingClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Пакет для публикации (Google Play / RuStore)", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Блок полного удаления данных (Google Play User Data & Account Deletion Policy)
                Text(
                    text = "Управление персональными данными", 
                    style = MaterialTheme.typography.titleSmall
                )
                Button(
                    onClick = { showDeleteAllDataConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить все персональные данные и реестр", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                userPreferences.saveProfile(name, address, phone)
                if (disablePinRequested) {
                    userPreferences.savePin("")
                    hasExistingPin = false
                } else if (newPin.length == 4) {
                    userPreferences.savePin(newPin)
                    hasExistingPin = true
                }
                SecureUiHelper.syncActivitySecureFlag(context)
                onDismiss()
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )

    // Диалог: Финансовый дисклеймер (Google Play Financial Services / Personal Loans Policy)
    if (showFinancialDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showFinancialDisclaimerDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Юридический дисклеймер", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. ПРАВОВОЙ СТАТУС СЕРВИСА:\n" +
                                "Мобильное приложение «ЮрПомощник» является персональным правовым калькулятором, справочно-аналитическим сервисом и реестром личного учета задолженностей.\n\n" +
                                "2. НЕ ЯВЛЯЕТСЯ КРЕДИТОРОМ:\n" +
                                "Сервис НЕ ЯВЛЯЕТСЯ микрофинансовой (МФК/МКК), кредитной или банковской организацией, НЕ ВЫДАЕТ потребительские кредиты, займы, ссуды и НЕ ОКАЗЫВАЕТ посреднических услуг по выдаче займов.\n\n" +
                                "3. СПРАВОЧНО-РАСЧЕТНЫЙ ХАРАКТЕР:\n" +
                                "Все расчеты «План / Факт» и срез неустоек основаны на официальных нормах законодательства РФ (ст. 333, 809 ГК РФ, ч. 23 ст. 5 ФЗ № 353-ФЗ). Итоговые суммы снижения подлежат утверждению судом или соглашением сторон.\n\n" +
                                "4. СВЕРКА ДОКУМЕНТОВ:\n" +
                                "Сформированные заявления и процессуальные документы носят типовой характер и требуют сверки с материалами дела перед подачей в суд или ФССП.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFinancialDisclaimerDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    // Диалог: Политика конфиденциальности (Privacy Policy & 152-ФЗ РФ)
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = { Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Политика конфиденциальности (152-ФЗ)", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. ЛОКАЛЬНОЕ ХРАНЕНИЕ И ПЕСОЧНИЦА ANDROID:\n" +
                                "По умолчанию реестр долгов хранится в локальной базе SQLite/Room (debts_database) внутри изолированной песочницы приложения (App Sandbox; база не использует дополнительное шифрование SQLCipher). PIN-код хранится только в виде солёного хэша PBKDF2-SHA256. Автоматический облачный бэкап Android Auto Backup и перенос D2D отключены.\n\n" +
                                "2. ОБЛАЧНАЯ СИНХРОНИЗАЦИЯ (ОПЦИОНАЛЬНО):\n" +
                                "При входе через Google-аккаунт записи реестра долгов и базовые метаданные профиля (UID, email, displayName) синхронизируются с Google Cloud Firestore (трансграничная передача данных — требуется оценка по ст. 12 и ч. 5 ст. 18 ФЗ № 152-ФЗ). В гостевом режиме «Инкогнито» облачная синхронизация не выполняется.\n\n" +
                                "3. НЕЙРОСЕТЕВОЙ АНАЛИЗ (GEMINI API):\n" +
                                "При отправке текстовых сообщений в ИИ встроенный фильтр PiiRedactor автоматически маскирует номера паспортов, карт, СНИЛС, ИНН, телефоны и email до отправки по HTTPS/TLS. При загрузке PDF/фотографий БКИ и договоров изображения передаются целиком без графического замазывания — перед отправкой запрашивается явное согласие пользователя.\n\n" +
                                "4. НЕПЕРЕДАЧА КОЛЛЕКТОРАМ И РЕКЛАМНЫМ СЕТЯМ:\n" +
                                "Приложение не содержит рекламных SDK и трекеров, не продаёт и не передаёт данные кредиторам или коллекторским агентствам.\n\n" +
                                "5. ПОЛНОЕ УДАЛЕНИЕ ДАННЫХ И АККАУНТА (DATA DELETION):\n" +
                                "Кнопка «Удалить все персональные данные и реестр» удаляет локальную БД Room, настройки, кэш сгенерированных PDF/CSV, локальные отчёты, а при активной Google-сессии — документы в Firestore и учётную запись Firebase Auth.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Понятно")
                }
            }
        )
    }

    // Диалог: Подтверждение полного удаления всех данных (Google Play Account / Data Deletion)
    if (showDeleteAllDataConfirmDialog) {
        AlertDialog(
            properties = SecureUiHelper.dialogProperties(context),
            onDismissRequest = { showDeleteAllDataConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить все данные?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Вы действительно хотите безвозвратно удалить профиль, реестр долгов (локально с очисткой SQLite VACUUM и в Firestore), кэш PDF-документов, историю чата и учетную запись?\n\n" +
                    "Это действие необратимо и выполняется в соответствии с Политикой удаления пользовательских данных.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.deleteAccountAndCloudData { resultMessage ->
                            onFullDataReset()
                            Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                        }
                        showDeleteAllDataConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить всё")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDataConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

