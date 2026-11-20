package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorePublishingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabTitles = listOf("Карточка стора", "Политика 152-ФЗ", "Data Safety", "Чек-лист релиза")

    // Метаданные стора
    val appTitle = "ЮрПомощник: защита от долгов"
    val shortDescription = "Правовая защита заемщиков, расчет списания штрафов и отмена судебных приказов."
    val keywordsAso = "долги, кредиты, мфо, юрист, судебный приказ, отмена пени, ст 333 гк рф, коллекторы, защита заемщика, реестр долгов, фссп, банкротство, возврат страховки, цб рф, 230-фз"
    val fullDescription = """
«ЮрПомощник» — мобильный правовой сервис для защиты прав заемщиков, анализа задолженностей и снижения долговой нагрузки в строгом соответствии с законами РФ.

⚖️ ОСНОВНЫЕ ВОЗМОЖНОСТИ:
1. РЕЕСТР ДОЛГОВ И РАСЧЕТ «ПЛАН / ФАКТ»
• Сравнение суммы требований кредитора (ФАКТ) с обоснованным долгом (ПЛАН).
• Снижение неустоек, пени и штрафов на основании ст. 333 ГК РФ.
• Проверка предельных лимитов переплаты по займам МФО до 1 года (ч. 23 ст. 5 ФЗ № 353-ФЗ: 100% с 01.04.2026 по ФЗ № 545-ФЗ / 130% для договоров 01.07.2023–31.03.2026).
• Возврат навязанных страховок и допуслуг (ч. 2.7–2.9 ст. 7 ФЗ № 353-ФЗ — период охлаждения 30 дней; ст. 958 ГК РФ, ч. 10–12 ст. 11 ФЗ № 353-ФЗ при досрочном погашении).

2. РЕЖИМ «ИНКОГНИТО» И ЗАЩИТА СЕМЬИ (Приоритет №1)
• Защита родственников и супругов: отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ № 230-ФЗ).
• Полный отказ от взаимодействия с кредитором и коллекторами при просрочке свыше 4 месяцев (ст. 8 ФЗ № 230-ФЗ).
• Инструкция по переходу на цифровую корреспонденцию (Заказное.Почта.ру) без бумажных писем на домашний адрес.

3. ИПОТЕЧНЫЙ ЩИТ (СОХРАНЕНИЕ ЕДИНСТВЕННОГО ЖИЛЬЯ)
• Защита ипотечной квартиры через отдельное мировое соглашение (ст. 213.10-1 ФЗ № 127-ФЗ, с согласия залогодержателя и утверждением судом).
• Учет имущественного иммунитета на единственное жилье по ст. 446 ГПК РФ.

4. ЮРИДИЧЕСКИЙ КАЛЕНДАРЬ ПРОЦЕССУАЛЬНЫХ СРОКОВ
• Контроль 10 календарных дней на отмену судебного приказа (ст. 128, 129 ГПК РФ).
• Контроль 30-дневного периода охлаждения по страховкам и допуслугам (ч. 2.7–2.9 ст. 7 ФЗ № 353-ФЗ).
• Расчет 3-летнего срока исковой давности по графику платежей (ст. 196, 199, 200 ГК РФ).

5. ГЕНЕРАТОР ПРОЦЕССУАЛЬНЫХ ДОКУМЕНТОВ
• Проекты возражений на судебный приказ мирового судьи (без признания долга).
• Заявления об отзыве согласия на взаимодействие с третьими лицами.
• Претензии о возврате страховых премий и заявления на кредитные каникулы (ст. 6.1-2 ФЗ № 353-ФЗ).
• Экспорт реестра в форматах PDF и Excel (CSV).

6. СПРАВОЧНИК МФО (СРЕЗ РЕЕСТРА ЦБ РФ)
• Быстрая офлайн-проверка крупных МФО со ссылкой для официальной сверки на cbr.ru/finorg/.

🔒 БЕЗОПАСНОСТЬ И КОНФИДЕНЦИАЛЬНОСТЬ (152-ФЗ РФ / GOOGLE PLAY DATA SAFETY):
• По умолчанию данные хранятся локально в песочнице Android (App Sandbox, SQLite/Room без дополнительного SQLCipher-шифрования) и исключены из Android Auto Backup / Device Transfer (allowBackup=false).
• Опциональный вход через Google Sign-In и облачная синхронизация в Firebase Cloud Firestore включаются только пользователем.
• При обращении к ИИ (Google Gemini API) передача идет по HTTPS/TLS; в текстовых сообщениях автоматически маскируются паспорт, карты, СНИЛС, ИНН, телефон и e-mail (прикрепляемые PDF/фото передаются в исходном виде).
• Вход защищен PIN-кодом (хэш PBKDF2-SHA256) и биометрией; включена защита экрана от скриншотов (FLAG_SECURE) в режиме «Инкогнито».
• Полное удаление локальных и облачных данных (включая Firebase Auth и Firestore) доступно в профиле.

⚠️ ПРАВОВОЙ ДИСКЛЕЙМЕР:
Приложение «ЮрПомощник» носит справочно-информационный характер и не заменяет очную консультацию адвоката. Приложение НЕ является банком, МФО или кредитным брокером, НЕ выдает займы и кредиты.
    """.trimIndent()

    val privacyPolicyText = """
ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ И ОБРАБОТКИ ДАННЫХ (ФЗ № 152-ФЗ / DATA SAFETY)

1. Общие положения
Настоящая Политика описывает фактический порядок обработки данных в мобильном приложении «ЮрПомощник».

2. Локальное хранилище и защита на устройстве
• Реестр долгов и история диалогов хранятся локально в базе SQLite/Room (`debts_database`) в изолированной песочнице приложения (Android App Sandbox) без дополнительного шифрования SQLCipher.
• Профиль (ФИО, адрес, телефон), бюджет и настройки хранятся в `user_prefs.xml`. PIN-код хранится исключительно в виде солёного криптографического хэша PBKDF2-SHA256.
• База данных, настройки и журнал жалоб на ИИ исключены из системного резервного копирования Android Auto Backup и переноса на другое устройство (`allowBackup=false`).

3. Опциональная облачная синхронизация (Google / Firebase)
• Только при добровольной авторизации через Google Sign-In (Firebase Authentication) данные реестра долгов и юридического календаря синхронизируются с Google Cloud Firestore (`users/{uid}/debts`, `users/{uid}/legal_actions`).
• Внимание (152-ФЗ): использование Google Firebase и Google Gemini API влечет трансграничную передачу данных на серверы Google за пределами РФ (ст. 12 ФЗ № 152-ФЗ). В гостевом офлайн-режиме синхронизация не выполняется.

4. Сервисы искусственного интеллекта (Google Gemini API)
• При отправке сообщения в ИИ-чат или загрузке документа данные передаются по HTTPS/TLS в Google Gemini API.
• Перед отправкой текстовых запросов встроенный фильтр (`PiiRedactor`) маскирует паспортные данные, номера банковских карт, СНИЛС, ИНН, телефоны и e-mail.
• Внимание: бинарные вложения (PDF-выписки БКИ и фотографии) передаются в Gemini в исходном виде — предварительно скрывайте чувствительные реквизиты перед загрузкой.

5. Непередача данных рекламным сетям и взыскателям
Данные не продаются и не передаются рекламным SDK, кредиторам, БКИ или коллекторским агентствам.

6. Контроль генеративного ИИ (AI Safety)
Пользователь может зафиксировать жалобу на некорректный ответ ИИ: маскированная запись сохраняется в локальный журнал `ai_safety_reports.jsonl` и может быть экспортирована/отправлена разработчику.

7. Полное удаление данных (User Data Deletion)
В разделе «Профиль» -> «Удалить все данные» выполняется удаление: локальной БД Room, `user_prefs.xml`, временных PDF/CSV в `cacheDir`, локального журнала жалоб на ИИ, а при активной авторизации — также всех документов пользователя в Cloud Firestore и учетной записи Firebase Auth.
    """.trimIndent()

    val dataSafetyText = """
ДЕКЛАРАЦИЯ БЕЗОПАСНОСТИ ДАННЫХ (GOOGLE PLAY DATA SAFETY — ФАКТИЧЕСКАЯ)

1. Собираемые данные (Data Collected — в зависимости от режима):
• Личная информация (Email / Google UID) — опционально, только при входе через Google Sign-In (цель: управление аккаунтом и синхронизация).
• Финансовая информация (записи реестра долгов и календарь) — хранится локально; при включенном Google Sign-In синхронизируется с Cloud Firestore (цель: функциональность приложения).
• Файлы и сообщения пользователя (промпты, PDF БКИ, фото документов) — передаются в Google Gemini API только по явному действию пользователя для анализа (эпизодическая обработка, на нашем сервере не сохраняются).
• Передача третьим лицам для рекламы / брокерам (Data Shared): НЕТ (0 рекламных SDK).

2. Методы обеспечения безопасности (Security Practices):
• Шифрование при передаче: HTTPS/TLS для Firebase и Gemini API (cleartext traffic запрещен).
• Исключение из бэкапов: `allowBackup=false`, явное исключение `debts_database*`, `user_prefs.xml`, `ai_safety_reports.jsonl`.
• Аутентификация входа: солёный хэш PIN (PBKDF2-SHA256), биометрия, `FLAG_SECURE` в режиме «Инкогнито».
• Удаление аккаунта и данных: кнопка в профиле удаляет локальные данные, документы Firestore и учетную запись Firebase Auth. (Требуется также веб-страница для внешнего запроса удаления аккаунта согласно Google Play Account Deletion Policy).

3. Соответствие Financial Services Policy Google Play:
• Приложение не является кредитором («Not a lending app»), не выдает займы и не является кредитным брокером.

4. Соответствие Generative AI Policy Google Play:
• Присутствует дисклеймер о справочном характере ответов ИИ.
• Реализовано реальное сохранение и экспорт жалоб на ответы ИИ (`AiReportStore`).
    """.trimIndent()

    // Интерактивные пункты чеклиста (честные дефолтные состояния до финализации релиза)
    var checkAppId by remember { mutableStateOf(false) }
    var checkTitleLen by remember { mutableStateOf(true) }
    var checkTargetSdk by remember { mutableStateOf(true) }
    var checkMonochromeIcon by remember { mutableStateOf(true) }
    var checkPrivacyPolicy by remember { mutableStateOf(false) }
    var checkDataSafety by remember { mutableStateOf(false) }
    var checkFinancialDisclaimer by remember { mutableStateOf(true) }
    var checkScreenshots by remember { mutableStateOf(false) }
    var checkAabBundle by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "✓ $label скопирован в буфер", Toast.LENGTH_SHORT).show()
    }

    fun shareFullKit() {
        val fullKit = """
=== ПАКЕТ ДЛЯ ПУБЛИКАЦИИ ПРИЛОЖЕНИЯ «ЮРПОМОЩНИК» В СТОРАХ ===

[1. МЕТАДАННЫЕ]
Название: $appTitle
Краткое описание (до 80 знаков): $shortDescription
Ключевые слова (ASO): $keywordsAso
Категории: Финансы / Инструменты / Правовые сервисы
Возрастной рейтинг: 12+

[2. ПОЛНОЕ ОПИСАНИЕ]
$fullDescription

[3. ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ (152-ФЗ)]
$privacyPolicyText

[4. ДЕКЛАРАЦИЯ DATA SAFETY]
$dataSafetyText
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Пакет публикации: ЮрПомощник (Google Play / RuStore)")
            putExtra(Intent.EXTRA_TEXT, fullKit)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться пакетом публикации"))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок диалога
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Публикация в маркетах",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Google Play • RuStore • AppGallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 12.dp
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontSize = 13.sp) }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            // Карточка стора (ASO)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Название
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Название приложения (${appTitle.length}/30 симв.)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(appTitle, "Название") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = appTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "✓ Строго до 30 символов, без эмодзи и кричащих слов (соответствует Google Play Policy)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Краткое описание
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Краткое описание (${shortDescription.length}/80 симв.)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(shortDescription, "Краткое описание") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = shortDescription,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                // Теги и категории
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Категории и ASO-ключи",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(keywordsAso, "Ключевые слова") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = "Категория: Финансы (Finance) / Инструменты\nВозраст: 12+\nКлючи: $keywordsAso",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                // Полное описание
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Полное описание приложения",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(fullDescription, "Полное описание") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = fullDescription,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Политика 152-ФЗ и условия
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Обязательное требование сторов",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Google Play и RuStore требуют указания ссылки на Политику конфиденциальности при публикации.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Текст Политики (152-ФЗ)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(privacyPolicyText, "Политика конфиденциальности") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = privacyPolicyText,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Data Safety
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Форма Data Safety для консоли",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Ответы для анкеты безопасности Google Play Console.",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Декларация Data Safety",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { copyToClipboard(dataSafetyText, "Декларация Data Safety") },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Копировать", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            text = dataSafetyText,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            // Чек-лист релиза
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Интерактивный чек-лист готовности к релизу:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                val items = listOf(
                                    Triple(checkAppId, { v: Boolean -> checkAppId = v }, "Свой постоянный ApplicationId (заменить тестовый com.aistudio.* перед публикацией)"),
                                    Triple(checkTitleLen, { v: Boolean -> checkTitleLen = v }, "Название приложения до 30 знаков (без капса и эмодзи)"),
                                    Triple(checkTargetSdk, { v: Boolean -> checkTargetSdk = v }, "Target SDK 36 (соответствует требованиям 2026 года)"),
                                    Triple(checkMonochromeIcon, { v: Boolean -> checkMonochromeIcon = v }, "Адаптивная иконка + Monochrome (Android 13+ Themed Icons)"),
                                    Triple(checkPrivacyPolicy, { v: Boolean -> checkPrivacyPolicy = v }, "Публичный URL Политики конфиденциальности + веб-форма удаления аккаунта (152-ФЗ / Play Policy)"),
                                    Triple(checkFinancialDisclaimer, { v: Boolean -> checkFinancialDisclaimer = v }, "Финансовый дисклеймер (Google Play Financial Services Policy)"),
                                    Triple(checkDataSafety, { v: Boolean -> checkDataSafety = v }, "Заполнена анкета Data Safety (с раскрытием опциональных Google Sign-In, Firestore и Gemini API)"),
                                    Triple(checkScreenshots, { v: Boolean -> checkScreenshots = v }, "Скриншоты для маркета (1080x1920 / 1080x2400)"),
                                    Triple(checkAabBundle, { v: Boolean -> checkAabBundle = v }, "Подписанный release AAB своим upload-ключом + серверный прокси для Gemini без ключа в APK")
                                )

                                items.forEach { (checked, onCheckedChange, label) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = onCheckedChange
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Нижняя панель действий
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shareFullKit() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Экспорт пакета", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Готово", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
