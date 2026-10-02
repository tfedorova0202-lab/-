package com.example.utils

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveProfile(name: String, address: String, phone: String) {
        prefs.edit()
            .putString("NAME", name)
            .putString("ADDRESS", address)
            .putString("PHONE", phone)
            .apply()
    }

    fun saveBudget(income: Float, expenses: Float) {
        prefs.edit()
            .putFloat("MONTHLY_INCOME", income)
            .putFloat("MONTHLY_EXPENSES", expenses)
            .apply()
    }
    
    fun savePin(pin: String) {
        prefs.edit().putString("PIN_CODE", pin).apply()
    }

    fun saveAuthSession(authType: String, identifier: String) {
        prefs.edit()
            .putBoolean("IS_LOGGED_IN", true)
            .putString("AUTH_TYPE", authType)
            .putString("AUTH_IDENTIFIER", identifier)
            .apply()
    }

    fun clearAuthSession() {
        prefs.edit()
            .putBoolean("IS_LOGGED_IN", false)
            .remove("AUTH_TYPE")
            .remove("AUTH_IDENTIFIER")
            .apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("IS_LOGGED_IN", false)
    fun getAuthType(): String = prefs.getString("AUTH_TYPE", "") ?: ""
    fun getAuthIdentifier(): String = prefs.getString("AUTH_IDENTIFIER", "") ?: ""

    fun saveCustomGeminiApiKey(key: String) {
        prefs.edit().putString("CUSTOM_GEMINI_API_KEY", key.trim()).apply()
    }

    fun getCustomGeminiApiKey(): String = prefs.getString("CUSTOM_GEMINI_API_KEY", "") ?: ""

    fun getPin(): String = prefs.getString("PIN_CODE", "") ?: ""

    fun getName(): String = prefs.getString("NAME", "") ?: ""
    fun getAddress(): String = prefs.getString("ADDRESS", "") ?: ""
    fun getPhone(): String = prefs.getString("PHONE", "") ?: ""
    fun getMonthlyIncome(): Float = prefs.getFloat("MONTHLY_INCOME", 0f)
    fun getMonthlyExpenses(): Float = prefs.getFloat("MONTHLY_EXPENSES", 0f)
}
