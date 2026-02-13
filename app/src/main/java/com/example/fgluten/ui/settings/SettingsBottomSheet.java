package com.example.fgluten.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.fgluten.R;
import com.example.fgluten.ui.auth.AuthViewModel;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet dialog for managing application settings.
 * 
 * This fragment provides a modal interface for users to configure application-wide
 * preferences including:
 * 
 * **Theme Settings:**
 * - Light theme mode
 * - Dark theme mode
 * - System default (follows device setting)
 * 
 * **Unit Preferences:**
 * - Distance measurement in kilometers/meters (metric)
 * - Distance measurement in miles/feet (imperial)
 * 
 * **User Experience:**
 * - Modal bottom sheet design for focused settings access
 * - Immediate application of theme changes
 * - Persistent storage via SettingsManager
 * - Intuitive radio button selection interface
 * 
 * The dialog integrates with SettingsManager for seamless preference management
 * and provides immediate visual feedback when settings are changed.
 * 
 * @see SettingsManager for underlying preference storage
 * @see AppCompatDelegate for theme management
 * 
 * @author FGluten Development Team
 */
public class SettingsBottomSheet extends BottomSheetDialogFragment {

    private AuthViewModel authViewModel;

    /**
     * Factory method for creating a new instance of the settings bottom sheet.
     * 
     * This method creates a new SettingsBottomSheet instance using the default constructor.
     * Since this fragment doesn't require any arguments, a simple factory method
     * provides a clean interface for instantiation consistent with Android patterns.
     * 
     * @return New instance of SettingsBottomSheet ready for display
     */
    public static SettingsBottomSheet newInstance() {
        return new SettingsBottomSheet();
    }

    /**
     * Creates and inflates the settings bottom sheet layout.
     * 
     * This lifecycle method is called when the fragment needs to create its user interface.
     * It inflates the bottom_sheet_settings layout which contains the settings interface
     * with theme and unit preference controls.
     * 
     * The method follows the standard Fragment onCreateView pattern:
     * 1. Takes a LayoutInflater to create views from XML layouts
     * 2. Inflates the settings layout (R.layout.bottom_sheet_settings)
     * 3. Attaches it to the provided container if available
     * 4. Returns the root view for the fragment
     * 
     * @param inflater Layout inflater for creating fragment views from XML
     * @param container Parent view group that this fragment's UI should be attached to
     * @param savedInstanceState Previously saved state for restoring fragment (nullable)
     * @return The root View of the settings bottom sheet interface
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_settings, container, false);
    }

    /**
     * Initializes the bottom sheet UI and sets up all event handlers.
     * 
     * This lifecycle method is called after onCreateView() when the fragment's view
     * has been created. It performs all the setup necessary for the settings interface:
     * 
     * 1. **View References**: Gets references to all UI components (RadioGroups, Button)
     * 2. **Current State Loading**: Reads current settings from SettingsManager
     * 3. **UI State Initialization**: Sets radio buttons to reflect current preferences
     * 4. **Event Handler Setup**: Configures listeners for user interactions
     * 5. **Settings Management**: Connects UI changes to SettingsManager persistence
     * 
     * **Theme Settings Initialization:**
     * - Reads current theme mode from SettingsManager
     * - Sets appropriate radio button based on mode (Light/Dark/System)
     * - Configures listener to apply theme changes immediately
     * 
     * **Unit Settings Initialization:**
     * - Reads current distance unit preference (miles vs kilometers)
     * - Sets appropriate radio button based on user preference
     * - Configures listener to apply unit changes immediately
     * 
     * **User Interface:**
     * - Integrates with BottomSheetDialogFragment for modal presentation
     * - Provides immediate feedback when settings are changed
     * - Dismisses bottom sheet when close button is tapped
     * 
     * @param view The root view of the fragment's UI (created in onCreateView)
     * @param savedInstanceState Previously saved state for restoring UI (nullable)
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ========== VIEW REFERENCES ==========
        /** RadioGroup for theme mode selection (Light/Dark/System) */
        RadioGroup themeGroup = view.findViewById(R.id.theme_group);
        
        /** RadioGroup for distance unit selection (Kilometers/Miles) */
        RadioGroup unitsGroup = view.findViewById(R.id.units_group);
        
        /** Close button to dismiss the bottom sheet */
        Button closeButton = view.findViewById(R.id.close_button);
        
        /** Delete account button */
        Button deleteAccountButton = view.findViewById(R.id.delete_account_button);

