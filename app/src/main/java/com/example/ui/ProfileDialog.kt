package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.utils.UserPreferences

import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.FirestoreManager
import kotlinx.coroutines.launch

@Composable
fun ProfileDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf(userPreferences.getName()) }
    var address by remember { mutableStateOf(userPreferences.getAddress()) }
    var phone by remember { mutableStateOf(userPreferences.getPhone()) }
    var pin by remember { mutableStateOf(userPreferences.getPin()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Профиль и Настройки", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Эти данные будут автоматически подставляться в документы.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Приватность (App Lock)", 
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Установите 4-значный ПИН-код для защиты приложения от посторонних. Оставьте пустым для отключения.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("ПИН-код") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Учетная запись", 
                    style = MaterialTheme.typography.titleMedium
                )

                val authIdentifier by authViewModel.authIdentifier.collectAsState()
                val authType by authViewModel.authType.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (authIdentifier.isNotBlank()) "Авторизован: $authIdentifier" else "Режим «Инкогнито» (Офлайн)",
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

                val scope = rememberCoroutineScope()
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
                            syncStatus = "Синхронизировано!"
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
                Text("Отмена")
            }
        }
    )
}
