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
) : Parcelable {

    /**
     * Calculates the average rating across all rating categories
     */
    fun getAverageRating(): Double {
        val ratings = listOf(
            overallRating,
            glutenFreeRating,
            safetyRating,
            serviceRating,
            menuOptionsRating,
            crossContaminationRating
        ).filter { it > 0 }
        
        return if (ratings.isNotEmpty()) {
            ratings.average()
        } else {
            0.0
        }
    }

    /**
     * Gets the weighted score for gluten-free suitability
     * Prioritizes gluten-free specific ratings over general ratings
     */
    fun getGlutenFreeScore(): Double {
        val gfWeight = 0.4
        val safetyWeight = 0.3
        val overallWeight = 0.2
        val ccWeight = 0.1
        
        return (
            (glutenFreeRating * gfWeight) +
            (safetyRating * safetyWeight) +
            (overallRating * overallWeight) +
            (crossContaminationRating * ccWeight)
        )
    }

    /**
     * Determines if this review should be highlighted as a quality review
     */
    fun isQualityReview(): Boolean {
        return when {
            isVerified && getAverageRating() >= 4.0 -> true
            helpfulVotes >= 5 && getAverageRating() >= 4.0 -> true
            wasSafeForCeliac && glutenFreeRating >= 4 && safetyRating >= 4 -> true
            dedicatedFacility && staffKnowledgeRating >= 4 -> true
            else -> false
        }
    }

    /**
     * Gets summary tags for easy filtering and searching
     */
    fun getSummaryTags(): List<String> {
        val tags = mutableListOf<String>()
        
        if (wasSafeForCeliac) tags.add("Celiac Safe")
        if (dedicatedFacility) tags.add("Dedicated Facility")
        if (recommendedToOthers) tags.add("Recommended")
        if (staffKnowledgeRating >= 4) tags.add("Knowledgeable Staff")
        if (crossContaminationRating >= 4) tags.add("Low Cross-Contamination Risk")
        if (glutenFreeRating >= 4) tags.add("Good GF Options")
        
        return tags
    }

    /**
     * Validates that all ratings are within acceptable ranges
     */
    fun isValid(): Boolean {
        return overallRating in 1..5 &&
               glutenFreeRating in 1..5 &&
               safetyRating in 1..5 &&
               serviceRating in 1..5 &&
               menuOptionsRating in 1..5 &&
               crossContaminationRating in 1..5 &&
               reviewText.isNotBlank() &&
               visitDate > 0
    }
}