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

    fun generateObjectionPdf(context: Context, debt: Debt): Uri? {
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

        drawText("Мировому судье судебного участка №___")
        drawText("От должника: $userName")
        if (userAddress.isNotBlank()) drawText("Адрес: $userAddress")
        if (userPhone.isNotBlank()) drawText("Телефон: $userPhone")
        drawText("Взыскатель: ${debt.creditorName}")
        y += lineHeight
        paint.textSize = 14f
        paint.isFakeBoldText = true
        drawText("ВОЗРАЖЕНИЕ НА ИСПОЛНЕНИЕ СУДЕБНОГО ПРИКАЗА")
        paint.textSize = 12f
        paint.isFakeBoldText = false
        y += lineHeight
        
        val body = mutableListOf(
            "Мною был получен судебный приказ о взыскании задолженности",
            "в пользу ${debt.creditorName} по договору ${debt.contractDate}.",
            "Требуемая сумма взыскания (ФАКТ): ${debt.factAmount} руб.",
            "",
            "С вынесенным судебным приказом не согласен, так как не согласен",
            "с суммой долга и начисленными штрафами."
        )

        if (debt.penaltyAmount > 0) {
            body.add("На основании ст. 333 ГК РФ сумма неустойки несоразмерна")
            body.add("последствиям нарушения обязательства и подлежит снижению.")
        }
        if (debt.insuranceAmount > 0) {
            body.add("На основании ст. 958 ГК РФ требую исключить из суммы взыскания")
            body.add("навязанные страховые премии в размере ${debt.insuranceAmount} руб.")
        }

        body.addAll(listOf(
            "Обоснованная законная сумма долга (ПЛАН): ${debt.planAmount} руб.",
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
        val file = File(docsDir, "Vozrazhenie_${debt.id}.pdf")
        
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

    fun generateAntiCollectorPdf(context: Context, debt: Debt): Uri? {
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

        drawText("Руководителю: ${debt.creditorName}")
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
            "Мною был заключен договор займа/кредита № ${debt.contractDate}",
            "с ${debt.creditorName}.",
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
        val file = File(docsDir, "Otziv_Soglasiya_${debt.id}.pdf")
        
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

    fun generateRefusalOfInteractionPdf(context: Context, debt: Debt): Uri? {
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

        drawText("Руководителю: ${debt.creditorName}")
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
            "Мною был заключен договор займа/кредита № ${debt.contractDate}",
            "с ${debt.creditorName}.",
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
        val file = File(docsDir, "Otkaz_Ot_Vzaimodeistviya_${debt.id}.pdf")
        
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

    fun generateInsuranceReturnPdf(context: Context, debt: Debt): Uri? {
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

        drawText("Руководителю страховой компании / Банка:")
        drawText("${debt.creditorName}")
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
            "Мною был заключен договор займа/кредита № ${debt.contractDate}",
            "с ${debt.creditorName}. Одновременно с кредитным договором,",
            "мне была навязана услуга личного страхования.",
            "",
            "Сумма удержанной (оплаченной) страховой премии: ${debt.insuranceAmount} руб.",
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
            "2. Возвратить мне уплаченную страховую премию в размере ${debt.insuranceAmount} руб.",
            "по указанным мною реквизитам в установленный законом срок."
        )
        
        body.forEach { drawText(it) }
        
        y += lineHeight * 2
        drawText("Дата: ______________  Подпись: ______________")

        pdfDocument.finishPage(page)

        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, "Vozvrat_Strahovki_${debt.id}.pdf")
        
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

    fun generateLackOfFundsPdf(context: Context, debt: Debt): Uri? {
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

        drawText("Руководителю: ${debt.creditorName}")
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
            "Мною якобы был заключен договор займа/кредита № ${debt.contractDate}",
            "с ${debt.creditorName}.",
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
            "1. Признать договор займа № ${debt.contractDate} незаключенным.",
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
        val file = File(docsDir, "Bezdeneshnost_${debt.id}.pdf")
        
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

    fun generateRestructuringPdf(context: Context, debt: Debt): Uri? {
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

        drawText("Руководителю: ${debt.creditorName}")
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
            "Между мной и вашей организацией был заключен договор № ${debt.contractDate}",
            "от ${debt.contractDate} (на сумму ${debt.factAmount} руб.).",
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
        val file = File(docsDir, "Restructurization_${debt.id}.pdf")
        
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
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IOException) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
