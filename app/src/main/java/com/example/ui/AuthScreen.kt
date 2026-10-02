package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isGuest by viewModel.isGuest.collectAsStateWithLifecycle()
    val isCustomAuth by viewModel.isCustomAuthenticated.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Email, 1: Phone, 2: Quick/Google
    
    // Email fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }

    // Phone fields
    var phone by remember { mutableStateOf("+7") }
    var smsCode by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var policyAccepted by remember { mutableStateOf(false) }
    val verificationCode by viewModel.verificationCode.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser, isCustomAuth, isGuest) {
        if (currentUser != null || isCustomAuth || isGuest) {
            onAuthSuccess()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Юридическая безопасность",
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Юридический Помощник",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Защита от коллекторов, аудит кредитов и расчет законного долга",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Почта") },
                    icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Телефон") },
                    icon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Инкогнито") },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = policyAccepted,
                    onCheckedChange = { policyAccepted = it }
                )
                Text(
                    text = "Я принимаю Политику конфиденциальности и правила информационной безопасности (ИБ)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // Вход по любой электронной почте
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Любая электронная почта (Email)") },
                                placeholder = { Text("например: lawyer@yandex.ru") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Пароль (от 6 символов)") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.signInWithEmail(email, password, isRegisterMode)
                                },
                                enabled = policyAccepted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(if (isRegisterMode) "Зарегистрироваться" else "Войти по почте")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                                    Text(if (isRegisterMode) "Уже есть аккаунт? Войти" else "Создать новый аккаунт")
                                }

                                TextButton(
                                    onClick = {
                                        val fallbackEmail = if (email.isNotBlank()) email else "user@offline.app"
                                        val fallbackPass = if (password.length >= 6) password else "123456"
                                        viewModel.signInWithEmail(fallbackEmail, fallbackPass, false)
                                    },
                                    enabled = policyAccepted
                                ) {
                                    Text("Быстрый вход")
                                }
                            }
                        }
                    }
                    1 -> {
                        // Вход по номеру телефона
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Номер мобильного телефона") },
                                placeholder = { Text("+7 999 000-00-00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!isCodeSent) {
                                Button(
                                    onClick = {
                                        if (phone.trim().length >= 10) {
                                            val sent = viewModel.sendVerificationSmsCode(phone)
                                            if (sent) {
                                                isCodeSent = true
                                                smsCode = "" // Оставляем поле пустым для ручного ввода
                                            }
                                        }
                                    },
                                    enabled = policyAccepted && phone.trim().length >= 10,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Получить проверочный SMS-код")
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "SMS с кодом отправлено на $phone",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (verificationCode != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Код подтверждения: $verificationCode",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = smsCode,
                                    onValueChange = { 
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            smsCode = it
                                        }
                                    },
                                    label = { Text("Код из SMS (4 цифры)") },
                                    placeholder = { Text("••••") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        viewModel.signInWithPhone(phone, smsCode)
                                    },
                                    enabled = policyAccepted && smsCode.length == 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Подтвердить и войти")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { 
                                            isCodeSent = false
                                            smsCode = ""
                                            viewModel.resetVerification()
                                        }
                                    ) {
                                        Text("Изменить номер")
                                    }

                                    TextButton(
                                        onClick = {
                                            smsCode = ""
                                            viewModel.sendVerificationSmsCode(phone)
                                        }
                                    ) {
                                        Text("Отправить код повторно")
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Быстрый вход и Инкогнито
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.signInWithGoogle(context) },
                                enabled = policyAccepted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Войти через Google")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.continueAsGuest()
                                    onContinueAsGuest()
                                },
                                enabled = policyAccepted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Режим «Инкогнито» (Офлайн)")
                            }

                            Text(
                                "В режиме «Инкогнито» все ваши данные, договоры и расчеты сохраняются исключительно в защищенной памяти вашего устройства и не передаются в интернет.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

