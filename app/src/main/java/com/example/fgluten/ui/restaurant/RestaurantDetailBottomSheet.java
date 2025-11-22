package com.example.fgluten.ui.restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

/**
 * Comprehensive restaurant detail bottom sheet with full interaction capabilities.
 * 
 * This dialog fragment provides an extensive interface for restaurant exploration
 * and user interaction. It serves as the primary restaurant detail view, offering
 * much more functionality than the lightweight marker bottom sheet.
 * 
 * **Core Features:**
 * - Complete restaurant information display
 * - Interactive favorite status management (safe/try/avoid)
 * - Crowd-sourced notes system with user contributions
 * - Menu scanning results and manual rescan functionality
 * - Direct links to restaurant menus and navigation
 * - Real-time updates from ViewModel data changes
 * 
 * **Information Display:**
 * - Restaurant name, address, and metadata
 * - Gluten-free status summary and detailed menu items
 * - Menu scan status with detailed results
 * - User-generated content (favorites and crowd notes)
 * - Distance calculation with user preference units
 * 
 * **User Interactions:**
 * - Favorite status toggle with clear/reset functionality
 * - Crowd note addition with input validation
 * - Manual menu rescan triggering
 * - External app integration (maps, web browser)
 * - Real-time data synchronization with ViewModel
 * 
 * **Architecture:**
 * - Integrates with RestaurantViewModel for business logic
 * - Uses Material Design components for consistent UI
 * - Follows Android Bottom Sheet Dialog patterns
 * - Implements reactive updates via LiveData observation
 * 
 * **Design Philosophy:**
 * - Rich, detailed information presentation
 * - Comprehensive user interaction capabilities
 * - Seamless integration with app's core features
 * - Professional, polished user experience
 * 
 * This bottom sheet is the primary interface for users who want to explore
 * restaurant details in depth and actively contribute to the app's community
 * features through favorites and crowd-sourced content.
 * 
 * @see RestaurantMarkerBottomSheet for lightweight marker interactions
 * @see RestaurantViewModel for business logic integration
 * @see Restaurant for comprehensive data model
 * 
 * @author FGluten Development Team
 */
public class RestaurantDetailBottomSheet extends BottomSheetDialogFragment {

    // ========== ARGUMENT CONSTANTS ==========
    
    /** Bundle key for passing restaurant data to the fragment */
    private static final String ARG_RESTAURANT = "arg_restaurant";

    // ========== COMPONENT REFERENCES ==========
    
    /** ViewModel for business logic and data management */
    private RestaurantViewModel viewModel;
    
    /** Current restaurant data being displayed */
    private Restaurant current;

