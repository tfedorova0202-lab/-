import re

with open('app/src/main/java/com/example/utils/DocumentGenerator.kt', 'r') as f:
    text = f.read()

new_function = """    fun generateSavingsPdf(
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
}"""

text = text.replace('}\n}', '}\n\n' + new_function)

with open('app/src/main/java/com/example/utils/DocumentGenerator.kt', 'w') as f:
    f.write(text)
print("Patched DocumentGenerator.kt")
