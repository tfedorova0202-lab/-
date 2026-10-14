package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FirebaseAuthManager
import com.example.api.FirestoreManager
import com.example.utils.UserPreferences
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = FirebaseAuthManager()
    private val userPrefs = UserPreferences(application)
    private val firestoreManager = FirestoreManager(application)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _isGuest = MutableStateFlow(userPrefs.getAuthType() == "GUEST")
    val isGuest: StateFlow<Boolean> = _isGuest

    private val _authIdentifier = MutableStateFlow(
        authManager.currentUser?.email 
            ?: authManager.currentUser?.displayName
            ?: userPrefs.getAuthIdentifier().takeIf { it.isNotBlank() } 
            ?: if (userPrefs.getAuthType() == "GUEST") "Инкогнито (Офлайн)" else ""
    )
    val authIdentifier: StateFlow<String> = _authIdentifier

    private val _authType = MutableStateFlow(userPrefs.getAuthType())
    val authType: StateFlow<String> = _authType

    val isUserLoggedIn: Boolean
        get() = _currentUser.value != null || _isGuest.value

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = authManager.signInWithGoogle(context)
            if (result.isSuccess) {
                val user = authManager.currentUser
                _currentUser.value = user
                val emailOrName = user?.email ?: user?.displayName ?: "Google Account"
                _authIdentifier.value = emailOrName
                _authType.value = "GOOGLE"
                _isGuest.value = false
                userPrefs.saveAuthSession("GOOGLE", emailOrName)
                
                // Synchronize incognito & family security preferences to Firestore
                val isIncognito = userPrefs.isIncognitoMode()
                val hideFamily = userPrefs.isHideFamilyInfo()
                firestoreManager.saveUserProfile(isIncognito, hideFamily)
            } else {
                val ex = result.exceptionOrNull()
                val msg = ex?.message ?: "Не удалось войти через Google"
                // Do not show error on user cancellation
                if (ex !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _error.value = msg
                }
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
        _authIdentifier.value = ""
        _authType.value = ""
    }

    fun clearError() {
        _error.value = null
    }
}
