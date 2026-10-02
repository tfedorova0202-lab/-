package com.example.api

import android.util.Log
import com.example.data.Debt
import com.example.data.LegalTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreManager {
    private val db: FirebaseFirestore? = try {
        Firebase.firestore
    } catch (e: Exception) {
        Log.w("FirestoreManager", "Firestore not available: ${e.message}")
        null
    }
    private val auth: FirebaseAuth? = try {
        Firebase.auth
    } catch (e: Exception) {
        null
    }

    suspend fun saveDebt(debt: Debt): Result<Unit> {
        val database = db ?: return Result.failure(Exception("Облачная база Firestore не настроена"))
        val user = auth?.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            database.collection("users").document(user.uid)
                .collection("debts").document(debt.id)
                .set(debt).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving debt", e)
            Result.failure(e)
        }
    }

    suspend fun getDebts(): Result<List<Debt>> {
        val database = db ?: return Result.failure(Exception("Облачная база Firestore не настроена"))
        val user = auth?.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val snapshot = database.collection("users").document(user.uid)
                .collection("debts").get().await()
            val debts = snapshot.documents.mapNotNull { it.toObject(Debt::class.java) }
            Result.success(debts)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error fetching debts", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteDebt(debtId: String): Result<Unit> {
        val database = db ?: return Result.failure(Exception("Облачная база Firestore не настроена"))
        val user = auth?.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            database.collection("users").document(user.uid)
                .collection("debts").document(debtId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error deleting debt", e)
            Result.failure(e)
        }
    }
}
