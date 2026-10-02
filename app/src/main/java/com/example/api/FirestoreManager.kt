package com.example.api

import android.util.Log
import com.example.data.Debt
import com.example.data.LegalTemplate
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreManager {
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun saveDebt(debt: Debt): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        return try {
            db.collection("users").document(user.uid)
                .collection("debts").document(debt.id.toString())
                .set(debt).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error saving debt", e)
            Result.failure(e)
        }
    }

    suspend fun getDebts(): Result<List<Debt>> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        return try {
            val snapshot = db.collection("users").document(user.uid)
                .collection("debts").get().await()
            val debts = snapshot.documents.mapNotNull { it.toObject(Debt::class.java) }
            Result.success(debts)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error fetching debts", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteDebt(debtId: Int): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        return try {
            db.collection("users").document(user.uid)
                .collection("debts").document(debtId.toString())
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error deleting debt", e)
            Result.failure(e)
        }
    }
}