        // Defensive null-safety: if any required view is missing, dismiss to avoid crashes
        if (themeGroup == null || unitsGroup == null || closeButton == null || deleteAccountButton == null) {
            // Views failed to inflate correctly; dismiss and bail out
            dismiss();
            return;
        }

        // ========== THEME SETTINGS INITIALIZATION ==========
        /**
         * Load current theme mode from persistent storage
         * 
         * Gets the user's previously selected theme preference and updates
         * the UI to reflect the current setting. The theme modes correspond
         * to AppCompatDelegate constants for proper Android theme management.
         */
        int currentMode = SettingsManager.getThemeMode(requireContext());
        
        // Set the appropriate radio button based on current theme mode
        switch (currentMode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                // Light theme mode - user prefers light interface
                ((RadioButton) view.findViewById(R.id.theme_light)).setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                // Dark theme mode - user prefers dark interface
                ((RadioButton) view.findViewById(R.id.theme_dark)).setChecked(true);
                break;
            default:
                // System theme mode - follow device default setting
                ((RadioButton) view.findViewById(R.id.theme_system)).setChecked(true);
                break;
        }

        // ========== UNIT SETTINGS INITIALIZATION ==========
        /**
         * Load current distance unit preference from persistent storage
         * 
         * Gets whether the user prefers miles or kilometers for distance display
         * and sets the appropriate radio button to reflect their choice.
         */
        boolean useMiles = SettingsManager.useMiles(requireContext());
        ((RadioButton) view.findViewById(useMiles ? R.id.unit_miles : R.id.unit_km)).setChecked(true);

        // ========== THEME CHANGE HANDLER ==========
        /**
         * Event listener for theme mode changes
         * 
         * When the user selects a different theme option, this handler:
         * 1. Determines the selected theme mode from the radio button ID
         * 2. Converts the selection to the appropriate AppCompatDelegate mode
         * 3. Persists the change via SettingsManager
         * 4. Immediately applies the theme change to the application
         */
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // Default to system
            if (checkedId == R.id.theme_light) {
                mode = AppCompatDelegate.MODE_NIGHT_NO; // Force light theme
            } else if (checkedId == R.id.theme_dark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES; // Force dark theme
            }
            SettingsManager.setThemeMode(requireContext(), mode);
        });

        // ========== UNIT CHANGE HANDLER ==========
        /**
         * Event listener for distance unit changes
         * 
         * When the user selects a different distance unit option, this handler:
         * 1. Determines if miles or kilometers was selected
         * 2. Persists the preference change via SettingsManager
         * 3. Updates all distance displays throughout the app (next session)
         */
        unitsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean miles = checkedId == R.id.unit_miles;
            SettingsManager.setUseMiles(requireContext(), miles);
        });

        // ========== AUTH VIEWMODEL INITIALIZATION ==========
        /**
         * Initialize AuthViewModel for account management functionality
         * 
         * Creates a ViewModel instance to handle authentication operations
         * including account deletion.
         */
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // ========== DELETE ACCOUNT BUTTON HANDLER ==========
        /**
         * Delete account button click handler
         * 
         * Shows a confirmation dialog when the user attempts to delete their account.
         * If confirmed, initiates the account deletion process through the AuthViewModel.
         * The deletion includes removing user profile, notes, reviews, and Firebase Auth account.
         */
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountConfirmation());

        // ========== CLOSE BUTTON HANDLER ==========
        /**
         * Close button click handler
         * 
         * Dismisses the bottom sheet when the user taps the close button,
         * providing a clear way to exit the settings interface.
         */
        closeButton.setOnClickListener(v -> dismiss());
    }

    /**
     * Show delete account confirmation dialog
     * 
     * Displays a confirmation dialog to ensure the user really wants to delete their account.
     * The dialog explains the consequences and requires explicit confirmation before proceeding
     * with the deletion process which includes:
     * - Deleting user profile from Firestore
     * - Deleting user's notes and reviews
     * - Deleting Firebase Auth account
     */
    private void showDeleteAccountConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.delete_account_confirmation);
        builder.setMessage(R.string.delete_account_warning);
        
        builder.setPositiveButton(R.string.delete_account_confirm, (dialog, which) -> {
            // User confirmed, proceed with account deletion
            authViewModel.deleteAccount(requireContext());
            dismiss(); // Close the settings sheet
        });
        
        builder.setNegativeButton(R.string.delete_account_cancel, (dialog, which) -> {
            // User cancelled, just dismiss the dialog
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
