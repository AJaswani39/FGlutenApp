package com.example.fgluten.data.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * User profile data model for Firebase Authentication integration.
 * 
 * This class represents a user's profile information stored in Firestore.
 * It complements Firebase Auth's built-in user data with additional
 * application-specific information for the FGluten app.
 * 
 * @property userId Firebase Auth user ID (UID)
 * @property email User's email address
 * @property displayName User's chosen display name
 * @property contributorName Name used for crowd notes attribution
 * @property profilePictureUrl URL to user's profile picture
 * @property dietaryRestrictions List of dietary restrictions beyond gluten-free
 * @property contributionCount Total number of crowd notes contributed
 * @property helpfulVotes Total helpful votes received on notes
 * @property reputationScore Calculated reputation score based on note quality
 * @property badges List of achievement badges
 * @property isProfileVisible Whether profile should be visible to other users
 * @property createdAt Timestamp when profile was created
 * @property lastActiveAt Timestamp of last app usage
 * @property isVerified Whether user has verified credentials (celiac diagnosis, etc.)
 * @property verificationType Type of verification (celiac, dietitian, chef, etc.)
 */
@Parcelize
data class UserProfile(
    val userId: String,
    val email: String,
    val displayName: String,
    val contributorName: String? = null,
    val profilePictureUrl: String? = null,
    val dietaryRestrictions: List<String> = emptyList(),
    val contributionCount: Int = 0,
    val helpfulVotes: Int = 0,
    val reputationScore: Double = 0.0,
    val badges: List<String> = emptyList(),
    val isProfileVisible: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val verificationType: String? = null
) : Parcelable {
    
    /**
     * Calculates the user's trust level based on reputation and verification status.
     * 
     * @return Trust level string: "New", "Trusted", "Verified", "Expert"
     */
    fun getTrustLevel(): String {
        return when {
            isVerified && reputationScore >= 100.0 -> "Expert"
            isVerified && reputationScore >= 50.0 -> "Verified"
            reputationScore >= 25.0 -> "Trusted"
            contributionCount >= 5 -> "Active"
            else -> "New"
        }
    }
    
    /**
     * Returns the name to display for crowd notes attribution.
     * Uses contributorName if available, otherwise falls back to displayName.
     */
    fun getAttributionName(): String {
        return contributorName?.takeIf { it.isNotBlank() } ?: displayName
    }
}