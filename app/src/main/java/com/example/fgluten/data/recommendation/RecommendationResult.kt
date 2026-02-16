package com.example.fgluten.data.recommendation

import com.example.fgluten.data.Restaurant

/**
 * Represents a restaurant recommendation with scoring and reasoning
 *
 * @property restaurant The restaurant being recommended
 * @property score Recommendation score (0-100), higher is better
 * @property reason Primary reason for this recommendation
 * @property reasons List of all matching recommendation criteria
 */
data class RecommendedRestaurant(
    val restaurant: Restaurant,
    val score: Float,
    val reason: String,
    val reasons: List<RecommendationReason> = emptyList()
)

/**
 * Summary of multiple recommendations for display
 *
 * @property recommendations Sorted list of recommended restaurants
 * @property totalCount Total number of recommendations
 */
data class RecommendationSummary(
    val recommendations: List<RecommendedRestaurant>,
    val totalCount: Int
)

/**
 * Enumeration of reasons why a restaurant is recommended
 */
enum class RecommendationReason(
    val displayName: String,
    val description: String,
    val emoji: String
) {
    FAVORITED_SAFE(
        "Marked Safe",
        "You marked this restaurant as safe for gluten-free",
        "✅"
    ),
    FAVORITED_TRY(
        "Want to Try",
        "You're interested in trying this restaurant",
        "⭐"
    ),
    HIGH_GF_OPTIONS(
        "Good GF Options",
        "Restaurant has confirmed gluten-free options",
        "🍽️"
    ),
    NEARBY_HIGHLY_RATED(
        "Highly Rated Nearby",
        "Highly rated restaurant close to you",
        "📍"
    ),
    PREVIOUSLY_VISITED(
        "Visited Before",
        "You've shown interest in this restaurant",
        "🔄"
    ),
    OPEN_NOW(
        "Open Now",
        "Restaurant is currently open",
        "🟢"
    ),
    COMMUNITY_APPROVED(
        "Community Favorite",
        "Other users have marked this as safe",
        "👥"
    ),
    NEW_TO_YOU(
        "New Discovery",
        "Similar to restaurants you like",
        "✨"
    )
}
