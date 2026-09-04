package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isSandboxMode: Boolean = false
)

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: AppUser) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Authentication Repository handling User Authentication via Firebase Auth
 * with resilient offline sandbox mode and local session persistence.
 */
class AuthRepository(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("reflect_ai_auth_prefs", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth? = try {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            FirebaseAuth.getInstance()
        } else {
            null
        }
    } catch (t: Throwable) {
        Log.w("AuthRepository", "Firebase Auth instance unavailable: ${t.message}")
        null
    }

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_UID = "auth_uid"
        private const val KEY_NAME = "auth_name"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_PHOTO = "auth_photo"
        private const val KEY_SANDBOX = "auth_sandbox"
    }

    private val _authState = MutableStateFlow<AuthState>(loadInitialAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Listen to Firebase Auth state changes when Firebase is active
        try {
            firebaseAuth?.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null) {
                    val appUser = fbUser.toAppUser()
                    persistUser(appUser)
                    _authState.value = AuthState.Authenticated(appUser)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error registering auth state listener: ${e.message}")
        }
    }

    private fun FirebaseUser.toAppUser(): AppUser {
        return AppUser(
            uid = uid,
            displayName = displayName?.ifBlank { null } ?: (email?.substringBefore("@") ?: "Reflective Explorer"),
            email = email ?: "user_$uid@reflectai.app",
            photoUrl = photoUrl?.toString(),
            isSandboxMode = false
        )
    }

    private fun loadInitialAuthState(): AuthState {
        // Check Firebase current user first
        val fbUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null }
        if (fbUser != null) {
            val user = fbUser.toAppUser()
            persistUser(user)
            return AuthState.Authenticated(user)
        }

        val uid = prefs.getString(KEY_UID, null)
        return if (uid != null) {
            val user = AppUser(
                uid = uid,
                displayName = prefs.getString(KEY_NAME, "Reflective Explorer") ?: "Reflective Explorer",
                email = prefs.getString(KEY_EMAIL, "explorer@reflectai.app") ?: "explorer@reflectai.app",
                photoUrl = prefs.getString(KEY_PHOTO, null),
                isSandboxMode = prefs.getBoolean(KEY_SANDBOX, true)
            )
            AuthState.Authenticated(user)
        } else {
            val defaultUid = "user_guest_" + UUID.randomUUID().toString().take(8)
            val defaultUser = AppUser(
                uid = defaultUid,
                displayName = "Reflective Explorer",
                email = "explorer@reflectai.app",
                photoUrl = null,
                isSandboxMode = true
            )
            persistUser(defaultUser)
            AuthState.Authenticated(defaultUser)
        }
    }

    /**
     * Authenticates with Firebase using Email and Password.
     */
    fun signInWithEmailPassword(email: String, pass: String): Flow<Result<AppUser>> = flow {
        val auth = firebaseAuth
        if (auth == null) {
            // Fallback gracefully in sandbox environment
            val sandboxUser = AppUser(
                uid = "fb_" + UUID.nameUUIDFromBytes(email.toByteArray()).toString().take(12),
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email,
                isSandboxMode = true
            )
            persistUser(sandboxUser)
            _authState.value = AuthState.Authenticated(sandboxUser)
            emit(Result.success(sandboxUser))
            return@flow
        }

        try {
            val result = suspendCancellableCoroutine<AppUser> { cont ->
                auth.signInWithEmailAndPassword(email.trim(), pass)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user?.toAppUser() ?: AppUser(
                            uid = "user_" + UUID.randomUUID().toString().take(8),
                            displayName = email.substringBefore("@"),
                            email = email
                        )
                        persistUser(user)
                        _authState.value = AuthState.Authenticated(user)
                        cont.resume(user)
                    }
                    .addOnFailureListener { ex ->
                        cont.resumeWithException(ex)
                    }
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signInWithEmailPassword failed: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Registers a new account with Firebase Auth.
     */
    fun signUpWithEmailPassword(email: String, pass: String, displayName: String): Flow<Result<AppUser>> = flow {
        val auth = firebaseAuth
        if (auth == null) {
            val sandboxUser = AppUser(
                uid = "fb_" + UUID.nameUUIDFromBytes(email.toByteArray()).toString().take(12),
                displayName = displayName.ifBlank { email.substringBefore("@") },
                email = email,
                isSandboxMode = true
            )
            persistUser(sandboxUser)
            _authState.value = AuthState.Authenticated(sandboxUser)
            emit(Result.success(sandboxUser))
            return@flow
        }

        try {
            val result = suspendCancellableCoroutine<AppUser> { cont ->
                auth.createUserWithEmailAndPassword(email.trim(), pass)
                    .addOnSuccessListener { authResult ->
                        val fbUser = authResult.user
                        if (fbUser != null && displayName.isNotBlank()) {
                            fbUser.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build()
                            )
                        }
                        val user = AppUser(
                            uid = fbUser?.uid ?: UUID.randomUUID().toString(),
                            displayName = displayName.ifBlank { email.substringBefore("@") },
                            email = email
                        )
                        persistUser(user)
                        _authState.value = AuthState.Authenticated(user)
                        cont.resume(user)
                    }
                    .addOnFailureListener { ex ->
                        cont.resumeWithException(ex)
                    }
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            Log.e(TAG, "Firebase signUpWithEmailPassword failed: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Authenticates anonymously with Firebase Auth.
     */
    fun signInAnonymously(): Flow<Result<AppUser>> = flow {
        val auth = firebaseAuth
        if (auth == null) {
            val guest = AppUser(
                uid = "anon_" + UUID.randomUUID().toString().take(8),
                displayName = "Quiet Guest",
                email = "guest@reflectai.app",
                isSandboxMode = true
            )
            persistUser(guest)
            _authState.value = AuthState.Authenticated(guest)
            emit(Result.success(guest))
            return@flow
        }

        try {
            val result = suspendCancellableCoroutine<AppUser> { cont ->
                auth.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user?.toAppUser() ?: AppUser(
                            uid = "anon_" + UUID.randomUUID().toString().take(8),
                            displayName = "Quiet Guest",
                            email = "guest@reflectai.app"
                        )
                        persistUser(user)
                        _authState.value = AuthState.Authenticated(user)
                        cont.resume(user)
                    }
                    .addOnFailureListener { ex ->
                        cont.resumeWithException(ex)
                    }
            }
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Authenticates the user in secure sandbox / guest mode.
     */
    fun signInSandboxUser(customName: String = "Reflective Explorer", email: String = "explorer@reflectai.app") {
        val sandboxUid = "user_" + UUID.nameUUIDFromBytes(email.toByteArray()).toString().take(12)
        val sandboxUser = AppUser(
            uid = sandboxUid,
            displayName = customName,
            email = email,
            photoUrl = null,
            isSandboxMode = true
        )
        persistUser(sandboxUser)
        _authState.value = AuthState.Authenticated(sandboxUser)
    }

    /**
     * Signs in with custom user profile.
     */
    fun signInWithCustomUser(appUser: AppUser) {
        persistUser(appUser)
        _authState.value = AuthState.Authenticated(appUser)
    }

    /**
     * Signs out the user from Firebase Auth and isolates session state.
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase signOut warning: ${e.message}")
        }
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    private fun persistUser(user: AppUser) {
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHOTO, user.photoUrl)
            .putBoolean(KEY_SANDBOX, user.isSandboxMode)
            .apply()
    }
}
