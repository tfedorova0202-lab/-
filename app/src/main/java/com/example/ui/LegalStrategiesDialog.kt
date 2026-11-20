package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.utils.DocumentGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalStrategiesDialog(
    onDismiss: () -> Unit,
    onOpenTemplates: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var lastSavedPath by remember { mutableStateOf<String?>(null) }
    var memoUri by remember { mutableStateOf<Uri?>(null) }

    fun downloadMemo() {
        val uri = DocumentGenerator.generateLegalStrategiesMemoPdf(context)
        if (uri != null) {
            memoUri = uri
            val saved = DocumentGenerator.saveToDownloads(context, uri, "Pamyatka_Strategii_2026.pdf")
            lastSavedPath = saved ?: "Папка Загрузки/ЮрПомощник"
            Toast.makeText(context, "Памятка сохранена в $lastSavedPath", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Не удалось сгенерировать PDF", Toast.LENGTH_SHORT).show()
        }
    }

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
                // Заголовок
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Правовая база и 5 стратегий",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Актуально на 2026 год • Решения и нормы права",
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

                // Вкладки
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("5 стратегий", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Законы 2026", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Инкогнито", fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Контент вкладок
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> FiveStrategiesTab(onOpenTemplates = onOpenTemplates)
                        1 -> LegalBaseOnlineTab()
                        2 -> IncognitoProtocolTab()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Нижняя панель действий
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lastSavedPath != null) {
                        Text(
                            text = "Файл сохранен: $lastSavedPath",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { downloadMemo() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Скачать памятку (PDF)", fontSize = 12.sp)
                        }

                        if (memoUri != null) {
                            OutlinedButton(
                                onClick = { DocumentGenerator.openPdf(context, memoUri!!) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            OutlinedButton(
                                onClick = { DocumentGenerator.sharePdf(context, memoUri!!, "Памятка: 5 стратегий и законы") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiveStrategiesTab(onOpenTemplates: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StrategyCard(
            number = "1",
            title = "Досудебная оптимизация и аудит переплат",
            targetAudience = "При первых просрочках, звонках МФО, навязанных страховках и комиссиях",
            legalBase = "ч. 23 ст. 5, ч. 2.7–2.9 ст. 7 ФЗ № 353-ФЗ • ст. 333 ГК РФ • ч. 7 ст. 4 ФЗ № 230-ФЗ • ст. 958 ГК РФ",
            description = "Помогает проверить законность начислений, оспорить превышение лимитов переплаты, ограничить звонки родственникам и вернуть деньги за навязанные услуги.",
            steps = listOf(
                "Проверить лимит МФО (ч. 23 ст. 5 ФЗ-353): сумма процентов, неустоек и штрафов не может превышать 100% от тела займа (договоры с 01.04.2026 по ФЗ № 545-ФЗ) или 130% (с 01.07.2023 по 31.03.2026).",
                "Направить отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ № 230-ФЗ), чтобы коллекторы не звонили близким и на работу.",
                "Подать заявление на возврат навязанных страховок и допуслуг в 30-дневный период охлаждения (ч. 2.7–2.9 ст. 7 ФЗ-353) или пропорциональный возврат при досрочном погашении (ст. 958 ГК РФ, ч. 10–12 ст. 11 ФЗ-353).",
                "Через 4 месяца просрочки направить отказ от взаимодействия (ст. 8 ФЗ № 230-ФЗ) для прекращения звонков и встреч (письменная почтовая переписка сохраняется)."
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
        )

        StrategyCard(
            number = "2",
            title = "Судебная защита и отмена приказов",
            targetAudience = "Когда банк или коллекторы подали мировому судье или в районный суд",
            legalBase = "ст. 128, 129 ГПК РФ • ст. 196, 199, 200 ГК РФ • ст. 333 ГК РФ • ст. 812 ГК РФ",
            description = "Своевременные возражения на судебный приказ (10 календарных дней) влекут его отмену мировым судьей. В исковом производстве можно заявить о сроке давности (3 года) и просить суд снизить неустойки.",
            steps = listOf(
                "При вынесении судебного приказа подать возражения мировому судье в течение 10 календарных дней с момента получения (ст. 128, 129 ГПК РФ) без признания части долга.",
                "Если кредитор подал иск: заявить в суде о пропуске срока исковой давности (3 года по ст. 196, 199 ГК РФ) по платежам старше 3 лет (при отсутствии перерыва/приостановления по ст. 202–204 ГК РФ).",
                "Заявить письменное ходатайство по ст. 333 ГК РФ о снижении явно несоразмерной неустойки (решение принимает суд с учетом ПП ВС РФ № 7).",
                "При мошеннических займах заявить возражения/иск о признании договора незаключенным по безденежности (ст. 812 ГК РФ)."
            ),
            color = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        StrategyCard(
            number = "3",
            title = "Исполнительное производство под контролем (ФССП)",
            targetAudience = "Когда открыто исполнительное производство у судебных приставов",
            legalBase = "ст. 4, 69, 99 ФЗ № 229-ФЗ • ст. 203, 434 ГПК РФ • ст. 446 ГПК РФ",
            description = "Позволяет защитить прожиточный минимум на одном банковском счете, сохранить единственное жилье (с учетом исключений закона) и ходатайствовать в суд о рассрочке.",
            steps = listOf(
                "Подать приставу заявление о сохранении прожиточного минимума трудоспособного населения (или регионального, если он выше) по ст. 69 ФЗ-229 с указанием одного счета.",
                "При тяжелом материальном положении обратиться в суд с заявлением о рассрочке исполнения решения (ст. 203 ГПК РФ; удовлетворение — на усмотрение суда).",
                "Учитывать исполнительский иммунитет единственного жилья по ст. 446 ГПК РФ (кроме залогового/ипотечного и случаев «роскошного жилья» по Постановлению КС РФ № 15-П)."
            ),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer
        )

        StrategyCard(
            number = "4",
            title = "Бесплатное внесудебное банкротство через МФЦ",
            targetAudience = "Долг от 25 000 до 1 000 000 руб. при соблюдении одного из 4 условий ст. 223.2 ФЗ-127",
            legalBase = "ст. 223.2 – 223.7 Федерального закона от 26.10.2002 № 127-ФЗ",
            description = "Бесплатная процедура через центры «Мои Документы» без госпошлины и финансового управляющего. Освобождение от обязательств наступает через 6 месяцев при соблюдении всех условий закона (кроме несписываемых долгов).",
            steps = listOf(
                "Проверить основание (ст. 223.2 ФЗ-127): закрытое ИП по п.4 ч.1 ст.46 ФЗ-229, ИЛИ пенсионер/получатель детского пособия с ИП > 1 года, ИЛИ исполнительный документ предъявлен > 7 лет назад и не исполнен.",
                "Собрать полный и точный список всех кредиторов с суммами (неуказанные долги не списываются).",
                "Подать заявление в МФЦ по месту жительства или пребывания.",
                "По истечении 6 месяцев при отсутствии оснований для прекращения процедуры публикуются сведения о завершении в ЕФРСБ."
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        StrategyCard(
            number = "5",
            title = "Банкротство с защитой ипотеки («Ипотечный щит»)",
            targetAudience = "Должники с единственным ипотечным жильем и непосильными необеспеченными кредитами",
            legalBase = "ст. 213.10-1, 213.28 Федерального закона № 127-ФЗ (в ред. ФЗ № 298-ФЗ)",
            description = "Механизм ст. 213.10-1 ФЗ № 127-ФЗ позволяет при согласии залогового банка и утверждении арбитражным судом заключить отдельное мировое соглашение и исключить единственное ипотечное жилье из торгов. Вопрос освобождения от прочих долгов решается судом по ст. 213.28 ФЗ № 127-ФЗ.",
            steps = listOf(
                "Подтвердить, что ипотечная квартира является единственным пригодным для проживания жильем гражданина и его семьи.",
                "Согласовать с банком-залогодержателем проект отдельного мирового соглашения (в т.ч. с участием третьего лица для внесения платежей).",
                "Утвердить соглашение в арбитражном суде по ст. 213.10-1 ФЗ № 127-ФЗ.",
                "Завершить процедуру банкротства; освобождение от прочих обязательств применяется судом с учетом ограничений ст. 213.28 ФЗ № 127-ФЗ."
            ),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            textColor = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Нужно готовое заявление?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("В приложении доступны официальные бланки по ст. 128 ГПК, ФЗ-230, ст. 333 ГК и ФЗ-229.", style = MaterialTheme.typography.bodySmall)
                }
                FilledTonalButton(onClick = onOpenTemplates, shape = RoundedCornerShape(10.dp)) {
                    Text("Шаблоны")
                }
            }
        }
    }
}

@Composable
private fun StrategyCard(
    number: String,
    title: String,
    targetAudience: String,
    legalBase: String,
    description: String,
    steps: List<String>,
    color: Color,
    textColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = textColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(number, fontWeight = FontWeight.Black, color = textColor, fontSize = 14.sp)
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = textColor.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Кому подходит: $targetAudience",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.95f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Норма права: $legalBase",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = textColor.copy(alpha = 0.2f))
                    Text(
                        text = "Пошаговый план реализации:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    steps.forEachIndexed { idx, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text("${idx + 1}.", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                            Text(step, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.9f), fontSize = 12.sp)
                        }
                    }
                }
            }

            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (expanded) "Скрыть шаги ▲" else "Показать шаги ▼",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LegalBaseOnlineTab() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LawSectionCard(
            title = "Федеральный закон № 230-ФЗ (Антиколлектор)",
            items = listOf(
                "ч. 7 ст. 4 ФЗ № 230-ФЗ" to "Взаимодействие с третьими лицами (родственниками, коллегами) допускается ТОЛЬКО при одновременном согласии должника и третьего лица. Заемщик имеет право в любой момент отозвать такое согласие.",
                "ст. 8 ФЗ № 230-ФЗ" to "Полный отказ от взаимодействия с кредитором и коллекторами подается через 4 месяца со дня возникновения просрочки. После этого любые звонки и встречи запрещены законом.",
                "ст. 7 ФЗ № 230-ФЗ" to "Лимиты контактов: звонки не чаще 1 раза в сутки, 2 раз в неделю, 8 раз в месяц. Запрет звонков ночью: в будни с 22:00 до 08:00, в выходные с 20:00 до 09:00.",
                "ст. 172.4 УК РФ" to "Уголовная ответственность за незаконную коллекторскую деятельность с угрозами, шантажом или насилием (до 5-10 лет лишения свободы)."
            )
        )

        LawSectionCard(
            title = "Федеральный закон № 353-ФЗ (Потребительский кредит)",
            items = listOf(
                "ч. 23 ст. 5 ФЗ № 353-ФЗ" to "Предельный лимит начислений по займам до 1 года: не более 100% от тела займа для договоров с 01.04.2026 (ФЗ № 545-ФЗ), 130% — с 01.07.2023 по 31.03.2026, 150% — ранее; ежедневная ставка — не более 0.8% в день (292% годовых).",
                "ч. 2.7–2.9 ст. 7 ФЗ № 353-ФЗ" to "Период охлаждения 30 календарных дней: право отказаться от дополнительных услуг и страховок, оформленных при получении потребительского кредита (для отдельных добровольных полисов — 14 дней по Указанию ЦБ РФ № 3854-У).",
                "ч. 10–12 ст. 11 ФЗ № 353-ФЗ" to "Возврат части страховой премии пропорционально неистекшему сроку при досрочном полном погашении кредита (если страхование обеспечивало кредитные обязательства и страховой случай не наступал)."
            )
        )

        LawSectionCard(
            title = "Гражданский кодекс РФ (ГК РФ)",
            items = listOf(
                "ст. 333 ГК РФ" to "Снижение неустойки: если подлежащая уплате неустойка явно несоразмерна последствиям нарушения обязательства, суд вправе снизить её по заявлению должника (ПП ВС РФ № 7 от 24.03.2016).",
                "ст. 196, 199, 200 ГК РФ" to "Срок исковой давности составляет 3 года и исчисляется отдельно по каждому платежу графика. Суд отказывает в иске по заявлению стороны (если срок не прерывался и не приостанавливался по ст. 202–204 ГК РФ).",
                "ст. 812 ГК РФ" to "Оспаривание по безденежности: если в суде доказано, что деньги фактически не поступали заемщику, договор займа считается незаключенным (п. 3 ст. 812 ГК РФ).",
                "ст. 958 ГК РФ" to "Возврат части страховой премии при досрочном прекращении договора страхования в предусмотренных законом случаях."
            )
        )

        LawSectionCard(
            title = "ГПК РФ и Федеральный закон № 229-ФЗ (Приставы и суд)",
            items = listOf(
                "ст. 128, 129 ГПК РФ" to "Судебный приказ выносится без вызова сторон. Должник имеет 10 календарных дней с момента получения копии приказа на подачу возражений, после чего судья отменяет приказ.",
                "ст. 203 ГПК РФ" to "Право должника просить суд об отсрочке или рассрочке исполнения решения суда (удовлетворение зависит от оценки доказательств судом).",
                "ст. 4, 69, 99 ФЗ № 229-ФЗ" to "Сохранение прожиточного минимума: по заявлению должника с указанием одного счета приставы и банк обязаны ежемесячно сохранять сумму не ниже величины ПМ (кроме алиментов и возмещения вреда).",
                "ст. 446 ГПК РФ" to "Исполнительский иммунитет единственного пригодного жилья (за исключением ипотеки/залога и случаев «роскошного жилья» по Постановлению КС РФ № 15-П)."
            )
        )

        LawSectionCard(
            title = "Федеральный закон № 127-ФЗ (Банкротство)",
            items = listOf(
                "ст. 213.10-1 ФЗ № 127-ФЗ" to "«Ипотечный щит»: возможность утвердить в арбитражном суде отдельное мировое соглашение с залоговым банком по единственному ипотечному жилью. Вопрос освобождения от прочих долгов решается судом по ст. 213.28 ФЗ № 127-ФЗ.",
                "ст. 223.2 ФЗ № 127-ФЗ" to "Внесудебное бесплатное банкротство через МФЦ при долге от 25 тыс. до 1 млн рублей при наличии одного из 4 оснований (п.4 ч.1 ст.46 ФЗ-229, пенсионеры/пособия с ИП > 1 года, ИП > 7 лет)."
            )
        )
    }
}

@Composable
private fun LawSectionCard(title: String, items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            items.forEach { (article, text) ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(article, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun IncognitoProtocolTab() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Режим «Инкогнито» (Защита семьи)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "По умолчанию мы исходим из того, что супруг, родственники и коллеги не должны узнать о ваших финансовых сложностях. Вот 3 обязательных цифровых шага для 100% защиты приватности.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        IncognitoStepCard(
            step = "1",
            title = "Подключение «Заказное.Почта.ру» (Электронные письма)",
            description = "Бумажные заказные письма от судов, МФО и приставов больше НЕ будут опускаться в почтовый ящик в подъезде.",
            action = "Перейдите на сайт zakaznoe.pochta.ru или в приложение «Почта России», авторизуйтесь через Госуслуги и включите получение электронных заказных писем. Все письма будут приходить строго в защищенный цифровой профиль."
        )

        IncognitoStepCard(
            step = "2",
            title = "Настройка Госпочты на портале Госуслуги",
            description = "Мгновенное получение электронных постановлений судебных приставов без визитов сотрудников на дом.",
            action = "В профиле Госуслуг в разделе «Уведомления» активируйте канал «Госпочта». Постановления ФССП будут доставляться вам электронно в момент их вынесения, давая 5 дней на добровольное урегулирование."
        )

        IncognitoStepCard(
            step = "3",
            title = "Отзыв согласия на контакт с близкими (ч. 7 ст. 4 ФЗ-230)",
            description = "Коллекторы и службы взыскания звонят маме, супругу или руководству? Это прямо запрещено законом при отзыве согласия.",
            action = "Направьте кредиторам заявление об отзыве согласия на взаимодействие с третьими лицами заказным письмом с описью. После вручения кредитор не имеет права делать ни единого звонка вашему окружению под угрозой штрафа до 500 000 руб."
        )
    }
}

@Composable
private fun IncognitoStepCard(step: String, title: String, description: String, action: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(step, color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Что сделать: $action",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
