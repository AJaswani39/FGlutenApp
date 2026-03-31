package io.fgluten.data.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * User profile data model.
 *
 * This class represents a user's profile information.
 *
 * @property userId user ID
 * @property email User's email address
 * @property displayName User's chosen display name
 * @property profilePictureUrl URL to user's profile picture
 * @property dietaryRestrictions List of dietary restrictions beyond gluten-free
 * @property helpfulVotes Total helpful votes received
 * @property reputationScore Calculated reputation score
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
    val profilePictureUrl: String? = null,
    val dietaryRestrictions: List<String> = emptyList(),
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
            else -> "New"
        }
    }
}