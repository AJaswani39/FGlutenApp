package com.example.fgluten.ui.home

import android.app.Application
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
        viewModelScope.launch {
            try {
                val ranked = recommendationRepository.getTopRecommendations(
                    restaurants = restaurants,
                    context = getApplication<Application>().applicationContext,
                    limit = 5
                )

                _recommendations.postValue(ranked)
            } catch (e: Exception) {
                // Log error but don't crash - recommendations are non-critical
            }
        }
    }

    /**
     * Clear all recommendations
     */
    fun clearRecommendations() {
        _recommendations.value = emptyList()
    }
}
