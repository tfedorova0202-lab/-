package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Debt

@Composable
fun AddDebtDialog(
    initialDebt: Debt?,
    onDismiss: () -> Unit,
    onSave: (id: String?, creditorName: String, contractDate: String, factAmount: Double, penaltyAmount: Double, insuranceAmount: Double, isMortgage: Boolean, deadlineDate: String, deadlineType: String) -> Unit
) {
    var creditorName by remember { mutableStateOf(initialDebt?.creditorName ?: "") }
    var contractDate by remember { mutableStateOf(initialDebt?.contractDate ?: "") }
    var factAmount by remember { mutableStateOf(if (initialDebt != null) initialDebt.factAmount.toString() else "") }
    var penaltyAmount by remember { mutableStateOf(if (initialDebt != null) initialDebt.penaltyAmount.toString() else "") }
    var insuranceAmount by remember { mutableStateOf(if (initialDebt != null) initialDebt.insuranceAmount.toString() else "") }
    var isMortgage by remember { mutableStateOf(initialDebt?.isMortgage ?: false) }
    var deadlineDate by remember { mutableStateOf(initialDebt?.deadlineDate ?: "") }
    var deadlineType by remember { mutableStateOf(initialDebt?.deadlineType ?: "") }

    // Состояния ошибок
    var creditorError by remember { mutableStateOf(false) }
    var contractDateError by remember { mutableStateOf(false) }
    var factAmountError by remember { mutableStateOf(false) }
    var penaltyAmountError by remember { mutableStateOf(false) }
    var insuranceAmountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDebt == null) "Добавить кредитора" else "Редактировать запись", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = creditorName,
                    onValueChange = { 
                        creditorName = it
                        creditorError = false
                    },
                    label = { Text("Кредитор (Банк/МФО)") },
                    isError = creditorError,
                    supportingText = if (creditorError) { { Text("Поле не может быть пустым") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contractDate,
                    onValueChange = { 
                        contractDate = it
                        contractDateError = false
                    },
                    label = { Text("Номер и дата договора") },
                    isError = contractDateError,
                    supportingText = if (contractDateError) { { Text("Поле не может быть пустым") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = factAmount,
                    onValueChange = { 
                        factAmount = it
                        factAmountError = false
                    },
                    label = { Text("ФАКТ (Сколько требуют, руб.)") },
                    isError = factAmountError,
                    supportingText = if (factAmountError) { { Text("Введите корректную сумму") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = penaltyAmount,
                    onValueChange = { 
                        penaltyAmount = it
                        penaltyAmountError = false
                    },
                    label = { Text("Пени к отмене (руб.)") },
                    isError = penaltyAmountError,
                    supportingText = if (penaltyAmountError) { { Text("Введите корректную сумму (можно 0)") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = insuranceAmount,
                    onValueChange = { 
                        insuranceAmount = it
                        insuranceAmountError = false
                    },
                    label = { Text("Страховка к возврату (руб.)") },
                    isError = insuranceAmountError,
                    supportingText = if (insuranceAmountError) { { Text("Введите корректную сумму (можно 0)") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Критический срок (Юридический календарь)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = deadlineDate,
                    onValueChange = { deadlineDate = it },
                    label = { Text("Дата (ДД.ММ.ГГГГ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = deadlineType,
                    onValueChange = { deadlineType = it },
                    label = { Text("Тип (например: Отмена приказа)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = isMortgage, onCheckedChange = { isMortgage = it })
                    Text("Единственное жилье (Ипотека)")
                }
                Text(
                    text = "Законная справка: Введенные пени будут отменены (ст. 333 ГК РФ). Страховка возвращена (ст. 958 ГК РФ). Ипотека будет защищена.", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val fact = factAmount.toDoubleOrNull()
                val penalty = penaltyAmount.toDoubleOrNull()
                val insurance = insuranceAmount.toDoubleOrNull()
                
                val isCreditorValid = creditorName.isNotBlank()
                val isContractValid = contractDate.isNotBlank()
                val isFactValid = fact != null && fact >= 0.0
                val isPenaltyValid = penalty != null && penalty >= 0.0
                val isInsuranceValid = insurance != null && insurance >= 0.0
                
                if (isCreditorValid && isContractValid && isFactValid && isPenaltyValid && isInsuranceValid) {
                    onSave(initialDebt?.id, creditorName, contractDate, fact!!, penalty!!, insurance!!, isMortgage, deadlineDate, deadlineType)
                } else {
                    creditorError = !isCreditorValid
                    contractDateError = !isContractValid
                    factAmountError = !isFactValid
                    penaltyAmountError = !isPenaltyValid
                    insuranceAmountError = !isInsuranceValid
                }
            }) {
                Text("Сохранить в реестр")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
