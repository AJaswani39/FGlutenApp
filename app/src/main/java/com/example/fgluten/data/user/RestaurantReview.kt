package com.example.fgluten.data.user

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Structured restaurant review model for comprehensive feedback.
 * 
 * This class provides a more detailed review system than crowd notes,
 * allowing users to rate specific aspects of their gluten-free dining experience.
 * 
 * @property id Unique identifier for the review
 * @property restaurantId Google Places ID of the restaurant
 * @property restaurantName Name of the restaurant for display
 * @property userId ID of the user who created the review
 * @property userDisplayName Display name of the reviewer
 * @property overallRating Overall rating (1-5 stars)
 * @property glutenFreeRating Gluten-free specific rating (1-5 stars)
 * @property safetyRating Food safety rating (1-5 stars)
 * @property serviceRating Service quality rating (1-5 stars)
 * @property menuOptionsRating Menu variety for GF options (1-5 stars)
 * @property crossContaminationRating Cross-contamination prevention (1-5 stars)
 * @property reviewText Detailed review text
 * @property visitDate Date of the restaurant visit
 * @property wasSafeForCeliac Whether the experience was safe for celiac disease
 * @property specificMenuItems List of specific gluten-free menu items tried
 * @property staffKnowledgeRating Staff knowledge about gluten-free needs (1-5 stars)
 * @property dedicatedFacility Whether restaurant has dedicated GF facility
 * @property recommendedToOthers Whether user recommends this restaurant
 * @property helpfulVotes Count of users who found this review helpful
 * @property createdAt Timestamp when review was created
 * @property updatedAt Timestamp when review was last updated
 * @property isVerified Whether this review is from a verified user
 * @property photos List of photo URLs from the visit
 */
@Parcelize
data class RestaurantReview(
    val id: String,
    val restaurantId: String,
    val restaurantName: String,
    val userId: String,
    val userDisplayName: String,
    val overallRating: Int,
    val glutenFreeRating: Int,
    val safetyRating: Int,
    val serviceRating: Int,
    val menuOptionsRating: Int,
    val crossContaminationRating: Int,
    val reviewText: String,
    val visitDate: Long,
    val wasSafeForCeliac: Boolean,
    val specificMenuItems: List<String> = emptyList(),
    val staffKnowledgeRating: Int = 0,
    val dedicatedFacility: Boolean = false,
    val recommendedToOthers: Boolean = true,
    val helpfulVotes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val photos: List<String> = emptyList()
) : Parcelable