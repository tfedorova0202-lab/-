package com.example

import com.example.data.CbrMfoRegistry
import com.example.data.MfoStatus
import org.junit.Assert.*
import org.junit.Test

class CbrMfoRegistryTest {

    @Test
    fun testCheckLegality_legalMfo_byBrandName() {
        val result = CbrMfoRegistry.checkLegality("Займер")
        assertEquals(MfoStatus.LEGAL, result.status)
        assertNotNull(result.entry)
        assertEquals("МФК", result.entry?.mfoType)
        assertEquals("651303532004088", result.entry?.regNumber)
        assertEquals("4205271785", result.entry?.inn)
        assertTrue(result.formattedCheckTime.contains("МСК"))
        assertTrue(result.legalBasis.contains("151-ФЗ"))
        assertTrue(result.legalBasis.contains("353-ФЗ"))
    }

    @Test
    fun testCheckLegality_legalMfo_byInn() {
        // Проверка по ИНН Мани Мен: 7704784072
        val result = CbrMfoRegistry.checkLegality("7704784072")
        assertEquals(MfoStatus.LEGAL, result.status)
        assertEquals("MoneyMan (Манимен)", result.entry?.brandName)
        assertEquals("2110177000478", result.entry?.regNumber)
    }

    @Test
    fun testCheckLegality_legalMfo_ekapusta() {
        val result = CbrMfoRegistry.checkLegality("екапуста")
        assertEquals(MfoStatus.LEGAL, result.status)
        assertEquals("МКК", result.entry?.mfoType)
        assertEquals("5408292849", result.entry?.inn)
    }

    @Test
    fun testCheckLegality_excludedMfo() {
        val result = CbrMfoRegistry.checkLegality("БыстроКредит Онлайн")
        assertEquals(MfoStatus.EXCLUDED, result.status)
        assertNotNull(result.entry?.exclusionDate)
        assertTrue(result.title.contains("ИСКЛЮЧЕНА"))
        assertTrue(result.statusExplanation.contains("ЛИШЕНА ПРАВА"))
    }

    @Test
    fun testCheckLegality_illegalLender_notInRegistry() {
        val result = CbrMfoRegistry.checkLegality("ДеньгиБезПаспортаОтказовНет")
        assertEquals(MfoStatus.ILLEGAL, result.status)
        assertNull(result.entry)
        assertTrue(result.title.contains("ОТСУТСТВУЕТ В РЕЕСТРЕ"))
        assertTrue(result.legalBasis.contains("171 УК РФ"))
        assertTrue(result.legalBasis.contains("14.56 КоАП РФ"))
    }

    @Test
    fun testCheckLegality_timestampOnMomentOfRequest() {
        val before = System.currentTimeMillis()
        val result = CbrMfoRegistry.checkLegality("Турбозайм")
        val after = System.currentTimeMillis()

        assertTrue(result.timestamp in before..after)
        assertFalse(result.formattedCheckTime.isBlank())
    }

    @Test
    fun testLegalLimitsCompliance() {
        val result = CbrMfoRegistry.checkLegality("Webbankir")
        assertTrue(result.maxDailyRate.contains("0.8%"))
        assertTrue(result.maxOverpaymentLimit.contains("100%"))
        assertTrue(result.maxOverpaymentLimit.contains("130%"))
        assertTrue(result.recommendations.any { it.contains("230-ФЗ") })
    }
}
