package com.example.fgluten.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.fgluten.data.repository.AuthRepository
import com.example.fgluten.data.user.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication functionality.
 * 
 * This ViewModel handles all authentication-related state management
 * and business logic, providing a clean interface for the UI layer.
 * It follows the MVVM pattern and integrates with the existing
 * architecture of the FGluten app.
 * 
 * @property authRepository Repository for authentication operations
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    // ========== AUTHENTICATION STATE ==========
    
    /**
     * Current authentication state
     */
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    /**
     * Current user profile data
     */
    private val _userProfile = MutableLiveData<UserProfile?>()
    val userProfile: LiveData<UserProfile?> = _userProfile
    
    /**
     * Loading state for UI feedback
     */
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * Error messages for display
     */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Initialize authentication state monitoring
     */
    init {
        observeAuthState()
    }

    /**
     * Observe Firebase Auth state changes and update ViewModel state accordingly
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { firebaseUser ->
                if (firebaseUser != null) {
                    // User is signed in, load their profile
                    _authState.value = AuthState.Authenticated
                    loadUserProfile(firebaseUser.uid)
                } else {
                    // User is signed out
                    _authState.value = AuthState.Unauthenticated
                    _userProfile.value = null
                }
            }
        }
    }

    /**
     * Load user profile from Firestore
     * 
     * @param userId User's Firebase UID
     */
    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val result = authRepository.getUserProfile(userId)
                if (result.isSuccess) {
                    _userProfile.value = result.getOrNull()
                } else {
                    _errorMessage.value = "Failed to load user profile: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading profile: ${e.message}"
            }
        }
    }

    /**
     * Register new user with email and password
     * 
     * @param email User's email address
     * @param password User's password
     * @param displayName User's chosen display name
     * @param contributorName Optional name for crowd notes attribution
     */
    fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        contributorName: String? = null
    ) {
        if (!validateEmailPassword(email, password) || !validateDisplayName(displayName)) {
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = authRepository.registerWithEmail(
                    email, password, displayName, contributorName
                )
                
                if (result.isSuccess) {
                    _authState.value = AuthState.Authenticated
                    _userProfile.value = result.getOrNull()
                } else {
                    _errorMessage.value = "Registration failed: ${result.exceptionOrNull()?.message}"
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _errorMessage.value = "Registration error: ${e.message}"
                _authState.value = AuthState.Unauthenticated
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign in with email and password
     * 
     * @param email User's email address
     * @param password User's password
     */
    fun signInWithEmail(email: String, password: String) {
        if (!validateEmailPassword(email, password)) {
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmail(email, password)
                
                if (result.isSuccess) {
                    _authState.value = AuthState.Authenticated
                    _userProfile.value = result.getOrNull()
                } else {
                    _errorMessage.value = "Sign in failed: ${result.exceptionOrNull()?.message}"
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sign in error: ${e.message}"
                _authState.value = AuthState.Unauthenticated
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        authRepository.signOut()
        _authState.value = AuthState.Unauthenticated
        _userProfile.value = null
        _errorMessage.value = null
    }

    /**
     * Clear error message (called after user acknowledges error)
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate email and password inputs
     * 
     * @param email Email address to validate
     * @param password Password to validate
     * @return true if validation passes, false otherwise
     */
    private fun validateEmailPassword(email: String, password: String): Boolean {
        val emailError = when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
        
        val passwordError = when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        
        if (emailError != null || passwordError != null) {
            _errorMessage.value = listOfNotNull(emailError, passwordError).joinToString("\n")
            return false
        }
        
        return true
    }

    /**
     * Validate display name
     * 
     * @param displayName Display name to validate
     * @return true if validation passes, false otherwise
     */
    private fun validateDisplayName(displayName: String): Boolean {
        val nameError = when {
            displayName.isBlank() -> "Display name is required"
            displayName.length < 2 -> "Display name must be at least 2 characters"
            displayName.length > 50 -> "Display name must be less than 50 characters"
            else -> null
        }
        
        if (nameError != null) {
            _errorMessage.value = nameError
            return false
        }
        
        return true
    }
}
