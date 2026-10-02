package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.UserPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserPreferencesTest {

    private lateinit var preferences: UserPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = UserPreferences(context)
    }

    @Test
    fun testProfileSaveAndRetrieve() {
        preferences.saveProfile(
            name = "Иванов Иван Иванович",
            address = "г. Москва, ул. Ленина, д. 10",
            phone = "+79991234567"
        )

        assertEquals("Иванов Иван Иванович", preferences.getName())
        assertEquals("г. Москва, ул. Ленина, д. 10", preferences.getAddress())
        assertEquals("+79991234567", preferences.getPhone())
    }

    @Test
    fun testPinCodeSaveAndRetrieve() {
        preferences.savePin("1234")
        assertEquals("1234", preferences.getPin())

        preferences.savePin("")
        assertEquals("", preferences.getPin())
    }

    @Test
    fun testBudgetSaveAndRetrieve() {
        preferences.saveBudget(income = 85000f, expenses = 35000f)
        assertEquals(85000f, preferences.getMonthlyIncome(), 0.01f)
        assertEquals(35000f, preferences.getMonthlyExpenses(), 0.01f)
    }

    @Test
    fun testAuthSessionLifecycle() {
        preferences.clearAuthSession()
        assertFalse(preferences.isLoggedIn())

        preferences.saveAuthSession("GUEST", "guest_session")
        assertTrue(preferences.isLoggedIn())
        assertEquals("GUEST", preferences.getAuthType())
        assertEquals("guest_session", preferences.getAuthIdentifier())

        preferences.clearAuthSession()
        assertFalse(preferences.isLoggedIn())
        assertEquals("", preferences.getAuthType())
        assertEquals("", preferences.getAuthIdentifier())
    }

    @Test
    fun testCustomGeminiApiKey() {
        preferences.saveCustomGeminiApiKey("AIzaSyTEST_KEY_123")
        assertEquals("AIzaSyTEST_KEY_123", preferences.getCustomGeminiApiKey())
    }
}
