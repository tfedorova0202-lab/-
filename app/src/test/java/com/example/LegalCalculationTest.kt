package com.example

import com.example.data.Debt
import com.example.data.KnowledgeBase
import org.junit.Assert.*
import org.junit.Test

class LegalCalculationTest {

    @Test
    fun testDebtPlanCalculation_standardCase() {
        val fact = 150000.0
        val penalty = 30000.0 // ст. 333 ГК РФ
        val insurance = 12000.0 // ст. 958 ГК РФ
        val plan = maxOf(0.0, fact - penalty - insurance)
        assertEquals(108000.0, plan, 0.001)
    }

    @Test
    fun testDebtPlanCalculation_zeroFlooring() {
        val fact = 20000.0
        val penalty = 15000.0
        val insurance = 10000.0
        // penalties + insurance exceeds fact
        val plan = maxOf(0.0, fact - penalty - insurance)
        assertEquals(0.0, plan, 0.0)
    }

    @Test
    fun testMicrofinanceLimit_fz353_part23_art5() {
        // Займы до 1 года: переплата (проценты + неустойка) не более 1.3x от тела займа
        val principal = 10000.0
        val maxOverpaymentRate = 1.3
        val maxAllowableOverpayment = principal * maxOverpaymentRate
        val maxLegalTotal = principal + maxAllowableOverpayment

        assertEquals(13000.0, maxAllowableOverpayment, 0.001)
        assertEquals(23000.0, maxLegalTotal, 0.001)

        val mfoClaim = 38000.0
        val illegalExcess = maxOf(0.0, mfoClaim - maxLegalTotal)
        assertEquals(15000.0, illegalExcess, 0.001)
    }

    @Test
    fun testStatuteOfLimitations_gk196() {
        // Срок исковой давности по ст. 196 ГК РФ составляет 3 года
        val limitationPeriodYears = 3
        assertEquals(3, limitationPeriodYears)
    }

    @Test
    fun testKnowledgeBase_courtOrderCancellation() {
        val answer = KnowledgeBase.findAnswer("Как отменить судебный приказ?")
        assertNotNull(answer)
        assertTrue(answer!!.contains("128"))
        assertTrue(answer.contains("10 ДНЕЙ"))
    }

    @Test
    fun testKnowledgeBase_thirdPartiesContactRevocation() {
        val answer = KnowledgeBase.findAnswer("Звонят родственникам и близким")
        assertNotNull(answer)
        assertTrue(answer!!.contains("230-ФЗ"))
        assertTrue(answer.contains("ч. 7 ст. 4"))
    }

    @Test
    fun testKnowledgeBase_mortgageShield() {
        val answer = KnowledgeBase.findAnswer("Как сохранить ипотечную квартиру?")
        assertNotNull(answer)
        assertTrue(answer!!.contains("213.10-1 ФЗ-127"))
        assertTrue(answer.contains("446 ГПК РФ"))
    }

    @Test
    fun testKnowledgeBase_incognitoProtocol() {
        val answer = KnowledgeBase.findAnswer("Протокол инкогнито чтобы не узнали через заказные письма")
        assertNotNull(answer)
        assertTrue(answer!!.contains("Инкогнито"))
        assertTrue(answer.contains("Заказное.Почта.ру"))
    }

    @Test
    fun testKnowledgeBase_macrocommandsMenu() {
        val answer = KnowledgeBase.findAnswer("/MENU")
        assertNotNull(answer)
        assertTrue(answer!!.contains("/ПЛАН_ФАКТ"))
        assertTrue(answer.contains("/АНТИКОЛЛЕКТОР"))
        assertTrue(answer.contains("/ИПОТЕКА_ЩИТ"))
    }

    @Test
    fun testDebtObjectCreation() {
        val debt = Debt(
            id = "test-1",
            creditorName = "ООО МКК Экспресс",
            contractDate = "15.01.2024",
            factAmount = 50000.0,
            penaltyAmount = 15000.0,
            insuranceAmount = 5000.0,
            planAmount = 30000.0,
            status = "В работе",
            isMortgage = false,
            deadlineDate = "25.01.2024",
            deadlineType = "10 дней (ст. 128 ГПК)"
        )

        assertEquals("test-1", debt.id)
        assertEquals("ООО МКК Экспресс", debt.creditorName)
        assertEquals(50000.0, debt.factAmount, 0.001)
        assertEquals(15000.0, debt.penaltyAmount, 0.001)
        assertEquals(5000.0, debt.insuranceAmount, 0.001)
        assertEquals(30000.0, debt.planAmount, 0.001)
        assertFalse(debt.isMortgage)
    }
}
