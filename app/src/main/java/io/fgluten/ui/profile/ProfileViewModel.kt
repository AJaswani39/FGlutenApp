package io.fgluten.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.fgluten.data.repository.AuthRepository
import io.fgluten.data.user.UserProfile
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

}