package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsCalculatorDialog(
    onDismiss: () -> Unit
) {
    var principalText by remember { mutableStateOf("") }
    var penaltyText by remember { mutableStateOf("") }
    
    val periods = listOf(
        "До 28.01.2019 (300%)" to 300f,
        "28.01.2019 - 30.06.2019 (250%)" to 250f,
        "01.07.2019 - 31.12.2019 (200%)" to 200f,
        "01.01.2020 - 30.06.2023 (150%)" to 150f,
        "С 01.07.2023 (130%)" to 130f
    )
    
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(4) }

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val penalty = penaltyText.toDoubleOrNull() ?: 0.0
    
    val interestRate = periods[selectedIndex].second

    // Max legally allowed
    val maxAllowed = principal + (principal * (interestRate / 100.0))
    val currentDemanded = principal + penalty
    val savings = if (currentDemanded > maxAllowed) currentDemanded - maxAllowed else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Авто-расчет лимитов МФО") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Узнайте законную максимальную сумму долга по ч.23 ст.5 ФЗ-353 в зависимости от года оформления займа.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = periods[selectedIndex].first,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Период оформления займа") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        periods.forEachIndexed { index, pair ->
                            DropdownMenuItem(
                                text = { Text(pair.first) },
                                onClick = {
                                    selectedIndex = index
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it },
                    label = { Text("Тело долга (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = penaltyText,
                    onValueChange = { penaltyText = it },
                    label = { Text("Требуют проценты/штрафы (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (savings > 0) {
                    Text(
                        "Законный предел долга: ${String.format("%,.2f", maxAllowed)} ₽",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Сумма к отмене: ${String.format("%,.2f", savings)} ₽",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (principal > 0 || penalty > 0) {
                    Text(
                        "Начисления в пределах лимита.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Понятно")
            }
        }
    )
}
