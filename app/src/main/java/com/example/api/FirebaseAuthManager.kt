package com.example.api

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    val auth: FirebaseAuth get() = Firebase.auth
    val currentUser get() = auth.currentUser

    /**
     * Interactive Google Sign-In via Jetpack CredentialManager using GetSignInWithGoogleOption.
     * Catches GetCredentialCancellationException cleanly to prevent UI hangs.
     */
    suspend fun signInWithGoogle(context: Context): Result<AuthResult> {
        val credentialManager = CredentialManager.create(context)
        
        // Resolve Web Client ID from generated resources or BuildConfig
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                context.getString(resId)
            } else {
                BuildConfig.GOOGLE_WEB_CLIENT_ID
            }
        } catch (_: Exception) {
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        }.ifBlank {
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        }

        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Web Client ID is not configured"))
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
                Log.d(TAG, "Google Sign-In successful for uid: ${authResult.user?.uid}")
                Result.success(authResult)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In was cancelled by user")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
            Log.d(TAG, "User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during signOut", e)
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"
    }
}
