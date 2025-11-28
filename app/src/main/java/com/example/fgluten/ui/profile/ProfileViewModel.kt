package com.example.fgluten.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fgluten.data.repository.AuthRepository
import com.example.fgluten.data.user.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for ProfileFragment managing user profile data and state
 * 
 * This ViewModel handles:
 * - Loading user profile from Firestore
 * - Managing profile-related UI state
 * - Profile data validation and updates
 * - Integration with AuthRepository for profile operations
 * 
 * @property authRepository Repository for authentication and profile operations
 */
class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Load current user profile from Firestore
     */
    fun loadUserProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error("No authenticated user")
            return
        }

        _uiState.value = ProfileUiState.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    _uiState.value = ProfileUiState.Success(userProfile)
                } else {
                    _uiState.value = ProfileUiState.Error(
                        "Failed to load profile: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Error loading profile: ${e.message}")
            }
        }
    }

    /**
     * Update user profile information
     * 
     * @param updates Map of field names to new values
     */
    fun updateProfile(updates: Map<String, Any>) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error("No authenticated user")
            return
        }

        _uiState.value = ProfileUiState.Loading

        viewModelScope.launch {
            try {
                val result = authRepository.updateUserProfile(currentUser.uid, updates)
                if (result.isSuccess) {
                    // Reload profile to reflect changes
                    loadUserProfile()
                } else {
                    _uiState.value = ProfileUiState.Error(
                        "Failed to update profile: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Error updating profile: ${e.message}")
            }
        }
    }

    /**
     * Update user's contribution statistics
     * 
     * @param helpfulVotesChange Change in helpful votes count
     */
    fun updateContributions(helpfulVotesChange: Int = 0) {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) return

        viewModelScope.launch {
            try {
                authRepository.updateUserContributions(currentUser.uid, helpfulVotesChange)
                // Reload profile to reflect updated statistics
                loadUserProfile()
            } catch (e: Exception) {
                // Silent fail for contribution updates as they're not critical
            }
        }
    }

    /**
     * Refresh profile data
     */
    fun refreshProfile() {
        loadUserProfile()
    }
}