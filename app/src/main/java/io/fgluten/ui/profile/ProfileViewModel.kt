package io.fgluten.ui.profile

import androidx.lifecycle.ViewModel
import io.fgluten.data.user.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for ProfileFragment managing user profile data and state
 * 
 * This ViewModel handles:
 * - Managing profile-related UI state
 * - Provides a default local profile since login is not required.
 */
class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * Load a default local user profile since there is no login system
     */
    fun loadUserProfile() {
        _uiState.value = ProfileUiState.Loading

        try {
            // Provide a static default profile
            val defaultProfile = UserProfile(
                userId = "local_user",
                email = "guest@fgluten.io",
                displayName = "Guest User",
                helpfulVotes = 0,
                reputationScore = 0.0,
                isVerified = false
            )
            _uiState.value = ProfileUiState.Success(defaultProfile)
        } catch (e: Exception) {
            _uiState.value = ProfileUiState.Error("Error loading profile: ${e.message}")
        }
    }
}