    /**
     * Factory method for creating a new instance of the detail bottom sheet.
     * 
     * This method creates a new fragment instance and packages the restaurant
     * data into the fragment's arguments bundle. The restaurant is serialized
     * using Parcelable for efficient passing between components.
     * 
     * @param restaurant Restaurant data to display in the bottom sheet
     * @return New instance of RestaurantDetailBottomSheet with restaurant data
     */
    public static RestaurantDetailBottomSheet newInstance(Restaurant restaurant) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_RESTAURANT, restaurant);
        RestaurantDetailBottomSheet sheet = new RestaurantDetailBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    /**
     * Convenience method to show the bottom sheet with proper error handling.
     * 
     * This method provides a simple way to display the restaurant detail
     * bottom sheet with built-in null checking and error prevention.
     * 
     * @param fragmentManager FragmentManager for dialog management
     * @param restaurant Restaurant data to display
     */
    public static void show(FragmentManager fragmentManager, Restaurant restaurant) {
        if (fragmentManager == null || restaurant == null) {
            return;
        }
        RestaurantDetailBottomSheet sheet = newInstance(restaurant);
        sheet.show(fragmentManager, "restaurant_detail_sheet");
    }

    /**
     * Creates and inflates the detailed restaurant layout.
     * 
     * This method inflates the comprehensive layout designed for full
     * restaurant exploration and user interaction.
     * 
     * @param inflater Layout inflater for creating views
     * @param container Parent view group for fragment attachment
     * @param savedInstanceState Previously saved instance state
     * @return The root view of the bottom sheet
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_restaurant_detail, container, false);
    }

    /**
     * Initializes the bottom sheet UI and event handlers.
     * 
     * This comprehensive method performs all necessary setup:
     * 
     * 1. **Data Initialization**: Extracts restaurant data and initializes ViewModel
     * 2. **UI Component Setup**: Gets references to all views and controls
     * 3. **Event Handler Configuration**: Sets up all user interaction handlers
     * 4. **Data Rendering**: Populates UI with restaurant information
     * 5. **LiveData Observation**: Monitors ViewModel changes for real-time updates
     * 
     * @param view The root view of the bottom sheet
     * @param savedInstanceState Previously saved instance state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ========== VIEWMODEL AND DATA INITIALIZATION ==========
        viewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
        current = getArguments() != null ? getArguments().getParcelable(ARG_RESTAURANT) : null;
        
        // Validate restaurant data - dismiss if invalid
        if (current == null) {
            dismissAllowingStateLoss();
            return;
        }

        // ========== UI COMPONENT REFERENCES ==========
        // Text display components
        TextView nameView = view.findViewById(R.id.sheet_name);
        TextView addressView = view.findViewById(R.id.sheet_address);
        TextView metaView = view.findViewById(R.id.sheet_meta);
        TextView gfStatusView = view.findViewById(R.id.sheet_gf_status);
        TextView menuStatusView = view.findViewById(R.id.sheet_menu_status);
        TextView menuItemsView = view.findViewById(R.id.sheet_menu_items);
        TextView notesList = view.findViewById(R.id.sheet_notes_list);
        TextView favStatus = view.findViewById(R.id.sheet_fav_status);
        
        // Input components
        EditText noteInput = view.findViewById(R.id.sheet_note_input);
        
        // Action buttons
        Button addNote = view.findViewById(R.id.sheet_note_add);
        Button openMaps = view.findViewById(R.id.sheet_open_maps);
        Button openMenu = view.findViewById(R.id.sheet_open_menu);
        Button rescanButton = view.findViewById(R.id.sheet_rescan);
        MaterialButtonToggleGroup favToggle = view.findViewById(R.id.sheet_favorite_toggle);
        Button favClear = view.findViewById(R.id.sheet_fav_clear);

        // ========== INITIAL DATA RENDERING ==========
        render(current, nameView, addressView, metaView, gfStatusView, menuStatusView, 
               menuItemsView, notesList, favStatus, openMenu, rescanButton, favToggle);

        // ========== EVENT HANDLERS ==========
        
        // Navigation to Google Maps
        openMaps.setOnClickListener(v -> openInMaps(current));
        
        // Open restaurant menu in web browser
        openMenu.setOnClickListener(v -> openMenuUrl(current));
        
        // Manual menu rescan trigger
        rescanButton.setOnClickListener(v -> {
            if (current != null) {
                menuStatusView.setText(R.string.menu_scan_pending_detail);
                viewModel.requestMenuRescan(current);
            }
        });

        // ========== FAVORITE STATUS MANAGEMENT ==========
        favToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || current == null) return;
            String status = null;
            if (checkedId == R.id.sheet_fav_safe) {
                status = "safe";
            } else if (checkedId == R.id.sheet_fav_try) {
                status = "try";
            } else if (checkedId == R.id.sheet_fav_avoid) {
                status = "avoid";
            }
            viewModel.setFavoriteStatus(current, status);
            favStatus.setText(status != null ? getString(R.string.favorite_status_label, status) : "");
        });

        // Clear favorite status
        favClear.setOnClickListener(v -> {
            if (current == null) return;
            favToggle.clearChecked();
            viewModel.setFavoriteStatus(current, null);
            favStatus.setText("");
        });

        // ========== CROWD NOTE MANAGEMENT ==========
        addNote.setOnClickListener(v -> {
            if (current == null) return;
            String note = noteInput.getText() != null ? noteInput.getText().toString().trim() : "";
            if (!note.isEmpty()) {
                String alias = SettingsManager.getContributorName(requireContext());
                if (!TextUtils.isEmpty(alias)) {
                    note = note + " — " + alias;
                }
                viewModel.addCrowdNote(current, note);
                noteInput.setText(""); // Clear input after successful addition
            }
        });

        // ========== REACTIVE DATA UPDATES ==========
        // Observe ViewModel for real-time updates to restaurant data
        viewModel.getRestaurantState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.getRestaurants() == null || current == null) return;
            
            // Find updated restaurant data in the new state
            Restaurant updated = findMatching(state.getRestaurants(), current);
            if (updated != null) {
                current = updated; // Update local reference
                // Re-render UI with new data
                render(updated, nameView, addressView, metaView, gfStatusView, 
                       menuStatusView, menuItemsView, notesList, favStatus, 
                       openMenu, rescanButton, favToggle);
            }
        });
    }

    // ========== DATA RENDERING METHODS ==========
    
    /**
     * Comprehensive method to render all restaurant information in the UI.
     * 
     * This method handles the complete population of all UI components with
     * restaurant data, including formatting and conditional display logic.
     * 
     * @param restaurant Restaurant data to display
     * @param nameView Restaurant name display
     * @param addressView Restaurant address display
     * @param metaView Meta information display (rating, hours, distance)
     * @param gfStatusView Gluten-free status summary
     * @param menuStatusView Menu scan status details
     * @param menuItemsView List of discovered gluten-free menu items
     * @param notesListView Crowd-sourced notes display
     * @param favStatusView Favorite status display
     * @param openMenuButton Menu opening button
     * @param rescanButton Menu rescan button
     * @param favToggle Favorite status toggle group
     */
    private void render(Restaurant restaurant,
                        TextView nameView,
                        TextView addressView,
                        TextView metaView,
                        TextView gfStatusView,
                        TextView menuStatusView,
                        TextView menuItemsView,
                        TextView notesListView,
                        TextView favStatusView,
                        Button openMenuButton,
                        Button rescanButton,
                        MaterialButtonToggleGroup favToggle) {
        if (restaurant == null) {
            return;
        }
        
        // ========== BASIC INFORMATION ==========
        nameView.setText(!TextUtils.isEmpty(restaurant.getName()) ? restaurant.getName() : getString(R.string.missing_data));
        addressView.setText(!TextUtils.isEmpty(restaurant.getAddress()) ? restaurant.getAddress() : "");

        // ========== COMPOSITE INFORMATION ==========
        metaView.setText(buildMeta(restaurant));
        String gfSummary = buildGfSummary(restaurant);
        gfStatusView.setText(gfSummary);
        String menuStatusText = buildMenuStatus(restaurant);
        menuStatusView.setText(menuStatusText);

        // ========== GLUTEN-FREE MENU ITEMS ==========
        List<String> menu = restaurant.getGlutenFreeMenu();
        if (menu != null && !menu.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String line : menu) {
                if (TextUtils.isEmpty(line)) continue;
                sb.append("• ").append(line.trim()).append("\n");
            }
            menuItemsView.setText(sb.toString().trim());
        } else {
            menuItemsView.setText(getString(R.string.gf_menu_unknown));
        }

        // ========== CROWD-SOURCED NOTES ==========
        List<String> notes = restaurant.getCrowdNotes();
        if (notes != null && !notes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String n : notes) {
                if (TextUtils.isEmpty(n)) continue;
                sb.append("• ").append(n.trim()).append("\n");
            }
            notesListView.setText(sb.toString().trim());
        } else {
            notesListView.setText(getString(R.string.crowd_notes_empty));
        }

        // ========== FAVORITE STATUS ==========
        favToggle.clearChecked();
        String status = restaurant.getFavoriteStatus();
        if ("safe".equals(status)) favToggle.check(R.id.sheet_fav_safe);
        if ("try".equals(status)) favToggle.check(R.id.sheet_fav_try);
        if ("avoid".equals(status)) favToggle.check(R.id.sheet_fav_avoid);
        favStatusView.setText(!TextUtils.isEmpty(status) ? getString(R.string.favorite_status_label, status) : "");

        // ========== BUTTON STATES ==========
        boolean hasMenuUrl = !TextUtils.isEmpty(restaurant.getMenuUrl());
        openMenuButton.setEnabled(hasMenuUrl);
        openMenuButton.setText(hasMenuUrl ? getString(R.string.detail_menu_button) : getString(R.string.detail_menu_unavailable));

        boolean canRescan = !TextUtils.isEmpty(restaurant.getPlaceId());
        rescanButton.setEnabled(canRescan);
    }

    // ========== INFORMATION BUILDING METHODS ==========
    
    /**
     * Builds meta information string combining rating, hours, and distance.
     * 
     * @param restaurant Restaurant to build meta info for
     * @return Formatted meta information string
     */
    private String buildMeta(Restaurant restaurant) {
        StringBuilder sb = new StringBuilder();
        
        // Add rating if available
        if (restaurant.getRating() != null) {
            sb.append(String.format("%.1f ★", restaurant.getRating()));
        }
        
        // Add operating status if available
        if (restaurant.getOpenNow() != null) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(restaurant.getOpenNow() ? getString(R.string.meta_open_now) : getString(R.string.meta_closed));
        }
        
        // Add distance with user preference units
        String distanceLabel = formatDistance(restaurant.getDistanceMeters());
        if (!TextUtils.isEmpty(distanceLabel)) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(distanceLabel);
        }
        
        return sb.toString();
    }

    /**
     * Formats distance based on user preferences (miles vs kilometers).
     * 
     * @param meters Distance in meters
     * @return Formatted distance string with appropriate units
     */
    private String formatDistance(double meters) {
        if (meters <= 0) return null;
        boolean useMiles = SettingsManager.useMiles(requireContext());
        if (useMiles) {
            double miles = meters / 1609.34;
            if (miles >= 0.1) {
                return getString(R.string.distance_miles_away, miles);
            }
            int feet = (int) Math.round(meters * 3.28084);
            return getString(R.string.distance_feet_away, feet);
        } else {
            if (meters >= 1000) {
                double km = meters / 1000.0;
                return getString(R.string.distance_km_away, km);
            }
            int roundedMeters = (int) Math.round(meters);
            return getString(R.string.distance_m_away, roundedMeters);
        }
    }

    /**
     * Builds a summary of gluten-free options availability.
     * 
     * @param restaurant Restaurant to analyze
     * @return Summary string describing GF options status
     */
    private String buildGfSummary(Restaurant restaurant) {
        List<String> menu = restaurant.getGlutenFreeMenu();
        if (restaurant.hasGlutenFreeOptions()) {
            if (menu != null && menu.size() >= 3) {
                return getString(R.string.gf_many);
            }
            return getString(R.string.gf_limited);
        }
        if (menu != null && !menu.isEmpty()) {
            return getString(R.string.gf_limited);
        }
        return getString(R.string.gf_menu_unknown);
    }

    /**
     * Builds detailed menu scan status information.
     * 
     * @param restaurant Restaurant to check scan status for
     * @return Detailed status string for menu scanning
     */
    private String buildMenuStatus(Restaurant restaurant) {
        Restaurant.MenuScanStatus scanStatus = restaurant.getMenuScanStatus();
        if (scanStatus == Restaurant.MenuScanStatus.FETCHING) {
            return getString(R.string.menu_scan_pending_detail);
        } else if (scanStatus == Restaurant.MenuScanStatus.SUCCESS) {
            if (restaurant.getGlutenFreeMenu() != null && !restaurant.getGlutenFreeMenu().isEmpty()) {
                return getString(R.string.menu_scan_scanned_with_gf);
            }
            return getString(R.string.menu_scan_scanned_no_gf);
        } else if (scanStatus == Restaurant.MenuScanStatus.NO_WEBSITE) {
            return getString(R.string.menu_scan_no_site);
        } else if (scanStatus == Restaurant.MenuScanStatus.FAILED) {
            return getString(R.string.menu_scan_unavailable);
        }
        return getString(R.string.menu_scan_not_started);
    }

    // ========== EXTERNAL ACTION METHODS ==========
    
    /**
     * Opens the restaurant's menu URL in the default web browser.
     * 
     * @param restaurant Restaurant to open menu for
     */
    private void openMenuUrl(Restaurant restaurant) {
        if (restaurant == null || TextUtils.isEmpty(restaurant.getMenuUrl())) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(restaurant.getMenuUrl()));
            startActivity(intent);
        } catch (Exception ignored) {
            // Handle gracefully if no browser app is available
        }
    }

    /**
     * Opens Google Maps with navigation to the restaurant.
     * 
     * @param restaurant Restaurant to navigate to
     */
    private void openInMaps(Restaurant restaurant) {
        if (restaurant == null) return;
        try {
            Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude() +
                    "?q=" + Uri.encode(restaurant.getName()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        } catch (Exception e) {
            try {
                Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception ignored) {
                // Handle gracefully if no maps app is available
            }
        }
    }

    // ========== UTILITY METHODS ==========
    
    /**
     * Finds a matching restaurant in a list based on place ID or name/address.
     * 
     * This method is used to synchronize the displayed restaurant data with
     * updates from the ViewModel, ensuring the UI always shows current information.
     * 
     * @param restaurants List of restaurants to search
     * @param target Restaurant to match against
     * @return Matching restaurant or null if not found
     */
    private Restaurant findMatching(List<Restaurant> restaurants, Restaurant target) {
        if (restaurants == null || target == null) return null;
        
        for (Restaurant r : restaurants) {
            // Match by place ID first (most reliable)
            if (r.getPlaceId() != null && target.getPlaceId() != null && r.getPlaceId().equals(target.getPlaceId())) {
                return r;
            }
            
            // Fallback to name and address matching
            if (target.getPlaceId() == null && r.getName() != null && r.getName().equals(target.getName())
                    && r.getAddress() != null && r.getAddress().equals(target.getAddress())) {
                return r;
            }
        }
        
        return null;
    }
}
