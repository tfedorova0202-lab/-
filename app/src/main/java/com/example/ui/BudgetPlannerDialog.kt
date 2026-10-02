package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.utils.UserPreferences

@Composable
fun BudgetPlannerDialog(
    userPreferences: UserPreferences,
    onDismiss: () -> Unit
) {
    var income by remember { mutableStateOf(if (userPreferences.getMonthlyIncome() > 0) userPreferences.getMonthlyIncome().toString() else "") }
    var expenses by remember { mutableStateOf(if (userPreferences.getMonthlyExpenses() > 0) userPreferences.getMonthlyExpenses().toString() else "") }

    val incomeValue = income.toFloatOrNull() ?: 0f
    val expensesValue = expenses.toFloatOrNull() ?: 0f
    val disposableIncome = incomeValue - expensesValue

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Бюджетный план", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Рассчитайте свободные средства, доступные для комфортного погашения долгов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = income,
                    onValueChange = { income = it },
                    label = { Text("Ежемесячный доход (₽)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = expenses,
                    onValueChange = { expenses = it },
                    label = { Text("Обязательные расходы (₽)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (disposableIncome > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Свободные средства (к погашению):",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (disposableIncome > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = String.format("%,.2f ₽", disposableIncome),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (disposableIncome > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        if (disposableIncome <= 0) {
                            Text(
                                text = "Внимание: расходы превышают доходы. Рекомендуется реструктуризация долгов.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                userPreferences.saveBudget(incomeValue, expensesValue)
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
