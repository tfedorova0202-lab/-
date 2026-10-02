package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.UserPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun testClearAllUserData_erasesAllPreferences() {
        // Заполняем профиль тестовыми данными
        userPreferences.saveProfile("Иванов Иван Иванович", "г. Москва, ул. Ленина 1", "+79991234567")
        userPreferences.savePin("1234")
        userPreferences.saveBudget(50000f, 30000f)
        userPreferences.saveAuthSession("EMAIL", "test@example.com")
        userPreferences.saveCustomGeminiApiKey("AIzaSyTestKey")

        assertEquals("Иванов Иван Иванович", userPreferences.getName())
        assertEquals("1234", userPreferences.getPin())
        assertEquals("AIzaSyTestKey", userPreferences.getCustomGeminiApiKey())
        assertTrue(userPreferences.isLoggedIn())

        // Исполняем протокол удаления пользовательских данных (Data Deletion Policy)
        userPreferences.clearAllUserData()

        // Проверяем полную очистку
        assertEquals("", userPreferences.getName())
        assertEquals("", userPreferences.getAddress())
        assertEquals("", userPreferences.getPhone())
        assertEquals("", userPreferences.getPin())
        assertEquals("", userPreferences.getCustomGeminiApiKey())
        assertFalse(userPreferences.isLoggedIn())
    }
}
