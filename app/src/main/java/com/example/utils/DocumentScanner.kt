package com.example.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

object DocumentScanner {
    
    suspend fun parseDebtFromBitmap(bitmap: Bitmap): ScannedDebtResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        
        try {
            val result = recognizer.process(image).await()
            val text = result.text
            
            val dateRegex = """(\d{2}\.\d{2}\.\d{4})""".toRegex()
            val amountRegex = """(\d{1,3}(?:[ \s]?\d{3})*(?:[.,]\d{2})?)""".toRegex()
            val contractDate = dateRegex.find(text)?.value ?: "01.01.2024"
            
            val creditorKeywords = listOf("банк", "мфо", "мкк", "ооо", "пао", "ао")
            var creditorName = "Неизвестный кредитор"
            for (line in result.textBlocks.flatMap { it.lines }) {
                val lineText = line.text.lowercase()
                if (creditorKeywords.any { lineText.contains(it) }) {
                    creditorName = line.text.take(30)
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
                factAmount = factAmount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Ошибка распознавания текста: ${e.message}")
        }
    }
}

data class ScannedDebtResult(
    val creditorName: String,
    val contractDate: String,
    val factAmount: Double
)
