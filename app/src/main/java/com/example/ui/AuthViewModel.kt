package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.FirebaseAuthManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authManager = FirebaseAuthManager()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

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
                _currentUser.value = authManager.auth.currentUser
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Unknown error"
            }
            
            _isLoading.value = false
        }
    }
    
    fun signOut() {
        authManager.signOut()
        _currentUser.value = null
    }
}
