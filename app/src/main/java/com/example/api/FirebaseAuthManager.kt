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
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    val auth: FirebaseAuth = Firebase.auth
    val currentUser = auth.currentUser

    suspend fun signInWithGoogle(context: Context): Result<AuthResult> {
        val credentialManager = CredentialManager.create(context)
        
        // Note: WEB_CLIENT_ID must be set in your google-services.json or strings.xml
        // Replace with your actual web client ID from Firebase Console.
        val webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
        
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

            if (credential is GoogleIdTokenCredential) {
                val idToken = credential.idToken
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                Result.success(authResult)
            } else {
                Result.failure(Exception("Unexpected type of credential"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Sign-in failed", e)
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
