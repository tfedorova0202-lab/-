package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Debt
import com.example.utils.DocumentGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalTemplatesDialog(
    debts: List<Debt>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedDebt by remember { mutableStateOf<Debt?>(debts.firstOrNull()) }
    var useBlankTemplate by remember { mutableStateOf(debts.isEmpty()) }
    var manualFillMode by remember { mutableStateOf(false) }
    var selectedTemplateIndex by remember { mutableStateOf(0) }
    
    var manualCreditorName by remember { mutableStateOf("") }
    var manualContractDate by remember { mutableStateOf("") }
    var manualFactAmount by remember { mutableStateOf("") }
    
    var debtDropdownExpanded by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }
    var lastSavedPath by remember { mutableStateOf<String?>(null) }
    var generatedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val templates = listOf(
        "Отзыв согласия (защита близких) - ФЗ-230" to "Otziv_Soglasiya.pdf",
        "Полный отказ от взаимодействия - ст. 8 ФЗ-230" to "Otkaz_Vzaimodeystvie.pdf",
        "Отмена судебного приказа - ст. 128 ГПК РФ" to "Otmena_Prikaza.pdf",
        "Возврат навязанной страховки - ст. 958 ГК РФ" to "Vozvrat_Strahovki.pdf",
        "Оспаривание по безденежности - ст. 812 ГК РФ" to "Bezdeneshnost.pdf",
        "Кредитные каникулы и реструктуризация" to "Kreditnye_Kanikuly.pdf",
        "Сохранение прожиточного минимума - ФЗ-229" to "Prozhitochniy_Minimum.pdf",
        "Рассрочка исполнения решения - ст. 203 ГПК РФ" to "Rassrochka_Suda.pdf",
        "Срок давности (3 года) и срез пени - ст. 196, 333 ГК" to "Iskovaya_Davnost_333.pdf",
        "Мировое соглашение по ипотеке - ст. 213.10-1 ФЗ-127" to "Mirovoe_Ipoteka_127FZ.pdf"
    )

    fun createPdf(): Pair<android.net.Uri?, String> {
        val debtToUse = when {
            useBlankTemplate -> null
            manualFillMode -> Debt(
                id = "manual",
                creditorName = manualCreditorName.ifBlank { "____________________________" },
                contractDate = manualContractDate.ifBlank { "«___» ________ 20__ г." },
                factAmount = manualFactAmount.toDoubleOrNull() ?: 0.0,
                penaltyAmount = 0.0,
                planAmount = manualFactAmount.toDoubleOrNull() ?: 0.0,
                status = "Вручную"
            )
            else -> selectedDebt
        }
        val fileName = "${templates[selectedTemplateIndex].second.substringBefore(".pdf")}_${debtToUse?.id ?: "Blank"}.pdf"
        val uri = when (selectedTemplateIndex) {
            0 -> DocumentGenerator.generateAntiCollectorPdf(context, debtToUse)
            1 -> DocumentGenerator.generateRefusalOfInteractionPdf(context, debtToUse)
            2 -> DocumentGenerator.generateObjectionPdf(context, debtToUse)
            3 -> DocumentGenerator.generateInsuranceReturnPdf(context, debtToUse)
            4 -> DocumentGenerator.generateLackOfFundsPdf(context, debtToUse)
            5 -> DocumentGenerator.generateRestructuringPdf(context, debtToUse)
            6 -> DocumentGenerator.generateLivingWagePreservationPdf(context, debtToUse)
            7 -> DocumentGenerator.generateInstallmentPlanPdf(context, debtToUse)
            8 -> DocumentGenerator.generateStatuteOfLimitationsPdf(context, debtToUse)
            9 -> DocumentGenerator.generateMortgageShieldSettlementPdf(context, debtToUse)
            else -> null
        }
        return Pair(uri, fileName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Юридические шаблоны") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Скачивайте готовые процессуальные заявления прямо в память телефона или сразу отправляйте в суд/банк.",
                    style = MaterialTheme.typography.bodySmall
                )

                // Выбор шаблона документа
                ExposedDropdownMenuBox(
                    expanded = templateDropdownExpanded,
                    onExpandedChange = { templateDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = templates[selectedTemplateIndex].first,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип документа") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = templateDropdownExpanded,
                        onDismissRequest = { templateDropdownExpanded = false }
                    ) {
                        templates.forEachIndexed { index, template ->
                            DropdownMenuItem(
                                text = { Text(template.first) },
                                onClick = {
                                    selectedTemplateIndex = index
                                    templateDropdownExpanded = false
                                    lastSavedPath = null
                                }
                            )
                        }
                    }
                }

                // Выбор долга или чистый бланк
                val debtFieldText = when {
                    useBlankTemplate -> "📝 Чистый образец (от руки)"
                    manualFillMode -> "✏️ Ручное заполнение (ввести сейчас)"
                    selectedDebt != null -> "🏦 ${selectedDebt?.creditorName} (${selectedDebt?.factAmount} ₽)"
                    else -> "Выберите вариант..."
                }

                ExposedDropdownMenuBox(
                    expanded = debtDropdownExpanded,
                    onExpandedChange = { debtDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = debtFieldText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Метод заполнения документа") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = debtDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = debtDropdownExpanded,
                        onDismissRequest = { debtDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("📝 Чистый образец (от руки)") },
                            onClick = {
                                useBlankTemplate = true
                                manualFillMode = false
                                selectedDebt = null
                                debtDropdownExpanded = false
                                lastSavedPath = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("✏️ Ручное заполнение (ввести сейчас)") },
                            onClick = {
                                useBlankTemplate = false
                                manualFillMode = true
                                selectedDebt = null
                                debtDropdownExpanded = false
                                lastSavedPath = null
                            }
                        )
                        debts.forEach { debt ->
                            DropdownMenuItem(
                                text = { Text("🏦 ${debt.creditorName} (${debt.factAmount} ₽)") },
                                onClick = {
                                    selectedDebt = debt
                                    useBlankTemplate = false
                                    manualFillMode = false
                                    debtDropdownExpanded = false
                                    lastSavedPath = null
                                }
                            )
                        }
                    }
                }
                
                if (manualFillMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualCreditorName,
                            onValueChange = { manualCreditorName = it },
                            label = { Text("Название кредитора (Банк/МФО)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualContractDate,
                            onValueChange = { manualContractDate = it },
                            label = { Text("Дата/Номер договора (если есть)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = manualFactAmount,
                            onValueChange = { manualFactAmount = it },
                            label = { Text("Сумма долга (₽)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (lastSavedPath != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Успешно сохранено на телефон:\n$lastSavedPath",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка скачать на телефон
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val (uri, fileName) = createPdf()
                            if (uri != null) {
                                generatedUri = uri
                                val savedLoc = DocumentGenerator.saveToDownloads(context, uri, fileName)
                                lastSavedPath = savedLoc ?: "Папка Загрузки"
                                Toast.makeText(context, "Документ сохранен: $lastSavedPath", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Ошибка при создании PDF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Скачать")
                    }
                    // Кнопка открыть/поделиться
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val (uri, _) = createPdf()
                            if (uri != null) {
                                generatedUri = uri
                                DocumentGenerator.openPdf(context, uri)
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Открыть")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val (uri, _) = createPdf()
                            if (uri != null) {
                                DocumentGenerator.sharePdf(context, uri, "Отправить документ")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Поделиться")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                }
            }
        }
    )
}

