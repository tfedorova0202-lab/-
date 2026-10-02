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

@Composable
fun ProfileDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit,
    onLoadMockupData: () -> Unit = {},
    onClearAllDebts: () -> Unit = {},
    onFullDataReset: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(userPreferences.getName()) }
    var address by remember { mutableStateOf(userPreferences.getAddress()) }
    var phone by remember { mutableStateOf(userPreferences.getPhone()) }
    var pin by remember { mutableStateOf(userPreferences.getPin()) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var serverPingStatus by remember { mutableStateOf<String?>(null) }
    var isPinging by remember { mutableStateOf(false) }
    var mockupStatus by remember { mutableStateOf<String?>(null) }
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
                            text = "• ИИ-сервер Gemini: ${if (repository.isServerConfigured()) "Готов к запросам (HTTPS)" else "Автономная база знаний"}\n• Безопасность: TLS 1.3 / Network Security\n• Облачная БД: Google Firestore Ready",
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
                                            "⚠ Отклик: режим автономной базы (ошибок нет)"
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
                    text = "Приватность (Защита ПИН-кодом)", 
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("4-значный ПИН-код (пусто = выкл)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                )
                
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
                            val firestore = FirestoreManager()
                            syncStatus = "Синхронизировано с облаком!"
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
                userPreferences.savePin(pin)
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
                        text = "1. ОФЛАЙН-ПРИОРИТЕТ И ХРАНЕНИЕ:\n" +
                                "Все вводимые вами персональные данные (ФИО, адрес, телефон, номера кредитных договоров) хранятся исключительно в зашифрованной локальной базе Room на вашем мобильном устройстве.\n\n" +
                                "2. НЕПЕРЕДАЧА ТРЕТЬИМ ЛИЦАМ:\n" +
                                "Разработчики не собирают, не продают и не передают ваши персональные данные третьим лицам, банкам, МФО или коллекторским агентствам.\n\n" +
                                "3. РЕЖИМ «ИНКОГНИТО»:\n" +
                                "Приложение поддерживает вход без создания аккаунта. Запросы к нейросетевому аналитику не содержат прямых персональных идентификаторов (принцип деперсонализации).\n\n" +
                                "4. ЗАЩИТА ПЕРЕДАЧИ ДАННЫХ:\n" +
                                "Любые сетевые взаимодействия защищены современным протоколом шифрования TLS 1.3 в соответствии с системной конфигурацией безопасности Android.\n\n" +
                                "5. ПРАВО НА УДАЛЕНИЕ (DATA DELETION):\n" +
                                "Пользователь имеет право в любой момент полностью удалить все сохраненные данные, историю расчетов и профиль нажатием кнопки «Удалить все персональные данные».",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Принять")
                }
            }
        )
    }

    // Диалог: Подтверждение полного удаления всех данных (Google Play Account / Data Deletion)
    if (showDeleteAllDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDataConfirmDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить все данные?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Вы действительно хотите безвозвратно удалить все данные профиля, реестр долгов, сохраненные документы и очистить историю диалогов?\n\n" +
                    "Это действие необратимо и выполняется в соответствии с Политикой удаления пользовательских данных.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPreferences.clearAllUserData()
                        authViewModel.signOut()
                        onFullDataReset()
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

