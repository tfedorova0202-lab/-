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
• Проверка предельных лимитов переплаты по займам МФО (ч. 23 ст. 5 ФЗ № 353-ФЗ: не более 130%).
• Возврат навязанных страховок и платных услуг (ст. 958 ГК РФ, период охлаждения 30 дней).

2. РЕЖИМ «ИНКОГНИТО» И ЗАЩИТА СЕМЬИ (Приоритет №1)
• Защита родственников и супругов: отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ № 230-ФЗ).
• Полный отказ от взаимодействия с кредитором и коллекторами при просрочке свыше 4 месяцев (ст. 8 ФЗ № 230-ФЗ).
• Инструкция по переходу на цифровую корреспонденцию (Заказное.Почта.ру) без бумажных писем на домашний адрес.

3. ИПОТЕЧНЫЙ ЩИТ (СОХРАНЕНИЕ ЕДИНСТВЕННОГО ЖИЛЬЯ)
• Защита ипотечной квартиры через отдельное мировое соглашение (ст. 213.10-1 ФЗ № 127-ФЗ).
• Применение имущественного иммунитета на единственное жилье по ст. 446 ГПК РФ.

4. ЮРИДИЧЕСКИЙ КАЛЕНДАРЬ ПРОЦЕССУАЛЬНЫХ СРОКОВ
• Контроль 10 дней на отмену судебного приказа (ст. 128 ГПК РФ).
• Контроль 30-дневного периода охлаждения по договорам страхования.
• Расчет 3-летнего срока исковой давности по графику платежей (ст. 196, 200 ГК РФ).

5. ГЕНЕРАТОР ПРОЦЕССУАЛЬНЫХ ДОКУМЕНТОВ
• Автоматическое составление возражений на судебный приказ мирового судьи.
• Заявления об отзыве согласия на взаимодействие с третьими лицами.
• Претензии о возврате страховых премий при досрочном погашении кредита.
• Экспорт реестра в форматах PDF и Excel (CSV).

6. ПРОВЕРКА МФО ПО РЕЕСТРУ ЦБ РФ
• Моментальный аудит легальности микрофинансовой организации в реестре Банка России (ФЗ-151, ФЗ-353).

🔒 БЕЗОПАСНОСТЬ И КОНФИДЕНЦИАЛЬНОСТЬ (152-ФЗ РФ / GDPR):
• Локальное хранение в изолированной песочнице Android (App Sandbox) в базе SQLite / Room.
• База долгов и личные настройки аппаратно исключены из автоматического облачного бэкапа (Android Auto Backup).
• В режиме «Инкогнито» доступна офлайн-работа без сетевых запросов.
• При использовании ИИ (Google Gemini) для разбора документов или консультаций передача осуществляется по TLS 1.3 с предварительным явным согласием пользователя.
• Никакие персональные данные не передаются коллекторам, кредиторам или рекламным сетям.
• Защита входа 4-значным PIN-кодом и мгновенное стирание всех данных в 1 клик.

⚠️ ПРАВОВОЙ ДИСКЛЕЙМЕР:
Приложение «ЮрПомощник» носит справочно-информационный характер. Приложение НЕ является банком, микрофинансовой организацией или кредитным брокером, НЕ выдает займы и кредиты и НЕ оказывает платных финансовых услуг.
    """.trimIndent()

    val privacyPolicyText = """
ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ И ОБРАБОТКИ ДАННЫХ (ФЗ № 152-ФЗ / GDPR)

1. Общие положения
Настоящая Политика определяет порядок обработки и защиты информации пользователей мобильного приложения «ЮрПомощник».
Сервис придерживается принципа «Privacy by Design» и режима «Инкогнито»: защита личной и финансовой тайны заемщика является высшим приоритетом.

2. Состав и архитектура хранения данных
• Локальное хранилище: Все финансовые записи (названия кредиторов, даты договоров, суммы долга, пени и штрафов) по умолчанию сохраняются исключительно на вашем устройстве в изолированной песочнице приложения (Android App Sandbox).
• Исключение из бэкапов: База данных SQLite/Room и пользовательские настройки исключены из системного облачного резервного копирования Android Auto Backup.
• Сервисы искусственного интеллекта: При обращении к интеллектуальному помощнику или загрузке документов для распознавания (OCR) текст или изображения передаются по защищенному каналу (TLS 1.3) в API нейросети (Google Gemini) только после явного согласия пользователя. Сервис не сохраняет документы и не использует их для обучения моделей.
• Локальная альтернатива: Пользователь вправе использовать встроенный сканер текста с камеры (ML Kit на устройстве) и автономную юридическую базу без обращения к внешним облачным сервисам.

