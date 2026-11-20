package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.Debt
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentGenerator {

    private const val PORTRAIT_WIDTH = 595
    private const val PORTRAIT_HEIGHT = 842
    private const val LANDSCAPE_WIDTH = 842
    private const val LANDSCAPE_HEIGHT = 595
    private const val DEFAULT_MARGIN = 50f

    /**
     * Multi-page A4 portrait writer backed by StaticLayout so Cyrillic text wraps cleanly
     * inside margins and never clips horizontally or overflows past the bottom margin.
     */
    private class PagedPdfWriter(
        private val pageWidth: Int = PORTRAIT_WIDTH,
        private val pageHeight: Int = PORTRAIT_HEIGHT,
        private val margin: Float = DEFAULT_MARGIN
    ) {
        val pdfDocument = PdfDocument()
        private var pageNumber = 1
        private var page: PdfDocument.Page = startNewPage(pageNumber)
        private var canvas = page.canvas
        private var currentY = margin
        private val contentWidth = (pageWidth - 2 * margin).toInt().coerceAtLeast(100)

        private fun startNewPage(num: Int): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, num).create()
            return pdfDocument.startPage(info)
        }

        private fun ensureSpace(neededHeight: Float) {
            if (currentY > margin && currentY + neededHeight > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                page = startNewPage(pageNumber)
                canvas = page.canvas
                currentY = margin
            }
        }

        fun addSpace(points: Float) {
            currentY += points
        }

        fun addParagraph(
            text: String,
            textSize: Float = 11.5f,
            bold: Boolean = false,
            color: Int = Color.BLACK,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
            spacingAfter: Float = 6f
        ) {
            if (text.isEmpty()) {
                addSpace(textSize * 0.7f)
                return
            }
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.textSize = textSize
                this.isFakeBoldText = bold
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(alignment)
                .setLineSpacing(2f, 1.1f)
                .setIncludePad(false)
                .build()

            val minLineHeight = textSize * 1.35f
            for (i in 0 until layout.lineCount) {
                val lineHeight = (layout.getLineBottom(i) - layout.getLineTop(i))
                    .toFloat()
                    .coerceAtLeast(minLineHeight)
                ensureSpace(lineHeight)
                val lineText = text.substring(layout.getLineStart(i), layout.getLineEnd(i))
                    .trimEnd('\n', '\r')
                val x = when (alignment) {
                    Layout.Alignment.ALIGN_CENTER -> {
                        val w = paint.measureText(lineText)
                        margin + ((contentWidth - w) / 2f).coerceAtLeast(0f)
                    }
                    Layout.Alignment.ALIGN_OPPOSITE -> {
                        val w = paint.measureText(lineText)
                        margin + (contentWidth - w).coerceAtLeast(0f)
                    }
                    else -> margin
                }
                val baseline = currentY - paint.ascent()
                canvas.drawText(lineText, x, baseline, paint)
                currentY += lineHeight
            }
            currentY += spacingAfter
        }

        fun saveToDocsCache(context: Context, fileName: String): Uri? {
            pdfDocument.finishPage(page)
            return writePdfToDocsDir(context, pdfDocument, fileName)
        }
    }

    private fun safeFileSuffix(rawId: String?): String {
        val cleaned = rawId?.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_")?.take(48).orEmpty()
        return cleaned.ifBlank { "Blank" }
    }

    private fun writePdfToDocsDir(context: Context, pdfDocument: PdfDocument, fileName: String): Uri? {
        val docsDir = File(context.cacheDir, "docs")
        if (!docsDir.exists()) docsDir.mkdirs()
        val file = File(docsDir, fileName)
        return try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun generateRegistryCsv(context: Context, debts: List<Debt>): Uri? {
        val stringBuilder = StringBuilder()
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
            FileOutputStream(file).use { out ->
                // UTF-8 BOM for Excel compatibility
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(stringBuilder.toString().toByteArray(Charsets.UTF_8))
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    fun generateRegistryPdf(context: Context, debts: List<Debt>): Uri? {
        val pdfDocument = PdfDocument()
        var pageNum = 1
        val startX = 40f
        val rowHeight = 24f
        val cols = floatArrayOf(
            startX,
            startX + 28f,
            startX + 180f,
            startX + 265f,
            startX + 345f,
            startX + 425f,
            startX + 505f,
            startX + 645f,
            startX + 762f
        )
        val headers = arrayOf("№", "Кредитор", "Договор", "Требуют", "К отмене", "Законный", "Основание", "Статус")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f; isFakeBoldText = true }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 14f; isFakeBoldText = true }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.7f }

        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT, pageNum).create())
        var canvas = page.canvas
        var y = 40f

        fun drawTableHeader(includeTitle: Boolean) {
            y = 40f
            if (includeTitle) {
                canvas.drawText("РЕЕСТР ЗАДОЛЖЕННОСТЕЙ (ПЛАН/ФАКТ)", startX, y, titlePaint)
                y += 18f
                val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                canvas.drawText("Дата формирования: $currentDate", startX, y, paint)
                y += 26f
            }
            for (i in headers.indices) {
                canvas.drawText(headers[i], cols[i] + 4f, y, boldPaint)
            }
            y += 6f
            canvas.drawLine(startX, y, cols[8], y, linePaint)
            y += rowHeight - 6f
        }

        fun clipCell(text: String, maxChars: Int): String {
            return if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
        }

        drawTableHeader(includeTitle = true)

        var totalFact = 0.0
        var totalCancel = 0.0
        var totalPlan = 0.0

        debts.forEachIndexed { index, debt ->
            if (y + rowHeight > LANDSCAPE_HEIGHT - 45f) {
                pdfDocument.finishPage(page)
                pageNum++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT, pageNum).create())
                canvas = page.canvas
                drawTableHeader(includeTitle = false)
            }

            val toCancel = debt.penaltyAmount + debt.insuranceAmount
            totalFact += debt.factAmount
            totalCancel += toCancel
            totalPlan += debt.planAmount

            val bases = mutableListOf<String>()
            if (debt.isMortgage) bases.add("ст.213.10-1")
            if (debt.penaltyAmount > 0) bases.add("ст.333 ГК")
            if (debt.insuranceAmount > 0) bases.add("ст.958 ГК")
            val basisText = if (bases.isNotEmpty()) bases.joinToString(", ") else "ст.809 ГК"

            canvas.drawText("${index + 1}", cols[0] + 4f, y, paint)
            canvas.drawText(clipCell(debt.creditorName, 24), cols[1] + 4f, y, paint)
            canvas.drawText(clipCell(debt.contractDate, 14), cols[2] + 4f, y, paint)
            canvas.drawText(String.format(Locale.US, "%,.2f", debt.factAmount), cols[3] + 4f, y, paint)
            canvas.drawText(String.format(Locale.US, "%,.2f", toCancel), cols[4] + 4f, y, paint)
            canvas.drawText(String.format(Locale.US, "%,.2f", debt.planAmount), cols[5] + 4f, y, boldPaint)
            canvas.drawText(clipCell(basisText, 24), cols[6] + 4f, y, paint)
            canvas.drawText(clipCell(debt.status, 18), cols[7] + 4f, y, paint)

            y += 6f
            canvas.drawLine(startX, y, cols[8], y, linePaint)
            y += rowHeight - 6f
        }

        if (y + 24f > LANDSCAPE_HEIGHT - 35f) {
            pdfDocument.finishPage(page)
            pageNum++
            page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(LANDSCAPE_WIDTH, LANDSCAPE_HEIGHT, pageNum).create())
            canvas = page.canvas
            y = 50f
        }
        y += 4f
        canvas.drawText("ИТОГО:", cols[2] + 4f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", totalFact), cols[3] + 4f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", totalCancel), cols[4] + 4f, y, boldPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", totalPlan), cols[5] + 4f, y, boldPaint)

        pdfDocument.finishPage(page)
        return writePdfToDocsDir(context, pdfDocument, "Registry_Plan_Fact.pdf")
    }

    fun generateObjectionPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."

        writer.addParagraph("Мировому судье судебного участка № ___")
        writer.addParagraph("От должника: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addParagraph("Взыскатель: $creditor")
        writer.addSpace(14f)

        writer.addParagraph(
            "ВОЗРАЖЕНИЕ НА ИСПОЛНЕНИЕ СУДЕБНОГО ПРИКАЗА",
            textSize = 13.5f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 14f
        )

        writer.addParagraph(
            "Мною «___» ____________ 20___ г. получена копия судебного приказа по делу № ____________ о взыскании задолженности в пользу $creditor по договору $contract."
        )
        writer.addParagraph(
            "С вынесенным судебным приказом не согласен(на), возражаю относительно его исполнения в полном объеме. На основании ст. 128, 129 ГПК РФ и п. 31 Постановления Пленума ВС РФ № 62 от 27.12.2016 указание конкретных мотивов несогласия не требуется; признание долга или его части не заявляется (во избежание перерыва срока исковой давности по ст. 203 ГК РФ)."
        )
        writer.addParagraph("В соответствии со ст. 128, 129 Гражданского процессуального кодекса РФ,")
        writer.addParagraph("ПРОШУ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Отменить судебный приказ по делу № ____________ о взыскании задолженности.")
        writer.addParagraph("2. Копию определения об отмене судебного приказа направить в мой адрес.")
        writer.addSpace(20f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: ______________ / $userName")

        return writer.saveToDocsCache(context, "Vozrazhenie_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateAntiCollectorPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."

        writer.addParagraph("Руководителю: $creditor")
        writer.addParagraph("От должника: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addSpace(14f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ\nоб отзыве согласия на взаимодействие с третьими лицами",
            textSize = 13.5f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 14f
        )

        writer.addParagraph("Мною был заключен договор займа/кредита № $contract с $creditor.")
        writer.addParagraph(
            "В соответствии с ч. 7 ст. 4 Федерального закона от 03.07.2016 № 230-ФЗ «О защите прав и законных интересов физических лиц при осуществлении деятельности по возврату просроченной задолженности» настоящим заявлением я отзываю свое согласие на осуществление направленного на возврат просроченной задолженности взаимодействия с любыми третьими лицами (членами семьи, родственниками, коллегами, соседями и иными физическими лицами)."
        )
        writer.addParagraph(
            "Требую с момента получения данного заявления прекратить любые звонки, отправку сообщений и иные способы взаимодействия с указанными третьими лицами."
        )
        writer.addParagraph(
            "В случае нарушения моего требования буду вынужден(а) обратиться с жалобой в Федеральную службу судебных приставов (ФССП России) и Банк России для привлечения к административной ответственности (ст. 14.57 КоАП РФ)."
        )
        writer.addSpace(20f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: ______________ / $userName")

        return writer.saveToDocsCache(context, "Otziv_Soglasiya_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateRefusalOfInteractionPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."

        writer.addParagraph("Руководителю: $creditor")
        writer.addParagraph("От должника: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addSpace(14f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ\nоб отказе от взаимодействия (ст. 8 ФЗ № 230-ФЗ)",
            textSize = 13.5f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 14f
        )

        writer.addParagraph("Мною был заключен договор займа/кредита № $contract с $creditor.")
        writer.addParagraph(
            "Учитывая, что с момента возникновения просроченной задолженности прошло более 4 (четырех) месяцев, в соответствии со ст. 8 Федерального закона от 03.07.2016 № 230-ФЗ настоящим заявлением я отказываюсь от взаимодействия с кредитором и/или лицом, действующим от его имени, способами, предусмотренными пп. 1, 2 ч. 1 ст. 4 ФЗ № 230-ФЗ (личные встречи, телефонные переговоры, телеграфные, текстовые и голосовые сообщения)."
        )
        writer.addParagraph(
            "Примечание: перед отправкой сверьте реквизиты с утвержденной ФССП России формой заявления и направьте заказным письмом с уведомлением о вручении или через нотариуса (ч. 2–4 ст. 8 ФЗ № 230-ФЗ). Взаимодействие почтовыми отправлениями сохраняется в пределах закона."
        )
        writer.addParagraph(
            "В случае нарушения данного требования буду вынужден(а) обратиться с жалобой в ФССП России и Банк России (ст. 14.57 КоАП РФ)."
        )
        writer.addSpace(20f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: ______________ / $userName")

        return writer.saveToDocsCache(context, "Otkaz_Ot_Vzaimodeistviya_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateInsuranceReturnPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."
        val insurance = debt?.insuranceAmount?.let { String.format(Locale.US, "%.2f руб.", it) } ?: "___________ руб."

        writer.addParagraph("Руководителю страховой компании / Банка: $creditor")
        writer.addParagraph("От заявителя: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addSpace(14f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ\nоб отказе от договора страхования и возврате страховой премии",
            textSize = 13.5f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 14f
        )

        writer.addParagraph(
            "Мною был заключен договор займа/кредита № $contract с $creditor. Одновременно с кредитным договором был оформлен договор личного страхования / дополнительная услуга."
        )
        writer.addParagraph("Сумма удержанной (оплаченной) страховой премии: $insurance", bold = true)
        writer.addParagraph(
            "В соответствии с ч. 2.7–2.9 ст. 7, ч. 10–12 ст. 11 Федерального закона от 21.12.2013 № 353-ФЗ «О потребительском кредите (займе)», Указанием Банка России № 3854-У и ст. 958 ГК РФ заемщик вправе отказаться от договора страхования/допуслуг в течение «периода охлаждения» (30 календарных дней при кредите / 14 дней по отдельному полису) либо потребовать пропорциональный возврат части премии при досрочном полном погашении кредита."
        )
        writer.addParagraph("Настоящим заявляю об одностороннем отказе от договора страхования и ТРЕБУЮ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Расторгнуть договор страхования.")
        writer.addParagraph("2. Возвратить уплаченную страховую премию (ее часть) в размере $insurance по указанным ниже банковским реквизитам в установленный законом срок.")
        writer.addParagraph("Реквизиты счета для возврата: Банк __________________________, р/с ______________________________, БИК ______________.")
        writer.addSpace(20f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: ______________ / $userName")

        return writer.saveToDocsCache(context, "Vozvrat_Strahovki_${safeFileSuffix(debt?.id)}.pdf")
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

            В случае отказа я буду вынужден(а) обратиться с иском в суд, а также направить жалобы в Банк России и Прокуратуру.

            Дата: «___» ___________ 20___ г.  Подпись: ______________ / $userName
        """.trimIndent()
    }

    fun generateLackOfFundsPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."

        writer.addParagraph("Руководителю: $creditor")
        writer.addParagraph("От должника: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addSpace(14f)

        writer.addParagraph(
            "ПРЕТЕНЗИЯ\nоб оспаривании договора займа по безденежности (ст. 812 ГК РФ)",
            textSize = 13.5f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 14f
        )

        writer.addParagraph("Мною якобы был заключен договор займа/кредита № $contract с $creditor.")
        writer.addParagraph("Однако заявляю, что фактически денежные средства по указанному договору мне не передавались ни наличным, ни безналичным путем.")
        writer.addParagraph(
            "В соответствии со ст. 812 ГК РФ заемщик вправе оспаривать договор займа по его безденежности, доказывая, что деньги или другие вещи в действительности не получены им от займодавца или получены в меньшем количестве, чем указано в договоре."
        )
        writer.addParagraph("Поскольку реальной передачи денежных средств не состоялось, на основании п. 3 ст. 812 ГК РФ договор займа считается незаключенным.")
        writer.addParagraph("На основании изложенного, ТРЕБУЮ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Признать договор займа № $contract незаключенным.")
        writer.addParagraph("2. Аннулировать незаконно начисленную задолженность.")
        writer.addParagraph("3. Удалить недостоверные сведения из моей кредитной истории (БКИ).")
        writer.addParagraph("В случае отказа я буду вынужден(а) обратиться с иском в суд, а также направить жалобы в Банк России и Прокуратуру.")
        writer.addSpace(20f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: ______________ / $userName")

        return writer.saveToDocsCache(context, "Bezdeneshnost_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateRestructuringPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_______________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."
        val fact = debt?.factAmount?.let { String.format(Locale.US, "%.2f руб.", it) } ?: "___________ руб."

        writer.addParagraph("Руководителю: $creditor")
        writer.addParagraph("От должника: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addSpace(14f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ\nо реструктуризации задолженности (кредитных каникулах)",
            textSize = 13.5f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 14f
        )

        writer.addParagraph("Между мной и вашей организацией был заключен договор № $contract на сумму $fact.")
        writer.addParagraph(
            "В настоящий момент мое финансовое положение существенно ухудшилось по независящим от меня объективным причинам (снижение дохода, потеря работы или временная нетрудоспособность)."
        )
        writer.addParagraph(
            "В соответствии со ст. 6.1-2 ФЗ № 353-ФЗ «О потребительском кредите (займе)» (для ипотечных договоров — ст. 6.1-1 ФЗ № 353-ФЗ), а также ст. 451 ГК РФ и рекомендациями Банка России по реструктуризации задолженности, заемщик вправе обратиться за предоставлением кредитных каникул или реструктуризацией."
        )
        writer.addParagraph("В связи с вышеизложенным, ПРОШУ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Рассмотреть вопрос о реструктуризации задолженности (уменьшить размер ежемесячного платежа за счет увеличения срока кредитования).")
        writer.addParagraph("2. Альтернативно: предоставить кредитные каникулы с отсрочкой выплаты основного долга при соблюдении условий ст. 6.1-2 (6.1-1) ФЗ № 353-ФЗ.")
        writer.addParagraph("3. Приостановить начисление неустоек, штрафов и пени на период рассмотрения заявления.")
        writer.addParagraph("Подтверждающие документы об ухудшении финансового положения прилагаю / обязуюсь предоставить по запросу.")
        writer.addSpace(20f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: ______________ / $userName")

        return writer.saveToDocsCache(context, "Restructurization_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateSavingsPdf(
        context: Context,
        principal: Double,
        penalty: Double,
        limitPercent: Int,
        limitAmount: Double,
        savings: Double
    ): Uri? {
        val writer = PagedPdfWriter()
        writer.addParagraph(
            "Расчет потенциальной экономии (применение лимита начислений)",
            textSize = 15f,
            bold = true,
            spacingAfter = 14f
        )
        writer.addParagraph("Сумма основного долга: ${String.format(Locale.US, "%.2f", principal)} руб.")
        writer.addParagraph("Требуемые штрафы и проценты: ${String.format(Locale.US, "%.2f", penalty)} руб.")
        writer.addParagraph("Общая сумма требований: ${String.format(Locale.US, "%.2f", principal + penalty)} руб.")
        writer.addSpace(10f)
        writer.addParagraph("Сценарий ограничения ($limitPercent% от суммы основного долга):", bold = true)
        writer.addParagraph("Максимально допустимая сумма: ${String.format(Locale.US, "%.2f", limitAmount)} руб.")
        writer.addSpace(10f)
        if (savings > 0) {
            writer.addParagraph(
                "Потенциальная экономия (к оспариванию/снижению): ${String.format(Locale.US, "%.2f", savings)} руб.",
                textSize = 13f,
                bold = true,
                color = Color.rgb(0, 128, 0)
            )
        } else {
            writer.addParagraph(
                "Начисления находятся в пределах выбранного лимита.",
                textSize = 13f,
                bold = true,
                color = Color.DKGRAY
            )
        }
        writer.addSpace(12f)
        writer.addParagraph(
            "Справочно: расчет носит ориентировочный характер; итоговый перерасчет подлежит проверке по условиям договора и нормам ч. 23 ст. 5 ФЗ № 353-ФЗ (ст. 333 ГК РФ).",
            textSize = 9.5f,
            color = Color.DKGRAY
        )

        return writer.saveToDocsCache(context, "savings_calculation.pdf")
    }

    fun generateLivingWagePreservationPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val userAddress = prefs.getAddress()
        val userPhone = prefs.getPhone()
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"

        writer.addParagraph("В Отделение судебных приставов (РОСП ФССП России)")
        writer.addParagraph("Судебному приставу-исполнителю")
        writer.addParagraph("От должника: $userName")
        if (userAddress.isNotBlank()) writer.addParagraph("Адрес регистрации: $userAddress")
        if (userPhone.isNotBlank()) writer.addParagraph("Телефон: $userPhone")
        writer.addParagraph("Взыскатель: $creditor")
        writer.addSpace(12f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ\nо сохранении заработной платы и иных доходов ежемесячно в размере прожиточного минимума (ФЗ № 229-ФЗ)",
            textSize = 13f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 12f
        )

        writer.addParagraph("В производстве РОСП ФССП находится исполнительное производство № ____________, возбужденное в пользу взыскателя: $creditor.")
        writer.addParagraph(
            "В соответствии со статьями 4, 69 и 99 Федерального закона от 02.10.2007 № 229-ФЗ «Об исполнительном производстве» должник-гражданин имеет право на ежемесячное сохранение заработной платы и иных доходов в размере величины прожиточного минимума трудоспособного населения в целом по РФ (или регионального ПМ, если он выше), за исключением взысканий по алиментам и возмещению вреда (ч. 3.1 ст. 99 ФЗ № 229-ФЗ)."
        )
        writer.addParagraph("Единственным источником моего дохода является заработная плата / пенсия, поступающая на банковский счет.")
        writer.addParagraph("На основании изложенного, руководствуясь ст. 4, 69, 99 Федерального закона № 229-ФЗ, ПРОШУ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Сохранять ежемесячно заработную плату (иные доходы) в размере величины прожиточного минимума при обращении взыскания.")
        writer.addParagraph("2. Направить соответствующее постановление в банк, обслуживающий указанный ниже счет.")
        writer.addParagraph("3. Реквизиты счета (одного счета в одном банке): Банк __________________________, номер расчетного счета __________________________________________.")
        writer.addParagraph("Приложение:", bold = true, spacingAfter = 2f)
        writer.addParagraph("1. Документы о доходах (2-НДФЛ / справка из СФР).")
        writer.addParagraph("2. Выписка по банковскому счету с реквизитами.")
        writer.addSpace(16f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: _________ / $userName")

        return writer.saveToDocsCache(context, "Living_Wage_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateInstallmentPlanPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val fact = debt?.factAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "___________"

        writer.addParagraph("В _____________________________ районный (городской) суд")
        writer.addParagraph("Судье: ______________________________________________")
        writer.addParagraph("Должник: $userName")
        writer.addParagraph("Взыскатель: $creditor")
        writer.addParagraph("Дело №: _____________________")
        writer.addSpace(12f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ\nо предоставлении рассрочки исполнения решения суда (ст. 203 ГПК РФ)",
            textSize = 13f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 12f
        )

        writer.addParagraph("Решением суда по делу № ____________ с меня в пользу $creditor была взыскана задолженность в размере $fact руб.")
        writer.addParagraph(
            "В настоящее время мое материальное и семейное положение не позволяет единовременно погасить указанную сумму в полном объеме ввиду существенного снижения доходов и наличия обязательств по обеспечению жизнедеятельности."
        )
        writer.addParagraph(
            "В силу ст. 203, 434 Гражданского процессуального кодекса РФ суд, рассмотревший дело, по заявлениям лиц, участвующих в деле, исходя из имущественного положения сторон вправе отсрочить или рассрочить исполнение решения суда."
        )
        writer.addParagraph("Я намерен(а) добросовестно и стабильно исполнять решение суда равными частями.")
        writer.addParagraph("ПРОШУ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Предоставить рассрочку исполнения решения суда по делу № ____________ сроком на ________ месяцев с ежемесячной выплатой по _________ рублей не позднее _____ числа каждого месяца.")
        writer.addParagraph("2. Уведомить РОСП ФССП о предоставленной рассрочке в порядке исполнения судебного акта.")
        writer.addParagraph("Приложение: документы, подтверждающие доход, состав семьи и тяжелое имущественное положение.")
        writer.addSpace(16f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: _________ / $userName")

        return writer.saveToDocsCache(context, "Installment_Plan_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateStatuteOfLimitationsPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________"
        val contract = debt?.contractDate?.takeIf { it.isNotBlank() } ?: "___ от «___» ________ 20___ г."

        writer.addParagraph("В _____________________________ суд")
        writer.addParagraph("Истец: $creditor")
        writer.addParagraph("Ответчик: $userName")
        writer.addParagraph("Гражданское дело №: _____________________")
        writer.addSpace(12f)

        writer.addParagraph(
            "ЗАЯВЛЕНИЕ (ХОДАТАЙСТВО)\nо применении срока исковой давности (ст. 196, 199 ГК РФ) и снижении неустойки (ст. 333 ГК РФ)",
            textSize = 13f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 12f
        )

        writer.addParagraph("В производстве суда находится гражданское дело по иску $creditor о взыскании задолженности по договору № $contract. Исковые требования не признаю.")
        writer.addParagraph("1. О применении срока исковой давности (ст. 196, 199, 200 ГК РФ):", bold = true, spacingAfter = 3f)
        writer.addParagraph(
            "В силу п. 1 ст. 196 ГК РФ общий срок исковой давности составляет 3 года. Согласно п. 24 Постановления Пленума Верховного Суда РФ № 43 от 29.09.2015 по обязательствам с графиком платежей срок исковой давности исчисляется отдельно по каждому просроченному платежу. Прошу применить последствия пропуска срока исковой давности к требованиям за пределами трехлетнего срока (с учетом правил приостановления по ст. 204 ГК РФ)."
        )
        writer.addParagraph("2. О снижении неустойки (ст. 333 ГК РФ — альтернативно, в части требований в пределах СИД):", bold = true, spacingAfter = 3f)
        writer.addParagraph(
            "В случае удовлетворения части требований, находящихся в пределах срока исковой давности, прошу на основании ст. 333 ГК РФ и ПП ВС РФ № 7 от 24.03.2016 снизить заявленные истцом неустойки и штрафы как явно несоразмерные последствиям нарушения обязательства."
        )
        writer.addParagraph("ПРОШУ СУД:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Применить последствия пропуска срока исковой давности и отказать истцу в удовлетворении требований за пределами 3-летнего срока.")
        writer.addParagraph("2. На основании ст. 333 ГК РФ снизить размер неустоек и штрафов в оставшейся части.")
        writer.addSpace(16f)
        writer.addParagraph("Дата: «___» ___________ 20___ г.    Подпись: _________ / $userName")

        return writer.saveToDocsCache(context, "Limitation_333_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateMortgageShieldSettlementPdf(context: Context, debt: Debt? = null): Uri? {
        val writer = PagedPdfWriter()
        val prefs = UserPreferences(context)
        val userName = prefs.getName().takeIf { it.isNotBlank() } ?: "_____________________________"
        val creditor = debt?.creditorName?.takeIf { it.isNotBlank() } ?: "________________________________ (Банк-залогодержатель)"

        writer.addParagraph("В Арбитражный суд ____________________________")
        writer.addParagraph("По делу о банкротстве №: ______________________")
        writer.addParagraph("Должник: $userName")
        writer.addParagraph("Кредитор (залогодержатель): $creditor")
        writer.addSpace(12f)

        writer.addParagraph(
            "ПРОЕКТ ОТДЕЛЬНОГО МИРОВОГО СОГЛАШЕНИЯ\nв отношении единственного ипотечного жилья (ст. 213.10-1 ФЗ № 127-ФЗ)",
            textSize = 13f,
            bold = true,
            alignment = Layout.Alignment.ALIGN_CENTER,
            spacingAfter = 12f
        )

        writer.addParagraph(
            "В соответствии со статьей 213.10-1 Федерального закона от 26.10.2002 № 127-ФЗ «О несостоятельности (банкротстве)» гражданин-должник и конкурсный кредитор, требования которого обеспечены ипотекой единственного пригодного для постоянного проживания жилого помещения, вправе заключить отдельное мировое соглашение, подлежащее утверждению арбитражным судом."
        )
        writer.addParagraph("УСЛОВИЯ СОГЛАШЕНИЯ:", bold = true, spacingAfter = 4f)
        writer.addParagraph("1. Жилое помещение, расположенное по адресу: ____________________________________, исключается из конкурсной массы и не подлежит реализации на торгах.")
        writer.addParagraph("2. Обязательства по ипотечному договору № ____________ от ___________ продолжают исполняться должником (или третьим лицом с его согласия) в соответствии с условиями договора и графика платежей.")
        writer.addParagraph("3. Залоговые требования $creditor не подлежат удовлетворению за счет иного имущества гражданина в рамках процедуры банкротства.")
        writer.addParagraph("4. Вопрос об освобождении гражданина от иных обязательств разрешается арбитражным судом в порядке ст. 213.28 ФЗ № 127-ФЗ по итогам процедуры банкротства (с учетом установленных законом исключений).")
        writer.addParagraph("ПРОСИМ СУД: Утвердить настоящее отдельное мировое соглашение на основании ст. 213.10-1 ФЗ № 127-ФЗ.", bold = true)
        writer.addSpace(16f)
        writer.addParagraph("Должник: _________ / $userName")
        writer.addParagraph("Представитель кредитора: _________ / _____________________")

        return writer.saveToDocsCache(context, "Mortgage_Shield_${safeFileSuffix(debt?.id)}.pdf")
    }

    fun generateLegalStrategiesMemoPdf(context: Context): Uri? {
        val writer = PagedPdfWriter(margin = 45f)
        writer.addParagraph(
            "ЮРИДИЧЕСКИЙ ПОМОЩНИК: ПРАВОВАЯ БАЗА И 5 СТРАТЕГИЙ ЗАЩИТЫ",
            textSize = 13f,
            bold = true,
            spacingAfter = 3f
        )
        writer.addParagraph(
            "Справочная памятка • Актуально на 2026 год • Защита прав заемщиков и сохранение имущества",
            textSize = 9.5f,
            color = Color.DKGRAY,
            spacingAfter = 10f
        )

        writer.addParagraph("1. СТРАТЕГИЯ ДОСУДЕБНОЙ ОПТИМИЗАЦИИ (Срез переплат и тишина)", textSize = 11f, bold = true, spacingAfter = 4f)
        writer.addParagraph("• ч. 7 ст. 4 ФЗ № 230-ФЗ: Отзыв согласия на взаимодействие с родственниками и третьими лицами подается в любой момент.", textSize = 10.5f)
        writer.addParagraph("• ст. 8 ФЗ № 230-ФЗ: Отказ от взаимодействия с самим должником (подается не ранее чем через 4 месяца просрочки).", textSize = 10.5f)
        writer.addParagraph("• ч. 23 ст. 5 ФЗ № 353-ФЗ: Лимит переплаты по займам до 1 года — 100% (с 01.04.2026 по ФЗ № 545-ФЗ) / 130% (01.07.2023–31.03.2026).", textSize = 10.5f)
        writer.addParagraph("• ч. 2.7–2.9 ст. 7 ФЗ № 353-ФЗ, ст. 958 ГК РФ: Возврат страховок/допуслуг в период охлаждения (30 дней) или пропорционально при досрочном погашении.", textSize = 10.5f, spacingAfter = 8f)

        writer.addParagraph("2. СТРАТЕГИЯ СУДЕБНОЙ ЗАЩИТЫ (Активная процессуальная оборона)", textSize = 11f, bold = true, spacingAfter = 4f)
        writer.addParagraph("• ст. 128, 129 ГПК РФ: Отмена судебного приказа при подаче возражений в течение 10 календарных дней со дня получения копии.", textSize = 10.5f)
        writer.addParagraph("• ст. 333 ГК РФ: Ходатайство о снижении явно несоразмерной неустойки (право суда, ПП ВС РФ № 7).", textSize = 10.5f)
        writer.addParagraph("• ст. 196, 199, 200 ГК РФ: Срок исковой давности 3 года (применяется по заявлению ответчика, с учетом ст. 202–204 ГК РФ).", textSize = 10.5f, spacingAfter = 8f)

        writer.addParagraph("3. СТРАТЕГИЯ КОНТРОЛЯ ИСПОЛНИТЕЛЬНОГО ПРОИЗВОДСТВА (ФССП)", textSize = 11f, bold = true, spacingAfter = 4f)
        writer.addParagraph("• ст. 4, 69, 99 ФЗ № 229-ФЗ: Сохранение прожиточного минимума на одном банковском счете по заявлению (кроме алиментов и возмещения вреда).", textSize = 10.5f)
        writer.addParagraph("• ст. 203 ГПК РФ: Ходатайство о судебной рассрочке исполнения решения суда (на усмотрение суда).", textSize = 10.5f)
        writer.addParagraph("• ст. 446 ГПК РФ: Имущественный иммунитет единственного жилья (кроме ипотеки и «роскошного жилья» по Постановлению КС РФ № 15-П).", textSize = 10.5f, spacingAfter = 8f)

        writer.addParagraph("4. ВНЕСУДЕБНОЕ БАНКРОТСТВО ЧЕРЕЗ МФЦ (ст. 223.2 ФЗ № 127-ФЗ)", textSize = 11f, bold = true, spacingAfter = 4f)
        writer.addParagraph("• Диапазон долга: от 25 000 до 1 000 000 рублей без госпошлины.", textSize = 10.5f)
        writer.addParagraph("• Основания: оконченное ИП по п. 4 ч. 1 ст. 46; пенсионеры/получатели детских пособий (ИП > 1 года); либо длительное ИП (> 7 лет).", textSize = 10.5f, spacingAfter = 8f)

        writer.addParagraph("5. СОХРАНЕНИЕ ЕДИНСТВЕННОЙ ИПОТЕКИ (ст. 213.10-1 ФЗ № 127-ФЗ)", textSize = 11f, bold = true, spacingAfter = 4f)
        writer.addParagraph("• Отдельное мировое соглашение (с согласия банка-залогодержателя и утверждением арбитражного суда): ипотечная квартира исключается из торгов, а освобождение от иных долгов решается судом по ст. 213.28 ФЗ № 127-ФЗ.", textSize = 10.5f, spacingAfter = 8f)

        writer.addParagraph("ПРОТОКОЛ «ИНКОГНИТО»: ЭЛЕКТРОННЫЕ ЗАКАЗНЫЕ ПИСЬМА", textSize = 11f, bold = true, spacingAfter = 4f)
        writer.addParagraph("Для снижения риска получения судебных извещений посторонними в домашнем почтовом ящике подключите электронные заказные письма через Госуслуги (Госпочта / Заказное.Почта.ру).", textSize = 10.5f)

        return writer.saveToDocsCache(context, "Pamyatka_Strategii_2026.pdf")
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
        } catch (_: Exception) {
            null
        }
    }

    fun generateAnalysisReportPdf(context: Context, analysisText: String): Uri? {
        val writer = PagedPdfWriter()
        writer.addParagraph("ОТЧЕТ ПО ПРАВОВОМУ АНАЛИЗУ", textSize = 15f, bold = true, spacingAfter = 4f)
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        writer.addParagraph("Дата генерации: ${sdf.format(Date())}", textSize = 10.5f, color = Color.DKGRAY, spacingAfter = 14f)

        for (paragraph in analysisText.split("\n")) {
            writer.addParagraph(paragraph, textSize = 11.5f, spacingAfter = 5f)
        }

        return writer.saveToDocsCache(context, "Analysis_Report_${System.currentTimeMillis()}.pdf")
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
