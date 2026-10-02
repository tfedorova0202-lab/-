package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var debtDao: DebtDao
    private lateinit var messageDao: MessageDao
    private lateinit var templateDao: LegalTemplateDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        debtDao = db.debtDao()
        messageDao = db.messageDao()
        templateDao = db.legalTemplateDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveDebt() = runBlocking {
        val debt = Debt(
            id = "debt-123",
            creditorName = "Банк ВТБ",
            contractDate = "10.05.2023",
            factAmount = 250000.0,
            penaltyAmount = 40000.0,
            insuranceAmount = 20000.0,
            planAmount = 190000.0,
            status = "В работе",
            isMortgage = false
        )

        debtDao.insertDebt(debt)
        val debts = debtDao.getAllDebts().first()

        assertEquals(1, debts.size)
        val retrieved = debts[0]
        assertEquals("debt-123", retrieved.id)
        assertEquals("Банк ВТБ", retrieved.creditorName)
        assertEquals(250000.0, retrieved.factAmount, 0.001)
        assertEquals(190000.0, retrieved.planAmount, 0.001)
    }

    @Test
    fun deleteDebt_andClearAll() = runBlocking {
        val debt1 = Debt("1", "Сбербанк", "01.01.2023", 100000.0, 10000.0, 0.0, 90000.0, "В работе")
        val debt2 = Debt("2", "Альфа-Банк", "02.02.2023", 200000.0, 20000.0, 0.0, 180000.0, "В работе")

        debtDao.insertDebt(debt1)
        debtDao.insertDebt(debt2)

        var debts = debtDao.getAllDebts().first()
        assertEquals(2, debts.size)

        debtDao.deleteDebtById("1")
        debts = debtDao.getAllDebts().first()
        assertEquals(1, debts.size)
        assertEquals("Альфа-Банк", debts[0].creditorName)

        debtDao.clearAll()
        debts = debtDao.getAllDebts().first()
        assertTrue(debts.isEmpty())
    }

    @Test
    fun insertAndRetrieveChatMessages() = runBlocking {
        val msg1 = MessageEntity(id = "m1", text = "Здравствуйте, как отменить приказ?", isUser = true, timestamp = 1000L)
        val msg2 = MessageEntity(id = "m2", text = "Подайте возражение по ст. 128 ГПК РФ", isUser = false, timestamp = 2000L)

        messageDao.insertMessage(msg1)
        messageDao.insertMessage(msg2)

        val messages = messageDao.getAllMessages()
        assertEquals(2, messages.size)
        assertEquals("Здравствуйте, как отменить приказ?", messages[0].text)
        assertTrue(messages[0].isUser)
        assertEquals("Подайте возражение по ст. 128 ГПК РФ", messages[1].text)
        assertFalse(messages[1].isUser)

        messageDao.clearHistory()
        val emptyMessages = messageDao.getAllMessages()
        assertTrue(emptyMessages.isEmpty())
    }

    @Test
    fun insertAndRetrieveLegalTemplates() = runBlocking {
        val template = LegalTemplate(
            id = 1,
            title = "Возражение на судебный приказ",
            category = "Судебная защита",
            content = "Мировому судье судебного участка..."
        )

        templateDao.insertTemplate(template)
        val templates = templateDao.getTemplatesByCategory("Судебная защита").first()

        assertEquals(1, templates.size)
        assertEquals("Возражение на судебный приказ", templates[0].title)
        assertEquals("Судебная защита", templates[0].category)
    }
}
