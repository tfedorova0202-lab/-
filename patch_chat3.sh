sed -i '/if (showApiKeyDialog) {/i\
    if (showDisclaimerDialog) {\
        AlertDialog(\
            onDismissRequest = { showDisclaimerDialog = false },\
            title = { Text("Важное уведомление об использовании ИИ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error) },\
            text = {\
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\
                    Text("Настоящее приложение использует технологии искусственного интеллекта на базе моделей Google Gemini. Пожалуйста, ознакомьтесь с правилами:", style = MaterialTheme.typography.bodySmall)\
                    Text("• Возможность ошибок: ИИ может генерировать неточную, неполную или недостоверную информацию (так называемые «галлюцинации»). Всегда перепроверяйте критически важные данные.", style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)\
                    Text("• Не является экспертной рекомендацией: Ответы ИИ в сфере юриспруденции носят ознакомительный характер и не заменяют официальную консультацию.", style = MaterialTheme.typography.bodySmall)\
                    Text("• Ограничение ответственности: Разработчик приложения не несет ответственности за убытки или последствия использования сгенерированной ИИ информации.", style = MaterialTheme.typography.bodySmall)\
                    Text("• Конфиденциальность: Не вводите в чат свои полные паспортные данные или номера банковских карт.", style = MaterialTheme.typography.bodySmall)\
                }\
            },\
            confirmButton = {\
                Button(onClick = { showDisclaimerDialog = false }) { Text("Понятно, согласен") }\
            }\
        )\
    }\
' app/src/main/java/com/example/ui/ChatScreen.kt
