package com.example.fgluten.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fgluten.data.Restaurant
import com.example.fgluten.data.recommendation.RecommendedRestaurant
import com.example.fgluten.data.repository.DefaultRecommendationRepository
import com.example.fgluten.data.repository.RecommendationRepository
import kotlinx.coroutines.launch
import kotlin.jvm.JvmOverloads

/**
 * ViewModel for restaurant recommendations feature
 *
 * This ViewModel manages the state and business logic for personalized restaurant
 * recommendations. It observes changes in the restaurant list and produces a scored,
 * ranked set of recommendations for display in the Home screen.
 *
 * @property recommendationRepository Repository providing recommendation scoring logic
 */
class RecommendationViewModel @JvmOverloads constructor(
    application: Application,
    private val recommendationRepository: RecommendationRepository = DefaultRecommendationRepository()
) : AndroidViewModel(application) {

    // ========== REACTIVE STATE MANAGEMENT ==========

    /**
     * List of top recommendations (max 5)
     */
    private val _recommendations = MutableLiveData<List<RecommendedRestaurant>>(emptyList())
    val recommendations: LiveData<List<RecommendedRestaurant>> = _recommendations

    /**
     * Loading state for recommendation computation
     */
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Error message if recommendation generation fails
     */
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // ========== PUBLIC METHODS ==========

    /**
     * Generate recommendations from a list of restaurants
     *
     * This method scores all restaurants and returns the top 5 recommendations.
     * Should be called whenever the available restaurant list changes.
     *
     * @param restaurants List of all available restaurants
     */
    fun generateRecommendations(restaurants: List<Restaurant>) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val ranked = recommendationRepository.getTopRecommendations(
                    restaurants = restaurants,
                    context = getApplication<Application>().applicationContext,
                    limit = 5
                )

                _recommendations.postValue(ranked)
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to generate recommendations: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Clear all recommendations
     */
    fun clearRecommendations() {
        _recommendations.value = emptyList()
        _errorMessage.value = null
    }

    /**
     * Get recommendations that meet a minimum score threshold
     *
     * Useful for filtering out lower-quality recommendations
     *
     * @param minScore Minimum recommendation score (0-100)
     * @return Filtered list of recommendations above the threshold
     */
    fun getRecommendationsAboveScore(minScore: Float): List<RecommendedRestaurant> {
        return _recommendations.value?.filter { it.score >= minScore } ?: emptyList()
    }

    /**
     * Get a single recommendation by restaurant place ID
     *
     * @param placeId Google Places ID
     * @return The recommendation if found, null otherwise
     */
    fun getRecommendationByPlaceId(placeId: String?): RecommendedRestaurant? {
        return _recommendations.value?.find { it.restaurant.placeId == placeId }
    }
}
