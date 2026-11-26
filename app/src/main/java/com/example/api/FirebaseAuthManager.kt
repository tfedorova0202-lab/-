package com.example.api

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

sealed class AccountDeleteOutcome {
    object Deleted : AccountDeleteOutcome()
    object NoActiveAccount : AccountDeleteOutcome()
    object RequiresRecentLogin : AccountDeleteOutcome()
    data class Failed(val reason: String) : AccountDeleteOutcome()
}

class FirebaseAuthManager {
    val auth: FirebaseAuth get() = Firebase.auth
    val currentUser get() = try {
        if (isInitialized()) auth.currentUser else null
    } catch (_: Throwable) {
        null
    }

    /**
     * Interactive Google Sign-In via Jetpack CredentialManager using GetSignInWithGoogleOption.
     * Lazily initializes FirebaseApp only after explicit user consent/action (FirebaseInitProvider removed).
     * Degrades cleanly on non-GMS devices (RuStore/Huawei) and handles cancellations gracefully.
     */
    suspend fun signInWithGoogle(context: Context): Result<AuthResult> {
        if (!ensureInitialized(context)) {
            return Result.failure(
                IllegalStateException("Облачная авторизация недоступна в текущей конфигурации. Используйте режим «Инкогнито» (офлайн).")
            )
        }

        val credentialManager = try {
            CredentialManager.create(context)
        } catch (_: Throwable) {
            return Result.failure(
                IllegalStateException("Сервисы Google Play (GMS) недоступны на устройстве. Используйте режим «Инкогнито» (офлайн).")
            )
        }

        val webClientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            ""
        }

        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Web Client ID не настроен. Используйте режим «Инкогнито»."))
        }

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                Result.success(authResult)
            } else {
                Result.failure(IllegalStateException("Неподдерживаемый тип учетных данных"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (_: GetCredentialProviderConfigurationException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Credential provider unavailable (non-GMS device)")
            Result.failure(
                IllegalStateException("Вход через Google недоступен без Google Play Services (GMS). Продолжите в режиме «Инкогнито» (офлайн).")
            )
        } catch (_: NoCredentialException) {
            Result.failure(
                IllegalStateException("На устройстве не найден активный Google-аккаунт. Вы можете продолжить в режиме «Инкогнито».")
            )
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Google Sign-In failed: ${e.javaClass.simpleName}")
            Result.failure(
                IllegalStateException("Не удалось войти через Google (проверьте наличие GMS и доступ к googleapis.com). Доступен режим «Инкогнито».")
            )
        }
    }

    suspend fun deleteCurrentAccount(): AccountDeleteOutcome {
        val user = currentUser ?: return AccountDeleteOutcome.NoActiveAccount
        return try {
            user.delete().await()
            AccountDeleteOutcome.Deleted
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firebase Auth deletion requires recent re-authentication")
            AccountDeleteOutcome.RequiresRecentLogin
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Failed to delete Firebase Auth account: ${e.javaClass.simpleName}")
            AccountDeleteOutcome.Failed(e.javaClass.simpleName)
        }
    }

    fun signOut() {
        if (!isInitialized()) return
        try {
            auth.signOut()
        } catch (e: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error during signOut: ${e.javaClass.simpleName}")
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"

        fun isInitialized(): Boolean {
            return try {
                FirebaseApp.getInstance() != null
            } catch (_: Throwable) {
                false
            }
        }

        fun ensureInitialized(context: Context): Boolean {
            return try {
                val appContext = context.applicationContext
                val app = FirebaseApp.getApps(appContext).firstOrNull()
                    ?: FirebaseApp.initializeApp(appContext)
                app?.setDataCollectionDefaultEnabled(false)
                app != null
            } catch (_: Throwable) {
                false
            }
        }
    }
}
