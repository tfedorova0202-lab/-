package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.Debt

import androidx.compose.material.icons.filled.Block

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtProtectionDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onGenerateObjection: () -> Unit,
    onGenerateAntiCollector: () -> Unit,
    onGenerateRefusal: () -> Unit,
    onGenerateLackOfFunds: () -> Unit,
    onGenerateInsuranceReturn: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Щит должника (Генерация документов)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ProtectionOption(
                icon = Icons.Default.Description,
                title = "Отмена судебного приказа (ст. 128 ГПК)",
                subtitle = "Отправьте, если пришло письмо из суда.",
                onClick = {
                    onGenerateObjection()
                    onDismiss()
                }
            )

            ProtectionOption(
                icon = Icons.Default.Shield,
                title = "Защита близких от коллекторов",
                subtitle = "Отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ-230).",
                onClick = {
                    onGenerateAntiCollector()
                    onDismiss()
                }
            )

            ProtectionOption(
                icon = Icons.Default.NoAccounts,
                title = "Полный отказ от общения",
                subtitle = "Доступно через 4 месяца после начала просрочки (ст. 8 ФЗ-230).",
                onClick = {
                    onGenerateRefusal()
                    onDismiss()
                }
            )

            ProtectionOption(
                icon = Icons.Default.Block,
                title = "Оспаривание по безденежности",
                subtitle = "Претензия по ст. 812 ГК РФ, если деньги фактически не поступали.",
                onClick = {
                    onGenerateLackOfFunds()
                    onDismiss()
                }
            )

            if (debt.insuranceAmount > 0) {
                ProtectionOption(
                    icon = Icons.Default.MoneyOff,
                    title = "Возврат страховки (ст. 958 ГК)",
                    subtitle = "Требование вернуть навязанную страховку на сумму ${debt.insuranceAmount} руб.",
                    onClick = {
                        onGenerateInsuranceReturn()
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun ProtectionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
