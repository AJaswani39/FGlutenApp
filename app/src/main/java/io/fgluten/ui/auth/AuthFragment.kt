package io.fgluten.ui.auth

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import io.fgluten.R
import io.fgluten.databinding.FragmentAuthBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Authentication fragment handling user sign-in and sign-up.
 * 
 * This fragment provides a unified interface for both login and registration,
 * with support for email/password authentication and Google Sign-In.
 * It follows Material Design guidelines and integrates with AuthViewModel
 * for authentication state management.
 */
class AuthFragment : Fragment() {

    // ========== DATA BINDING & VIEWMODEL ==========
    
    /** Data binding object for the fragment's layout views */
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    
    /** ViewModel for authentication business logic */
    private val authViewModel: AuthViewModel by viewModels()

    /** Current authentication mode (Sign In or Sign Up) */
    private var isSignUpMode = false

    /**
     * Fragment view creation and initialization.
     * 
     * This method:
     * 1. Inflates the fragment layout
     * 2. Sets up ViewModel observers
     * 3. Configures UI event handlers
     * 4. Initializes the authentication interface
     * 
     * @param inflater Layout inflater for creating fragment views
     * @param container Parent view group for fragment attachment
     * @param savedInstanceState Previously saved instance state
     * @return The root view of the fragment
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Fragment view initialization after creation.
     * 
     * Sets up observers, event handlers, and initializes the UI state.
     * 
     * @param view The fragment's root view
     * @param savedInstanceState Previously saved instance state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupObservers()
        setupEventHandlers()
        updateUIForMode(false) // Start in sign-in mode
    }

    /**
     * Set up ViewModel observers for authentication state changes.
     * 
     * Observes:
     * - Authentication state changes
     * - Loading states
     * - Error messages
     * - User profile updates
     */
    private fun setupObservers() {
        // Observe authentication state changes
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                handleAuthStateChange(state)
            }
        }

        // Observe loading state for UI feedback
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            updateLoadingState(isLoading)
        }

        // Observe error messages for user feedback
        authViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showError(it)
                authViewModel.clearError() // Clear error after showing
            }
        }

        // Observe user profile changes (successful authentication)
        authViewModel.userProfile.observe(viewLifecycleOwner) { userProfile ->
            userProfile?.let {
                // Navigate to main app on successful authentication
                navigateToMain()
            }
        }
    }

    /**
     * Set up click event handlers for all UI interactions.
     */
    private fun setupEventHandlers() {
        // Main authentication button (Sign In/Sign Up)
        binding.authActionButton.setOnClickListener {
            handleAuthentication()
        }

        // Google Sign-In button
        binding.authGoogleButton.setOnClickListener {
            handleGoogleSignIn()
        }

        // Forgot password link
        binding.authForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        // Switch between Sign In and Sign Up modes
        binding.authSwitchMode.setOnClickListener {
            toggleAuthMode()
        }

        // Clear error message when user interacts with form
        binding.authEmailInput.setOnClickListener { authViewModel.clearError() }
        binding.authPasswordInput.setOnClickListener { authViewModel.clearError() }
        binding.authDisplayNameInput?.setOnClickListener { authViewModel.clearError() }
    }

    /**
     * Handle authentication form submission.
     * 
     * Validates input and calls appropriate authentication method
     * based on current mode (Sign In vs Sign Up).
     */
    private fun handleAuthentication() {
        val email = binding.authEmailInput.text?.toString()?.trim() ?: ""
        val password = binding.authPasswordInput.text?.toString() ?: ""
        
        if (isSignUpMode) {
            val displayName = binding.authDisplayNameInput?.text?.toString()?.trim() ?: ""
            val contributorName = binding.authContributorNameInput?.text?.toString()?.trim()
            
            if (validateSignUpInput(email, password, displayName)) {
                authViewModel.registerWithEmail(email, password, displayName, contributorName)
            }
        } else {
            if (validateSignInInput(email, password)) {
                authViewModel.signInWithEmail(email, password)
            }
        }
    }

    /**
     * Handle Google Sign-In authentication.
     * 
     * Note: This is a placeholder implementation. Google Sign-In requires
     * additional setup including Google Sign-In SDK integration.
     */
    private fun handleGoogleSignIn() {
        // TODO: Implement Google Sign-In
        // This requires:
        // 1. Google Sign-In SDK integration
        // 2. Google Sign-In button configuration
        // 3. ID token retrieval and passing to AuthViewModel

        Toast.makeText(context, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Handle forgot password navigation.
     */
    private fun navigateToForgotPassword() {
        // TODO: Navigate to forgot password fragment
        showError("Forgot password feature coming soon!")
    }

    /**
     * Toggle between Sign In and Sign Up modes.
     * 
     * Updates UI labels and form fields based on the new mode.
     */
    private fun toggleAuthMode() {
        isSignUpMode = !isSignUpMode
        updateUIForMode(isSignUpMode)
    }

    /**
     * Update UI components based on authentication mode.
     * 
     * @param signUpMode True for Sign Up mode, false for Sign In mode
     */
    private fun updateUIForMode(signUpMode: Boolean) {
        // Update main action button text
        val buttonText = if (signUpMode) R.string.auth_sign_up else R.string.auth_sign_in
        binding.authActionButton.setText(buttonText)

        // Update switch mode link text
        val switchText = if (signUpMode) R.string.auth_already_have_account else R.string.auth_dont_have_account
        binding.authSwitchMode.setText(switchText)

        // Show/hide additional fields for sign-up
        binding.authDisplayNameLayout.visibility = if (signUpMode) View.VISIBLE else View.GONE
        binding.authContributorNameLayout.visibility = if (signUpMode) View.VISIBLE else View.GONE

        // Update forgot password visibility (hide in sign-up mode)
        binding.authForgotPassword.visibility = if (signUpMode) View.GONE else View.VISIBLE

        // Clear form fields when switching modes
        clearFormFields()
    }

    /**
     * Clear all input fields.
     */
    private fun clearFormFields() {
        binding.authEmailInput.text?.clear()
        binding.authPasswordInput.text?.clear()
        binding.authDisplayNameInput?.text?.clear()
        binding.authContributorNameInput?.text?.clear()
    }

    /**
     * Handle authentication state changes from ViewModel.
     * 
     * @param state Current authentication state
     */
    private fun handleAuthStateChange(state: AuthState) {
        when (state) {
            is AuthState.Initializing -> {
                // Show loading if needed
            }
            is AuthState.Authenticated -> {
                // User is signed in, navigate to main app
                navigateToMain()
            }
            is AuthState.Unauthenticated -> {
                // User is signed out, ensure we're showing auth form
                // This should be the default state when fragment loads
            }
            is AuthState.Error -> {
                showError(state.message)
            }
        }
    }

    /**
     * Update loading state for UI components.
     * 
     * @param isLoading True if authentication is in progress
     */
    private fun updateLoadingState(isLoading: Boolean) {
        binding.authLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.authActionButton.isEnabled = !isLoading
        binding.authGoogleButton.isEnabled = !isLoading
    }

    /**
     * Navigate to the main app after successful authentication.
     */
    private fun navigateToMain() {
        // Navigate back to home fragment or main app area
        // Since auth was a separate flow, we can navigate back to home
        findNavController().navigate(R.id.nav_home)
    }

    /**
     * Show error message to user.
     * 
     * @param message Error message to display
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Dismiss") { }
            .show()
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate sign-in input.
     * 
     * @param email User's email address
     * @param password User's password
     * @return true if validation passes, false otherwise
     */
    private fun validateSignInInput(email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                showError("Email is required")
                false
            }
            TextUtils.isEmpty(password) -> {
                showError("Password is required")
                false
            }
            else -> true
        }
    }

    /**
     * Validate sign-up input.
     * 
     * @param email User's email address
     * @param password User's password
     * @param displayName User's display name
     * @return true if validation passes, false otherwise
     */
    private fun validateSignUpInput(email: String, password: String, displayName: String): Boolean {
        return when {
            TextUtils.isEmpty(email) -> {
                showError("Email is required")
                false
            }
            TextUtils.isEmpty(password) -> {
                showError("Password is required")
                false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters")
                false
            }
            TextUtils.isEmpty(displayName) -> {
                showError("Display name is required")
                false
            }
            displayName.length < 2 -> {
                showError("Display name must be at least 2 characters")
                false
            }
            else -> true
        }
    }

    /**
     * Fragment cleanup when view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}