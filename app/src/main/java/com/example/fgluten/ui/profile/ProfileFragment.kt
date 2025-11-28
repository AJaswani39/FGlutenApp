package com.example.fgluten.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fgluten.R
import com.example.fgluten.databinding.FragmentProfileBinding
import com.example.fgluten.ui.auth.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Profile Fragment - Comprehensive user management interface
 * 
 * This fragment serves as the central hub for all user account management,
 * providing access to profile editing, favorites, reviews, contributions,
 * and account settings. It follows Material Design guidelines and integrates
 * seamlessly with the app's navigation structure.
 * 
 * Features:
 * - User profile display with avatar, display name, and trust level
 * - Quick access to all user-related features
 * - Contribution statistics overview
 * - Account management options
 * - Sign out functionality
 * 
 * @see ProfileViewModel for data management
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        observeViewModel()
        setupClickListeners()
        loadUserProfile()
    }

    /**
     * Setup toolbar with menu and navigation
     */
    private fun setupToolbar() {
        binding.toolbar.setTitle(R.string.profile_title)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_profile -> {
                    navigateToEditProfile()
                    true
                }
                R.id.action_settings -> {
                    navigateToSettings()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Observe ViewModel data and state changes
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            profileViewModel.uiState.collect { uiState ->
                when (uiState) {
                    is ProfileUiState.Loading -> {
                        showLoading(true)
                        showContent(false)
                    }
                    is ProfileUiState.Success -> {
                        showLoading(false)
                        showContent(true)
                        displayUserProfile(uiState.userProfile)
                    }
                    is ProfileUiState.Error -> {
                        showLoading(false)
                        showContent(true)
                        showError(uiState.message)
                    }
                    is ProfileUiState.Initial -> {
                        showLoading(false)
                        showContent(false)
                    }
                }
            }
        }

        // Observe auth state for sign out handling
        authViewModel.authState.collect { authState ->
            if (authState.isUnauthenticated()) {
                findNavController().navigate(R.id.action_to_auth)
            }
        }
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private fun setupClickListeners() {
        // Quick action cards
        binding.cardFavorites.setOnClickListener {
            navigateToFavorites()
        }
        
        binding.cardReviews.setOnClickListener {
            navigateToReviews()
        }
        
        binding.cardNotes.setOnClickListener {
            navigateToNotes()
        }
        
        binding.cardContributions.setOnClickListener {
            navigateToContributions()
        }

        // Profile action buttons
        binding.editProfileButton.setOnClickListener {
            navigateToEditProfile()
        }
        
        binding.signOutButton.setOnClickListener {
            showSignOutConfirmation()
        }

        // Trust level info
        binding.trustLevelCard.setOnClickListener {
            showTrustLevelInfo()
        }
    }

    /**
     * Load user profile data
     */
    private fun loadUserProfile() {
        profileViewModel.loadUserProfile()
    }

    /**
     * Display user profile information
     */
    private fun displayUserProfile(userProfile: com.example.fgluten.data.user.UserProfile) {
        binding.apply {
            // Basic profile info
            profileName.text = userProfile.displayName
            profileEmail.text = userProfile.email
            contributorName.text = userProfile.getAttributionName()
            
            // Trust level and reputation
            trustLevel.text = userProfile.getTrustLevel()
            reputationScore.text = "%.1f".format(userProfile.reputationScore)
            helpfulVotes.text = userProfile.helpfulVotes.toString()
            
            // Contribution stats
            contributionCount.text = userProfile.contributionCount.toString()
            
            // Profile picture (placeholder for now)
            profileImage.setImageResource(R.drawable.ic_launcher_foreground)
            
            // Show/hide verification badge
            verificationBadge.visibility = if (userProfile.isVerified) {
                View.VISIBLE
            } else {
                View.GONE
            }
            
            // Update trust level color
            updateTrustLevelAppearance(userProfile.getTrustLevel())
        }
    }

    /**
     * Update trust level visual appearance
     */
    private fun updateTrustLevelAppearance(trustLevel: String) {
        val (backgroundColor, textColor) = when (trustLevel) {
            "Expert" -> Pair(R.color.trust_expert_bg, R.color.trust_expert_text)
            "Verified" -> Pair(R.color.trust_verified_bg, R.color.trust_verified_text)
            "Trusted" -> Pair(R.color.trust_trusted_bg, R.color.trust_trusted_text)
            "Active" -> Pair(R.color.trust_active_bg, R.color.trust_active_text)
            else -> Pair(R.color.trust_new_bg, R.color.trust_new_text)
        }
        
        binding.trustLevelCard.setCardBackgroundColor(
            resources.getColor(backgroundColor, requireContext().theme)
        )
        binding.trustLevel.setTextColor(
            resources.getColor(textColor, requireContext().theme)
        )
    }

    /**
     * Show loading state
     */
    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    /**
     * Show content state
     */
    private fun showContent(show: Boolean) {
        binding.contentContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.emptyState.visibility = if (!show) View.VISIBLE else View.GONE
    }

    /**
     * Show error message
     */
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    // Navigation methods
    private fun navigateToEditProfile() {
        findNavController().navigate(R.id.action_profile_to_edit_profile)
    }
    
    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_profile_to_settings)
    }
    
    private fun navigateToFavorites() {
        findNavController().navigate(R.id.action_profile_to_favorites)
    }
    
    private fun navigateToReviews() {
        findNavController().navigate(R.id.action_profile_to_reviews)
    }
    
    private fun navigateToNotes() {
        findNavController().navigate(R.id.action_profile_to_notes)
    }
    
    private fun navigateToContributions() {
        findNavController().navigate(R.id.action_profile_to_contributions)
    }

    /**
     * Show sign out confirmation dialog
     */
    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sign_out_confirmation)
            .setMessage(R.string.sign_out_message)
            .setPositiveButton(R.string.sign_out) { _, _ ->
                authViewModel.signOut()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Show trust level information dialog
     */
    private fun showTrustLevelInfo() {
        val message = getString(R.string.trust_level_explanation)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.trust_level_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Profile UI States for handling different loading and error states
 */
sealed class ProfileUiState {
    data object Initial : ProfileUiState()
    data object Loading : ProfileUiState()
    data class Success(val userProfile: com.example.fgluten.data.user.UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}