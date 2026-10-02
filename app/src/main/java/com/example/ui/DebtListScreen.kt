package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Debt

import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    debts: List<Debt>,
    totalFact: Double,
    totalPlan: Double,
    totalToCancel: Double,
    onAddClick: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit,
    onProtectionClick: (Debt) -> Unit,
    onStrategiesClick: () -> Unit = {},
    onCheckMfoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Реестр задолженностей") },
                actions = {
                    IconButton(onClick = { onCheckMfoClick("") }) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Проверка МФО в ЦБ РФ")
                    }
                    IconButton(onClick = onStrategiesClick) {
                        Icon(imageVector = Icons.Default.Gavel, contentDescription = "Правовая база и стратегии")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить долг")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Итоговая сводка
            DebtSummaryDashboard(
                debts = debts,
                totalFact = totalFact,
                totalPlan = totalPlan,
                totalToCancel = totalToCancel,
                modifier = Modifier.padding(16.dp)
            )

            if (debts.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.empty_state_finance_1789342102585),
                            contentDescription = "Пусто",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Реестр чист.\nНажмите '+' чтобы добавить кредитора.", 
                            style = MaterialTheme.typography.titleMedium, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).testTag("debt_list")
                ) {
                    itemsIndexed(debts, key = { _, debt -> debt.id }) { index, debt ->
                        DebtItemCard(
                            index = index + 1,
                            debt = debt,
                            onEdit = { onEditClick(debt) },
                            onDelete = { onDeleteClick(debt) },
                            onProtection = { onProtectionClick(debt) },
                            onCheckMfo = { onCheckMfoClick(debt.creditorName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebtItemCard(
    index: Int,
    debt: Debt,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onProtection: () -> Unit,
    onCheckMfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().testTag("debt_item_${debt.id}"),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "№$index | ${debt.creditorName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(onClick = onCheckMfo, modifier = Modifier.testTag("check_cbr_button_${debt.id}")) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Проверить в реестре ЦБ РФ", tint = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = onProtection, modifier = Modifier.testTag("protection_button_${debt.id}")) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = "Щит должника (Документы)", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_button_${debt.id}")) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Редактировать запись")
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_button_${debt.id}")) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить запись", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Text(text = "Договор: ${debt.contractDate}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Статус: ${debt.status}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Данные расчетов
            if (debt.deadlineDate.isNotEmpty() && debt.deadlineType.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("СРОК: ${debt.deadlineDate} (${debt.deadlineType})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ФАКТ (Требуют)", style = MaterialTheme.typography.labelMedium)
                    Text(String.format("%.2f ₽", debt.factAmount), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("К отмене", style = MaterialTheme.typography.labelMedium)
                    Text(String.format("%.2f ₽", debt.penaltyAmount + debt.insuranceAmount), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("ПЛАН (Законный)", style = MaterialTheme.typography.labelMedium)
                    Text(String.format("%.2f ₽", debt.planAmount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Правовое основание
            val bases = mutableListOf<String>()
            if (debt.isMortgage) bases.add("ст. 213.10-1 ФЗ № 127-ФЗ")
            if (debt.penaltyAmount > 0) bases.add("ст. 333 ГК РФ")
            if (debt.insuranceAmount > 0) bases.add("ст. 958 ГК РФ")
            val legalBasis = if (bases.isNotEmpty()) bases.joinToString(", ") else "ст. 810 ГК РФ"
            
            Text(
                text = "Основание: $legalBasis", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DebtSummaryDashboard(
    debts: List<Debt>,
    totalFact: Double,
    totalPlan: Double,
    totalToCancel: Double,
    modifier: Modifier = Modifier
) {
    val mortgageFact = debts.filter { it.isMortgage }.sumOf { it.factAmount }
    val nonMortgageFact = totalFact - mortgageFact
    val progress = if (totalFact > 0) (totalPlan / totalFact).toFloat() else 0f

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("СВОДНАЯ АНАЛИТИКА (ПЛАН / ФАКТ)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Требуют (Факт)", style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%,.2f ₽", totalFact), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("К списанию", style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%,.2f ₽", totalToCancel), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Законный (План)", style = MaterialTheme.typography.bodySmall)
                    Text(String.format("%,.2f ₽", totalPlan), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar representing Plan vs Fact
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
            
            val totalFact = debts.sumOf { it.factAmount }
            val mortgageFact = debts.filter { it.isMortgage }.sumOf { it.factAmount }
            val nonMortgageFact = totalFact - mortgageFact
            val total = if (totalFact > 0) totalFact else 1.0

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Структура долга (ФАКТ):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Круговая диаграмма (Pie Chart)
                androidx.compose.foundation.Canvas(modifier = Modifier.size(64.dp)) {
                    val mortgageSweep = (mortgageFact / total * 360).toFloat()
                    val otherSweep = (nonMortgageFact / total * 360).toFloat()
                    
                    drawArc(
                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                        startAngle = -90f,
                        sweepAngle = mortgageSweep,
                        useCenter = true
                    )
                    drawArc(
                        color = androidx.compose.ui.graphics.Color(0xFFF44336),
                        startAngle = -90f + mortgageSweep,
                        sweepAngle = otherSweep,
                        useCenter = true
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) { drawCircle(androidx.compose.ui.graphics.Color(0xFF4CAF50)) }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ипотека:", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(String.format("%,.2f ₽", mortgageFact), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) { drawCircle(androidx.compose.ui.graphics.Color(0xFFF44336)) }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Остальное:", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(String.format("%,.2f ₽", nonMortgageFact), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
