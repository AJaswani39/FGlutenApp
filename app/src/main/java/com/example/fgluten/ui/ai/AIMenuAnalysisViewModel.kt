package com.example.fgluten.ui.ai

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
