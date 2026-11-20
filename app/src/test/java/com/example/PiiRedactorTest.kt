package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.api.FirestoreManager
import com.example.utils.AiReportStore
import com.example.utils.PiiRedactor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PiiRedactorTest {

    @Test
    fun testRedactMasksSensitiveRussianPii() {
        val input = "Паспорт 4510 123456, карта 4276 3800 1234 5678, СНИЛС 123-456-789 01, ИНН 770123456789, тел +7 (999) 123-45-67, почта ivan.petrov@example.ru"
        val redacted = PiiRedactor.redact(input)

        assertFalse("Passport must be masked", redacted.contains("4510"))
        assertFalse("Passport number must be masked", redacted.contains("123456"))
        assertFalse("Card number must be masked", redacted.contains("4276"))
        assertFalse("SNILS must be masked", redacted.contains("123-456-789"))
        assertFalse("Personal INN must be masked", redacted.contains("770123456789"))
        assertFalse("Phone must be masked", redacted.contains("999"))
        assertFalse("Email must be masked", redacted.contains("ivan.petrov@example.ru"))

        assertTrue(redacted.contains("[ПАСПОРТ_СКРЫТ]"))
        assertTrue(redacted.contains("[КАРТА_СКРЫТА]"))
        assertTrue(redacted.contains("[СНИЛС_СКРЫТ]"))
        assertTrue(redacted.contains("[ИНН_СКРЫТ]"))
        assertTrue(redacted.contains("[ТЕЛЕФОН_СКРЫТ]"))
        assertTrue(redacted.contains("[EMAIL_СКРЫТ]"))
    }

    @Test
    fun testRedactPreservesDebtAmountsAndStatutoryCitations() {
        val input = "Долг по МФО 35000 руб. по ст. 333 ГК РФ и ч. 23 ст. 5 ФЗ № 353-ФЗ, дата договора 15.04.2026"
        val redacted = PiiRedactor.redact(input)
        assertEquals(input, redacted)
    }

    @Test
    fun testFirestoreStatusNormalizationMatchesSecurityRulesEnum() {
        val allowed = setOf("ACTIVE", "CLOSED", "COURT_ORDER", "COLLECTORS", "DISPUTED", "CANCELLED")
        val russianInputs = listOf(
            "В работе",
            "Ипотека (Иммунитет)",
            "Судебный приказ",
            "Коллекторы",
            "Оспаривается",
            "Отменен",
            "Закрыт",
            "Анализ документа",
            "Произвольный статус"
        )
        for (raw in russianInputs) {
            val normalized = FirestoreManager.normalizeStatusForFirestore(raw)
            assertTrue("Status '$raw' normalized to '$normalized' must be in allowed enum", normalized in allowed)
        }
        assertEquals("COURT_ORDER", FirestoreManager.normalizeStatusForFirestore("Судебный приказ"))
        assertEquals("COLLECTORS", FirestoreManager.normalizeStatusForFirestore("Коллекторы"))
        assertEquals("CLOSED", FirestoreManager.normalizeStatusForFirestore("Закрыт"))
        assertEquals("ACTIVE", FirestoreManager.normalizeStatusForFirestore("В работе"))
    }

    @Test
    fun testAiReportStoreRedactsAndPersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AiReportStore.clearAllReports(context)
        val report = AiReportStore.saveReport(
            context = context,
            reason = "Галлюцинация",
            comment = "Мой тел +7 916 555-44-33",
            messageText = "Проверьте карту 2202 2000 1111 2222"
        )
        assertEquals(1, AiReportStore.getReportCount(context))
        assertFalse(report.comment.contains("916"))
        assertFalse(report.messageExcerpt.contains("2202"))
        assertTrue(AiReportStore.buildExportPayload(report).contains("Галлюцинация"))
        AiReportStore.clearAllReports(context)
        assertEquals(0, AiReportStore.getReportCount(context))
    }

    @Test
    fun testRedactAdversarialBankAccountsBikMultilinePassportFioAndApiKeys() {
        val input = """
            ФИО: Сидоров Алексей Николаевич
            Должник: Петрова Мария
            Паспорт: 45-10
            654321
            Также 45 10 № 987654
            Р/с 40817810099910004312, корр. счет 30101810400000000225, БИК 044525225
            Счет с пробелами: 40817 810 0 9991 0004312
            ИНН: 7707083893
            URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=AIzaSyTestSecretKey1234567890abcd
        """.trimIndent()

        val redacted = PiiRedactor.redact(input)

        assertFalse("FIO must be masked", redacted.contains("Сидоров"))
        assertFalse("Labeled debtor name must be masked", redacted.contains("Петрова"))
        assertFalse("Multiline passport must be masked", redacted.contains("654321"))
        assertFalse("Raw passport with № must be masked", redacted.contains("987654"))
        assertFalse("20-digit bank account must be masked", redacted.contains("40817810099910004312"))
        assertFalse("Correspondent account must be masked", redacted.contains("30101810400000000225"))
        assertFalse("Grouped bank account must be masked", redacted.contains("0004312"))
        assertFalse("BIK must be masked", redacted.contains("044525225"))
        assertFalse("10-digit INN must be masked", redacted.contains("7707083893"))
        assertFalse("API key in query must be masked", redacted.contains("AIzaSyTestSecretKey"))

        assertTrue(redacted.contains("[ФИО_СКРЫТО]"))
        assertTrue(redacted.contains("[ПАСПОРТ_СКРЫТ]"))
        assertTrue(redacted.contains("[СЧЕТ_СКРЫТ]"))
        assertTrue(redacted.contains("[БИК_СКРЫТ]"))
        assertTrue(redacted.contains("[ИНН_СКРЫТ]"))
        assertTrue(redacted.contains("[API_KEY_СКРЫТ]"))
    }

    @Test
    fun testFirestoreSanitizationEnforcesSecurityRulesConstraints() {
        assertEquals("valid_id-123", FirestoreManager.sanitizeFirestoreId("valid_id-123"))
        assertEquals("bad_id_with_spaces", FirestoreManager.sanitizeFirestoreId("bad id/with.spaces"))
        assertEquals(128, FirestoreManager.sanitizeFirestoreId("a".repeat(200)).length)

        assertEquals(0.0, FirestoreManager.sanitizeAmount(Double.NaN), 0.0)
        assertEquals(0.0, FirestoreManager.sanitizeAmount(Double.POSITIVE_INFINITY), 0.0)
        assertEquals(0.0, FirestoreManager.sanitizeAmount(-500.0), 0.0)
        assertEquals(1500.5, FirestoreManager.sanitizeAmount(1500.5), 0.0)
    }
}
