package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.utils.UserPreferences

@Composable
fun ProfileDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit
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
