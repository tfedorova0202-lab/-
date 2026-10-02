package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Debt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class LegalDeadlineItem(
    val debt: Debt,
    val title: String,
    val deadlineDateStr: String,
    val targetDate: Date?,
    val daysLeft: Long?,
    val legalArticle: String,
    val urgentLevel: UrgentLevel,
    val recommendation: String
)

enum class UrgentLevel {
    EXPIRED,   // Просрочен
    CRITICAL,  // 1-3 дня
    WARNING,   // 4-10 дней
    NORMAL     // >10 дней или плановый
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalCalendarDialog(
    debts: List<Debt>,
    onDismiss: () -> Unit,
    onOpenTemplates: () -> Unit,
    onDebtSelected: (Debt) -> Unit
) {
    val context = LocalContext.current
    val items = remember(debts) { calculateDeadlines(debts) }
    var selectedFilter by remember { mutableStateOf<UrgentLevel?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Юридический процессуальный календарь",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Контроль сроков 10 дней, периода охлаждения и давности",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alert summary banner
                val criticalCount = items.count { it.urgentLevel == UrgentLevel.CRITICAL || it.urgentLevel == UrgentLevel.EXPIRED }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (criticalCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (criticalCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (criticalCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (criticalCount > 0) "Внимание: $criticalCount критических сроков!" else "Процессуальные сроки под контролем",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (criticalCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Отмена приказа (10 дней по ст. 128 ГПК), возврат страховки (30 дней по ФЗ-353) и исковая давность (3 года по ст. 196 ГК).",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (criticalCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Все (${items.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == UrgentLevel.CRITICAL,
                        onClick = { selectedFilter = if (selectedFilter == UrgentLevel.CRITICAL) null else UrgentLevel.CRITICAL },
                        label = { Text("Срочные", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == UrgentLevel.WARNING,
                        onClick = { selectedFilter = if (selectedFilter == UrgentLevel.WARNING) null else UrgentLevel.WARNING },
                        label = { Text("Внимание", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List
                val filteredItems = items.filter { item ->
                    selectedFilter == null || item.urgentLevel == selectedFilter
                }

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Нет активных сроков с жесткими датами.\nПри добавлении долга вы можете указать точную дату получения приказа или договора.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems) { deadline ->
                            LegalDeadlineCard(
                                item = deadline,
                                onOpenTemplates = onOpenTemplates,
                                onCardClick = { onDebtSelected(deadline.debt) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenTemplates,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сформировать документ", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Понятно", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun LegalDeadlineCard(
    item: LegalDeadlineItem,
    onOpenTemplates: () -> Unit,
    onCardClick: () -> Unit
) {
    val (badgeBg, badgeText, statusLabel) = when (item.urgentLevel) {
        UrgentLevel.EXPIRED -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.error,
            "Срок истек! Нужен повод для восстановления"
        )
        UrgentLevel.CRITICAL -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.error,
            "Критический срок! Осталось: ${item.daysLeft} дн."
        )
        UrgentLevel.WARNING -> Triple(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.tertiary,
            "Внимание: осталось ${item.daysLeft} дн."
        )
        UrgentLevel.NORMAL -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary,
            if (item.daysLeft != null) "Осталось: ${item.daysLeft} дн." else "Бессрочно / В любой момент"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.debt.creditorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${item.title} • ${item.deadlineDateStr}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Основание: ${item.legalArticle}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = item.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onOpenTemplates,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Подать заявление", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun calculateDeadlines(debts: List<Debt>): List<LegalDeadlineItem> {
    val results = mutableListOf<LegalDeadlineItem>()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val today = Calendar.getInstance().time

    for (debt in debts) {
        // 1. Прямой дедлайн, если пользователь задал deadlineDate
        if (debt.deadlineDate.isNotBlank()) {
            try {
                val targetDate = dateFormat.parse(debt.deadlineDate.trim())
                if (targetDate != null) {
                    val diffDays = ((targetDate.time - today.time) / (1000 * 60 * 60 * 24))
                    val level = when {
                        diffDays < 0 -> UrgentLevel.EXPIRED
                        diffDays <= 3 -> UrgentLevel.CRITICAL
                        diffDays <= 10 -> UrgentLevel.WARNING
                        else -> UrgentLevel.NORMAL
                    }
                    val typeName = debt.deadlineType.ifBlank { "Процессуальный срок" }
                    val article = if (typeName.contains("приказ", ignoreCase = true)) "ст. 128 ГПК РФ" else "ст. 11 ФЗ № 353-ФЗ"
                    results.add(
                        LegalDeadlineItem(
                            debt = debt,
                            title = typeName,
                            deadlineDateStr = debt.deadlineDate,
                            targetDate = targetDate,
                            daysLeft = diffDays,
                            legalArticle = article,
                            urgentLevel = level,
                            recommendation = if (diffDays < 0) {
                                "Срок пропущен. Подайте ходатайство о восстановлении пропущенного срока по уважительной причине (болезнь, неполучение письма, смена адреса)."
                            } else {
                                "Подайте возражение или заявление мировому судье/банку до указанной даты. Шаблон готов в разделе «Шаблоны»."
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }

        // 2. Период охлаждения по страховкам (30 дней по ч. 12 ст. 11 ФЗ-353)
        if (debt.insuranceAmount > 0 && debt.contractDate.isNotBlank()) {
            val parsedDate = parseContractDate(debt.contractDate)
            if (parsedDate != null) {
                val coolingCal = Calendar.getInstance().apply {
                    time = parsedDate
                    add(Calendar.DAY_OF_MONTH, 30)
                }
                val coolingDate = coolingCal.time
                val diffDays = ((coolingDate.time - today.time) / (1000 * 60 * 60 * 24))
                val level = when {
                    diffDays < 0 -> UrgentLevel.NORMAL // После 30 дней можно вернуть при досрочном погашении по ст. 958 ГК
                    diffDays <= 3 -> UrgentLevel.CRITICAL
                    else -> UrgentLevel.WARNING
                }
                results.add(
                    LegalDeadlineItem(
                        debt = debt,
                        title = "Возврат страховки (${debt.insuranceAmount.toInt()} ₽)",
                        deadlineDateStr = dateFormat.format(coolingDate),
                        targetDate = coolingDate,
                        daysLeft = diffDays,
                        legalArticle = "ч. 12 ст. 11 ФЗ № 353-ФЗ, ст. 958 ГК РФ",
                        urgentLevel = level,
                        recommendation = if (diffDays >= 0) {
                            "Действует 30-дневный период охлаждения! Вы имеете законное право вернуть 100% стоимости навязанной страховки."
                        } else {
                            "Период охлаждения истек. Возврат возможен пропорционально при досрочном погашении кредита (ст. 958 ГК РФ)."
                        }
                    )
                )
            }
        }

        // 3. Отзыв согласия на звонки третьим лицам (ч. 7 ст. 4 ФЗ-230)
        results.add(
            LegalDeadlineItem(
                debt = debt,
                title = "Защита семьи от звонков (Инкогнито)",
                deadlineDateStr = "В любой момент",
                targetDate = null,
                daysLeft = null,
                legalArticle = "ч. 7 ст. 4 ФЗ № 230-ФЗ",
                urgentLevel = UrgentLevel.NORMAL,
                recommendation = "Подается в любой момент без ожидания просрочки. Запрещает банку и коллекторам звонить супругу, родителям и коллегам."
            )
        )
    }

    // Sort: critical first, then warning, then normal
    return results.sortedWith(compareBy({ it.urgentLevel.ordinal }, { it.daysLeft ?: Long.MAX_VALUE }))
}

private fun parseContractDate(text: String): Date? {
    val formats = listOf("dd.MM.yyyy", "yyyy-MM-dd", "dd/MM/yyyy")
    val regex = Regex("\\b\\d{2}\\.\\d{2}\\.\\d{4}\\b")
    val match = regex.find(text)
    val dateStr = match?.value ?: text.trim()
    for (f in formats) {
        try {
            return SimpleDateFormat(f, Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            // continue
        }
    }
    return null
}
