package com.example.utils

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AiSafetyReport(
    val id: String,
    val timestampIso: String,
    val reason: String,
    val comment: String,
    val messageExcerpt: String
)

/**
 * Persists user-submitted Generative AI safety reports locally (redacted) so reports are
 * actually recorded and exportable to the developer moderation channel rather than discarded.
 * Excluded from Android Auto Backup and wiped on "Delete All Data".
 */
object AiReportStore {

    const val REPORT_FILE_NAME = "ai_safety_reports.jsonl"

    fun saveReport(
        context: Context,
        reason: String,
        comment: String,
        messageText: String
    ): AiSafetyReport {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val report = AiSafetyReport(
            id = UUID.randomUUID().toString(),
            timestampIso = isoFormat.format(Date()),
            reason = reason.trim(),
            comment = PiiRedactor.redact(comment.trim()).take(500),
            messageExcerpt = PiiRedactor.redact(messageText.trim()).take(400)
        )

        val json = JSONObject().apply {
            put("id", report.id)
            put("timestamp", report.timestampIso)
            put("reason", report.reason)
            put("comment", report.comment)
            put("messageExcerpt", report.messageExcerpt)
        }

        val file = File(context.filesDir, REPORT_FILE_NAME)
        file.appendText(json.toString() + "\n", Charsets.UTF_8)
        return report
    }

    fun getReportCount(context: Context): Int {
        val file = File(context.filesDir, REPORT_FILE_NAME)
        if (!file.exists()) return 0
        return file.readLines(Charsets.UTF_8).count { it.isNotBlank() }
    }

    fun buildExportPayload(report: AiSafetyReport): String {
        return """
            Отчет о некорректном ответе ИИ (AI Safety Report)
            ID: ${report.id}
            Время (UTC): ${report.timestampIso}
            Причина: ${report.reason}
            Комментарий пользователя: ${report.comment.ifBlank { "не указан" }}
            Фрагмент ответа ИИ (обезличен):
            ${report.messageExcerpt}
        """.trimIndent()
    }

    fun clearAllReports(context: Context) {
        val file = File(context.filesDir, REPORT_FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }
}
