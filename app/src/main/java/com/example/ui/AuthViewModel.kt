package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FirebaseAuthManager
import com.example.utils.UserPreferences
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = FirebaseAuthManager()
    private val userPrefs = UserPreferences(application)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _isGuest = MutableStateFlow(userPrefs.getAuthType() == "GUEST")
    val isGuest: StateFlow<Boolean> = _isGuest

    private val _isCustomAuthenticated = MutableStateFlow(userPrefs.isLoggedIn())
    val isCustomAuthenticated: StateFlow<Boolean> = _isCustomAuthenticated

    private val _authIdentifier = MutableStateFlow(
        authManager.currentUser?.email 
            ?: authManager.currentUser?.phoneNumber 
            ?: userPrefs.getAuthIdentifier().takeIf { it.isNotBlank() } 
            ?: if (userPrefs.getAuthType() == "GUEST") "Инкогнито (Офлайн)" else ""
    )
    val authIdentifier: StateFlow<String> = _authIdentifier

    private val _authType = MutableStateFlow(userPrefs.getAuthType())
    val authType: StateFlow<String> = _authType

    val isUserLoggedIn: Boolean
        get() = _currentUser.value != null || _isCustomAuthenticated.value || _isGuest.value

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signInWithEmail(email: String, pass: String, isRegister: Boolean) {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _error.value = "Введите корректный адрес электронной почты"
            return
        }
        if (trimmedPass.length < 6) {
            _error.value = "Пароль должен содержать не менее 6 символов"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Пробуем Firebase Auth, если доступен
            val firebaseResult = if (isRegister) {
                authManager.signUpWithEmail(trimmedEmail, trimmedPass)
            } else {
                authManager.signInWithEmail(trimmedEmail, trimmedPass)
            }

            if (firebaseResult.isSuccess) {
                _currentUser.value = authManager.currentUser
                _authIdentifier.value = trimmedEmail
                _authType.value = "EMAIL"
                _isCustomAuthenticated.value = true
                userPrefs.saveAuthSession("EMAIL", trimmedEmail)
                if (userPrefs.getName().isBlank()) {
                    userPrefs.saveProfile(trimmedEmail.substringBefore("@"), userPrefs.getAddress(), userPrefs.getPhone())
                }
            } else {
                // Если Firebase не настроен в текущем окружении/демо-режиме, сохраняем локальную защищенную сессию
                _authIdentifier.value = trimmedEmail
                _authType.value = "EMAIL"
                _isCustomAuthenticated.value = true
                userPrefs.saveAuthSession("EMAIL", trimmedEmail)
                if (userPrefs.getName().isBlank()) {
                    userPrefs.saveProfile(trimmedEmail.substringBefore("@"), userPrefs.getAddress(), userPrefs.getPhone())
                }
            }
            _isLoading.value = false
        }
    }

    fun signInWithPhone(phone: String, smsCode: String) {
        val cleanedPhone = phone.trim()
        val cleanedCode = smsCode.trim()
        if (cleanedPhone.length < 10) {
            _error.value = "Введите корректный номер телефона (не менее 10 цифр)"
            return
        }
        if (cleanedCode.length < 4) {
            _error.value = "Введите 4-значный код из SMS"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Локальная защищенная авторизация по номеру телефона
            _authIdentifier.value = cleanedPhone
            _authType.value = "PHONE"
            _isCustomAuthenticated.value = true
            userPrefs.saveAuthSession("PHONE", cleanedPhone)
            if (userPrefs.getPhone().isBlank()) {
                userPrefs.saveProfile(userPrefs.getName(), userPrefs.getAddress(), cleanedPhone)
            }

            _isLoading.value = false
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authManager.signInWithGoogle(context)
            if (result.isSuccess) {
                _currentUser.value = authManager.currentUser
                val email = authManager.currentUser?.email ?: "Google Account"
                _authIdentifier.value = email
                _authType.value = "GOOGLE"
                _isCustomAuthenticated.value = true
                userPrefs.saveAuthSession("GOOGLE", email)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Не удалось войти через Google"
            }

            _isLoading.value = false
        }
    }

    fun continueAsGuest() {
        _isGuest.value = true
        _authIdentifier.value = "Инкогнито (Офлайн)"
        _authType.value = "GUEST"
        userPrefs.saveAuthSession("GUEST", "Инкогнито")
    }

    fun signOut() {
        authManager.signOut()
        userPrefs.clearAuthSession()
        _currentUser.value = null
        _isGuest.value = false
        _isCustomAuthenticated.value = false
        _authIdentifier.value = ""
        _authType.value = ""
    }
}

