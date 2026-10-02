package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Debt
import com.example.utils.UserPreferences
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayoffCalculatorDialog(
    debts: List<Debt>,
    userPreferences: UserPreferences,
    onDismiss: () -> Unit
) {
    val income = userPreferences.getMonthlyIncome()
    val expenses = userPreferences.getMonthlyExpenses()
    val disposableIncome = (income - expenses).toDouble()

    var selectedStrategy by remember { mutableStateOf(Strategy.SNOWBALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("План погашения", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            if (disposableIncome <= 0) {
                Text(
                    "Ваш свободный бюджет равен нулю или отрицателен. Сначала настройте «Бюджетный план», чтобы выделить средства на погашение.",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (debts.isEmpty()) {
                Text("В реестре нет долгов.")
            } else {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    Text(
                        "Свободные средства: ${String.format("%,.2f ₽/мес", disposableIncome)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(
                            selected = selectedStrategy == Strategy.SNOWBALL,
                            onClick = { selectedStrategy = Strategy.SNOWBALL },
                            label = { Text("Снежный ком") }
                        )
                        FilterChip(
                            selected = selectedStrategy == Strategy.AVALANCHE,
                            onClick = { selectedStrategy = Strategy.AVALANCHE },
                            label = { Text("Лавина") }
                        )
                    }
                    
                    Text(
                        text = if (selectedStrategy == Strategy.SNOWBALL) "Сначала закрываем мелкие долги для быстрой мотивации." else "Сначала закрываем самые крупные долги.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider()
                    
                    val sortedDebts = if (selectedStrategy == Strategy.SNOWBALL) {
                        debts.sortedBy { it.planAmount }
                    } else {
                        debts.sortedByDescending { it.planAmount }
                    }
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        var cumulativeDebt = 0.0
                        val results = sortedDebts.map { debt ->
                            cumulativeDebt += debt.planAmount
                            val month = ceil(cumulativeDebt / disposableIncome).toInt()
                            debt to month
                        }
                        
                        items(results) { (debt, month) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(debt.creditorName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(String.format("%,.2f ₽", debt.planAmount), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "Месяц $month",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Свобода от долгов через: ${results.maxOfOrNull { it.second } ?: 0} мес.",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

enum class Strategy {
    SNOWBALL, AVALANCHE
}
