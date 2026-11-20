package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import com.example.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.example.BuildConfig

object BkiParser {
    
    private const val MAX_RENDER_DIMENSION = 1600

    // Convert PDF page to base64 string with safe FD/PdfRenderer cleanup and OOM cap
    private suspend fun pdfToBase64Images(context: Context, uri: Uri): List<String> = withContext(Dispatchers.IO) {
        val base64Images = mutableListOf<String>()
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { pdfRenderer ->
                val limit = minOf(pdfRenderer.pageCount, 10)
                for (i in 0 until limit) {
                    pdfRenderer.openPage(i).use { page ->
                        val rawW = (page.width * 2).coerceAtLeast(1)
                        val rawH = (page.height * 2).coerceAtLeast(1)
                        val scale = minOf(
                            1f,
                            MAX_RENDER_DIMENSION.toFloat() / maxOf(rawW, rawH).toFloat()
                        )
                        val targetW = (rawW * scale).toInt().coerceAtLeast(1)
                        val targetH = (rawH * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        try {
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                            base64Images.add(Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP))
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        }
        return@withContext base64Images
    }

    suspend fun parseBkiReport(context: Context, uri: Uri): List<ParsedDebt> = withContext(Dispatchers.IO) {
        val images = pdfToBase64Images(context, uri)
        if (images.isEmpty()) return@withContext emptyList()

        val parts = mutableListOf<Part>()
        parts.add(Part(text = "Ты — опытный юрист и финансовый аналитик. Проанализируй эти страницы кредитного отчета из БКИ (Бюро Кредитных Историй). Найди все активные кредиты, займы (МФО) и долги. Для каждого долга извлеки: Название кредитора (creditorName), дату договора (contractDate, если нет - пиши 'б/н от [дата]'), текущую сумму задолженности или остаток долга в рублях (factAmount, числом). Если это ипотека - поставь isMortgage=true. Верни результат строго в формате JSON, соответствующем предоставленной схеме."))

        for (img in images) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = img)))
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                temperature = 0.1f,
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = buildJsonObject {
                            put("type", "ARRAY")
                            putJsonObject("items") {
                                put("type", "OBJECT")
                                putJsonObject("properties") {
                                    putJsonObject("creditorName") { put("type", "STRING") }
                                    putJsonObject("contractDate") { put("type", "STRING") }
                                    putJsonObject("factAmount") { put("type", "NUMBER") }
                                    putJsonObject("isMortgage") { put("type", "BOOLEAN") }
                                }
                                putJsonArray("required") {
                                    add("creditorName")
                                    add("contractDate")
                                    add("factAmount")
                                }
                            }
                        }
                    )
                )
            )
        )

        try {
            val userPrefs = UserPreferences(context)
            val customKey = userPrefs.getCustomGeminiApiKey().trim()
            val apiKey = if (customKey.isNotBlank()) customKey else BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "dummy_key") {
                throw Exception("Для распознавания отчета БКИ через нейросеть укажите Gemini API ключ (в чате иконка ключа) или внесите данные вручную в Реестре.")
            }
            val response = RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "[]"

            // Clean possible markdown code fences
            val cleanJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = Json.parseToJsonElement(cleanJson).jsonArray
            val results = mutableListOf<ParsedDebt>()
            for (element in jsonArray) {
                val obj = element.jsonObject
                val creditorName = obj["creditorName"]?.jsonPrimitive?.content ?: "Неизвестно"
                val contractDate = obj["contractDate"]?.jsonPrimitive?.content ?: "б/н"
                val factAmount = obj["factAmount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val isMortgage = obj["isMortgage"]?.jsonPrimitive?.booleanOrNull ?: false

                results.add(ParsedDebt(creditorName, contractDate, factAmount, isMortgage))
            }
            return@withContext results
        } catch (e: Exception) {
            val safeMsg = PiiRedactor.redact(e.message ?: "Ошибка сети или разбора PDF")
            throw Exception(safeMsg)
        }
    }
}

data class ParsedDebt(
    val creditorName: String,
    val contractDate: String,
    val factAmount: Double,
    val isMortgage: Boolean
)
