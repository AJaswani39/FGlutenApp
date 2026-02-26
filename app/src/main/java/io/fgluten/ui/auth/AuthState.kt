package io.fgluten.ui.auth

/**
 * Sealed class representing the different authentication states.
 * 
 * This class provides a type-safe way to represent the various states
 * of user authentication throughout the app lifecycle.
 */
sealed class AuthState {
    
    /**
     * Initial state when the app is starting up
     */
    object Initializing : AuthState()
    
    /**
     * User is signed in and authenticated
     */
    object Authenticated : AuthState()
    
    /**
     * User is not signed in
     */
    object Unauthenticated : AuthState()
    
    /**
     * Authentication failed due to an error
     */
    data class Error(val message: String) : AuthState()
}