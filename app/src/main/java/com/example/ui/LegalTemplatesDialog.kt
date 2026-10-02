package com.example.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    var selectedTemplateIndex by remember { mutableStateOf(0) }
    
    var debtDropdownExpanded by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }

    val templates = listOf(
        "Отзыв согласия (защита близких) - ФЗ-230",
        "Полный отказ от взаимодействия - ФЗ-230",
        "Отмена судебного приказа - ст. 128 ГПК",
        "Возврат страховки - ст. 958 ГК",
        "Оспаривание по безденежности - ст. 812 ГК",
        "Запрос реструктуризации (кредитные каникулы)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Юридические шаблоны") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Выберите кредитора из реестра и тип документа, чтобы сгенерировать готовое заявление.",
                    style = MaterialTheme.typography.bodySmall
                )

                // Выбор долга
                ExposedDropdownMenuBox(
                    expanded = debtDropdownExpanded,
                    onExpandedChange = { debtDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDebt?.creditorName ?: "Выберите кредитора",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Кредитор") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = debtDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = debtDropdownExpanded,
                        onDismissRequest = { debtDropdownExpanded = false }
                    ) {
                        if (debts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет сохраненных долгов") },
                                onClick = { debtDropdownExpanded = false }
                            )
                        } else {
                            debts.forEach { debt ->
                                DropdownMenuItem(
                                    text = { Text("${debt.creditorName} (${debt.factAmount} ₽)") },
                                    onClick = {
                                        selectedDebt = debt
                                        debtDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Выбор шаблона
                ExposedDropdownMenuBox(
                    expanded = templateDropdownExpanded,
                    onExpandedChange = { templateDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = templates[selectedTemplateIndex],
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
                                text = { Text(template) },
                                onClick = {
                                    selectedTemplateIndex = index
                                    templateDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedDebt != null) {
                    Text(
                        "Документ будет заполнен данными кредитора и вашими личными данными из Профиля.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val debt = selectedDebt ?: return@Button
                    val uri = when (selectedTemplateIndex) {
                        0 -> DocumentGenerator.generateAntiCollectorPdf(context, debt)
                        1 -> DocumentGenerator.generateRefusalOfInteractionPdf(context, debt)
                        2 -> DocumentGenerator.generateObjectionPdf(context, debt)
                        3 -> DocumentGenerator.generateInsuranceReturnPdf(context, debt)
                        4 -> DocumentGenerator.generateLackOfFundsPdf(context, debt)
                        5 -> DocumentGenerator.generateRestructuringPdf(context, debt)
                        else -> null
                    }
                    
                    if (uri != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Отправить документ"))
                        onDismiss()
                    }
                },
                enabled = selectedDebt != null
            ) {
                Text("Сгенерировать PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
