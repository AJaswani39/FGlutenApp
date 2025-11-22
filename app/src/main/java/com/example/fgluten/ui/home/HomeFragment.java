package com.example.fgluten.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.databinding.FragmentHomeBinding;
import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.util.SettingsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Welcome and dashboard fragment for the FGluten Android application.
 * 
 * This fragment serves as the app's main landing page, providing users with:
 * 
 * **App Introduction:**
 * - Welcome screen with clear app purpose and value proposition
 * - Call-to-action button to start restaurant discovery
 * - Clean, focused design that guides users to primary functionality
 * 
 * **Cached Data Display:**
 * - Shows recently found restaurants from previous app sessions
 * - Quick access to user's last restaurant searches
 * - Distance and gluten-free indicators for cached restaurants
 * - Seamless integration with cached restaurant data from HomeViewModel
 * 
 * **Permission Management:**
 * - Permission banner for location access when not granted
 * - Guided flow to restaurant discovery when permission is denied
 * - Clear messaging about why location permission is needed
 * 
 * **User Experience:**
 * - Minimalist design focused on conversion to main functionality
 * - Immediate feedback on permission status
 * - Integration with navigation component for seamless app flow
 * 
 * The fragment follows Android Fragment lifecycle best practices and integrates
 * with HomeViewModel for data management and cached restaurant display.
 * 
 * @see HomeViewModel for cached restaurant data management
 * @see Restaurant for restaurant data model
 * 
 * @author FGluten Development Team
 */
public class HomeFragment extends Fragment {

    // ========== DATA BINDING & COMPONENTS ==========
    
    /** Data binding object for the fragment's layout views */
    private FragmentHomeBinding binding;
    
    /** RecyclerView adapter for displaying cached restaurants */
    private CachedAdapter cachedAdapter;

    /**
     * Fragment view creation and initialization.
     * 
     * This method performs all setup necessary for the home dashboard interface:
     * 
     * 1. **Data Binding**: Inflates layout and gets view references
     * 2. **ViewModel Integration**: Connects to HomeViewModel for data management
     * 3. **UI Setup**: Configures RecyclerView for cached restaurant display
     * 4. **Event Handlers**: Sets up click listeners for navigation and actions
     * 5. **Observers**: Watches for data changes and permission status updates
     * 
     * The method follows Android Fragment lifecycle best practices and ensures
     * all UI components are properly initialized before the view is displayed.
     * 
     * @param inflater Layout inflater for creating fragment views
     * @param container Parent view group for fragment attachment
     * @param savedInstanceState Previously saved instance state
     * @return The root view of the fragment
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        
        // ========== VIEWMODEL INTEGRATION ==========
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        // ========== DATA BINDING SETUP ==========
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ========== NAVIGATION SETUP ==========
        // Main call-to-action button that navigates to restaurant discovery
        binding.ctaFindRestaurants.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.nav_restaurant_list));
        binding.homeCtaMeta.setText(getString(R.string.home_cta_meta));

        String savedAlias = SettingsManager.getContributorName(requireContext());
        binding.homeAliasInput.setText(savedAlias);
        binding.homeAliasSave.setOnClickListener(v -> {
            String alias = binding.homeAliasInput.getText() != null
                    ? binding.homeAliasInput.getText().toString().trim()
                    : "";
            SettingsManager.setContributorName(requireContext(), alias);
            Toast.makeText(requireContext(), R.string.home_alias_saved, Toast.LENGTH_SHORT).show();
        });

        // ========== RECYCLERVIEW CONFIGURATION ==========
        // Setup RecyclerView for displaying cached restaurants from previous sessions
        cachedAdapter = new CachedAdapter();
        RecyclerView recyclerView = binding.cachedRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(cachedAdapter);

        // ========== OBSERVERS FOR UI STATE ==========
        // Observe text content for dynamic titles and subtitles
        homeViewModel.getText().observe(getViewLifecycleOwner(), title -> {
            binding.homeTitle.setText(getString(R.string.home_title));
            binding.homeSubtitle.setText(getString(R.string.home_subtitle));
        });

        // Observe cached restaurant data for display
        homeViewModel.getCachedRestaurants().observe(getViewLifecycleOwner(), restaurants -> {
            boolean hasData = restaurants != null && !restaurants.isEmpty();
            // Show/hide the cached restaurants card based on data availability
            binding.lastNearbyCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
            cachedAdapter.setRestaurants(restaurants);
            int count = restaurants != null ? restaurants.size() : 0;
            String label = getResources().getQuantityString(R.plurals.cached_restaurants_count, count, count);
            binding.homeCachedSummary.setText(label);
            binding.homeCachedSummary.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            if (hasData) {
                int favorites = 0;
                int scans = 0;
                long latestScan = 0;
                for (Restaurant restaurant : restaurants) {
                    if (!TextUtils.isEmpty(restaurant.getFavoriteStatus())) {
                        favorites++;
                    }
                    if (restaurant.getMenuScanStatus() == Restaurant.MenuScanStatus.SUCCESS) {
                        scans++;
                    }
                    long scanTimestamp = restaurant.getMenuScanTimestamp();
                    if (scanTimestamp > latestScan) {
                        latestScan = scanTimestamp;
                    }
                }
                binding.homeHeroChipGroup.setVisibility(View.VISIBLE);
                binding.homeChipCached.setText(getString(R.string.home_chip_cached, count));
                binding.homeChipFavorites.setText(getString(R.string.home_chip_favorites, favorites));
                binding.homeChipScans.setText(getString(R.string.home_chip_scans, scans));
                if (latestScan > 0) {
                    CharSequence relative = DateUtils.getRelativeTimeSpanString(
                            latestScan, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE);
                    binding.homeChipTimestamp.setText(getString(R.string.home_last_scan, relative));
                    binding.homeChipTimestamp.setVisibility(View.VISIBLE);
                } else {
                    binding.homeChipTimestamp.setVisibility(View.GONE);
                }
            } else {
                binding.homeHeroChipGroup.setVisibility(View.GONE);
                binding.homeChipTimestamp.setVisibility(View.GONE);
            }
        });

        // Observe location permission status for showing/hiding permission banner
        homeViewModel.isPermissionGranted().observe(getViewLifecycleOwner(), granted -> {
            boolean showBanner = granted != null && !granted;
            binding.permissionBanner.setVisibility(showBanner ? View.VISIBLE : View.GONE);
            
            // Setup permission banner click handler to navigate to restaurant discovery
            // This provides a clear path for users to grant permission when ready
            binding.permissionButton.setOnClickListener(v -> {
                NavHostFragment.findNavController(HomeFragment.this)
                        .navigate(R.id.nav_restaurant_list);
            });
        });
        
        return root;
    }

    /**
     * Fragment cleanup when view is destroyed.
     * 
     * This method is called when the fragment's view is being destroyed.
     * It performs cleanup of view-related references to prevent memory leaks.
     * 
     * Important to null out the binding reference to allow garbage collection
     * of the fragment and its views.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ========== CACHED RESTAURANTS ADAPTER ==========
    
    /**
     * RecyclerView adapter for displaying cached restaurant data.
     * 
     * This inner class handles the display of restaurants from previous app sessions
     * in a simple, efficient list format. It provides quick access to recently
     * found restaurants without requiring a new search.
     * 
     * The adapter is lightweight and focused on displaying essential information:
     * - Restaurant name
     * - Distance from last known location
     * - Gluten-free indicator if applicable
     */
    private static class CachedAdapter extends RecyclerView.Adapter<CachedAdapter.CachedViewHolder> {
        
