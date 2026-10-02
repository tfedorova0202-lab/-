package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Debt
import com.example.utils.DocumentGenerator

import androidx.compose.material.icons.automirrored.filled.Chat

import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    REGISTRY("Реестр", Icons.AutoMirrored.Filled.List),
    ANALYTICS("Аналитика", Icons.Default.Analytics),
    TOOLS("Инструменты", Icons.Default.Build),
    CHAT("Помощник", Icons.AutoMirrored.Filled.Chat),
    PROFILE("Профиль", Icons.Default.Person)
}

@Composable
fun ToolsTab(
    onBkiClick: () -> Unit,
    onScanClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onStrategiesClick: () -> Unit = {},
    onCheckMfoClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onPublishingKitClick: () -> Unit = {},
    debts: List<Debt>
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Инструменты", style = MaterialTheme.typography.titleLarge)

        Card(
            onClick = onCalendarClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = "Юридический процессуальный календарь",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Юридический календарь сроков", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        "Контроль 10 дней на отмену приказа (ст. 128 ГПК), 30 дней периода охлаждения и исковой давности.", 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Card(
            onClick = onCheckMfoClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Проверка МФО в ЦБ РФ",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Проверка МФО в ЦБ РФ", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        "Проверка легальности в реестре Банка России на момент запроса (ФЗ-151, ФЗ-353). Защита от черных кредиторов.", 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Card(
            onClick = onStrategiesClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Правовая база и 5 стратегий", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Text("Актуальные нормы законов 2026 года, срез штрафов, отмена приказов и сохранение жилья.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(onClick = onBkiClick, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Анализ БКИ", style = MaterialTheme.typography.titleMedium)
                    Text("Загрузить PDF-отчет для автоматического разбора.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Скан договора (OCR)", style = MaterialTheme.typography.titleMedium)
                    Text("Сфотографировать договор для распознавания суммы и дат.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(onClick = onTemplatesClick, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Юридические шаблоны", style = MaterialTheme.typography.titleMedium)
                    Text("Генерация Возражений, Отказов от взаимодействия и Претензий.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(
            onClick = {
                val uri = DocumentGenerator.generateRegistryPdf(context, debts)
                if (uri != null) {
                    val savedPath = DocumentGenerator.saveToDownloads(context, uri, "Reestr_Dolgov_${System.currentTimeMillis()}.pdf")
                    android.widget.Toast.makeText(context, "Файл сохранен: ${savedPath ?: "Загрузки"}", android.widget.Toast.LENGTH_LONG).show()
                    DocumentGenerator.sharePdf(context, uri, "Отправить Реестр (PDF)")
                }
            }, 
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Экспорт Реестра (PDF)", style = MaterialTheme.typography.titleMedium)
                    Text("Скачать таблицу План/Факт на телефон и отправить.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(
            onClick = {
                val uri = DocumentGenerator.generateRegistryCsv(context, debts)
                if (uri != null) {
                    val savedPath = DocumentGenerator.saveToDownloads(context, uri, "Reestr_Dolgov_${System.currentTimeMillis()}.csv")
                    android.widget.Toast.makeText(context, "Файл сохранен: ${savedPath ?: "Загрузки"}", android.widget.Toast.LENGTH_LONG).show()
                    DocumentGenerator.sharePdf(context, uri, "Отправить Реестр (CSV)")
                }
            }, 
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Экспорт Реестра (CSV / Excel)", style = MaterialTheme.typography.titleMedium)
                    Text("Скачать таблицу в формате Excel на телефон.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(
            onClick = onPublishingKitClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = "Публикация в маркетах",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Публикация в маркетах приложений", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        "Пакет для Google Play, RuStore: карточка ASO, Data Safety, Политика 152-ФЗ и релизный чеклист.", 
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


@Composable
fun AnalyticsTab(
    onBudgetClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onSavingsClick: () -> Unit,
    onSavingsSliderClick: () -> Unit,
    debts: List<Debt>
) {
    val scrollState = rememberScrollState()
    val totalFact = debts.sumOf { it.factAmount }
    val totalPlan = debts.sumOf { it.planAmount }
    val totalToCancel = debts.sumOf { it.penaltyAmount + it.insuranceAmount }
    val mortgageCount = debts.count { it.isMortgage }
    val totalSavedPct = if (totalFact > 0) ((totalToCancel / totalFact) * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Image(
            painter = painterResource(id = R.drawable.analytics_banner_1788906027857),
            contentDescription = "Analytics Banner",
            modifier = Modifier.fillMaxWidth().height(140.dp),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Финансово-правовая аналитика", style = MaterialTheme.typography.titleLarge)

            // Сводная карточка эффективности
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сводка правового среза долга", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Всего требований:", style = MaterialTheme.typography.bodySmall)
                            Text(String.format("%,.0f ₽", totalFact), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Column {
                            Text("Потенциал списания:", style = MaterialTheme.typography.bodySmall)
                            Text(String.format("%,.0f ₽ (-%d%%)", totalToCancel, totalSavedPct), color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Column {
                            Text("Законный долг:", style = MaterialTheme.typography.bodySmall)
                            Text(String.format("%,.0f ₽", totalPlan), color = MaterialTheme.colorScheme.tertiary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                    if (mortgageCount > 0) {
                        Text(
                            text = "🛡 Под защитой ст. 213.10-1 ФЗ-127: $mortgageCount ипотечный договор (единственное жилье неприкосновенно).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            Card(onClick = onSavingsClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Проверка переплат (МФО)", style = MaterialTheme.typography.titleMedium)
                    Text("Калькулятор списания незаконных начислений по ФЗ-353.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(onClick = onSavingsSliderClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Интерактивная экономия", style = MaterialTheme.typography.titleMedium)
                    Text("Слайдер процентных ставок для расчета выгоды.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(onClick = onBudgetClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Бюджетный план", style = MaterialTheme.typography.titleMedium)
                    Text("Рассчитать свободные средства к погашению.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Card(onClick = onCalculatorClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Калькулятор погашения", style = MaterialTheme.typography.titleMedium)
                    Text("Снежный ком или Лавина? Узнайте срок освобождения.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
}