3. Цели обработки информации
Информация используется исключительно для:
• Автоматического расчета правового среза задолженности (ст. 333 ГК РФ, ч. 23 ст. 5 ФЗ № 353-ФЗ);
• Формирования проектов процессуальных заявлений для мировых судов, ФССП и банков;
• Контроля процессуальных сроков по ст. 128 ГПК РФ и периодов охлаждения;
• Проверки легальности МФО по открытому государственному реестру Банка России.

4. Непередача данных третьим лицам
Данные пользователя НЕ продаются и НЕ передаются коллекторским агентствам, банкам, микрофинансовым организациям или рекламным сетям.

5. Защита от генеративного контента (AI Safety)
В соответствии с требованиями Google Play к приложениям с генеративным ИИ, пользователи имеют возможность в один клик пожаловаться на любой ответ нейросети при обнаружении юридической неточности или галлюцинации.

6. Хранение и удаление данных (User Data Deletion)
Пользователь имеет право в любой момент полностью удалить все сохраненные записи реестра, историю диалогов и персональные настройки через кнопку «Удалить все данные» в профиле. Данные стираются безвозвратно.
    """.trimIndent()

    val dataSafetyText = """
ДЕКЛАРАЦИЯ БЕЗОПАСНОСТИ ДАННЫХ (GOOGLE PLAY DATA SAFETY)

1. Сбор и передача данных (Data Collection & Sharing):
• Финансовая информация: Суммы задолженностей и расчетов сохраняются локально на устройстве (Local on-device storage).
• Документы и файлы: Передаются в API нейросети (Google Gemini) только при явном действии пользователя (выбор файла для ИИ-анализа) по зашифрованному протоколу TLS 1.3. Временная обработка для OCR, без сохранения на сервере.
• Передача сторонним компаниям: 0 байт данных передается рекламодателям, брокерам или коллекторам (No third-party ad/broker sharing).

2. Методы обеспечения безопасности (Security Practices):
• Шифрование данных в пути: Все сетевые запросы используют протокол HTTPS / TLS 1.3.
• Изоляция хранилища: Данные защищены системной песочницей Android (App Sandbox) и исключены из Android Auto Backup.
• Защита входа локальным PIN-кодом устройства.
• Запрос на удаление данных: Полное стирание всех сохраненных записей в 1 клик (In-app Account & Data Deletion).

3. Соответствие Financial Services Policy Google Play:
• Приложение не является кредитором («Not a lending app»).
• Не выдает потребительские или краткосрочные кредиты («Does not issue personal loans»).
• Не взимает комиссий за подбор кредитов («Not a loan broker»).
• Является правовым калькулятором и органайзером правовой помощи заемщикам.

4. Соответствие политике Генеративного ИИ (Generative AI Policy):
• Пользователю предоставлен дисклеймер об ограничениях и возможных ошибках ИИ.
• Встроена функция подачи жалобы («Report AI content») на любой сгенерированный ответ.
    """.trimIndent()

    // Интерактивные пункты чеклиста
    var checkAppId by remember { mutableStateOf(true) }
    var checkTitleLen by remember { mutableStateOf(true) }
    var checkTargetSdk by remember { mutableStateOf(true) }
    var checkMonochromeIcon by remember { mutableStateOf(true) }
    var checkPrivacyPolicy by remember { mutableStateOf(true) }
    var checkDataSafety by remember { mutableStateOf(true) }
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
                                    Triple(checkAppId, { v: Boolean -> checkAppId = v }, "Уникальный ApplicationId: com.aistudio.legaldebthelp.vxbq"),
                                    Triple(checkTitleLen, { v: Boolean -> checkTitleLen = v }, "Название приложения до 30 знаков (без капса и эмодзи)"),
                                    Triple(checkTargetSdk, { v: Boolean -> checkTargetSdk = v }, "Target SDK 36 (соответствует требованиям 2026 года)"),
                                    Triple(checkMonochromeIcon, { v: Boolean -> checkMonochromeIcon = v }, "Адаптивная иконка + Monochrome (Android 13+ Themed Icons)"),
                                    Triple(checkPrivacyPolicy, { v: Boolean -> checkPrivacyPolicy = v }, "Политика конфиденциальности и соответствие 152-ФЗ"),
                                    Triple(checkFinancialDisclaimer, { v: Boolean -> checkFinancialDisclaimer = v }, "Финансовый дисклеймер (Google Play Financial Services Policy)"),
                                    Triple(checkDataSafety, { v: Boolean -> checkDataSafety = v }, "Локальная БД Room без передачи персональных данных третьим лицам"),
                                    Triple(checkScreenshots, { v: Boolean -> checkScreenshots = v }, "Скриншоты для маркета (1080x1920 / 1080x2400)"),
                                    Triple(checkAabBundle, { v: Boolean -> checkAabBundle = v }, "Сборка подписанного Android App Bundle (.aab)")
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
