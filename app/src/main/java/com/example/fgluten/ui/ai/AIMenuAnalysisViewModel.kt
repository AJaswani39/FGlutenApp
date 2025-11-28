package com.example.fgluten.ui.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fgluten.data.ai.MenuAnalysisResult
import com.example.fgluten.data.ai.AnalysisSource
import com.example.fgluten.data.repository.AIRepository
import com.example.fgluten.data.repository.DefaultAIRepository
import com.example.fgluten.data.repository.AIStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AI-powered menu analysis functionality
 * 
 * This ViewModel manages the state and business logic for AI-powered gluten-free
 * menu analysis features. It provides a clean interface for the UI layer to:
 * 
 * - Trigger menu analysis for restaurants
 * - Display analysis results with proper loading states
 * - Manage AI service status and model downloads
 * - Cache analysis results for offline access
 * 
 * The ViewModel follows MVVM architecture and integrates seamlessly with
 * the existing restaurant detail views and bottom sheets.
 * 
 * @property aiRepository Repository providing AI analysis services
 */
class AIMenuAnalysisViewModel(
    private val aiRepository: AIRepository = DefaultAIRepository()
) : ViewModel() {

    // ========== UI STATE MANAGEMENT ==========
    
    /**
     * Current state of AI analysis for UI display
     */
    private val _uiState = MutableStateFlow<AIMenuAnalysisUiState>(AIMenuAnalysisUiState.Initial)
    val uiState: StateFlow<AIMenuAnalysisUiState> = _uiState.asStateFlow()
    
    /**
     * Current status of AI services
     */
    private val _aiStatus = MutableStateFlow<AIStatus>(AIStatus.Downloading)
    val aiStatus: StateFlow<AIStatus> = _aiStatus.asStateFlow()

    // ========== ANALYSIS METHODS ==========
    
    /**
     * Analyze menu text for gluten-free safety
     * 
     * @param menuText Raw menu text to analyze
     * @param restaurantName Name of the restaurant for context
     * @param sourceType Source of the menu data (website, photo, etc.)
     */
    fun analyzeMenu(
        menuText: String,
        restaurantName: String,
        sourceType: AnalysisSource = AnalysisSource.WEBSITE
    ) {
        // Validate input
        if (menuText.isBlank()) {
            _uiState.value = AIMenuAnalysisUiState.Error("Menu text is required for analysis")
            return
        }
        
        if (restaurantName.isBlank()) {
            _uiState.value = AIMenuAnalysisUiState.Error("Restaurant name is required")
            return
        }

        // Update UI state to loading
        _uiState.value = AIMenuAnalysisUiState.Loading

        viewModelScope.launch {
            try {
                // Check if AI services are ready
                if (!aiRepository.isReadyForAnalysis()) {
                    _uiState.value = AIMenuAnalysisUiState.Error("AI services are not ready. Please try again.")
                    return@launch
                }

                // Perform analysis
                val result = aiRepository.analyzeMenuText(menuText, restaurantName, sourceType)
                
                // Update UI with successful result
                _uiState.value = AIMenuAnalysisUiState.Success(result)
                
            } catch (e: Exception) {
                // Handle analysis errors
                val errorMessage = when (e) {
                    is IllegalStateException -> "AI service unavailable: ${e.message}"
                    else -> "Analysis failed: ${e.message}"
                }
                _uiState.value = AIMenuAnalysisUiState.Error(errorMessage)
            }
        }
    }

    /**
     * Retry analysis with previously provided menu text
     * This is called when the user taps "Retry" after an error
     */
    fun retryAnalysis() {
        val currentState = _uiState.value
        if (currentState is AIMenuAnalysisUiState.Error && currentState.lastAttempt != null) {
            analyzeMenu(
                menuText = currentState.lastAttempt.menuText,
                restaurantName = currentState.lastAttempt.restaurantName,
                sourceType = currentState.lastAttempt.sourceType
            )
        }
    }

    /**
     * Clear current analysis results and return to initial state
     */
    fun clearAnalysis() {
        _uiState.value = AIMenuAnalysisUiState.Initial
    }

    /**
     * Download AI models for offline functionality
     * 
     * @param context Android context for model storage
     */
    fun downloadModels(context: Context) {
        viewModelScope.launch {
            try {
                _aiStatus.value = AIStatus.Downloading
                val result = aiRepository.downloadModels(context)
                
                if (result.isSuccess) {
                    _aiStatus.value = AIStatus.Ready
                } else {
                    _aiStatus.value = AIStatus.Error("Failed to download AI models: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _aiStatus.value = AIStatus.Error("Download failed: ${e.message}")
            }
        }
    }

    /**
     * Refresh AI service status
     */
    fun refreshStatus() {
        viewModelScope.launch {
            try {
                _aiStatus.value = aiRepository.getStatus()
            } catch (e: Exception) {
                _aiStatus.value = AIStatus.Error("Status check failed: ${e.message}")
            }
        }
    }

    /**
     * Check if AI analysis should be triggered for a restaurant
     * 
     * This method provides smart triggers for when to analyze menus
     * based on restaurant data availability and analysis cache.
     * 
     * @param restaurantName Name of the restaurant
     * @param menuUrl URL to restaurant menu (if available)
     * @param hasExistingAnalysis Whether this restaurant already has analysis
     * @return true if analysis should be triggered, false otherwise
     */
    fun shouldAnalyzeMenu(
        restaurantName: String,
        menuUrl: String?,
        hasExistingAnalysis: Boolean
    ): Boolean {
        // Don't re-analyze if we already have results
        if (hasExistingAnalysis) return false
        
        // Analyze if we have menu text or URL
        return menuUrl != null || restaurantName.isNotBlank()
    }

    /**
     * Generate sample menu text for testing purposes
     * 
     * This is useful for development and testing the AI analysis functionality
     * without requiring real restaurant data.
     */
    fun generateSampleMenuText(): Pair<String, String> {
        val sampleRestaurants = listOf(
            Pair(
                "Bella Vista Italian Restaurant",
                """
                Appetizers
                Bruschetta - Grilled bread with fresh tomatoes, basil, and garlic - $8
                Calamari Fritti - Lightly breaded and fried squid with marinara sauce - $12
                
                Pasta
                Spaghetti Carbonara - Traditional Roman pasta with eggs, cheese, pancetta, and black pepper - $16
                Gluten-Free Pasta with Bolognese - House-made GF pasta with meat sauce - $18
                Fettuccine Alfredo - Creamy sauce with parmesan and butter - $15
                
                Main Courses
                Grilled Salmon - Fresh Atlantic salmon with herbs and lemon - $24
                Gluten-Free Pizza Margherita - Dedicated gluten-free oven, fresh mozzarella and basil - $20
                Chicken Parmesan - Breaded chicken with marinara and mozzarella - $22
                Risotto ai Funghi - Creamy mushroom risotto (naturally gluten-free) - $19
                
                Desserts
                Tiramisu - Traditional Italian dessert with ladyfingers - $8
                Gluten-Free Panna Cotta - Vanilla bean custard with berry sauce - $7
                """
            ),
            Pair(
                "American Grill & Sports Bar",
                """
                Starters
                Buffalo Wings - Chicken wings tossed in buffalo sauce - $11
                Loaded Nachos - Tortilla chips with cheese, jalapeños, and sour cream - $13
                Onion Rings - Beer-battered onion rings - $9
                
                Burgers & Sandwiches
                Classic Cheeseburger - Beef patty with cheese, lettuce, tomato, onion - $14
                Grilled Chicken Sandwich - Marinated chicken breast with avocado and mayo - $13
                Turkey Club - Roasted turkey with bacon, lettuce, and tomato - $12
                
                Salads
                Caesar Salad - Romaine lettuce with parmesan and croutons - $11
                Gluten-Free Quinoa Bowl - Quinoa with roasted vegetables and tahini dressing - $14
                
                Main Courses
                Ribeye Steak - 12oz ribeye with garlic mashed potatoes - $28
                Salmon Teriyaki - Grilled salmon with teriyaki glaze and rice - $26
                Fish & Chips - Beer-battered cod with coleslaw - $18
                """
            )
        )
        
        return sampleRestaurants.random()
    }
}

/**
 * UI states for AI menu analysis functionality
 * 
 * These states provide a complete representation of all possible states
 * that the AI analysis feature can be in, enabling proper UI rendering
 * and user feedback.
 */
sealed class AIMenuAnalysisUiState {
    data object Initial : AIMenuAnalysisUiState()
    
    data object Loading : AIMenuAnalysisUiState()
    
    data class Success(
        val result: MenuAnalysisResult
    ) : AIMenuAnalysisUiState()
    
    data class Error(
        val message: String,
        val lastAttempt: LastAnalysisAttempt? = null
    ) : AIMenuAnalysisUiState()
    
    data class NoAnalysis(
        val reason: String
    ) : AIMenuAnalysisUiState()
}

/**
 * Data class to store the last failed analysis attempt for retry functionality
 */
data class LastAnalysisAttempt(
    val menuText: String,
    val restaurantName: String,
    val sourceType: AnalysisSource
)
