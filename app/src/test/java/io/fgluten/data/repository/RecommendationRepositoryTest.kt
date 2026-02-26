package io.fgluten.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.fgluten.data.Restaurant
import io.fgluten.data.recommendation.RecommendationReason
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for DefaultRecommendationRepository
 *
 * Tests cover all scoring signals and edge cases including:
 * - Favorite status scoring
 * - GF options detection
 * - Distance calculations
 * - Rating influence
 * - Empty/null input handling
 */
@RunWith(RobolectricTestRunner::class)
class RecommendationRepositoryTest {

    private lateinit var repository: DefaultRecommendationRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        repository = DefaultRecommendationRepository()
        context = ApplicationProvider.getApplicationContext()
    }

    // ========== FAVORITE STATUS SCORING TESTS ==========

    @Test
    fun testSafeFavoritedRestaurantScoresHighest() = runBlocking {
        val restaurantA = createTestRestaurant(
            "Safe Restaurant",
            placeId = "place1",
            hasGFMenu = false,
            rating = 3.0
        )
        val restaurantB = createTestRestaurant(
            "Unrated Restaurant",
            placeId = "place2",
            hasGFMenu = false,
            rating = 3.0
        )

        // Mark A as "safe"
        setFavoriteStatus(context, restaurantA.placeId, "safe")

        val recommendations = repository.getRecommendations(
            listOf(restaurantA, restaurantB),
            context
        )

        assert(recommendations[0].restaurant.placeId == "place1")
        assert(recommendations[0].score > recommendations[1].score)
    }

    @Test
    fun testAvoidedRestaurantScoresLowest() = runBlocking {
        val restaurantA = createTestRestaurant(
            "Avoided Restaurant",
            placeId = "place1",
            hasGFMenu = true,
            rating = 5.0
        )
        val restaurantB = createTestRestaurant(
            "Good Restaurant",
            placeId = "place2",
            hasGFMenu = true,
            rating = 4.0
        )

        // Mark A as "avoid"
        setFavoriteStatus(context, restaurantA.placeId, "avoid")

        val recommendations = repository.getRecommendations(
            listOf(restaurantA, restaurantB),
            context
        )

        // B should score higher than A
        assert(recommendations[0].restaurant.placeId == "place2")
        assert(recommendations[1].score < 50f) // Avoided restaurants should score below baseline
    }

    @Test
    fun testTryFavoritedRestaurantScoresModerate() = runBlocking {
        val restaurantA = createTestRestaurant(
            "Try Restaurant",
            placeId = "place1",
            hasGFMenu = false,
            rating = 3.0
        )
        val restaurantB = createTestRestaurant(
            "Unrated Restaurant",
            placeId = "place2",
            hasGFMenu = false,
            rating = 3.0
        )

        // Mark A as "try"
        setFavoriteStatus(context, restaurantA.placeId, "try")

        val recommendations = repository.getRecommendations(
            listOf(restaurantA, restaurantB),
            context
        )

        // A should score higher than B but not as high as "safe"
        assert(recommendations[0].restaurant.placeId == "place1")
        val tryScore = recommendations[0].score
        val unratedScore = recommendations[1].score
        assert(tryScore > unratedScore)
        assert(tryScore < 80f) // "try" should be moderate, not high
    }

    // ========== GF OPTIONS SCORING TESTS ==========

    @Test
    fun testRestaurantWithGFOptionsScoresHigher() = runBlocking {
        val restaurantA = createTestRestaurant(
            "GF Restaurant",
            placeId = "place1",
            hasGFMenu = true,
            rating = 3.0
        )
        val restaurantB = createTestRestaurant(
            "No GF Restaurant",
            placeId = "place2",
            hasGFMenu = false,
            rating = 3.0
        )

        val recommendations = repository.getRecommendations(
            listOf(restaurantA, restaurantB),
            context
        )

        assert(recommendations[0].restaurant.placeId == "place1")
        assert(recommendations[0].score > recommendations[1].score)
        assert(recommendations[0].reasons.contains(RecommendationReason.HIGH_GF_OPTIONS))
    }

    // ========== DISTANCE SCORING TESTS ==========

    @Test
    fun testCloserRestaurantScoresHigher() = runBlocking {
        val restaurantA = createTestRestaurant(
            "Close Restaurant",
            placeId = "place1",
            hasGFMenu = false,
            rating = 3.0,
            distanceMeters = 500.0
        )
        val restaurantB = createTestRestaurant(
            "Far Restaurant",
            placeId = "place2",
            hasGFMenu = false,
            rating = 3.0,
            distanceMeters = 5000.0
        )

        val recommendations = repository.getRecommendations(
            listOf(restaurantA, restaurantB),
            context
        )

        assert(recommendations[0].restaurant.placeId == "place1")
        assert(recommendations[0].score > recommendations[1].score)
    }

    // ========== RATING SCORING TESTS ==========

    @Test
    fun testHigherRatedRestaurantScoresHigher() = runBlocking {
        val restaurantA = createTestRestaurant(
            "High Rated",
            placeId = "place1",
            hasGFMenu = false,
            rating = 5.0,
            distanceMeters = 1000.0
        )
        val restaurantB = createTestRestaurant(
            "Low Rated",
            placeId = "place2",
            hasGFMenu = false,
            rating = 2.0,
            distanceMeters = 1000.0
        )

        val recommendations = repository.getRecommendations(
            listOf(restaurantA, restaurantB),
            context
        )

        assert(recommendations[0].restaurant.placeId == "place1")
        assert(recommendations[0].score > recommendations[1].score)
    }

    // ========== COMBINED SIGNAL TESTS ==========

    @Test
    fun testCombinedSignalsWork() = runBlocking {
        val restaurants = listOf(
            createTestRestaurant(
                "Great Safe Place",
                placeId = "place1",
                hasGFMenu = true,
                rating = 5.0,
                distanceMeters = 500.0,
                openNow = true
            ),
            createTestRestaurant(
                "Mediocre Place",
                placeId = "place2",
                hasGFMenu = false,
                rating = 3.0,
                distanceMeters = 2000.0,
                openNow = false
            ),
            createTestRestaurant(
                "Good Place But Far",
                placeId = "place3",
                hasGFMenu = true,
                rating = 4.5,
                distanceMeters = 10000.0,
                openNow = true
            )
        )

        // Mark first as safe
        setFavoriteStatus(context, "place1", "safe")

        val recommendations = repository.getRecommendations(restaurants, context)

        // Place1 should be first (safe + GF + high rating + close + open)
        assert(recommendations[0].restaurant.placeId == "place1")
        assert(recommendations[0].score > 80f)
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun testEmptyListReturnsEmpty() = runBlocking {
        val recommendations = repository.getRecommendations(emptyList(), context)
        assert(recommendations.isEmpty())
    }

    @Test
    fun testNullRatingDoesntCrash() = runBlocking {
        val restaurant = createTestRestaurant(
            "No Rating",
            placeId = "place1",
            rating = null
        )

        val recommendations = repository.getRecommendations(listOf(restaurant), context)

        assert(recommendations.size == 1)
        assert(recommendations[0].score >= 0f && recommendations[0].score <= 100f)
    }

    @Test
    fun testNullOpenNowDoesntCrash() = runBlocking {
        val restaurant = createTestRestaurant(
            "Unknown Hours",
            placeId = "place1",
            openNow = null
        )

        val recommendations = repository.getRecommendations(listOf(restaurant), context)

        assert(recommendations.size == 1)
        assert(recommendations[0].score >= 0f && recommendations[0].score <= 100f)
    }

    @Test
    fun testScoresAreClamped() = runBlocking {
        val restaurant = createTestRestaurant(
            "Perfect Restaurant",
            placeId = "place1",
            hasGFMenu = true,
            rating = 5.0,
            distanceMeters = 100.0,
            openNow = true
        )

        setFavoriteStatus(context, "place1", "safe")

        val recommendations = repository.getRecommendations(listOf(restaurant), context)

        assert(recommendations[0].score <= 100f)
        assert(recommendations[0].score >= 0f)
    }

    @Test
    fun testTopRecommendationsLimitsResults() = runBlocking {
        val restaurants = (1..10).map { i ->
            createTestRestaurant(
                "Restaurant $i",
                placeId = "place$i",
                rating = (5 - i/2).toDouble()
            )
        }

        val topRecommendations = repository.getTopRecommendations(restaurants, context, limit = 5)

        assert(topRecommendations.size == 5)
    }

    @Test
    fun testRecommendationsAreSorted() = runBlocking {
        val restaurants = listOf(
            createTestRestaurant("Place A", placeId = "placeA", rating = 3.0),
            createTestRestaurant("Place B", placeId = "placeB", rating = 5.0),
            createTestRestaurant("Place C", placeId = "placeC", rating = 2.0)
        )

        val recommendations = repository.getRecommendations(restaurants, context)

        // Should be sorted by score (descending)
        assert(recommendations[0].score >= recommendations[1].score)
        assert(recommendations[1].score >= recommendations[2].score)
    }

    // ========== HELPER METHODS ==========

    private fun createTestRestaurant(
        name: String,
        placeId: String,
        hasGFMenu: Boolean = false,
        rating: Double? = null,
        distanceMeters: Double = 1000.0,
        openNow: Boolean? = null
    ): Restaurant {
        return Restaurant(
            name = name,
            address = "123 Test St",
            hasGFMenu = hasGFMenu,
            gfMenu = if (hasGFMenu) listOf("Test Item") else emptyList(),
            latitude = 40.7128,
            longitude = -74.0060,
            rating = rating,
            openNow = openNow,
            placeId = placeId
        ).apply {
            setDistanceMeters(distanceMeters)
        }
    }

    private fun setFavoriteStatus(context: Context, placeId: String?, status: String) {
        val prefs = context.getSharedPreferences("restaurant_favorites", Context.MODE_PRIVATE)
        val favoritesJson = prefs.getString("favorites_map", "{}") ?: "{}"
        val favorites = org.json.JSONObject(favoritesJson)
        favorites.put("pid:$placeId", status)
        prefs.edit().putString("favorites_map", favorites.toString()).apply()
    }
}