        /** List of cached restaurants to display */
        private List<Restaurant> restaurants = new ArrayList<>();

        /**
         * Updates the adapter's restaurant data and refreshes the display.
         * 
         * @param data New list of restaurants to display (null results in empty list)
         */
        void setRestaurants(List<Restaurant> data) {
            restaurants = data != null ? data : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public CachedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the layout for cached restaurant list items
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_home_cached_restaurant, parent, false);
            return new CachedViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CachedViewHolder holder, int position) {
            // Bind restaurant data to the ViewHolder
            holder.bind(restaurants.get(position));
        }

        @Override
        public int getItemCount() {
            return restaurants.size();
        }

        // ========== VIEW HOLDER PATTERN ==========
        
        /**
         * ViewHolder for caching restaurant list item views.
         * 
         * This static inner class caches view references for efficient recycling
         * and handles the binding of restaurant data to UI components.
         */
        static class CachedViewHolder extends RecyclerView.ViewHolder {
            
            // ========== VIEW REFERENCES ==========
            /** Restaurant name display */
            private final TextView name;
            
            /** Meta information display (distance, GF indicator) */
            private final TextView meta;
            
            /** Context for accessing resources and settings */
            private final Context context;

            /**
             * Constructor that initializes view references from the layout.
             * 
             * @param itemView The root view of a cached restaurant list item
             */
            CachedViewHolder(@NonNull View itemView) {
                super(itemView);
                // Cache view references for performance
                name = itemView.findViewById(R.id.cached_name);
                meta = itemView.findViewById(R.id.cached_meta);
                context = itemView.getContext();
            }

            /**
             * Binds restaurant data to the ViewHolder's UI components.
             * 
             * This method formats and displays:
             * 1. Restaurant name
             * 2. Distance from last known location (with unit preference)
             * 3. Gluten-free indicator if applicable
             * 
             * @param restaurant Restaurant data to display
             */
            void bind(Restaurant restaurant) {
                // ========== BASIC INFORMATION ==========
                name.setText(restaurant.getName());
                
                // ========== META INFORMATION BUILDING ==========
                StringBuilder sb = new StringBuilder();
                
                // Add gluten-free indicator if applicable
                if (restaurant.hasGlutenFreeOptions()) {
                    sb.append(itemView.getContext().getString(R.string.gluten_free_badge));
                }
                
                // ========== DISTANCE FORMATTING ==========
                double dist = restaurant.getDistanceMeters();
                if (dist > 0) {
                    // Add separator if we already have content
                    if (sb.length() > 0) sb.append(" • ");
                    
                    // Format distance based on user preferences
                    boolean useMiles = SettingsManager.useMiles(context);
                    if (useMiles) {
                        // Format distance in miles/feet (imperial)
                        double miles = dist / 1609.34;
                        if (miles >= 0.1) {
                            sb.append(String.format("%.1f mi", miles));
                        } else {
                            // Show in feet for very short distances
                            int feet = (int) Math.round(dist * 3.28084);
                            sb.append(String.format("%d ft", feet));
                        }
                    } else {
                        // Format distance in kilometers/meters (metric)
                        if (dist >= 1000) {
                            sb.append(String.format("%.1f km", dist / 1000.0));
                        } else {
                            sb.append(String.format("%d m", Math.round(dist)));
                        }
                    }
                }
                
                // ========== FINAL META DISPLAY ==========
                meta.setText(sb.toString());
            }
        }
    }
}
