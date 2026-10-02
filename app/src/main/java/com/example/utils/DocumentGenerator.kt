package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import java.io.IOException
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Debt
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentGenerator {

    fun generateRegistryCsv(context: Context, debts: List<Debt>): Uri? {
        val stringBuilder = StringBuilder()
        // Header
        stringBuilder.append("Кредитор;Договор;Требование (Факт);К отмене (Штрафы);План (Законный долг);Страховка;Ипотека;Статус\n")
        
        for (debt in debts) {
            val name = debt.creditorName.replace(";", ",")
            val contract = debt.contractDate.replace(";", ",")
            val fact = String.format(Locale.US, "%.2f", debt.factAmount)
            val penalty = String.format(Locale.US, "%.2f", debt.penaltyAmount)
            val plan = String.format(Locale.US, "%.2f", debt.planAmount)
            val insurance = String.format(Locale.US, "%.2f", debt.insuranceAmount)
            val isMortgage = if (debt.isMortgage) "Да" else "Нет"
            val status = debt.status.replace(";", ",")
            
            stringBuilder.append("$name;$contract;$fact;$penalty;$plan;$insurance;$isMortgage;$status\n")
        }

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        
        val fileName = "Reestr_Dolgov_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
        val file = File(docsDir, fileName)
        
        return try {
            // Write BOM for Excel to properly recognize UTF-8
            file.writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            file.appendText(stringBuilder.toString(), Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateRegistryPdf(context: Context, debts: List<Debt>): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(842, 595, 1).create() // A4 Landscape
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = Paint().apply { color = Color.BLACK; textSize = 10f }
        val boldPaint = Paint().apply { color = Color.BLACK; textSize = 10f; isFakeBoldText = true }
        val titlePaint = Paint().apply { color = Color.BLACK; textSize = 14f; isFakeBoldText = true }

        var y = 40f
        val startX = 40f
        val rowHeight = 25f

        // Заголовок
        canvas.drawText("РЕЕСТР ЗАДОЛЖЕННОСТЕЙ (ПЛАН/ФАКТ)", startX, y, titlePaint)
        y += 20f
        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        canvas.drawText("Дата формирования: $currentDate", startX, y, paint)
        y += 30f

        // Колонки
        val cols = floatArrayOf(startX, startX + 30f, startX + 180f, startX + 260f, startX + 340f, startX + 420f, startX + 500f, startX + 650f, startX + 760f)
        val headers = arrayOf("№", "Кредитор", "Договор", "Требуют", "К отмене", "Законный", "Основание", "Статус")

        // Рисуем заголовок таблицы
        for (i in headers.indices) {
            canvas.drawText(headers[i], cols[i] + 5f, y, boldPaint)
        }
        y += rowHeight

        var totalFact = 0.0
        var totalCancel = 0.0
        var totalPlan = 0.0

        // Рисуем строки
        debts.forEachIndexed { index, debt ->
            val toCancel = debt.penaltyAmount + debt.insuranceAmount
            totalFact += debt.factAmount
            totalCancel += toCancel
            totalPlan += debt.planAmount
            
            val bases = mutableListOf<String>()
            if (debt.isMortgage) bases.add("ст.213.10-1")
            if (debt.penaltyAmount > 0) bases.add("ст.333 ГК")
            if (debt.insuranceAmount > 0) bases.add("ст.958 ГК")
            val basisText = if (bases.isNotEmpty()) bases.joinToString(", ") else "ст.810 ГК"

            val truncatedName = if (debt.creditorName.length > 25) debt.creditorName.take(22) + "..." else debt.creditorName

            canvas.drawText("${index + 1}", cols[0] + 5f, y, paint)
            canvas.drawText(truncatedName, cols[1] + 5f, y, paint)
            canvas.drawText(debt.contractDate, cols[2] + 5f, y, paint)
            canvas.drawText(String.format("%,.2f", debt.factAmount), cols[3] + 5f, y, paint)
            canvas.drawText(String.format("%,.2f", toCancel), cols[4] + 5f, y, paint)
            canvas.drawText(String.format("%,.2f", debt.planAmount), cols[5] + 5f, y, boldPaint)
            canvas.drawText(basisText, cols[6] + 5f, y, paint)
            canvas.drawText(debt.status, cols[7] + 5f, y, paint)
            
            y += rowHeight
            // Рисуем линию
            canvas.drawLine(startX, y - rowHeight + 5f, cols[8], y - rowHeight + 5f, paint)
        }
        
        canvas.drawLine(startX, y - rowHeight + 5f, cols[8], y - rowHeight + 5f, paint)
        y += 10f

        // Итого
        canvas.drawText("ИТОГО:", cols[2] + 5f, y, boldPaint)
        canvas.drawText(String.format("%,.2f", totalFact), cols[3] + 5f, y, boldPaint)
        canvas.drawText(String.format("%,.2f", totalCancel), cols[4] + 5f, y, boldPaint)
        canvas.drawText(String.format("%,.2f", totalPlan), cols[5] + 5f, y, boldPaint)

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Registry_Plan_Fact.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateObjectionPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."
        val fact = debt?.factAmount?.let { "$it руб." } ?: "___________ руб."
        val plan = debt?.planAmount?.let { "$it руб." } ?: "___________ руб."

        drawText("Мировому судье судебного участка №___")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        drawText("Взыскатель: $creditor")
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ВОЗРАЖЕНИЕ НА ИСПОЛНЕНИЕ СУДЕБНОГО ПРИКАЗА")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = mutableListOf(
            "Мною был получен судебный приказ о взыскании задолженности",
            "в пользу $creditor по договору $contract.",
            "Требуемая сумма взыскания (ФАКТ): $fact.",
            "",
            "С вынесенным судебным приказом не согласен, так как не согласен",
            "с суммой долга и начисленными штрафами."
        )

        if ((debt?.penaltyAmount ?: 0.0) > 0) {
            body.add("На основании ст. 333 ГК РФ сумма неустойки несоразмерна")
            body.add("последствиям нарушения обязательства и подлежит снижению.")
        } else {
            body.add("На основании ст. 333 ГК РФ требую снижения неустойки и штрафов.")
        }
        if ((debt?.insuranceAmount ?: 0.0) > 0) {
            body.add("На основании ст. 958 ГК РФ требую исключить из суммы взыскания")
            body.add("навязанные страховые премии в размере ${debt?.insuranceAmount} руб.")
        }

        body.addAll(listOf(
            "Обоснованная законная сумма долга (ПЛАН): $plan.",
            "",
            "В соответствии со ст. 128, 129 ГПК РФ,",
            "",
            "ПРОШУ:",
            "1. Отменить судебный приказ о взыскании задолженности.",
            "2. Копию определения об отмене выслать в мой адрес."
        ))
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Vozrazhenie_${debt?.id ?: "Blank"}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateAntiCollectorPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."

        drawText("Руководителю: $creditor")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ЗАЯВЛЕНИЕ")
        drawText("об отзыве согласия на взаимодействие с третьими лицами")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = listOf(
            "Мною был заключен договор займа/кредита № $contract",
            "с $creditor.",
            "",
            "В соответствии с ч. 7 ст. 4 Федерального закона от 03.07.2016 № 230-ФЗ",
            "«О защите прав и законных интересов физических лиц при осуществлении",
            "деятельности по возврату просроченной задолженности»,",
            "",
            "Настоящим заявлением я отзываю свое согласие на осуществление",
            "направленного на возврат просроченной задолженности взаимодействия",
            "с любыми третьими лицами (членами моей семьи, родственниками,",
            "иными проживающими со мной лицами, соседями и любыми другими",
            "физическими лицами).",
            "",
            "Требую с момента получения данного заявления прекратить любые",
            "звонки, отправку сообщений и иные способы взаимодействия с",
            "указанными третьими лицами.",
            "",
            "В случае нарушения моего требования буду вынужден обратиться с",
            "жалобой в Федеральную службу судебных приставов (ФССП) и",
            "Центральный Банк РФ для привлечения вас к административной",
            "ответственности."
        )
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Otziv_Soglasiya_${debt?.id ?: "Blank"}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateRefusalOfInteractionPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."

        drawText("Руководителю: $creditor")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ЗАЯВЛЕНИЕ")
        drawText("об отказе от взаимодействия (ст. 8 ФЗ № 230)")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = listOf(
            "Мною был заключен договор займа/кредита № $contract",
            "с $creditor.",
            "",
            "Учитывая, что с момента возникновения просроченной задолженности",
            "прошло более 4 (четырех) месяцев,",
            "",
            "В соответствии со ст. 8 Федерального закона от 03.07.2016 № 230-ФЗ",
            "«О защите прав и законных интересов физических лиц при осуществлении",
            "деятельности по возврату просроченной задолженности»,",
            "",
            "Настоящим заявлением я полностью ОТКАЗЫВАЮСЬ от взаимодействия",
            "со взыскателем и/или лицом, действующим от его имени, любыми",
            "способами, предусмотренными пп. 1, 2 ч. 1 ст. 4 ФЗ № 230-ФЗ",
            "(встречи, телефонные переговоры, телеграфные, текстовые и",
            "голосовые сообщения).",
            "",
            "В случае нарушения моего требования буду вынужден обратиться с",
            "жалобой в Федеральную службу судебных приставов (ФССП) и",
            "Центральный Банк РФ."
        )
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Otkaz_Ot_Vzaimodeistviya_${debt?.id ?: "Blank"}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateInsuranceReturnPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."
        val insurance = debt?.insuranceAmount?.let { "$it руб." } ?: "___________ руб."

        drawText("Руководителю страховой компании / Банка:")
        drawText(creditor)
        drawText("От заявителя: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ЗАЯВЛЕНИЕ")
        drawText("об отказе от договора страхования и возврате страховой премии")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = listOf(
            "Мною был заключен договор займа/кредита № $contract",
            "с $creditor. Одновременно с кредитным договором,",
            "мне была навязана услуга личного страхования.",
            "",
            "Сумма удержанной (оплаченной) страховой премии: $insurance",
            "",
            "В соответствии с ч. 12 ст. 11 Федерального закона от 21.12.2013 № 353-ФЗ",
            "«О потребительском кредите (займе)», а также Указанием Банка России,",
            "заемщик имеет право отказаться от договора добровольного страхования",
            "в течение «периода охлаждения» с возвратом уплаченной премии,",
            "либо при досрочном погашении кредита (ст. 958 ГК РФ).",
            "",
            "Настоящим заявляю об одностороннем отказе от договора страхования",
            "и ТРЕБУЮ:",
            "1. Расторгнуть договор страхования.",
            "2. Возвратить мне уплаченную страховую премию в размере $insurance",
            "по указанным мною реквизитам в установленный законом срок."
        )
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Vozvrat_Strahovki_${debt?.id ?: "Blank"}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun getLackOfFundsText(context: Context, debt: Debt): String {
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()

        return """
            Руководителю: ${debt.creditorName}
            От должника: $userName
            ${if (userAddress.isNotBlank()) "Адрес: $userAddress\n" else ""}${if (userPhone.isNotBlank()) "Телефон: $userPhone\n" else ""}
            ПРЕТЕНЗИЯ
            об оспаривании договора займа по безденежности (ст. 812 ГК РФ)

            Мною якобы был заключен договор займа/кредита № ${debt.contractDate} с ${debt.creditorName}.

            Однако заявляю, что фактически денежные средства по указанному договору мне не передавались ни наличным, ни безналичным путем.

            В соответствии со ст. 812 ГК РФ заемщик вправе оспаривать договор займа по его безденежности, доказывая, что деньги или другие вещи в действительности не получены им от займодавца или получены в меньшем количестве, чем указано в договоре.

            Поскольку реальной передачи денежных средств не состоялось, на основании п. 3 ст. 812 ГК РФ договор займа считается незаключенным.

            На основании изложенного, ТРЕБУЮ:
            1. Признать договор займа № ${debt.contractDate} незаключенным.
            2. Аннулировать незаконно начисленную задолженность.
            3. Удалить недостоверные сведения из моей кредитной истории (БКИ).

            В случае отказа я буду вынужден обратиться с иском в суд, а также направить жалобы в Центральный Банк РФ и Прокуратуру.

            Дата: ______________  Подпись: ______________
        """.trimIndent()
    }

    fun generateLackOfFundsPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }

        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."

        drawText("Руководителю: $creditor")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ПРЕТЕНЗИЯ")
        drawText("об оспаривании договора займа по безденежности (ст. 812 ГК РФ)")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = listOf(
            "Мною якобы был заключен договор займа/кредита № $contract",
            "с $creditor.",
            "",
            "Однако заявляю, что фактически денежные средства по указанному",
            "договору мне не передавались ни наличным, ни безналичным путем.",
            "",
            "В соответствии со ст. 812 ГК РФ заемщик вправе оспаривать договор",
            "займа по его безденежности, доказывая, что деньги или другие вещи",
            "в действительности не получены им от займодавца или получены в",
            "меньшем количестве, чем указано в договоре.",
            "",
            "Поскольку реальной передачи денежных средств не состоялось, на",
            "основании п. 3 ст. 812 ГК РФ договор займа считается незаключенным.",
            "",
            "На основании изложенного, ТРЕБУЮ:",
            "1. Признать договор займа № $contract незаключенным.",
            "2. Аннулировать незаконно начисленную задолженность.",
            "3. Удалить недостоверные сведения из моей кредитной истории (БКИ).",
            "",
            "В случае отказа я буду вынужден обратиться с иском в суд, а также",
            "направить жалобы в Центральный Банк РФ и Прокуратуру."
        )
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Bezdeneshnost_${debt?.id ?: "Blank"}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateRestructuringPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        var y = 50f
        val margin = 50f
        val lineHeight = 20f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."
        val fact = debt?.factAmount?.let { "$it руб." } ?: "___________ руб."

        drawText("Руководителю: $creditor")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ЗАЯВЛЕНИЕ")
        drawText("о реструктуризации задолженности (кредитных каникулах)")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = listOf(
            "Между мной и вашей организацией был заключен договор № $contract",
            "на сумму $fact.",
            "",
            "В настоящий момент мое финансовое положение существенно ухудшилось",
            "по независящим от меня объективным причинам (снижение дохода, ",
            "потеря работы или временная нетрудоспособность).",
            "",
            "В соответствии со ст. 451 ГК РФ (Существенное изменение обстоятельств),",
            "ст. 6.1-1 ФЗ № 353 «О потребительском кредите (займе)», а также с учетом",
            "рекомендаций Банка России по реструктуризации кредитов,",
            "заемщик имеет право на изменение условий кредитного договора.",
            "",
            "В связи с вышеизложенным, ПРОШУ:",
            "1. Рассмотреть вопрос о реструктуризации моей задолженности,",
            "в частности: уменьшить размер ежемесячного платежа за счет",
            "увеличения общего срока кредитования.",
            "2. Альтернативно: предоставить кредитные каникулы с отсрочкой",
            "выплаты основного долга.",
            "3. Приостановить начисление неустоек, штрафов и пени на период",
            "рассмотрения данного заявления.",
            "",
            "Подтверждающие документы об ухудшении финансового положения",
            "обязуюсь предоставить по вашему запросу."
        )
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")
        
        pdfDocument.finishPage(page)
        
        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Restructurization_${debt?.id ?: "Blank"}.pdf")
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateSavingsPdf(
        context: Context,
        principal: Double,
        penalty: Double,
        limitPercent: Int,
        limitAmount: Double,
        savings: Double
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 16f
        paint.isFakeBoldText = true
        var yPos = 50f
        canvas.drawText("Расчет потенциальной экономии (списание штрафов)", 50f, yPos, paint)
        
        paint.isFakeBoldText = false
        paint.textSize = 12f
        yPos += 40f
        canvas.drawText("Сумма основного долга: " + String.format(Locale.US, "%.2f", principal) + " руб.", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Требуемые штрафы/проценты: " + String.format(Locale.US, "%.2f", penalty) + " руб.", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Общая сумма требований: " + String.format(Locale.US, "%.2f", principal + penalty) + " руб.", 50f, yPos, paint)
        
        yPos += 40f
        paint.isFakeBoldText = true
        canvas.drawText("Применение законного лимита ($limitPercent%)", 50f, yPos, paint)
        paint.isFakeBoldText = false
        
        yPos += 20f
        canvas.drawText("Максимально допустимая сумма: " + String.format(Locale.US, "%.2f", limitAmount) + " руб.", 50f, yPos, paint)
        
        yPos += 30f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        if (savings > 0) {
            paint.color = android.graphics.Color.GREEN
            canvas.drawText("Потенциальная экономия (списание): " + String.format(Locale.US, "%.2f", savings) + " руб.", 50f, yPos, paint)
        } else {
            paint.color = android.graphics.Color.GRAY
            canvas.drawText("Начисления находятся в пределах лимита.", 50f, yPos, paint)
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "savings_calculation.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: IOException) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateLivingWagePreservationPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }

        var y = 45f
        val margin = 50f
        val lineHeight = 18f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName ?: "________________________________"

        drawText("В Отделение судебных приставов (РОСП ФССП России)")
        drawText("Судебному приставу-исполнителю")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес регистрации: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        drawText("Взыскатель: $creditor")
        y += 8f

        paint.textSize = 13f
        paint.isFakeBoldText = true
        drawText("ЗАЯВЛЕНИЕ")
        drawText("о сохранении заработной платы и иных доходов ежемесячно")
        drawText("в размере прожиточного минимума (ФЗ № 229-ФЗ)")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        y += 8f

        val body = listOf(
            "В производстве РОСП ФССП находится исполнительное производство,",
            "возбужденное на основании исполнительного документа в пользу взыскателя: $creditor.",
            "",
            "В соответствии со статьями 4, 69 и 99 Федерального закона от 02.10.2007 № 229-ФЗ",
            "«Об исполнительном производстве», должник-гражданин имеет право на ежемесячное",
            "сохранение заработной платы и иных доходов в размере прожиточного минимума",
            "трудоспособного населения в целом по РФ (или регионального ПМ, если он выше).",
            "",
            "Единственным источником моего дохода является заработная плата / пенсия,",
            "поступающая на банковский счет.",
            "",
            "На основании изложенного, руководствуясь ст. 4, 69, 99 Федерального закона № 229-ФЗ,",
            "",
            "ПРОШУ:",
            "1. Сохранять ежемесячно заработную плату (иные доходы) в размере величины",
            "   прожиточного минимума трудоспособного населения при обращении взыскания.",
            "2. Направить соответствующее постановление в банк, обслуживающий мой счет.",
            "3. Реквизиты счета для сохранения дохода: Банк: ______________________,",
            "   Номер расчетного счета: __________________________________________.",
            "",
            "Приложение:",
            "1. Справка о доходах (2-НДФЛ / справка из СФР).",
            "2. Выписка по банковскому счету с реквизитами.",
            "",
            "Дата: «___» ___________ 2026 г.                 Подпись: _________ / $userName"
        )

        for (line in body) {
            drawText(line)
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Living_Wage_${debt?.id ?: "Blank"}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateInstallmentPlanPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }

        var y = 45f
        val margin = 50f
        val lineHeight = 18f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val creditor = debt?.creditorName ?: "________________________________"
        val fact = debt?.factAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "___________"

        drawText("В _____________________________ районный (городской) суд")
        drawText("Судье: ______________________________________________")
        drawText("Должник: $userName")
        drawText("Взыскатель: $creditor")
        drawText("Дело №: _____________________")
        y += 8f

        paint.textSize = 13f
        paint.isFakeBoldText = true
        drawText("ЗАЯВЛЕНИЕ")
        drawText("о предоставлении рассрочки исполнения решения суда (ст. 203 ГПК РФ)")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        y += 8f

        val body = listOf(
            "Решением суда по делу № ____________ с меня в пользу $creditor",
            "была взыскана задолженность в размере $fact руб.",
            "",
            "В настоящее время мое материальное и семейное положение не позволяет",
            "единовременно погасить указанную сумму в полном объеме ввиду существенного",
            "снижения доходов и наличия обязательств по обеспечению жизнедеятельности.",
            "",
            "В силу ст. 203, 434 Гражданского процессуального кодекса РФ, суд, рассмотревший дело,",
            "по заявлениям лиц, участвующих в деле, исходя из имущественного положения сторон",
            "вправе отсрочить или рассрочить исполнение решения суда.",
            "",
            "Я намерен(а) добросовестно и стабильно исполнять решение суда равными частями.",
            "",
            "ПРОШУ:",
            "1. Предоставить рассрочку исполнения решения суда по делу № ____________",
            "   сроком на ________ месяцев с ежемесячной выплатой по _________ рублей",
            "   не позднее _____ числа каждого месяца.",
            "2. Приостановить принудительные меры взыскания ФССП на период рассрочки.",
            "",
            "Приложение: документы, подтверждающие доход и тяжелое имущественное положение.",
            "",
            "Дата: «___» ___________ 2026 г.                 Подпись: _________ / $userName"
        )

        for (line in body) {
            drawText(line)
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Installment_Plan_${debt?.id ?: "Blank"}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateStatuteOfLimitationsPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }

        var y = 45f
        val margin = 50f
        val lineHeight = 18f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val creditor = debt?.creditorName ?: "________________________________"
        val contract = debt?.contractDate ?: "___ от «___» ________ 20___ г."

        drawText("В _____________________________ суд")
        drawText("Истец: $creditor")
        drawText("Ответчик: $userName")
        drawText("Гражданское дело №: _____________________")
        y += 8f

        paint.textSize = 13f
        paint.isFakeBoldText = true
        drawText("ХОДАТАЙСТВО")
        drawText("о применении срока исковой давности (ст. 196, 199 ГК РФ)")
        drawText("и снижении неустойки (ст. 333 ГК РФ)")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        y += 8f

        val body = listOf(
            "В производстве суда находится гражданское дело по иску $creditor",
            "о взыскании задолженности по кредитному договору № $contract.",
            "",
            "1. О применении срока исковой давности (ст. 196, 199, 200 ГК РФ):",
            "В силу п. 1 ст. 196 ГК РФ общий срок исковой давности составляет 3 года.",
            "Согласно разъяснениям Пленума Верховного Суда РФ № 43, по обязательствам с графиком",
            "платежей исковая давность исчисляется отдельно по каждому платежу.",
            "Все платежи, срок уплаты которых наступил более чем за 3 года до даты обращения",
            "истца в суд, находятся за пределами срока исковой давности и не подлежат взысканию.",
            "",
            "2. О снижении неустойки (ст. 333 ГК РФ):",
            "Заявленная истцом неустойка носит явно карательный характер и несоразмерна",
            "последствиям нарушения обязательства. Прошу снизить размер пени до ставки ЦБ РФ.",
            "",
            "ПРОШУ СУД:",
            "1. Применить последствия пропуска срока исковой давности и отказать истцу",
            "   в удовлетворении требований за период, превышающий 3 года.",
            "2. На основании ст. 333 ГК РФ снизить заявленные неустойки и штрафы.",
            "",
            "Дата: «___» ___________ 2026 г.                 Подпись: _________ / $userName"
        )

        for (line in body) {
            drawText(line)
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Limitation_333_${debt?.id ?: "Blank"}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateMortgageShieldSettlementPdf(context: Context, debt: Debt? = null): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
        }

        var y = 45f
        val margin = 50f
        val lineHeight = 18f

        fun drawText(text: String) {
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val creditor = debt?.creditorName ?: "________________________________ (Банк-залогодержатель)"

        drawText("В Арбитражный суд ____________________________")
        drawText("По делу о банкротстве №: ______________________")
        drawText("Должник: $userName")
        drawText("Кредитор (залогодержатель): $creditor")
        y += 8f

        paint.textSize = 13f
        paint.isFakeBoldText = true
        drawText("ПРОЕКТ МИРОВОГО СОГЛАШЕНИЯ")
        drawText("в отношении единственного ипотечного жилья (ст. 213.10-1 ФЗ № 127-ФЗ)")
        paint.textSize = 11f
        paint.isFakeBoldText = false
        y += 8f

        val body = listOf(
            "В соответствии со статьей 213.10-1 Федерального закона от 26.10.2002 № 127-ФЗ",
            "«О несостоятельности (банкротстве)», гражданин-должник и конкурсный кредитор,",
            "требования которого обеспечены ипотекой единственного пригодного для проживания",
            "жилого помещения, вправе заключить отдельное мировое соглашение.",
            "",
            "УСЛОВИЯ СОГЛАШЕНИЯ:",
            "1. Жилое помещение (квартира), расположенная по адресу: ______________________,",
            "   исключается из конкурсной массы и не подлежит реализации на торгах.",
            "2. Обязательства по договору ипотеки № ____________ от ___________ продолжают",
            "   исполняться должником (или третьим лицом) в соответствии с первоначальным графиком.",
            "3. Залоговые требования $creditor не подлежат удовлетворению за счет",
            "   иного имущества гражданина в рамках процедуры банкротства.",
            "4. Все остальные необеспеченные кредиты гражданина подлежат списанию судом.",
            "",
            "ПРОСИМ СУД: Утвердить отдельное мировое соглашение по ст. 213.10-1 ФЗ № 127-ФЗ.",
            "",
            "Должник: _________ / $userName           Представитель банка: _________ / _____________"
        )

        for (line in body) {
            drawText(line)
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Mortgage_Shield_${debt?.id ?: "Blank"}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun generateLegalStrategiesMemoPdf(context: Context): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
        }

        var y = 40f
        val margin = 45f
        val lineHeight = 16f

        fun drawText(text: String, bold: Boolean = false, size: Float = 10.5f) {
            paint.isFakeBoldText = bold
            paint.textSize = size
            canvas.drawText(text, margin, y, paint)
            y += lineHeight
        }

        drawText("ЮРИДИЧЕСКИЙ ПОМОЩНИК: ПРАВОВАЯ БАЗА И 5 СТРАТЕГИЙ ЗАЩИТЫ", bold = true, size = 13f)
        drawText("Актуально на 2026 год • Защита прав заемщиков и сохранение имущества", bold = false, size = 10f)
        y += 6f

        drawText("1. СТРАТЕГИЯ ДОСУДЕБНОЙ ОПТИМИЗАЦИИ (Срез переплат и тишина)", bold = true, size = 11f)
        drawText("• ч. 7 ст. 4 ФЗ № 230-ФЗ: Запрет звонков родственникам и работодателю. Отзыв подается в любой момент.")
        drawText("• ст. 8 ФЗ № 230-ФЗ: Полный отказ от взаимодействия с самим должником (подается через 4 месяца).")
        drawText("• ч. 23 ст. 5 ФЗ № 353-ФЗ: Предельный потолок переплаты МФО — не более 1.3x (130%) от тела займа.")
        drawText("• ст. 958 ГК РФ / 353-ФЗ: Возврат навязанных страховок в 30-дневный период охлаждения или при погашении.")
        y += 6f

        drawText("2. СТРАТЕГИЯ СУДЕБНОЙ ЗАЩИТЫ (Активная процессуальная оборона)", bold = true, size = 11f)
        drawText("• ст. 128, 129 ГПК РФ: Безусловная отмена судебного приказа за 10 рабочих дней с момента получения.")
        drawText("• ст. 333 ГК РФ: Снижение несоразмерных неустоек, пеней и штрафов до уровня ключевой ставки Банка России.")
        drawText("• ст. 196, 200 ГК РФ: Срок исковой давности 3 года — исчисляется отдельно по каждому платежу графика.")
        y += 6f

        drawText("3. СТРАТЕГИЯ КОНТРОЛЯ ИСПОЛНИТЕЛЬНОГО ПРОИЗВОДСТВА (ФССП)", bold = true, size = 11f)
        drawText("• ст. 4, 69 ФЗ № 229-ФЗ: Неприкосновенность прожиточного минимума гражданина ежемесячно.")
        drawText("• ст. 203 ГПК РФ: Судебная рассрочка исполнения решения суда комфортными ежемесячными долями.")
        drawText("• ст. 446 ГПК РФ: Абсолютный имущественный иммунитет на единственное пригодное для жизни жилье.")
        y += 6f

        drawText("4. СТРАТЕГИЯ БЕСПЛАТНОГО ВНЕСУДЕБНОГО СПИСАНИЯ В МФЦ", bold = true, size = 11f)
        drawText("• Размер задолженности: от 25 000 до 1 000 000 рублей без суда, госпошлин и управляющих.")
        drawText("• Доступно: при закрытом ИП по п.4 ч.1 ст.46 229-ФЗ; пенсионерам и получателям пособий (ИП > 1 года).")
        y += 6f

        drawText("5. СТРАТЕГИЯ СОХРАНЕНИЯ ЕДИНСТВЕННОЙ ИПОТЕКИ (ст. 213.10-1 ФЗ № 127-ФЗ)", bold = true, size = 11f)
        drawText("• Локальное мировое соглашение: ипотечная квартира исключается из торгов и сохраняется за семьей,")
        drawText("  а все остальные необеспеченные долги (карты, потребительские кредиты, микрозаймы) списываются.")
        y += 8f

        drawText("ПРОТОКОЛ «ИНКОГНИТО»: ПОДКЛЮЧЕНИЕ СЕРВИСА ЗАКАЗНОЕ.ПОЧТА.РУ", bold = true, size = 11f)
        drawText("Для предотвращения поступления повесток и писем в домашний почтовый ящик подключите")
        drawText("электронные заказные письма через Госуслуги (Заказное.Почта.ру) — письма станут конфиденциальными.")

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Pamyatka_Strategii_2026.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun saveToDownloads(context: Context, uri: Uri, displayFileName: String): String? {
        return try {
            val resolver = context.contentResolver
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayFileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, if (displayFileName.endsWith(".csv")) "text/csv" else "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/ЮрПомощник")
                }
                val destinationUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (destinationUri != null) {
                    resolver.openInputStream(uri)?.use { input ->
                        resolver.openOutputStream(destinationUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    "Папка Загрузки/ЮрПомощник/$displayFileName"
                } else null
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val subDir = File(downloadsDir, "ЮрПомощник")
                if (!subDir.exists()) subDir.mkdirs()
                val destFile = File(subDir, displayFileName)
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateAnalysisReportPdf(context: Context, analysisText: String): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50f
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        val textPaint = android.text.TextPaint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }
        val titlePaint = android.text.TextPaint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val contentWidth = (pageWidth - 2 * margin).toInt()
        val lines = analysisText.split("\n")
        var currentY = 50f
        
        canvas.drawText("ОТЧЕТ ПО ПРАВОВОМУ АНАЛИЗУ", margin, currentY, titlePaint)
        currentY += 20f
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Дата генерации: ${sdf.format(Date())}", margin, currentY, textPaint)
        currentY += 30f
        
        for (paragraph in lines) {
            if (paragraph.isBlank()) {
                currentY += textPaint.textSize
                continue
            }
            val staticLayout = android.text.StaticLayout.Builder.obtain(paragraph, 0, paragraph.length, textPaint, contentWidth)
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0.0f, 1.0f)
                .setIncludePad(false)
                .build()
            if (currentY + staticLayout.height > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin
            }
            canvas.save()
            canvas.translate(margin, currentY)
            staticLayout.draw(canvas)
            canvas.restore()
            currentY += staticLayout.height + 5f
        }

        pdfDocument.finishPage(page)
        val file = File(context.cacheDir, "Analysis_Report_${System.currentTimeMillis()}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun openPdf(context: Context, uri: Uri) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Открыть документ"))
    }

    fun sharePdf(context: Context, uri: Uri, title: String = "Поделиться документом") {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = if (uri.toString().endsWith(".csv")) "text/csv" else "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, title))
    }
}
