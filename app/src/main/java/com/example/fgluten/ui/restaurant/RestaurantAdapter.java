package com.example.fgluten.ui.restaurant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying Restaurant objects in a scrollable list.
 * 
 * This adapter implements the ViewHolder pattern to efficiently display restaurant
 * information with proper recycling of views. It handles:
 * 
 * **Data Display:**
 * - Restaurant name and address
 * - Distance from user location
 * - Gluten-free option indicators
 * - Rating and operating hours
 * - Menu scanning status
 * - User favorites and crowd notes
 * 
 * **User Interactions:**
 * - Click to open restaurant details
 * - "Open in Maps" button for navigation
 * - Unit conversion (miles/km) based on user preferences
 * 
 * **Performance Optimization:**
 * - ViewHolder pattern prevents excessive findViewById calls
 * - Efficient data binding with minimal operations
 * - Smart formatting for distance and meta information
 * 
 * The adapter follows Android's RecyclerView best practices and integrates
 * with the app's settings system for personalized display.
 * 
 * @see Restaurant for the data model being displayed
 * @see SettingsManager for unit preference integration
 * 
 * @author FGluten Development Team
 */
public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder> {

    // ========== CLICK HANDLER INTERFACE ==========
    
    /**
     * Interface for handling restaurant item clicks from the RecyclerView.
     * 
     * This callback allows the containing fragment or activity to respond to
     * user interactions with restaurant items, typically opening a detail view
     * or bottom sheet with more information about the selected restaurant.
     */
    public interface OnRestaurantClickListener {
        /** Called when a restaurant item is clicked by the user */
        void onRestaurantClick(Restaurant restaurant);
    }

    // ========== DATA & CALLBACKS ==========
    
    /** List of restaurants to display in the RecyclerView */
    private List<Restaurant> restaurants = new ArrayList<>();
    
    /** Callback for handling restaurant item clicks */
    private final OnRestaurantClickListener listener;

    /**
     * Constructor for creating the adapter with initial data and click handler.
     * 
     * @param restaurants Initial list of restaurants to display (can be null or empty)
     * @param listener Callback for handling user interactions with restaurant items
     */
    public RestaurantAdapter(List<Restaurant> restaurants, OnRestaurantClickListener listener) {
        if (restaurants != null) {
            this.restaurants = restaurants;
        }
        this.listener = listener;
    }

    /**
     * Updates the adapter's data and triggers UI refresh.
     * 
     * This method replaces the current restaurant list with new data and notifies
     * the RecyclerView that the underlying data has changed. The RecyclerView will
     * then update the visible items accordingly.
     * 
     * @param restaurants New list of restaurants to display (null results in empty list)
     */
    public void setRestaurants(List<Restaurant> restaurants) {
        this.restaurants = restaurants != null ? restaurants : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RestaurantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant, parent, false);
        return new RestaurantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestaurantViewHolder holder, int position) {
        Restaurant restaurant = restaurants.get(position);
        holder.bind(restaurant, listener);
    }

    @Override
    public int getItemCount() {
        return restaurants.size();
    }

    // ========== VIEW HOLDER PATTERN IMPLEMENTATION ==========
    
    /**
     * ViewHolder class for recycling restaurant list item views efficiently.
     * 
     * This static inner class follows the RecyclerView ViewHolder pattern, which
     * improves performance by caching view references and reducing findViewById calls.
     * Each ViewHolder represents a single restaurant item in the list.
     * 
     * The ViewHolder holds references to all UI components needed to display
     * restaurant information, including text views, buttons, and status indicators.
     */
    static class RestaurantViewHolder extends RecyclerView.ViewHolder {

        // ========== VIEW REFERENCES ==========
        /** Restaurant name display */
        private final TextView nameTextView;
        
        /** Restaurant address display */
        private final TextView addressTextView;
        
        /** Distance from user location */
        private final TextView distanceTextView;
        
        /** Gluten-free options badge */
        private final TextView gfBadgeView;
        
        /** Meta information (rating, hours, status) */
        private final TextView metaView;
        
        /** "Open in Maps" navigation button */
        private final Button openMapsButton;

        /**
         * Constructor that initializes view references from the layout.
         * 
         * Uses findViewById to cache references to all UI components in the
         * restaurant list item layout. These references are reused for each
         * item that this ViewHolder represents.
         * 
         * @param itemView The root view of a single restaurant list item
         */
        public RestaurantViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Cache all view references for performance
            nameTextView = itemView.findViewById(R.id.restaurant_name);
            addressTextView = itemView.findViewById(R.id.restaurant_address);
            distanceTextView = itemView.findViewById(R.id.restaurant_distance);
            gfBadgeView = itemView.findViewById(R.id.restaurant_gf_badge);
            metaView = itemView.findViewById(R.id.restaurant_meta);
            openMapsButton = itemView.findViewById(R.id.restaurant_open_maps);
        }

        /**
         * Binds restaurant data to the ViewHolder's UI components.
         * 
         * This method performs all data binding operations needed to display
         * restaurant information in the list item. It handles:
         * 
         * 1. **Basic Information**: Name and address display
         * 2. **Distance Calculation**: Formats distance based on user preferences
         * 3. **Visual Indicators**: Shows GF badge when applicable
         * 4. **Meta Information**: Displays rating, hours, menu status, etc.
         * 5. **Click Handlers**: Sets up interactions for item click and maps button
         * 
         * The method follows a defensive programming approach, checking for null
         * values and handling edge cases gracefully.
         * 
         * @param restaurant Restaurant data to display
         * @param listener Callback for handling restaurant item clicks
         */
        public void bind(Restaurant restaurant, OnRestaurantClickListener listener) {
            // ========== BASIC INFORMATION DISPLAY ==========
            nameTextView.setText(restaurant.getName());
            addressTextView.setText(restaurant.getAddress());

            // ========== DISTANCE DISPLAY ==========
            double distanceMeters = restaurant.getDistanceMeters();
            String distanceLabel = formatDistance(itemView.getContext(), distanceMeters);
            if (distanceLabel != null) {
                distanceTextView.setText(distanceLabel);
                distanceTextView.setVisibility(View.VISIBLE);
            } else {
                distanceTextView.setVisibility(View.GONE);
            }

            // ========== GLUTEN-FREE BADGE ==========
            if (restaurant.hasGlutenFreeOptions()) {
                gfBadgeView.setVisibility(View.VISIBLE);
            } else {
                gfBadgeView.setVisibility(View.GONE);
            }

            // ========== META INFORMATION ==========
            String meta = buildMeta(restaurant);
            if (meta != null && !meta.isEmpty()) {
                metaView.setVisibility(View.VISIBLE);
                metaView.setText(meta);
            } else {
                metaView.setVisibility(View.GONE);
            }

            // ========== CLICK HANDLERS ==========
            // Handle clicks on the entire item to open details
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRestaurantClick(restaurant);
                }
            });

            // Handle "Open in Maps" button to start navigation
            openMapsButton.setOnClickListener(v -> openInMaps(restaurant));
        }

        private String formatDistance(Context context, double meters) {
            if (meters <= 0) {
                return null;
            }
            boolean useMiles = SettingsManager.useMiles(context);
            if (useMiles) {
                double miles = meters / 1609.34;
                if (miles >= 0.1) {
                    return context.getString(R.string.distance_miles_away, miles);
                } else {
                    int feet = (int) Math.round(meters * 3.28084);
                    return context.getString(R.string.distance_feet_away, feet);
                }
            } else {
                if (meters >= 1000) {
                    double km = meters / 1000.0;
                    return context.getString(R.string.distance_km_away, km);
                }
                int roundedMeters = (int) Math.round(meters);
                return context.getString(R.string.distance_m_away, roundedMeters);
            }
        }

        private String buildMeta(Restaurant restaurant) {
            StringBuilder sb = new StringBuilder();
            if (restaurant.getRating() != null) {
                sb.append(String.format("%.1f \u2605", restaurant.getRating()));
            }
            if (restaurant.getOpenNow() != null) {
                if (sb.length() > 0) sb.append(" \u2022 ");
                sb.append(restaurant.getOpenNow() ? itemView.getContext().getString(R.string.meta_open_now)
                        : itemView.getContext().getString(R.string.meta_closed));
            }
            String scanLabel = menuScanLabel(itemView.getContext(), restaurant);
            if (!TextUtils.isEmpty(scanLabel)) {
                if (sb.length() > 0) sb.append(" \u2022 ");
                sb.append(scanLabel);
            }
            String favLabel = favoriteLabel(itemView.getContext(), restaurant);
            if (!TextUtils.isEmpty(favLabel)) {
                if (sb.length() > 0) sb.append(" \u2022 ");
                sb.append(favLabel);
            }
            return sb.toString();
        }

        private String menuScanLabel(Context context, Restaurant restaurant) {
            Restaurant.MenuScanStatus status = restaurant.getMenuScanStatus();
            if (status == null) {
                return null;
            }
            if (status == Restaurant.MenuScanStatus.FETCHING) {
                return context.getString(R.string.menu_scan_pending_short);
            } else if (status == Restaurant.MenuScanStatus.SUCCESS) {
                if (restaurant.getGlutenFreeMenu() != null && !restaurant.getGlutenFreeMenu().isEmpty()) {
                    return context.getString(R.string.menu_scan_found_gf);
                }
                return context.getString(R.string.menu_scan_none_found);
            } else if (status == Restaurant.MenuScanStatus.NO_WEBSITE) {
                return context.getString(R.string.menu_scan_no_site);
            } else if (status == Restaurant.MenuScanStatus.FAILED) {
                return context.getString(R.string.menu_scan_unavailable);
            }
            return null;
        }

        private String favoriteLabel(Context context, Restaurant restaurant) {
            String status = restaurant.getFavoriteStatus();
            if (TextUtils.isEmpty(status)) {
                return null;
            }
            if ("safe".equals(status)) {
                return context.getString(R.string.favorite_safe);
            }
            if ("try".equals(status)) {
                return context.getString(R.string.favorite_try);
            }
            if ("avoid".equals(status)) {
                return context.getString(R.string.favorite_avoid);
            }
            return null;
        }

        private void openInMaps(Restaurant restaurant) {
            try {
                Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude() +
                        "?q=" + Uri.encode(restaurant.getName()));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setPackage("com.google.android.apps.maps");
                itemView.getContext().startActivity(intent);
            } catch (Exception e) {
                try {
                    Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    itemView.getContext().startActivity(intent);
                } catch (Exception ex) {
                    if (itemView.getContext() != null) {
                        android.widget.Toast.makeText(itemView.getContext(), itemView.getContext().getString(R.string.marker_directions_error), android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
}
