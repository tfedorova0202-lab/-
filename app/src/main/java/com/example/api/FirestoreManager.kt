package com.example.api

import android.content.Context
import android.util.Log
import com.example.BuildConfig
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
    private val dbProvider: () -> FirebaseFirestore,
    private val authProvider: () -> FirebaseAuth = { Firebase.auth }
) {
    private val db: FirebaseFirestore get() = dbProvider()
    private val auth: FirebaseAuth get() = authProvider()

    constructor(
        db: FirebaseFirestore,
        auth: FirebaseAuth = Firebase.auth
    ) : this(dbProvider = { db }, authProvider = { auth })

    // Convenience constructor: resolves the named database ID lazily on first cloud access
    constructor(context: Context) : this(
        dbProvider = {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        },
        authProvider = { Firebase.auth }
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
        val safeId = sanitizeFirestoreId(debt.id)
        if (safeId.isBlank()) {
            return Result.failure(IllegalArgumentException("Некорректный идентификатор записи"))
        }
        return try {
            val docRef = db.collection("users").document(uid)
                .collection("debts").document(safeId)

            val payload = mapOf(
                "id" to safeId,
                "creditorName" to debt.creditorName.trim().ifBlank { "Кредитор не указан" }.take(150),
                "contractDate" to debt.contractDate.trim().take(100),
                "factAmount" to sanitizeAmount(debt.factAmount),
                "penaltyAmount" to sanitizeAmount(debt.penaltyAmount),
                "insuranceAmount" to sanitizeAmount(debt.insuranceAmount),
                "planAmount" to sanitizeAmount(debt.planAmount),
                "status" to normalizeStatusForFirestore(debt.status),
                "isMortgage" to debt.isMortgage,
                "deadlineDate" to debt.deadlineDate.trim().take(50),
                "deadlineType" to debt.deadlineType.trim().take(50),
                "legalBasis" to debt.legalBasis.trim().take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error saving debt to Firestore: ${e.javaClass.simpleName}")
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
            if (BuildConfig.DEBUG) Log.w(TAG, "Error fetching debts from Firestore: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    suspend fun deleteDebt(debtId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        val safeId = sanitizeFirestoreId(debtId)
        if (safeId.isBlank()) return Result.failure(IllegalArgumentException("Debt ID must not be blank"))
        return try {
            db.collection("users").document(uid)
                .collection("debts").document(safeId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error deleting debt from Firestore: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    /**
     * Deletes all user documents stored in Cloud Firestore (debts, legal_actions, and user profile).
     * Must be invoked while the user is still authenticated before calling FirebaseUser.delete() or signOut().
     */
    suspend fun deleteAllUserData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        return try {
            val userDoc = db.collection("users").document(uid)
            val debtsSnapshot = userDoc.collection("debts").get().await()
            for (doc in debtsSnapshot.documents) {
                doc.reference.delete().await()
            }
            val actionsSnapshot = userDoc.collection("legal_actions").get().await()
            for (doc in actionsSnapshot.documents) {
                doc.reference.delete().await()
            }
            userDoc.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error deleting all Firestore user data: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    /**
     * Terminates active Firestore listeners and clears the local SQLite persistence cache (databases/firestore.*).
     * Best-effort cleanup invoked during full user data erasure.
     */
    suspend fun clearLocalCache(): Result<Unit> {
        return try {
            val instance = db
            instance.terminate().await()
            instance.clearPersistence().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firestore local persistence clear skipped: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(isIncognito: Boolean, hideFamilyInfo: Boolean): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val docRef = db.collection("users").document(user.uid)
            val profileData = mapOf(
                "userId" to user.uid,
                "email" to (user.email ?: "").take(200),
                "displayName" to (user.displayName ?: "").take(100),
                "isIncognito" to isIncognito,
                "hideFamilyInfo" to hideFamilyInfo,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(profileData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error saving user profile to Firestore: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirestoreManager"

        private val VALID_FIRESTORE_STATUSES = setOf(
            "ACTIVE", "CLOSED", "COURT_ORDER", "COLLECTORS", "DISPUTED", "CANCELLED"
        )

        private val VALID_ID_CHARS = Regex("""[^a-zA-Z0-9_\-]""")

        fun sanitizeFirestoreId(rawId: String): String {
            return rawId.trim().replace(VALID_ID_CHARS, "_").take(128)
        }

        fun sanitizeAmount(value: Double): Double {
            return if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0
        }

        fun normalizeStatusForFirestore(rawStatus: String): String {
            val upper = rawStatus.trim().uppercase()
            if (upper in VALID_FIRESTORE_STATUSES) return upper
            return when {
                upper.contains("ПРИКАЗ") || upper.contains("СУД") -> "COURT_ORDER"
                upper.contains("КОЛЛЕКТ") -> "COLLECTORS"
                upper.contains("СПОР") || upper.contains("ПРЕТЕНЗ") -> "DISPUTED"
                upper.contains("ЗАКРЫТ") || upper.contains("ПОГАШ") -> "CLOSED"
                upper.contains("ОТМЕН") -> "CANCELLED"
                else -> "ACTIVE"
            }
        }
    }
}
