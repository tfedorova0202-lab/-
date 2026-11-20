package com.example.utils

/**
 * Client-side PII & secret redactor applied before sending free-form user text to external LLM APIs
 * and before logging/displaying network error diagnostics.
 *
 * Masks:
 * - RF 20-digit bank/correspondent accounts (408/407/406/405/301/302 prefixes) and labeled БИК
 * - RF passport series+number (single-line, hyphenated, or split across newline)
 * - 16-19 digit payment card numbers
 * - SNILS (xxx-xxx-xxx xx)
 * - 10-digit and 12-digit labeled INN
 * - Labeled Russian full names (ФИО / Должник / Заемщик / Заявитель / От)
 * - Russian phone numbers (+7 / 8) and email addresses
 * - Google/Gemini API keys (AIza... and ?key=... query parameters)
 *
 * Note: Binary PDF/image payloads sent for OCR cannot be visually redacted here;
 * users are explicitly warned before uploading images/PDFs.
 */
object PiiRedactor {

    private val apiKeyQueryRegex = Regex("""(?i)([?&]key=)[^&\s]+""")
    private val googleApiKeyRegex = Regex("""\bAIza[0-9A-Za-z_\-]{20,}\b""")
    private val bankAccountLabeledRegex = Regex(
        """(?iu)((?:р/с|р\.с\.?|к/с|к\.с\.?|сч[её]т|расч[её]тный\s+сч[её]т|корр\.?\s*сч[её]т)\s*[:№]?\s*)(?:\d[\s-]?){20}\b"""
    )
    private val rfBankAccountRawRegex = Regex(
        """(?<!\d)(?:408|407|406|405|301|302)\d{2}[\s-]?\d{3}[\s-]?\d[\s-]?\d{4}[\s-]?\d{7}(?!\d)"""
    )
    private val bikRegex = Regex("""(?iu)(бик\s*[:№]?\s*)\d{9}\b""")
    private val cardNumberRegex = Regex("""\b(?:\d[ -]*?){16,19}\b""")
    private val snilsRegex = Regex("""\b\d{3}[-\s]?\d{3}[-\s]?\d{3}[-\s]\d{2}\b""")
    private val passportLabeledRegex = Regex(
        """(?iu)(паспорт(?:ные\s+данные)?|серия\s*(?:и\s*номер)?|с\.?\s*№?)\s*[:№]?\s*\d{2}[\s-]?\d{2}(?:[\s\r\n№-]+(?:номер\s*)?)?\d{6}\b"""
    )
    private val passportRawRegex = Regex("""(?<!\d)\d{2}[\s-]?\d{2}(?:[\s\r\n]+|[\s]*№[\s]*)\d{6}(?!\d)""")
    private val innLabeledRegex = Regex("""(?iu)(инн\s*[:№]?\s*)(?:\d{12}|\d{10})\b""")
    private val labeledFioRegex = Regex(
        """(?u)((?:ФИО|Ф\.И\.О\.|Должник|За[её]мщик|Заявитель|От\s+должника|От\s+заявителя|От)\s*:\s*)[А-ЯЁ][а-яё\-]{1,30}\s+[А-ЯЁ][а-яё\-]{1,30}(?:\s+[А-ЯЁ][а-яё\-]{1,30})?"""
    )
    private val phoneRegex = Regex("""(?<!\d)(?:\+7|8)[\s\-()]{0,3}\d{3}[\s\-()]{0,3}\d{3}[\s\-]?\d{2}[\s\-]?\d{2}(?!\d)""")
    private val emailRegex = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")

    fun redact(text: String): String {
        if (text.isBlank()) return text
        var result = text
        result = apiKeyQueryRegex.replace(result) { "${it.groupValues[1]}[API_KEY_СКРЫТ]" }
        result = googleApiKeyRegex.replace(result, "[API_KEY_СКРЫТ]")
        result = bankAccountLabeledRegex.replace(result) { "${it.groupValues[1]}[СЧЕТ_СКРЫТ]" }
        result = rfBankAccountRawRegex.replace(result, "[СЧЕТ_СКРЫТ]")
        result = bikRegex.replace(result) { "${it.groupValues[1]}[БИК_СКРЫТ]" }
        result = passportLabeledRegex.replace(result, "[ПАСПОРТ_СКРЫТ]")
        result = snilsRegex.replace(result, "[СНИЛС_СКРЫТ]")
        result = cardNumberRegex.replace(result) { match ->
            val digits = match.value.filter { it.isDigit() }
            if (digits.length in 16..19) "[КАРТА_СКРЫТА]" else match.value
        }
        result = passportRawRegex.replace(result, "[ПАСПОРТ_СКРЫТ]")
        result = innLabeledRegex.replace(result) { match ->
            "${match.groupValues[1]}[ИНН_СКРЫТ]"
        }
        result = labeledFioRegex.replace(result) { match ->
            "${match.groupValues[1]}[ФИО_СКРЫТО]"
        }
        result = phoneRegex.replace(result, "[ТЕЛЕФОН_СКРЫТ]")
        result = emailRegex.replace(result, "[EMAIL_СКРЫТ]")
        return result
    }
}
