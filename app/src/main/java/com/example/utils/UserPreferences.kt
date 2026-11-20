package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserPreferences(context: Context) {
    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyPlaintextPinIfNeeded()
    }

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

    /**
     * Stores PIN strictly as a salted PBKDF2-HMAC-SHA256 hash, never as plaintext.
     * Passing an empty string disables PIN protection and removes stored hash/salt.
     */
    fun savePin(pin: String) {
        val trimmed = pin.trim()
        if (trimmed.isEmpty()) {
            prefs.edit()
                .remove(KEY_LEGACY_PIN)
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .apply()
            return
        }
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = derivePinHash(trimmed, salt)
        prefs.edit()
            .remove(KEY_LEGACY_PIN)
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun hasPin(): Boolean {
        migrateLegacyPlaintextPinIfNeeded()
        return !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun verifyPin(candidate: String): Boolean {
        migrateLegacyPlaintextPinIfNeeded()
        val storedHashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return try {
            val salt = Base64.decode(storedSaltB64, Base64.NO_WRAP)
            val expectedHash = Base64.decode(storedHashB64, Base64.NO_WRAP)
            val candidateHash = derivePinHash(candidate.trim(), salt)
            MessageDigest.isEqual(expectedHash, candidateHash)
        } catch (_: Exception) {
            false
        }
    }

    private fun migrateLegacyPlaintextPinIfNeeded() {
        val legacyPin = prefs.getString(KEY_LEGACY_PIN, null)
        if (!legacyPin.isNullOrBlank()) {
            savePin(legacyPin)
        } else if (prefs.contains(KEY_LEGACY_PIN)) {
            prefs.edit().remove(KEY_LEGACY_PIN).apply()
        }
    }

    private fun derivePinHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
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

    fun getName(): String = prefs.getString("NAME", "") ?: ""
    fun getAddress(): String = prefs.getString("ADDRESS", "") ?: ""
    fun getPhone(): String = prefs.getString("PHONE", "") ?: ""
    fun getMonthlyIncome(): Float = prefs.getFloat("MONTHLY_INCOME", 0f)
    fun getMonthlyExpenses(): Float = prefs.getFloat("MONTHLY_EXPENSES", 0f)

    fun isIncognitoMode(): Boolean = prefs.getBoolean("INCOGNITO_MODE", true)
    fun setIncognitoMode(enabled: Boolean) {
        prefs.edit().putBoolean("INCOGNITO_MODE", enabled).apply()
    }

    fun isHideFamilyInfo(): Boolean = prefs.getBoolean("HIDE_FAMILY_INFO", true)
    fun setHideFamilyInfo(enabled: Boolean) {
        prefs.edit().putBoolean("HIDE_FAMILY_INFO", enabled).apply()
    }

    fun clearAllUserData() {
        prefs.edit().clear().commit()
        clearCachedArtifacts()
        AiReportStore.clearAllReports(appContext)
    }

    /**
     * Deletes generated PDF/CSV documents left in internal cache directories.
     */
    fun clearCachedArtifacts() {
        try {
            val docsDir = File(appContext.cacheDir, "docs")
            if (docsDir.exists()) {
                docsDir.deleteRecursively()
            }
            appContext.cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && (file.name.endsWith(".pdf", ignoreCase = true) ||
                            file.name.endsWith(".csv", ignoreCase = true))
                ) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    companion object {
        const val PREFS_NAME = "user_prefs"
        private const val KEY_LEGACY_PIN = "PIN_CODE"
        private const val KEY_PIN_HASH = "PIN_HASH_V2"
        private const val KEY_PIN_SALT = "PIN_SALT_V2"
        private const val PBKDF2_ITERATIONS = 10_000
        private const val PBKDF2_KEY_LENGTH_BITS = 256
    }
}
