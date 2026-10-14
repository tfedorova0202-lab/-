package com.example

import com.example.api.FirestoreManager
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.Debt
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtFirestoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveDebt_authenticatedOwner_persistsSuccessfully() = runBlocking {
        val uid = signInTestUser("lawyer_alice@example.com")
        val manager = FirestoreManager(firestore, auth)

        val testDebt = Debt(
            id = "test-debt-1",
            creditorName = "АО Альфа-Банк",
            contractDate = "15.03.2023",
            factAmount = 180000.0,
            penaltyAmount = 45000.0,
            insuranceAmount = 12000.0,
            planAmount = 123000.0,
            status = "ACTIVE",
            isMortgage = false,
            legalBasis = "ст. 333 ГК РФ, ст. 958 ГК РФ"
        )

        val saveResult = withTimeout(5000L) {
            manager.saveDebt(testDebt)
        }
        assertTrue("Expected saveDebt to succeed", saveResult.isSuccess)

        val debtsResult = withTimeout(5000L) {
            manager.getDebts()
        }
        assertTrue("Expected getDebts to succeed: ${debtsResult.exceptionOrNull()?.message}", debtsResult.isSuccess)
        val debts = debtsResult.getOrThrow()
        assertEquals(1, debts.size)
        assertEquals("test-debt-1", debts[0].id)
        assertEquals("АО Альфа-Банк", debts[0].creditorName)
        assertEquals(180000.0, debts[0].factAmount, 0.001)
    }

    @Test
    fun deleteDebt_authenticatedOwner_deletesSuccessfully() = runBlocking {
        signInTestUser("lawyer_alice_delete@example.com")
        val manager = FirestoreManager(firestore, auth)

        val testDebt = Debt(
            id = "test-debt-del",
            creditorName = "МФК Быстроденьги",
            contractDate = "01.01.2024",
            factAmount = 30000.0,
            planAmount = 15000.0,
            status = "ACTIVE"
        )

        withTimeout(5000L) { manager.saveDebt(testDebt) }
        val delResult = withTimeout(5000L) { manager.deleteDebt(testDebt.id) }
        assertTrue("Expected deleteDebt to succeed", delResult.isSuccess)

        val remaining = withTimeout(5000L) { manager.getDebts() }.getOrThrow()
        assertTrue("Expected empty list after delete", remaining.none { it.id == testDebt.id })
    }

    @Test
    fun operations_unauthenticated_failsCleanly() = runBlocking {
        auth.signOut()
        val manager = FirestoreManager(firestore, auth)

        val testDebt = Debt(
            id = "test-unauth",
            creditorName = "Банк",
            contractDate = "01.01.2023",
            factAmount = 50000.0,
            planAmount = 50000.0,
            status = "ACTIVE"
        )

        val saveResult = manager.saveDebt(testDebt)
        assertTrue("Expected unauthenticated write to fail", saveResult.isFailure)

        val getResult = manager.getDebts()
        assertTrue("Expected unauthenticated read to fail", getResult.isFailure)
    }
}
