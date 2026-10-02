package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.utils.DocumentGenerator
import android.content.Intent


@Composable
fun SavingsSliderDialog(
    onDismiss: () -> Unit
) {
    var principalText by remember { mutableStateOf("") }
    var penaltyText by remember { mutableStateOf("") }
    var sliderValue by remember { mutableStateOf(100f) } // Default 100%

    val principal = principalText.toDoubleOrNull() ?: 0.0
    val penalty = penaltyText.toDoubleOrNull() ?: 0.0
    val totalDemanded = principal + penalty

    // Limit based on slider
    val limit = principal + (principal * (sliderValue / 100.0))
    
    // Savings calculation
    val savings = if (totalDemanded > limit) totalDemanded - limit else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Интерактивный калькулятор экономии") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("Используйте слайдер для изменения максимального процента накрутки и оценки потенциальной выгоды при оспаривании долга.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it },
                    label = { Text("Основной долг (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = penaltyText,
                    onValueChange = { penaltyText = it },
                    label = { Text("Штрафы и проценты (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Сценарий ограничения: ${sliderValue.toInt()}%", fontWeight = FontWeight.Bold)
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..300f,
                    steps = 29,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (savings > 0) {
                    Text(
                        "Законный предел: ${String.format("%,.2f", limit)} ₽",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Потенциальная выгода: ${String.format("%,.2f", savings)} ₽",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (principal > 0 || penalty > 0) {
                    Text(
                        "Начисления находятся в пределах выбранного лимита.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрыть")
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = DocumentGenerator.generateSavingsPdf(
                                context = context,
                                principal = principal,
                                penalty = penalty,
                                limitPercent = sliderValue.toInt(),
                                limitAmount = limit,
                                savings = savings
                            )
                            if (uri != null) {
                                val savedLoc = DocumentGenerator.saveToDownloads(context, uri, "Raschet_Ekonomii_${System.currentTimeMillis()}.pdf")
                                android.widget.Toast.makeText(context, "Файл сохранен: ${savedLoc ?: "Папка Загрузки"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = principal > 0
                    ) {
                        Text("Скачать")
                    }

                    Button(
                        onClick = {
                            val uri = DocumentGenerator.generateSavingsPdf(
                                context = context,
                                principal = principal,
                                penalty = penalty,
                                limitPercent = sliderValue.toInt(),
                                limitAmount = limit,
                                savings = savings
                            )
                            if (uri != null) {
                                DocumentGenerator.sharePdf(context, uri, "Отправить расчет")
                            }
                        },
                        enabled = principal > 0
                    ) {
                        Text("Поделиться")
                    }
                }
            }
        }
    )
}
