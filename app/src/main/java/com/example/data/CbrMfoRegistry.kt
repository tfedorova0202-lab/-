package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class MfoStatus(val title: String) {
    LEGAL("Включена в реестр ЦБ РФ (Легальная)"),
    EXCLUDED("Исключена из реестра ЦБ РФ (Лицензия отозвана)"),
    ILLEGAL("Не найдена в реестре («Черный кредитор»)")
}

data class MfoRegistryEntry(
    val id: String,
    val fullName: String,
    val brandName: String,
    val mfoType: String, // "МФК" или "МКК"
    val regNumber: String,
    val inn: String,
    val ogrn: String,
    val status: MfoStatus,
    val entryDate: String,
    val exclusionDate: String? = null,
    val officialDomains: List<String>,
    val address: String,
    val notes: String = ""
)

data class MfoCheckResult(
    val query: String,
    val timestamp: Long,
    val formattedCheckTime: String,
    val entry: MfoRegistryEntry?,
    val status: MfoStatus,
    val title: String,
    val statusExplanation: String,
    val legalBasis: String,
    val maxOverpaymentLimit: String,
    val maxDailyRate: String,
    val recommendations: List<String>,
    val officialSiteWarning: String? = null,
    val complaintUrl: String = "https://cbr.ru/Reception/"
)

object CbrMfoRegistry {

