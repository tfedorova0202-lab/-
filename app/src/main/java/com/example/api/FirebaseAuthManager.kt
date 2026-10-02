package com.example.api

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import com.example.BuildConfig
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    val auth: FirebaseAuth? = try {
        Firebase.auth
    } catch (e: Exception) {
        Log.w("FirebaseAuthManager", "Firebase Auth not available: ${e.message}")
        null
    }
    val currentUser get() = auth?.currentUser

    fun setupGoogleAuth() {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            Log.w("FirebaseAuthManager", "Google Web Client ID не настроен в конфигурации сборки")
            return
        }
        Log.d("FirebaseAuthManager", "Google Auth сконфигурирован безопасно")
    }

    suspend fun signInWithGoogle(context: Context): Result<AuthResult> {
        val authInstance = auth ?: return Result.failure(Exception("Сервисы Firebase не настроены на этом устройстве"))
        val credentialManager = CredentialManager.create(context)
        
        val webClientId = if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        } else {
            "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
        }
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = authInstance.signInWithCredential(firebaseCredential).await()
                Result.success(authResult)
            } else {
                Result.failure(Exception("Неожиданный тип учетных данных"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> {
        val authInstance = auth ?: return Result.failure(Exception("Сервисы Firebase не настроены на этом устройстве"))
        return try {
            val result = authInstance.signInWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Email sign-in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult> {
        val authInstance = auth ?: return Result.failure(Exception("Сервисы Firebase не настроены на этом устройстве"))
        return try {
            val result = authInstance.createUserWithEmailAndPassword(email, password).await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Email sign-up failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<AuthResult> {
        val authInstance = auth ?: return Result.failure(Exception("Сервисы Firebase не настроены"))
        return try {
            val result = authInstance.signInAnonymously().await()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Anonymous sign-in failed", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Sign out error", e)
        }
    }
}
