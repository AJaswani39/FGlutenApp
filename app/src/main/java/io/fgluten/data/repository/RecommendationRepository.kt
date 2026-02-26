package io.fgluten.data.repository

import android.content.Context
import io.fgluten.data.Restaurant
import io.fgluten.data.recommendation.RecommendationReason
import io.fgluten.data.recommendation.RecommendedRestaurant
import io.fgluten.data.recommendation.UserInteractionTracker
import org.json.JSONObject

/**
 * Repository interface for personalized restaurant recommendations
 *
 * This interface provides a clean abstraction for restaurant recommendation logic,
 * allowing for easy testing and potential swapping of recommendation implementations.
 */
interface RecommendationRepository {

    /**
     * Get personalized restaurant recommendations
     *
     * @param restaurants List of restaurants to rank
     * @param context Android context for accessing SharedPreferences and interaction data
     * @return Sorted list of recommendations with scores and reasons
     */
    suspend fun getRecommendations(
        restaurants: List<Restaurant>,
        context: Context
    ): List<RecommendedRestaurant>

    /**
     * Get top N recommendations
     *
     * @param restaurants List of restaurants to rank
     * @param context Android context
     * @param limit Maximum number of recommendations to return
     * @return Top N recommendations
     */
    suspend fun getTopRecommendations(
        restaurants: List<Restaurant>,
        context: Context,
        limit: Int = 5
    ): List<RecommendedRestaurant>
}

/**
 * Default implementation of RecommendationRepository using content-based and preference-based scoring
 *
 * Scoring formula (0-100 scale):
 * - Favorite status: "safe"=+40, "try"=+15, "avoid"=-60 (40 points)
 * - Has GF options: +20 (20 points)
 * - Google Places rating: 0-5 → 0-15 (15 points)
 * - Distance: Closer = higher (15 points)
 * - User notes: +5 if user wrote notes (5 points)
 * - Currently open: +5 if open now (5 points)
 * - Previously visited: +10 if viewed ≥2 times (10 points)
 */
class DefaultRecommendationRepository : RecommendationRepository {

    override suspend fun getRecommendations(
        restaurants: List<Restaurant>,
        context: Context
    ): List<RecommendedRestaurant> {
        if (restaurants.isEmpty()) return emptyList()

        return restaurants
            .map { restaurant -> scoreRestaurant(restaurant, context) }
            .sortedByDescending { it.score }
    }

    override suspend fun getTopRecommendations(
        restaurants: List<Restaurant>,
        context: Context,
        limit: Int
    ): List<RecommendedRestaurant> {
        return getRecommendations(restaurants, context).take(limit)
    }

    // ========== PRIVATE SCORING LOGIC ==========

    /**
     * Score a single restaurant across all recommendation signals
     */
    private fun scoreRestaurant(
        restaurant: Restaurant,
        context: Context
    ): RecommendedRestaurant {
        var score = 50f // Start at baseline of 50
        val reasons = mutableListOf<RecommendationReason>()

        // 1. Favorite status (±40 points)
        val favoriteStatus = getFavoriteStatus(context, restaurant.placeId)
        when (favoriteStatus) {
            "safe" -> {
                score += 40
                reasons.add(RecommendationReason.FAVORITED_SAFE)
            }
            "try" -> {
                score += 15
                reasons.add(RecommendationReason.FAVORITED_TRY)
            }
            "avoid" -> {
                score -= 60 // Heavily penalize avoided restaurants
                reasons.add(RecommendationReason.FAVORITED_TRY) // Will be filtered out anyway
            }
        }

        // 2. GF options (20 points)
        if (restaurant.hasGlutenFreeOptions()) {
            score += 20
            reasons.add(RecommendationReason.HIGH_GF_OPTIONS)
        }

        // 3. Google Places rating (0-15 points)
        if (restaurant.rating != null && restaurant.rating > 0) {
            val ratingScore = (restaurant.rating / 5.0) * 15.0f
            score += ratingScore.toFloat()
            if (restaurant.rating >= 4.0) {
                reasons.add(RecommendationReason.NEARBY_HIGHLY_RATED)
            }
        }

        // 4. Distance penalty (0-15 points, closer is better)
        val distanceScore = calculateDistanceScore(restaurant.distanceMeters)
        score += distanceScore
        if (restaurant.distanceMeters < 1000 && score > 75) { // Close and highly scored
            reasons.add(RecommendationReason.NEARBY_HIGHLY_RATED)
        }

        // 5. User notes (5 points)
        if (hasUserNotes(context, restaurant.placeId)) {
            score += 5
            // Don't add reason here as it's implicit in other signals
        }

        // 6. Currently open (5 points)
        if (restaurant.openNow == true) {
            score += 5
            reasons.add(RecommendationReason.OPEN_NOW)
        }

        // 7. Previously visited (10 points)
        val viewCount = UserInteractionTracker.getViewCount(context, restaurant.placeId)
        if (viewCount >= 2) {
            score += 10
            reasons.add(RecommendationReason.PREVIOUSLY_VISITED)
        } else if (viewCount == 0 && score > 70) {
            // New discovery: similar to what they like but haven't seen yet
            reasons.add(RecommendationReason.NEW_TO_YOU)
        }

        // Clamp score to 0-100 range
        score = score.coerceIn(0f, 100f)

        // Generate primary reason if we have none
        val primaryReason = reasons.firstOrNull()?.displayName
            ?: when {
                favoriteStatus == "avoid" -> "Previously marked to avoid"
                restaurant.hasGlutenFreeOptions() -> "Has gluten-free options"
                restaurant.rating != null && restaurant.rating >= 4.0 -> "Highly rated"
                else -> "Nearby restaurant"
            }

        return RecommendedRestaurant(
            restaurant = restaurant,
            score = score,
            reason = primaryReason,
            reasons = reasons.distinctBy { it.name }
        )
    }

    /**
     * Calculate score boost based on distance
     * Closer restaurants score higher, max 15 points at 0m, min 0 at 10km+
     */
    private fun calculateDistanceScore(distanceMeters: Double): Float {
        return when {
            distanceMeters < 500 -> 15f
            distanceMeters < 1000 -> 12f
            distanceMeters < 2000 -> 10f
            distanceMeters < 5000 -> 7f
            distanceMeters < 10000 -> 3f
            else -> 0f
        }
    }

    /**
     * Get favorite status from SharedPreferences
     * Returns "safe", "avoid", "try", or null
     */
    private fun getFavoriteStatus(context: Context, placeId: String?): String? {
        return try {
            val prefs = context.getSharedPreferences("restaurant_favorites", Context.MODE_PRIVATE)
            val favoritesJson = prefs.getString("favorites_map", "{}")
                ?: "{}"

            val favorites = JSONObject(favoritesJson)
            val key = "pid:$placeId"

            if (favorites.has(key)) {
                favorites.getString(key)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if user has written notes for this restaurant
     */
    private fun hasUserNotes(context: Context, placeId: String?): Boolean {
        return try {
            val prefs = context.getSharedPreferences("restaurant_notes", Context.MODE_PRIVATE)
            val notesJson = prefs.getString("notes_map", "{}")
                ?: "{}"

            val notes = JSONObject(notesJson)
            val key = "pid:$placeId"

            notes.has(key) && notes.getJSONArray(key).length() > 0
        } catch (e: Exception) {
            false
        }
    }
}