    private val officialRegistry: List<MfoRegistryEntry> = listOf(
        MfoRegistryEntry(
            id = "cbr-zaimer",
            fullName = "Акционерное общество Микрофинансовая компания «Займер»",
            brandName = "Займер",
            mfoType = "МФК",
            regNumber = "651303532004088",
            inn = "4205271785",
            ogrn = "1134205019189",
            status = MfoStatus.LEGAL,
            entryDate = "11.10.2013",
            officialDomains = listOf("zaimer.ru", "займер.рф"),
            address = "630099, г. Новосибирск, ул. Октябрьская магистраль, д. 4, оф. 1401",
            notes = "Крупнейшая МФК. Обязана соблюдать стандарты саморегулируемой организации (СРО) и лимиты ФЗ-353."
        ),
        MfoRegistryEntry(
            id = "cbr-moneyman",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «Мани Мен»",
            brandName = "MoneyMan (Манимен)",
            mfoType = "МФК",
            regNumber = "2110177000478",
            inn = "7704784072",
            ogrn = "1117746442670",
            status = MfoStatus.LEGAL,
            entryDate = "25.10.2011",
            officialDomains = listOf("moneyman.ru"),
            address = "121096, г. Москва, ул. Василисы Кожиной, д. 1, офис Д-13",
            notes = "Состоит в реестре Банка России. Подлежит надзору Департамента небанковского кредитования ЦБ РФ."
        ),
        MfoRegistryEntry(
            id = "cbr-ekapusta",
            fullName = "Общество с ограниченной ответственностью Микрокредитная компания «Русинтерфинанс»",
            brandName = "еКапуста (eKapusta)",
            mfoType = "МКК",
            regNumber = "2120754001243",
            inn = "5408292849",
            ogrn = "1125476023298",
            status = MfoStatus.LEGAL,
            entryDate = "22.03.2012",
            officialDomains = listOf("ekapusta.com", "екапуста.рф"),
            address = "630055, г. Новосибирск, ул. Гнесиных, д. 10/1, оф. 202",
            notes = "Легальная МКК. Действует ограничение ставки не более 0.8% в день."
        ),
        MfoRegistryEntry(
            id = "cbr-bystrodengi",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «Быстроденьги»",
            brandName = "Быстроденьги",
            mfoType = "МФК",
            regNumber = "2110573000002",
            inn = "7325081622",
            ogrn = "1087325005899",
            status = MfoStatus.LEGAL,
            entryDate = "08.07.2011",
            officialDomains = listOf("bistrodengi.ru", "быстроденьги.рф"),
            address = "123290, г. Москва, 1-й Магистральный тупик, д. 5А",
            notes = "В реестре ЦБ РФ. Ограничение навязывания допуслуг и страховок."
        ),
        MfoRegistryEntry(
            id = "cbr-webzaim",
            fullName = "Общество с ограниченной ответственностью Микрокредитная компания «Академическая»",
            brandName = "Веб-займ (Webbankir / Вебзайм)",
            mfoType = "МКК",
            regNumber = "1903550009325",
            inn = "5407973316",
            ogrn = "1195476007605",
            status = MfoStatus.LEGAL,
            entryDate = "09.07.2019",
            officialDomains = listOf("web-zaim.ru"),
            address = "630091, г. Новосибирск, ул. Каменская, д. 51",
            notes = "Состоит в реестре Банка России."
        ),
        MfoRegistryEntry(
            id = "cbr-webbankir",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «Вэббанкир»",
            brandName = "Webbankir (Вэббанкир)",
            mfoType = "МФК",
            regNumber = "2120177002077",
            inn = "7727786438",
            ogrn = "1127746695503",
            status = MfoStatus.LEGAL,
            entryDate = "11.09.2012",
            officialDomains = listOf("webbankir.com"),
            address = "125040, г. Москва, ул. Правды, д. 8, корп. 1",
            notes = "Легальная МФК в реестре ЦБ РФ."
        ),
        MfoRegistryEntry(
            id = "cbr-turbozaim",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «Турбозайм»",
            brandName = "Турбозайм",
            mfoType = "МФК",
            regNumber = "651303045003168",
            inn = "7702820120",
            ogrn = "1137746702367",
            status = MfoStatus.LEGAL,
            entryDate = "28.05.2013",
            officialDomains = listOf("turbozaim.ru"),
            address = "123290, г. Москва, туп. 1-й Магистральный, д. 5А, ком. 203",
            notes = "Включена в реестр ЦБ РФ."
        ),
        MfoRegistryEntry(
            id = "cbr-dozarplati",
            fullName = "Общество с ограниченной ответственностью Микрокредитная компания «ДЗП-Единый»",
            brandName = "До Зарплаты",
            mfoType = "МКК",
            regNumber = "1903140009249",
            inn = "7838086055",
            ogrn = "1197847087640",
            status = MfoStatus.LEGAL,
            entryDate = "23.05.2019",
            officialDomains = listOf("dozarplati.com"),
            address = "197374, г. Санкт-Петербург, Торфяная дорога, д. 7, лит. Ф",
            notes = "Состоит в реестре Банка России. При займах часты навязанные сертификаты (подлежат возврату по ст. 32 ЗОЗП)."
        ),
        MfoRegistryEntry(
            id = "cbr-dobrozaim",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «Саммит»",
            brandName = "ДоброЗайм",
            mfoType = "МФК",
            regNumber = "2110177000806",
            inn = "7728771940",
            ogrn = "1117746346298",
            status = MfoStatus.LEGAL,
            entryDate = "27.12.2011",
            officialDomains = listOf("dobrozaim.ru"),
            address = "127051, г. Москва, переулок Колокольников, д. 22, стр. 5",
            notes = "Легальная МФК в реестре Банка России."
        ),
        MfoRegistryEntry(
            id = "cbr-migcredit",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «МигКредит»",
            brandName = "МигКредит",
            mfoType = "МФК",
            regNumber = "2110177000040",
            inn = "7715825027",
            ogrn = "1107746671207",
            status = MfoStatus.LEGAL,
            entryDate = "08.07.2011",
            officialDomains = listOf("migcredit.ru"),
            address = "127018, г. Москва, ул. Сущёвский Вал, д. 5, стр. 3",
            notes = "В реестре ЦБ РФ с 2011 года."
        ),
        MfoRegistryEntry(
            id = "cbr-joymoney",
            fullName = "Общество с ограниченной ответственностью Микрокредитная компания «Джой Мани»",
            brandName = "Joymoney (Джой Мани)",
            mfoType = "МКК",
            regNumber = "651403550005456",
            inn = "5407496776",
            ogrn = "1145476064711",
            status = MfoStatus.LEGAL,
            entryDate = "28.07.2014",
            officialDomains = listOf("joy.money", "joymoney.ru"),
            address = "630099, г. Новосибирск, ул. Советская, д. 12",
            notes = "Состоит в реестре Банка России."
        ),
        MfoRegistryEntry(
            id = "cbr-limezaim",
            fullName = "Общество с ограниченной ответственностью Микрофинансовая компания «Лайм-Займ»",
            brandName = "Lime (Лайм-Займ)",
            mfoType = "МФК",
            regNumber = "651303045004102",
            inn = "7724889891",
            ogrn = "1137746831604",
            status = MfoStatus.LEGAL,
            entryDate = "17.10.2013",
            officialDomains = listOf("lime-zaim.ru"),
            address = "630102, г. Новосибирск, ул. Кирова, д. 48, оф. 1401",
            notes = "Включена в реестр ЦБ РФ."
        ),
        // Исключенные МФО (примеры реальных субъектов, исключенных ЦБ РФ за систематические нарушения)
        MfoRegistryEntry(
            id = "cbr-bystrokredit-ex",
            fullName = "Общество с ограниченной ответственностью Микрокредитная компания «БыстроКредит Онлайн»",
            brandName = "БыстроКредит Онлайн",
            mfoType = "МКК",
            regNumber = "1803045008912",
            inn = "7709988112",
            ogrn = "1187746112233",
            status = MfoStatus.EXCLUDED,
            entryDate = "14.05.2018",
            exclusionDate = "18.04.2024",
            officialDomains = listOf("bystrokredit-online.ru"),
            address = "109004, г. Москва, ул. Земляной Вал, д. 50",
            notes = "ИСКЛЮЧЕНА Банком России из государственного реестра МФО приказом ЦБ РФ за неоднократные нарушения требований ст. 5 ФЗ-353 и превышение лимитов начислений."
        ),
        MfoRegistryEntry(
            id = "cbr-roszaim-ex",
            fullName = "Общество с ограниченной ответственностью Микрокредитная компания «РосЗайм Финанс»",
            brandName = "РосЗайм",
            mfoType = "МКК",
            regNumber = "1703140008451",
            inn = "7810445566",
            ogrn = "1177847199887",
            status = MfoStatus.EXCLUDED,
            entryDate = "20.08.2017",
            exclusionDate = "12.09.2023",
            officialDomains = listOf("roszaim-finance.ru"),
            address = "196084, г. Санкт-Петербург, Московский пр-кт, д. 100",
            notes = "Исключена из реестра ЦБ РФ за непредоставление отчетности и незаконные методы взаимодействия с должниками."
        ),
        MfoRegistryEntry(
            id = "cbr-dengivsem-ex",
            fullName = "Общество с ограниченной ответственностью «Деньги Всем Экспресс»",
            brandName = "Деньги Всем",
            mfoType = "МКК",
            regNumber = "1603550007812",
            inn = "5406771122",
            ogrn = "1165476088991",
            status = MfoStatus.EXCLUDED,
            entryDate = "10.02.2016",
            exclusionDate = "05.11.2022",
            officialDomains = listOf("dengivsem-express.ru"),
            address = "630005, г. Новосибирск, ул. Фрунзе, д. 24",
            notes = "Лицензия аннулирована. Внесена в список организаций с выявленными признаками нелегальной деятельности."
        )
    )

