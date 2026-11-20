package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.AiReportStore
import com.example.utils.UserPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MarketComplianceTest {

    private lateinit var context: Context
    private lateinit var userPreferences: UserPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        userPreferences = UserPreferences(context)
    }

    @Test
    fun testClearAllUserData_erasesAllPreferencesAndArtifacts() {
        // Заполняем профиль, PIN, жалобу на ИИ и кэш PDF
        userPreferences.saveProfile("Иванов Иван Иванович", "г. Москва, ул. Ленина 1", "+79991234567")
        userPreferences.savePin("1234")
        userPreferences.saveBudget(50000f, 30000f)
        userPreferences.saveAuthSession("EMAIL", "test@example.com")
        userPreferences.saveCustomGeminiApiKey("AIzaSyTestKey")
        AiReportStore.saveReport(context, "Юридическая неточность", "Неверный срок", "Ответ ИИ с тел +79991234567")
        val docsDir = File(context.cacheDir, "docs").apply { mkdirs() }
        val cachedPdf = File(docsDir, "test_export.pdf").apply { writeText("PDF") }

        assertEquals("Иванов Иван Иванович", userPreferences.getName())
        assertTrue(userPreferences.hasPin())
        assertTrue(userPreferences.verifyPin("1234"))
        assertEquals("AIzaSyTestKey", userPreferences.getCustomGeminiApiKey())
        assertTrue(userPreferences.isLoggedIn())
        assertEquals(1, AiReportStore.getReportCount(context))
        assertTrue(cachedPdf.exists())

        // Исполняем протокол удаления пользовательских данных (Data Deletion Policy)
        userPreferences.clearAllUserData()

        // Проверяем полную очистку
        assertEquals("", userPreferences.getName())
        assertEquals("", userPreferences.getAddress())
        assertEquals("", userPreferences.getPhone())
        assertFalse(userPreferences.hasPin())
        assertFalse(userPreferences.verifyPin("1234"))
        assertEquals("", userPreferences.getCustomGeminiApiKey())
        assertFalse(userPreferences.isLoggedIn())
        assertEquals(0, AiReportStore.getReportCount(context))
        assertFalse(cachedPdf.exists())
    }
}
