package com.example.fgluten.ui.restaurant;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Lightweight bottom sheet for quick restaurant information display from map markers.
 * 
 * This dialog fragment provides a concise summary of restaurant information when
 * users interact with restaurant markers on the map. It's designed for quick
 * reference and navigation rather than detailed exploration.
 * 
 * **Key Features:**
 * - Displays essential restaurant information in a compact format
 * - Quick access to navigation functionality
 * - Lightweight alternative to the detailed bottom sheet
 * - Designed specifically for map marker interactions
 * 
 * **Information Display:**
 * - Restaurant name and address
 * - Rating and operating status
 * - Favorite status indicator (if set)
 * - Crowd notes count (if available)
 * - Menu scan status summary
 * 
 * **User Actions:**
 * - "Navigate" button for immediate turn-by-turn directions
 * - Opens Google Maps app with restaurant location
 * - Graceful fallback if Google Maps is unavailable
 * 
 * **Design Philosophy:**
 * - Minimalist interface for quick information consumption
 * - Fast loading and responsive interactions
 * - Focus on navigation over exploration
 * - Consistent with map marker interaction patterns
 * 
 * The fragment is optimized for the map view context where users want quick
 * information and immediate navigation rather than detailed restaurant exploration.
 * 
 * @see RestaurantDetailBottomSheet for detailed restaurant information
 * @see Restaurant for restaurant data model
 * 
 * @author FGluten Development Team
 */
public class RestaurantMarkerBottomSheet extends BottomSheetDialogFragment {

    // ========== ARGUMENT CONSTANTS ==========
    
    /** Bundle key for passing restaurant data to the fragment */
    private static final String ARG_RESTAURANT = "arg_restaurant";

