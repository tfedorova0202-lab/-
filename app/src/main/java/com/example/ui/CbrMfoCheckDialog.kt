package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CbrMfoRegistry
import com.example.data.MfoCheckResult
import com.example.data.MfoStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CbrMfoCheckDialog(
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onAddDebtFromMfo: (name: String) -> Unit = {}
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf(initialQuery) }
    var checkResult by remember { mutableStateOf<MfoCheckResult?>(
        if (initialQuery.isNotBlank()) CbrMfoRegistry.checkLegality(initialQuery) else null
    ) }
    val scrollState = rememberScrollState()

    val popularMfos = listOf(
        "Займер", "еКапуста", "MoneyMan", "Быстроденьги", 
        "Webbankir", "БыстроКредит Онлайн", "Деньги Всем"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Реестр ЦБ РФ",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Проверка МФО в ЦБ РФ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Государственный реестр Банка России", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Введите бренд, название, ИНН, ОГРН или домен МФО. Проверка выполняется в строгом соответствии с ФЗ № 151-ФЗ и ФЗ № 353-ФЗ на момент запроса.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Поле ввода
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    label = { Text("Название МФО, ИНН или сайт") },
                    placeholder = { Text("Например: Займер или 4205271785") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (queryText.isNotBlank()) {
                            IconButton(onClick = { queryText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Очистить")
                            }
                        }
                    }
                )

                // Быстрые подсказки
                Text("Популярные запросы:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularMfos) { mfo ->
                        SuggestionChip(
                            onClick = {
                                queryText = mfo
                                checkResult = CbrMfoRegistry.checkLegality(mfo)
                            },
                            label = { Text(mfo, fontSize = 12.sp) }
                        )
                    }
                }

                // Кнопка запуска проверки
                Button(
                    onClick = {
                        if (queryText.isNotBlank()) {
                            checkResult = CbrMfoRegistry.checkLegality(queryText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = queryText.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Проверить статус в ЦБ РФ")
                }

                // Результаты проверки
                checkResult?.let { result ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val (containerColor, contentColor, icon) = when (result.status) {
                        MfoStatus.LEGAL -> Triple(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            Icons.Default.CheckCircle
                        )
                        MfoStatus.EXCLUDED -> Triple(
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                            Icons.Default.Warning
                        )
                        MfoStatus.ILLEGAL -> Triple(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            Icons.Default.GppBad
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = result.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor
                                )
                            }

                            // Время проверки
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Дата и время запроса: ${result.formattedCheckTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = result.statusExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor
                            )

                            result.entry?.let { entry ->
                                HorizontalDivider(color = contentColor.copy(alpha = 0.3f))
                                Text(
                                    text = "Реквизиты из госреестра ЦБ РФ:\n" +
                                            "• Тип: ${entry.mfoType} | Рег. номер: ${entry.regNumber}\n" +
                                            "• ИНН: ${entry.inn} | ОГРН: ${entry.ogrn}\n" +
                                            "• Дата внесения: ${entry.entryDate}" +
                                            (if (entry.exclusionDate != null) "\n• Дата исключения: ${entry.exclusionDate}" else "") +
                                            "\n• Официальный сайт: ${entry.officialDomains.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor
                                )
                            }
                        }
                    }

                    // Нормы закона и лимиты
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Правовые гарантии (ФЗ № 353-ФЗ):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "• Предельная ставка: ${result.maxDailyRate}\n• Лимит переплаты: ${result.maxOverpaymentLimit}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Нормативная база: ${result.legalBasis}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Рекомендации правового защитника
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Рекомендации адвоката:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        result.recommendations.forEach { rec ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(rec, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Кнопки взаимодействия
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.complaintUrl))
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Приемная ЦБ", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val nameToAdd = result.entry?.brandName ?: result.query
                                onAddDebtFromMfo(nameToAdd)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("В реестр долгов", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
