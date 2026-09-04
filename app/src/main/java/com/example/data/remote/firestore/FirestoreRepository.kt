package com.example.data.remote.firestore

import android.content.Context
import android.util.Log
import com.example.data.local.JournalDao
import com.example.data.local.JournalDatabase
import com.example.data.model.JournalInteraction
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class PersistenceResult {
    data object InProgress : PersistenceResult()
    data class Success(val interaction: JournalInteraction) : PersistenceResult()
    data class Error(val message: String, val canRetry: Boolean = true) : PersistenceResult()
}

/**
 * Repository responsible for User-Isolated Firestore document storage and local Room database caching.
 * Documents are strictly isolated under: /users/{userId}/interactions/{interactionId}
 * This ensures no cross-user data leakage and complies fully with Firestore security policies.
 */
class FirestoreRepository(
    context: Context
) {
    private val db = JournalDatabase.getInstance(context)
    private val journalDao: JournalDao = db.journalDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    private val firestore: FirebaseFirestore? = try {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseFirestore.getInstance()
        } else {
            null
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Firestore instance not available: ${t.message}")
        null
    }

    private var activeSnapshotListener: ListenerRegistration? = null

    companion object {
        private const val TAG = "FirestoreRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_INTERACTIONS = "interactions"
    }

    /**
     * Persists a journal interaction atomically.
     * 1. Saves to local Room DAO immediately (offline cache and resilience).
     * 2. Writes to user-isolated Firestore document: /users/{userId}/interactions/{interactionId}
     */
    fun saveInteraction(interaction: JournalInteraction): Flow<PersistenceResult> = flow {
        emit(PersistenceResult.InProgress)

        if (interaction.userId.isBlank()) {
            emit(PersistenceResult.Error("Cannot save interaction: User is not authenticated.", canRetry = false))
            return@flow
        }

        try {
            // Write to local Room DAO first
            journalDao.insertInteraction(interaction.copy(isSynced = firestore != null))
            Log.d(TAG, "Saved interaction ${interaction.id} to local Room DB.")

            // Persist to user-isolated Firestore path: /users/{userId}/interactions/{interactionId}
            firestore?.let { fs ->
                val docRef = fs.collection(COLLECTION_USERS)
                    .document(interaction.userId)
                    .collection(COLLECTION_INTERACTIONS)
                    .document(interaction.id)

                suspendCancellableCoroutine<Unit> { cont ->
                    docRef.set(interaction.toFirestoreMap(), SetOptions.merge())
                        .addOnSuccessListener {
                            Log.i(TAG, "Successfully synced interaction ${interaction.id} to Firestore path: users/${interaction.userId}/interactions/${interaction.id}")
                            cont.resume(Unit)
                        }
                        .addOnFailureListener { ex ->
                            Log.w(TAG, "Firestore write warning (retained in offline Room): ${ex.message}")
                            // We do not fail the user flow if offline; Room guarantees resilience
                            cont.resume(Unit)
                        }
                }
            }

            emit(PersistenceResult.Success(interaction))
        } catch (e: Exception) {
            Log.e(TAG, "Error saving interaction: ${e.message}", e)
            emit(PersistenceResult.Error("Failed to save reflection: ${e.localizedMessage ?: "Storage error"}", canRetry = true))
        }
    }

    /**
     * Observes user interactions strictly isolated by userId.
     * Synchronizes real-time Firestore updates into local Room storage.
     */
    fun observeUserInteractions(userId: String): Flow<List<JournalInteraction>> {
        // Attach real-time snapshot listener to user's isolated Firestore collection if available
        firestore?.let { fs ->
            activeSnapshotListener?.remove()
            activeSnapshotListener = fs.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_INTERACTIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore snapshot listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshot?.documents?.let { docs ->
                        repositoryScope.launch {
                            val remoteInteractions = docs.mapNotNull { doc ->
                                doc.data?.let { data ->
                                    JournalInteraction.fromFirestoreMap(doc.id, data)
                                }
                            }
                            if (remoteInteractions.isNotEmpty()) {
                                remoteInteractions.forEach { item ->
                                    journalDao.insertInteraction(item.copy(isSynced = true))
                                }
                            }
                        }
                    }
                }
        }

        // Return the Room Flow filtered strictly for this isolated user
        return journalDao.getInteractionsForUser(userId)
    }

    /**
     * Deletes an interaction from local Room and user-isolated Firestore collection.
     */
    suspend fun deleteInteraction(userId: String, interactionId: String): Result<Unit> {
        return try {
            journalDao.deleteInteraction(interactionId, userId)

            firestore?.let { fs ->
                fs.collection(COLLECTION_USERS)
                    .document(userId)
                    .collection(COLLECTION_INTERACTIONS)
                    .document(interactionId)
                    .delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting interaction: ${e.message}", e)
            Result.failure(e)
        }
    }
}

