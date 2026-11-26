package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object DocumentScanner {
    
    /**
     * ВАЖНОЕ ОГРАНИЧЕНИЕ OCR:
     * Используемый движок ML Kit (`com.google.mlkit:text-recognition:16.0.1`, Latin)
     * официально распознаёт только латиницу и арабские цифры (даты и суммы),
     * но НЕ поддерживает кириллицу в русских договорах и справках БКИ.
     * Для кириллического текста требуется либо ввод названия кредитора вручную,
     * либо полноценный кириллический OCR (Tesseract/PaddleOCR/серверный OCR).
     *
     * MlKitInitProvider и транспорт телеметрии datatransport отключены в AndroidManifest.xml;
     * инициализация выполняется лениво только при явном сканировании документа.
     */
    suspend fun parseDebtFromBitmap(context: Context, bitmap: Bitmap): ScannedDebtResult {
        try {
            MlKitContext.initializeIfNeeded(context.applicationContext)
        } catch (_: Throwable) {
            throw Exception("Распознавание с камеры недоступно (требуется GMS/ML Kit). Внесите параметры долга вручную.")
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        try {
            val result = recognizer.process(image).await()
            val text = result.text
            
            val dateRegex = """(\d{2}\.\d{2}\.\d{4})""".toRegex()
            val amountRegex = """(\d{1,3}(?:[ \s]?\d{3})*(?:[.,]\d{2})?)""".toRegex()
            val contractDate = dateRegex.find(text)?.value ?: ""
            
            // Подбираем латинизированные/транслитерированные бренды или просим проверить вручную
            val creditorKeywords = listOf("bank", "sber", "tinkoff", "alfa", "vtb", "mfo", "zaim", "money", "банк", "мфо", "мкк", "ооо", "пао", "ао")
            var creditorName = "Укажите кредитора вручную (OCR: только цифры/латиница)"
            for (line in result.textBlocks.flatMap { it.lines }) {
                val lineText = line.text.lowercase()
                if (creditorKeywords.any { lineText.contains(it) }) {
                    creditorName = line.text.take(40)
                    break
                }
            }
            
            val amounts = amountRegex.findAll(text)
                .map { it.value.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0 }
                .filter { it > 500.0 }
                .toList()
                
            val factAmount = amounts.maxOrNull() ?: 0.0
            
            return ScannedDebtResult(
                creditorName = creditorName,
                contractDate = contractDate,
                factAmount = factAmount,
                cyrillicLimitationNotice = "Встроенный ML Kit OCR распознаёт только цифры и латиницу; проверьте название кредитора и даты вручную."
            )
        } catch (e: Throwable) {
            throw Exception("Распознавание с камеры недоступно (требуется GMS/ML Kit). Внесите параметры долга вручную.")
        } finally {
            try {
                recognizer.close()
            } catch (_: Throwable) {
                // ignore
            }
        }
    }
}

data class ScannedDebtResult(
    val creditorName: String,
    val contractDate: String,
    val factAmount: Double,
    val cyrillicLimitationNotice: String = ""
)