    /**
     * Factory method for creating a new instance of the marker bottom sheet.
     * 
     * This method creates a new fragment instance and packages the restaurant
     * data into the fragment's arguments bundle. The restaurant is serialized
     * using Parcelable since Restaurant implements the Parcelable interface.
     * 
     * @param restaurant Restaurant data to display in the bottom sheet
     * @return New instance of RestaurantMarkerBottomSheet with restaurant data
     */
    public static RestaurantMarkerBottomSheet newInstance(Restaurant restaurant) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_RESTAURANT, restaurant);
        RestaurantMarkerBottomSheet sheet = new RestaurantMarkerBottomSheet();
        sheet.setArguments(args);
        return sheet;
    }

    /**
     * Creates and inflates the bottom sheet layout.
     * 
     * This method inflates the lightweight marker-specific layout designed
     * for quick information display and navigation access.
     * 
     * @param inflater Layout inflater for creating views
     * @param container Parent view group for fragment attachment
     * @param savedInstanceState Previously saved instance state
     * @return The root view of the bottom sheet
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_marker, container, false);
    }

    /**
     * Initializes the bottom sheet UI and event handlers.
     * 
     * This method:
     * 1. Extracts restaurant data from fragment arguments
     * 2. Validates restaurant data and dismisses if invalid
     * 3. Sets up all UI components with restaurant information
     * 4. Configures the navigate button for map directions
     * 5. Formats meta information for compact display
     * 
     * @param view The root view of the bottom sheet
     * @param savedInstanceState Previously saved instance state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ========== DATA EXTRACTION ==========
        Restaurant restaurant = getArguments() != null ? getArguments().getParcelable(ARG_RESTAURANT) : null;
        
        // Validate restaurant data - dismiss if invalid
        if (restaurant == null) {
            dismiss();
            return;
        }

        // ========== UI COMPONENT REFERENCES ==========
        TextView title = view.findViewById(R.id.marker_title);
        TextView meta = view.findViewById(R.id.marker_meta);
        TextView address = view.findViewById(R.id.marker_address);
        Button navigate = view.findViewById(R.id.marker_navigate);

        // ========== BASIC INFORMATION DISPLAY ==========
        title.setText(restaurant.getName());
        address.setText(restaurant.getAddress());

        // ========== META INFORMATION BUILDING ==========
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
        
        // Add favorite status if set
        String favLabel = favoriteLabel(restaurant);
        if (favLabel != null && !favLabel.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(favLabel);
        }
        
        // Add crowd notes count if available
        String notesLabel = notesLabel(restaurant);
        if (notesLabel != null && !notesLabel.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(notesLabel);
        }
        
        // Add menu scan status if available
        String scanLabel = menuScanLabel(restaurant);
        if (scanLabel != null && !scanLabel.isEmpty()) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(scanLabel);
        }
        
        // ========== META INFORMATION DISPLAY ==========
        meta.setText(sb.toString());
        // Hide meta view if no information to display
        meta.setVisibility(sb.length() > 0 ? View.VISIBLE : View.GONE);

        // ========== NAVIGATION HANDLER ==========
        navigate.setOnClickListener(v -> openMaps(restaurant));
    }

    // ========== LABEL GENERATION METHODS ==========
    
    /**
     * Generates a human-readable label for the restaurant's favorite status.
     * 
     * @param restaurant Restaurant to check for favorite status
     * @return Localized label for the favorite status, or null if not favorited
     */
    private String favoriteLabel(Restaurant restaurant) {
        String status = restaurant.getFavoriteStatus();
        if (status == null) return null;
        if ("safe".equals(status)) return getString(R.string.favorite_safe);
        if ("try".equals(status)) return getString(R.string.favorite_try);
        if ("avoid".equals(status)) return getString(R.string.favorite_avoid);
        return null;
    }

    /**
     * Generates a label indicating the number of crowd-sourced notes available.
     * 
     * @param restaurant Restaurant to check for crowd notes
     * @return Localized label showing notes count, or null if no notes
     */
    private String notesLabel(Restaurant restaurant) {
        if (restaurant.getCrowdNotes() == null || restaurant.getCrowdNotes().isEmpty()) {
            return null;
        }
        return getString(R.string.crowd_notes_count, restaurant.getCrowdNotes().size());
    }

    /**
     * Generates a label summarizing the restaurant's menu scan status.
     * 
     * @param restaurant Restaurant to check for menu scan status
     * @return Localized label describing the scan status, or null if not applicable
     */
    private String menuScanLabel(Restaurant restaurant) {
        Restaurant.MenuScanStatus status = restaurant.getMenuScanStatus();
        if (status == null) {
            return null;
        }
        if (status == Restaurant.MenuScanStatus.FETCHING) {
            return getString(R.string.menu_scan_pending_short);
        } else if (status == Restaurant.MenuScanStatus.SUCCESS) {
            if (restaurant.getGlutenFreeMenu() != null && !restaurant.getGlutenFreeMenu().isEmpty()) {
                return getString(R.string.menu_scan_found_gf);
            }
            return getString(R.string.menu_scan_none_found);
        } else if (status == Restaurant.MenuScanStatus.NO_WEBSITE) {
            return getString(R.string.menu_scan_no_site);
        } else if (status == Restaurant.MenuScanStatus.FAILED) {
            return getString(R.string.menu_scan_unavailable);
        }
        return null;
    }

    // ========== NAVIGATION FUNCTIONALITY ==========
    
    /**
     * Opens the Google Maps application with navigation to the restaurant.
     * 
     * This method creates an intent to open Google Maps with the restaurant's
     * location and provides two levels of fallback:
     * 1. First attempt: Google Maps app with restaurant search query
     * 2. Fallback: Google Maps app with just coordinates
     * 3. Final fallback: Show error toast if no maps app is available
     * 
     * The method handles all potential exceptions gracefully to ensure
     * the app doesn't crash if mapping apps are unavailable.
     * 
     * @param restaurant Restaurant to navigate to
     */
    private void openMaps(Restaurant restaurant) {
        try {
            // ========== PRIMARY NAVIGATION INTENT ==========
            // Create intent for Google Maps app with restaurant name search
            Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude() +
                    "?q=" + Uri.encode(restaurant.getName()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            // Specifically target Google Maps package for better compatibility
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        } catch (Exception e) {
            try {
                // ========== FALLBACK NAVIGATION INTENT ==========
                // Fallback: Open Google Maps with just coordinates
                Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } catch (Exception ex) {
                // ========== ERROR HANDLING ==========
                // Final fallback: Show error message to user
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), 
                            getString(R.string.marker_directions_error), 
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
