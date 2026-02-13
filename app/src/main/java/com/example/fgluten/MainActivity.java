package com.example.fgluten;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.lifecycle.ViewModelProvider;

import com.example.fgluten.databinding.ActivityMainBinding;
import com.example.fgluten.ui.settings.SettingsBottomSheet;
import com.example.fgluten.ui.restaurant.RestaurantViewModel;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;

/**
 * Main entry point and primary activity for the FGluten Android application.
 * 
 * This activity serves as the host for the app's navigation architecture, providing:
 * - Bottom navigation for primary app destinations
 * - Toolbar with action bar functionality
 * - Fragment container for different app screens
 * - Global loading overlay management
 * - Settings access and theme management
 * 
 * The activity follows Android's single-activity pattern using Navigation Component,
 * with bottom navigation providing access to Home and Restaurant List sections.
 * It also manages global UI state and loading indicators across all fragments.
 * 
 * Architecture:
 * - Uses Data Binding for view references
 * - Integrates with RestaurantViewModel for global loading state management
 * - Implements splash screen support for modern Android versions
 * - Handles theme application from user preferences
 * 
 * @author FGluten Development Team
 */
public class MainActivity extends AppCompatActivity {

    // ========== NAVIGATION & UI CONFIGURATION ==========
    /** AppBar configuration for navigation component integration */
    private AppBarConfiguration mAppBarConfiguration;
    
    /** Bottom navigation view for primary app destinations */
    private BottomNavigationView bottomNavigationView;
    
    /** Global loading overlay that spans across all fragments */
    private View globalLoadingOverlay;
    
    /** Startup splash screen overlay (shown only during initial app load) */
    private View startupLoadingOverlay;

    /**
     * Main activity lifecycle method that initializes the entire application.
     * 
     * This method handles:
     * 1. Splash screen installation for modern Android versions
     * 2. Theme application from user preferences
     * 3. Data binding setup for view references
     * 4. Navigation component configuration
     * 5. Bottom navigation setup
     * 6. Global loading state observation
     * 
     * @param savedInstanceState Previously saved instance state (null for new instances)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        // Apply saved theme before inflating the activity layout
        SettingsManager.setThemeMode(this, SettingsManager.getThemeMode(this));
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        super.onCreate(savedInstanceState);

        // Initialize data binding for the activity layout
        com.example.fgluten.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        super.setContentView(binding.getRoot());
        
        // Cache references to global loading overlays and bottom navigation for performance
        globalLoadingOverlay = findViewById(R.id.global_loading_overlay);
        startupLoadingOverlay = findViewById(R.id.startup_loading_overlay);
        bottomNavigationView = findViewById(R.id.bottom_nav);

        // ========== TOOLBAR SETUP ==========
        setSupportActionBar(binding.appBarMain.toolbar);
        
        // ========== NAVIGATION COMPONENT SETUP ==========
        
        // Configure navigation destinations - these are top-level destinations
        // that should be considered as app sections rather than sub-navigation
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_restaurant_list)
                .build();
        
        // Set up navigation between fragments using the Navigation Component
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Start observing global loading state across all fragments
        observeGlobalLoading();
    }

    /**
     * Sets up global loading state observation across all fragments.
     * 
     * This method connects to the RestaurantViewModel to monitor loading states
     * and shows/hides appropriate loading overlays. It manages both:
     * - Global loading overlay (shown during any restaurant loading operation)
     * - Startup loading overlay (shown only during initial app startup)
     * 
     * The startup overlay is automatically hidden once the first loading operation
     * completes, providing a smooth transition from splash screen to app content.
     */
    private void observeGlobalLoading() {
        RestaurantViewModel restaurantViewModel = new ViewModelProvider(this).get(RestaurantViewModel.class);
        restaurantViewModel.getRestaurantState().observe(this, state -> {
            // Guard clause for null checks
            if ((globalLoadingOverlay == null && startupLoadingOverlay == null) || state == null) {
                return;
            }
            
            // Determine if we should show loading indicators
            boolean show = state.getStatus() == com.example.fgluten.ui.restaurant.RestaurantViewModel.Status.LOADING;
            
            // Update global loading overlay visibility
            if (globalLoadingOverlay != null) {
                globalLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            
            // Handle startup overlay - hide once initial loading completes
            if (startupLoadingOverlay != null) {
                // Hide startup overlay once we leave the very first loading state
                if (!show) {
                    startupLoadingOverlay.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * Creates and inflates the options menu for the action bar.
     * 
     * This menu provides access to global app settings and other top-level actions.
     * Currently includes a settings menu item that opens the SettingsBottomSheet.
     * 
     * @param menu The options menu in which items are placed
     * @return true to display the menu, false otherwise
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Handles selections from the options menu.
     * 
     * Processes menu item clicks and performs appropriate actions. Currently handles
     * the settings menu item by opening the settings bottom sheet dialog.
     * 
     * @param item The selected menu item
     * @return true if the menu item was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Open settings bottom sheet for theme and unit preferences
            SettingsBottomSheet.newInstance().show(getSupportFragmentManager(), "settings_bottom_sheet");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles navigation up (back button) behavior for the app.
     * 
     * Integrates with the Navigation Component to provide proper back navigation
     * behavior. Bottom navigation doesn't require special back handling as it's
     * designed to stay on the current tab rather than navigate back.
     * 
     * @return true if navigation was handled, false otherwise
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