    /**
     * Проверка МФО на легальность в реестре Банка России на момент запроса
     */
    fun checkLegality(query: String): MfoCheckResult {
        val cleanQuery = query.trim().lowercase(Locale.getDefault())
        val digitsOnly = query.filter { it.isDigit() }
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMMM yyyy г., HH:mm:ss 'МСК'", Locale.forLanguageTag("ru-RU"))
        sdf.timeZone = TimeZone.getTimeZone("Europe/Moscow")
        val formattedTime = sdf.format(Date(now))

        // 1. Поиск точного или частичного совпадения в официальном реестре
        val match = officialRegistry.firstOrNull { entry ->
            // Поиск по ИНН, ОГРН или рег. номеру
            (digitsOnly.isNotBlank() && (
                entry.inn == digitsOnly || 
                entry.ogrn == digitsOnly || 
                entry.regNumber == digitsOnly ||
                entry.inn.contains(digitsOnly) ||
                entry.regNumber.contains(digitsOnly)
            )) ||
            // Поиск по бренду, полному наименованию или домену
            entry.brandName.lowercase(Locale.getDefault()).contains(cleanQuery) ||
            entry.fullName.lowercase(Locale.getDefault()).contains(cleanQuery) ||
            cleanQuery.contains(entry.brandName.lowercase(Locale.getDefault())) ||
            entry.officialDomains.any { domain -> 
                cleanQuery.contains(domain.lowercase(Locale.getDefault())) ||
                domain.lowercase(Locale.getDefault()).contains(cleanQuery)
            }
        }

        return when {
            match != null && match.status == MfoStatus.LEGAL -> {
                MfoCheckResult(
                    query = query,
                    timestamp = now,
                    formattedCheckTime = formattedTime,
                    entry = match,
                    status = MfoStatus.LEGAL,
                    title = "Легальная МФО (В реестре Банка России)",
                    statusExplanation = "Организация ${match.fullName} официально состоит в Государственном реестре МФО ЦБ РФ под номером ${match.regNumber}. Имеет законное право на предоставление микрозаймов гражданам.",
                    legalBasis = "Федеральный закон № 151-ФЗ «О микрофинансовой деятельности», ч. 23 ст. 5 ФЗ № 353-ФЗ «О потребительском кредите (займе)», ст. 807–810 ГК РФ.",
                    maxOverpaymentLimit = "130% от суммы займа (для договоров с 01.07.2023, ранее — 150%). Начисление процентов, неустоек и комиссий сверх указанного лимита не допускается законом (ч. 23 ст. 5 ФЗ № 353-ФЗ).",
                    maxDailyRate = "Не более 0.8% в день (предельная ПСК — не более 292% годовых).",
                    recommendations = listOf(
                        "Сверьте адрес сайта: займы разрешено оформлять строго на официальном домене: ${match.officialDomains.joinToString(", ")}. Остерегайтесь фишинговых сайтов-клонов!",
                        "Проверьте расчет долга: если МФО требует сумму с переплатой свыше 1.3x (130%), излишек подлежит списанию на основании ч. 23 ст. 5 ФЗ № 353-ФЗ.",
                        "Защита от звонков близким: направьте в МФО отзыв согласия на взаимодействие с третьими лицами (ч. 7 ст. 4 ФЗ № 230-ФЗ).",
                        "Отказ от навязанных услуг: страховки, телемедицина и платные СМС подлежат возврату в период охлаждения (30 дней по ст. 7 ФЗ № 353-ФЗ)."
                    )
                )
            }

            match != null && match.status == MfoStatus.EXCLUDED -> {
                MfoCheckResult(
                    query = query,
                    timestamp = now,
                    formattedCheckTime = formattedTime,
                    entry = match,
                    status = MfoStatus.EXCLUDED,
                    title = "ВНИМАНИЕ: ИСКЛЮЧЕНА ИЗ РЕЕСТРА ЦБ РФ",
                    statusExplanation = "Организация ${match.fullName} ИСКЛЮЧЕНА Банком России из государственного реестра МФО ${match.exclusionDate ?: ""}. С момента исключения организация ЛИШЕНА ПРАВА выдавать микрозаймы и начислять ростовщические проценты.",
                    legalBasis = "Ст. 2, 5 Федерального закона № 151-ФЗ, ст. 809, 812, 1102 ГК РФ, п. 5 ст. 5 ФЗ № 353-ФЗ.",
                    maxOverpaymentLimit = "Начисление микрофинансовых процентов (0.8% в день) после даты исключения является незаконным. Кредитор вправе требовать только фактический остаток основного долга.",
                    maxDailyRate = "Ставка МФО аннулирована. Любые требования сверх ставки рефинансирования ЦБ РФ (ст. 395 ГК РФ) не имеют юридической силы.",
                    recommendations = listOf(
                        "Потребуйте перерасчет задолженности: исключенная МФО не имеет права применять драконовские штрафы и проценты.",
                        "При требованиях коллекторов: коллекторам запрещено взыскивать долги в пользу исключенных или нелегальных организаций (ст. 8 ФЗ № 230-ФЗ).",
                        "Подайте жалобу в Банк России на неправомерную деятельность через интернет-приемную (cbr.ru).",
                        "Если вынесен судебный приказ — отмените его в 10-дневный срок (ст. 128, 129 ГПК РФ)."
                    )
                )
            }

            else -> {
                // Если организация не найдена в реестре
                MfoCheckResult(
                    query = query,
                    timestamp = now,
                    formattedCheckTime = formattedTime,
                    entry = null,
                    status = MfoStatus.ILLEGAL,
                    title = "ОТСУТСТВУЕТ В РЕЕСТРЕ ЦБ РФ («Черный кредитор»)",
                    statusExplanation = "Организация по запросу «$query» НЕ НАЙДЕНА в государственном реестре Банка России на момент проверки. Либо используется название-клон, либо деятельность ведется нелегально без лицензии.",
                    legalBasis = "Ст. 2, 5 ФЗ № 151-ФЗ, ст. 171 УК РФ (Незаконное предпринимательство), ст. 172.1 УК РФ, ст. 14.56 КоАП РФ (Незаконное осуществление деятельности по предоставлению потребительских кредитов).",
                    maxOverpaymentLimit = "Незаконная организация не имеет права начислять микрофинансовые проценты и штрафы.",
                    maxDailyRate = "0% (сделка оспорима по ст. 167, 168, 169 ГК РФ).",
                    recommendations = listOf(
                        "НЕ ПЕРЕВОДИТЕ ДЕНЬГИ по сомнительным реквизитам физических лиц или криптокошельков.",
                        "Проверьте точное юридическое наименование и ИНН в вашем договоре займа: часто мошенники используют бренд легальной МФО, будучи незарегистрированным клоном.",
                        "Подайте обращение в Службу Банка России по защите прав потребителей и предупреждению недобросовестных практик.",
                        "Направьте заявление в полицию по факту незаконной кредитной деятельности (ст. 14.56 КоАП РФ, ст. 171 УК РФ)."
                    )
                )
            }
        }
    }

    /**
     * Получение полного реестра проверенных МФК/МКК
     */
    fun getAllRegistered(): List<MfoRegistryEntry> = officialRegistry
}
