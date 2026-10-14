package com.example.api

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.Debt
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreManager(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    // Convenience constructor: resolves the named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    fun requireUserId(): String {
        return auth.currentUser?.uid ?: error("User must be authenticated for this operation")
    }

    fun observeDebts(userId: String): Flow<List<Debt>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return db.collection("users").document(userId)
            .collection("debts")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Debt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
    }

    suspend fun saveDebt(debt: Debt): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        require(debt.id.isNotBlank()) { "Debt ID must not be blank" }
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("debts").document(debt.id)
            
            val payload = mapOf(
                "id" to debt.id,
                "creditorName" to debt.creditorName,
                "contractDate" to debt.contractDate,
                "factAmount" to debt.factAmount,
                "penaltyAmount" to debt.penaltyAmount,
                "insuranceAmount" to debt.insuranceAmount,
                "planAmount" to debt.planAmount,
                "status" to debt.status,
                "isMortgage" to debt.isMortgage,
                "deadlineDate" to debt.deadlineDate,
                "deadlineType" to debt.deadlineType,
                "legalBasis" to debt.legalBasis,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving debt to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun getDebts(): Result<List<Debt>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("debts").get().await()
            val debts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Debt::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let { d ->
                    if (d.id.isBlank()) d.copy(id = doc.id) else d
                }
            }
            Result.success(debts)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching debts from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDebt(debtId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        require(debtId.isNotBlank()) { "Debt ID must not be blank" }
        return try {
            db.collection("users").document(uid)
                .collection("debts").document(debtId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting debt from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(isIncognito: Boolean, hideFamilyInfo: Boolean): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val docRef = db.collection("users").document(user.uid)
            val profileData = mapOf(
                "userId" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "isIncognito" to isIncognito,
                "hideFamilyInfo" to hideFamilyInfo,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(profileData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user profile to Firestore", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirestoreManager"
    }
}
