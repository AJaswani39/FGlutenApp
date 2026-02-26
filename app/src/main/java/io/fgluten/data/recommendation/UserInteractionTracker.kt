package io.fgluten.data.recommendation

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

/**
 * Singleton object for tracking user interactions with restaurants
 *
 * Logs when users view restaurant cards or open detail sheets.
 * Data is stored locally in SharedPreferences as JSON for efficient access.
 *
 * Format: {"pid:xxx": {"views": 3, "lastViewed": 1234567890, "detailOpens": 2, "lastDetailOpen": 1234567890}}
 */
object UserInteractionTracker {

    enum class InteractionType {
        VIEW,      // User saw the restaurant in a list/card
        DETAIL_OPEN // User opened the detail bottom sheet
    }

    private const val PREFS_NAME = "restaurant_interactions"
    private const val INTERACTIONS_KEY = "interactions_map"

    /**
     * Record a user interaction with a restaurant
     *
     * @param context Android context for SharedPreferences access
     * @param placeId Google Places ID of the restaurant
     * @param type Type of interaction (VIEW or DETAIL_OPEN)
     */
    @JvmStatic
    fun recordInteraction(context: Context, placeId: String, type: InteractionType) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val interactionsJson = prefs.getString(INTERACTIONS_KEY, "{}")
                ?: "{}"

            val interactions = JSONObject(interactionsJson)
            val key = "pid:$placeId"

            val restaurantInteractions = if (interactions.has(key)) {
                interactions.getJSONObject(key)
            } else {
                JSONObject()
            }

            val now = System.currentTimeMillis()

            when (type) {
                InteractionType.VIEW -> {
                    val views = restaurantInteractions.optInt("views", 0) + 1
                    restaurantInteractions.put("views", views)
                    restaurantInteractions.put("lastViewed", now)
                }
                InteractionType.DETAIL_OPEN -> {
                    val detailOpens = restaurantInteractions.optInt("detailOpens", 0) + 1
                    restaurantInteractions.put("detailOpens", detailOpens)
                    restaurantInteractions.put("lastDetailOpen", now)
                }
            }

            interactions.put(key, restaurantInteractions)

            prefs.edit().putString(INTERACTIONS_KEY, interactions.toString()).apply()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    /**
     * Get the view count for a restaurant
     *
     * @param context Android context
     * @param placeId Google Places ID
     * @return Number of times the user has viewed this restaurant
     */
    @JvmStatic
    fun getViewCount(context: Context, placeId: String): Int {
        return getInteractionCount(context, placeId, "views")
    }

    /**
     * Get the detail-open count for a restaurant
     *
     * @param context Android context
     * @param placeId Google Places ID
     * @return Number of times the user has opened this restaurant's details
     */
    @JvmStatic
    fun getDetailOpenCount(context: Context, placeId: String): Int {
        return getInteractionCount(context, placeId, "detailOpens")
    }

    // ========== PRIVATE HELPERS ==========

    /**
     * Generic method to get an interaction count field
     */
    private fun getInteractionCount(context: Context, placeId: String, field: String): Int {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val interactionsJson = prefs.getString(INTERACTIONS_KEY, "{}")
                ?: "{}"

            val interactions = JSONObject(interactionsJson)
            val key = "pid:$placeId"

            if (interactions.has(key)) {
                interactions.getJSONObject(key).optInt(field, 0)
            } else {
                0
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            0
        }
    }

}
