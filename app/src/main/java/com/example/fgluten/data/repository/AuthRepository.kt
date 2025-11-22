package com.example.fgluten.data.repository

import android.content.Context
import com.example.fgluten.data.user.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Authentication repository for Firebase Authentication integration.
 * 
 * This class provides a clean interface for all authentication operations
 * including email/password login, Google Sign-In, and user profile management.
 * It handles both Firebase Auth and Firestore user profile synchronization.
 * 
 * @property firebaseAuth Firebase Authentication instance
 * @property firestore Firestore database instance
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Authentication state flow that emits current user state changes
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser

    /**
     * Check if user is currently authenticated
     */
    fun isUserSignedIn(): Boolean = firebaseAuth.currentUser != null

    /**
     * Register new user with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @param displayName User's chosen display name
     * @param contributorName Optional name for crowd notes attribution
     * @return Result containing UserProfile on success or error message on failure
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        contributorName: String? = null
    ): Result<UserProfile> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("User creation failed")

            // Update display name in Firebase Auth
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdate).await()

            // Create user profile in Firestore
            val userProfile = UserProfile(
                userId = user.uid,
                email = user.email ?: "",
                displayName = displayName,
                contributorName = contributorName
            )
            
            saveUserProfile(userProfile).await()
            
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @return Result containing UserProfile on success or error message on failure
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Sign in failed")
            
            val userProfile = getUserProfile(user.uid).getOrThrow()
            Result.success(userProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google account
     * 
     * @param idToken Google ID token from Google Sign-In
     * @param displayName User's chosen display name
     * @param contributorName Optional name for crowd notes attribution
     * @return Result containing UserProfile on success or error message on failure
     */
    suspend fun signInWithGoogle(
        idToken: String,
        displayName: String,
        contributorName: String? = null
    ): Result<UserProfile> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google sign in failed")

            // Check if this is a new user (no Firestore profile exists)
            val existingProfile = getUserProfile(user.uid)
            if (existingProfile.isFailure) {
                // Create new user profile
                val userProfile = UserProfile(
                    userId = user.uid,
                    email = user.email ?: "",
                    displayName = displayName.ifBlank { user.displayName ?: "User" },
                    contributorName = contributorName ?: displayName.ifBlank { user.displayName }
                )
                saveUserProfile(userProfile).await()
                Result.success(userProfile)
            } else {
                // Update last active time for existing user
                val profile = existingProfile.getOrThrow()
                val updatedProfile = profile.copy(lastActiveAt = System.currentTimeMillis())
                saveUserProfile(updatedProfile).await()
                Result.success(updatedProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send password reset email
     * 
     * @param email Email address to send reset link to
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Get user profile from Firestore
     * 
     * @param userId User's Firebase UID
     * @return Result containing UserProfile on success or error on failure
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val document = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                if (userProfile != null) {
                    Result.success(userProfile)
                } else {
                    Result.failure(Exception("Failed to parse user profile"))
                }
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save user profile to Firestore
     * 
     * @param userProfile User profile data to save
     * @return Result indicating success or failure
     */
    suspend fun saveUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userProfile.userId)
                .set(userProfile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile information
     * 
     * @param userId User's Firebase UID
     * @param updates Map of field names to new values
     * @return Result indicating success or failure
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user's contribution statistics
     * 
     * @param userId User's Firebase UID
     * @param helpfulVotesChange Change in helpful votes count
     * @return Result indicating success or failure
     */
    suspend fun updateUserContributions(userId: String, helpfulVotesChange: Int = 0): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val updates = mutableMapOf<String, Any>()
            
            if (helpfulVotesChange != 0) {
                updates["helpfulVotes"] = com.google.firebase.firestore.FieldValue.increment(helpfulVotesChange.toLong())
            }
            
            updates["lastActiveAt"] = System.currentTimeMillis()
            
            userRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user account and associated data
     * 
     * @param context Android context for resource access
     * @return Result indicating success or failure
     */
    suspend fun deleteAccount(context: Context): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: throw Exception("No user signed in")
            
            // Delete user profile from Firestore
            firestore.collection("users").document(user.uid).delete().await()
            
            // Delete user's notes and reviews
            deleteUserNotes(user.uid).await()
            deleteUserReviews(user.uid).await()
            
            // Delete Firebase Auth account
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete all notes created by a user (for account deletion)
     * 
     * @param userId User's Firebase UID
     */
    private suspend fun deleteUserNotes(userId: String) {
        firestore.collection("crowd_notes")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .forEach { document ->
                document.reference.delete().await()
            }
    }

    /**
     * Delete all reviews created by a user (for account deletion)
     * 
     * @param userId User's Firebase UID
     */
    private suspend fun deleteUserReviews(userId: String) {
        firestore.collection("restaurant_reviews")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .forEach { document ->
                document.reference.delete().await()
            }
    }
}