package com.example.fgluten.ui.restaurant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.databinding.FragmentRestaurantListBinding;
import com.example.fgluten.ui.restaurant.RestaurantViewModel.SortMode;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Map;

/**
 * Main fragment for displaying restaurant search results with dual-view support.
 * 
 * This fragment provides the primary user interface for restaurant discovery and browsing,
 * featuring:
 * 
 * **Dual View Modes:**
 * - List view with detailed restaurant cards
 * - Map view with restaurant markers and user location
 * 
 * **Core Features:**
 * - Location permission handling with user-friendly prompts
 * - Real-time restaurant data from Google Places API
 * - Advanced filtering (GF-only, open hours, distance, rating)
 * - Sorting options (distance, name)
 * - Interactive map with markers and info windows
 * - Pull-to-refresh and manual refresh capabilities
 * 
 * **User Experience:**
 * - Smooth transitions between list and map views
 * - Loading states and error handling
 * - Permission request flow with helpful messaging
 * - Cached data display for offline scenarios
 * 
 * **Architecture:**
 * - MVVM pattern with ViewModel for business logic
 * - Data binding for efficient UI updates
 * - Fragment-based navigation integration
 * - Google Maps SDK for location services
 * 
 * The fragment integrates with RestaurantViewModel for data operations and
 * RestaurantAdapter for list display, providing a complete restaurant browsing experience.
 * 
 * @see RestaurantViewModel for data management
 * @see RestaurantAdapter for list display
 * 
 * @author FGluten Development Team
 */
public class RestaurantListFragment extends Fragment {

    // ========== DATA BINDING & COMPONENTS ==========
    
    /** Data binding object for the fragment's layout views */
    private FragmentRestaurantListBinding binding;
    
    /** RecyclerView adapter for displaying restaurant list items */
    private RestaurantAdapter adapter;
    
    /** ViewModel for managing restaurant data and business logic */
    private RestaurantViewModel restaurantViewModel;
    
    /** Activity result launcher for handling location permission requests */
    private ActivityResultLauncher<String[]> permissionLauncher;
    
    /** Google Maps instance for map view functionality */
    private GoogleMap googleMap;
    
    /** Flag indicating whether the map is ready for interaction */
    private boolean isMapReady = false;
    
    /** Cached UI state for recentering map and other operations */
    private RestaurantViewModel.RestaurantUiState lastUiState = null;

    /**
     * Fragment initialization for setting up permission handling.
     * 
     * This method registers a permission result launcher that will handle
     * the user's response to location permission requests. The launcher
     * automatically handles the permission request lifecycle and calls
     * the appropriate callback method.
     * 
     * @param savedInstanceState Previously saved instance state
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Register permission launcher for location access requests
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onPermissionResult
        );
    }

    /**
     * Main fragment view creation and initialization.
     * 
     * This method performs all the setup necessary for the restaurant list interface:
     * 
     * 1. **Data Binding**: Inflates layout and gets view references
     * 2. **RecyclerView Setup**: Configures list display with adapter and layout manager
     * 3. **ViewModel Integration**: Connects to business logic layer
     * 4. **Event Handlers**: Sets up click listeners for all interactive elements
     * 5. **UI Controls**: Initializes filter controls, toggle buttons, and map
     * 6. **Data Loading**: Initiates the first restaurant search
     * 
     * The method follows Android Fragment lifecycle best practices and ensures
     * all UI components are properly initialized before the view is displayed.
     * 
     * @param inflater Layout inflater for creating fragment views
     * @param container Parent view group for fragment attachment
     * @param savedInstanceState Previously saved instance state
     * @return The root view of the fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // ========== DATA BINDING SETUP ==========
        binding = FragmentRestaurantListBinding.inflate(inflater, container, false);
        RecyclerView recyclerView = binding.restaurantRecycler;

        // ========== RECYCLERVIEW CONFIGURATION ==========
        adapter = new RestaurantAdapter(new ArrayList<>(), restaurant ->
                RestaurantDetailBottomSheet.show(getParentFragmentManager(), restaurant));

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // ========== VIEWMODEL INTEGRATION ==========
        restaurantViewModel = new ViewModelProvider(requireActivity()).get(RestaurantViewModel.class);
        restaurantViewModel.getRestaurantState().observe(getViewLifecycleOwner(), this::renderUiState);

        // ========== EVENT HANDLERS ==========
        binding.stateAction.setOnClickListener(v -> requestLocationFlow());
        binding.buttonRefresh.setOnClickListener(v -> requestLocationFlow());
        binding.cachedDismiss.setOnClickListener(v -> binding.cachedBanner.setVisibility(View.GONE));
        binding.stateSettings.setOnClickListener(v -> openAppSettings());
        binding.errorRetry.setOnClickListener(v -> requestLocationFlow());
        binding.errorDismiss.setOnClickListener(v -> binding.errorBanner.setVisibility(View.GONE));
        
        // ========== UI CONTROL SETUP ==========
        setupToggleButtons();
        setupFilterControls();
        setupMap();
        binding.buttonRecenter.setOnClickListener(v -> recenterMap());

        // ========== DATA INITIALIZATION ==========
        // kick off first load to show permission state or fetch if already granted
        restaurantViewModel.loadNearbyRestaurants();

        return binding.getRoot();
    }

    private void requestLocationFlow() {
        Log.d("RestaurantListFragment", "Use my location tapped");
        Toast.makeText(requireContext(), "Use my location tapped", Toast.LENGTH_SHORT).show();
        if (hasLocationPermission()) {
            restaurantViewModel.loadNearbyRestaurants();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void renderUiState(RestaurantViewModel.RestaurantUiState state) {
        if (state == null || binding == null) {
            return;
        }
        lastUiState = state;
        boolean isLoading = state.getStatus() == RestaurantViewModel.Status.LOADING;
        binding.loadingOverlay.setVisibility(View.GONE);
        binding.skeletonContainer.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.stateSettings.setVisibility(View.GONE);

        // updates the count of restaurant results
        updateResultsCount(state);

        if (state.getRestaurants() != null) {
            adapter.setRestaurants(state.getRestaurants());
        }
        boolean hasData = state.getRestaurants() != null && !state.getRestaurants().isEmpty();
        if (isLoading) {
            hasData = false;
        }
        applyContentVisibility(hasData);

        boolean shouldShowMessage = !hasData && state.getMessage() != null;
        binding.stateContainer.setVisibility(shouldShowMessage ? View.VISIBLE : View.GONE);
        binding.stateAction.setVisibility(View.GONE);

        if (shouldShowMessage) {
            binding.stateMessage.setText(state.getMessage());
            if (state.getStatus() == RestaurantViewModel.Status.PERMISSION_REQUIRED) {
                binding.stateAction.setText(R.string.enable_location);
                binding.stateAction.setVisibility(View.VISIBLE);
                binding.stateSettings.setVisibility(View.VISIBLE);
            } else if (state.getStatus() == RestaurantViewModel.Status.ERROR) {
                binding.stateAction.setText(R.string.retry);
                binding.stateAction.setVisibility(View.VISIBLE);
                binding.stateSettings.setVisibility(View.GONE);
                Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (state.getStatus() == RestaurantViewModel.Status.SUCCESS && state.getMessage() != null) {
            Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
            binding.stateSettings.setVisibility(View.GONE);
        }

        if (state.getStatus() == RestaurantViewModel.Status.SUCCESS && state.getMessage() != null) {
            binding.cachedBanner.setVisibility(View.VISIBLE);
            binding.cachedText.setText(state.getMessage());
        } else {
            binding.cachedBanner.setVisibility(View.GONE);
        }

        if (state.getStatus() == RestaurantViewModel.Status.ERROR) {
            binding.errorBanner.setVisibility(View.VISIBLE);
            binding.errorText.setText(state.getMessage());
        } else {
            binding.errorBanner.setVisibility(View.GONE);
        }

        renderMap(state);
    }

    private void applyContentVisibility(boolean hasData) {
        int checkedId = binding.viewToggle.getCheckedButtonId();
        boolean showMap = checkedId == binding.toggleMap.getId();
        binding.restaurantRecycler.setVisibility(!showMap && hasData ? View.VISIBLE : View.GONE);
        binding.restaurantMapContainer.setVisibility(showMap && hasData ? View.VISIBLE : View.GONE);
    }

    private void setupToggleButtons() {
        binding.viewToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            boolean showMap = checkedId == binding.toggleMap.getId();
            binding.restaurantMapContainer.setVisibility(showMap ? View.VISIBLE : View.GONE);
            binding.restaurantRecycler.setVisibility(showMap ? View.GONE : View.VISIBLE);
        });
    }

    private void setupFilterControls() {
        binding.chipGfOnly.setOnCheckedChangeListener((button, isChecked) -> restaurantViewModel.setGfOnly(isChecked));
        binding.chipOpenNow.setOnCheckedChangeListener((button, isChecked) -> restaurantViewModel.setOpenNowOnly(isChecked));
        binding.sortToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == binding.sortDistance.getId()) {
                restaurantViewModel.setSortMode(SortMode.DISTANCE);
            } else if (checkedId == binding.sortName.getId()) {
                restaurantViewModel.setSortMode(SortMode.NAME);
            }
        });

        binding.sliderDistance.addOnChangeListener((slider, value, fromUser) -> {
            updateDistanceLabel(value);
            double meters = value <= 0 ? 0 : value * 1000.0;
            restaurantViewModel.setMaxDistanceMeters(meters);
        });
        binding.sliderRating.addOnChangeListener((slider, value, fromUser) -> {
            updateRatingLabel(value);
            restaurantViewModel.setMinRating(value);
        });
        // initialize labels
        updateDistanceLabel(binding.sliderDistance.getValue());
        updateRatingLabel(binding.sliderRating.getValue());
    }

    private void updateDistanceLabel(float valueKm) {
        String text = valueKm <= 0 ? getString(R.string.filter_distance_any)
                : getString(R.string.filter_distance_km_value, (int) valueKm);
        binding.valueMaxDistance.setText(text);
    }

    private void updateRatingLabel(float value) {
        String text = value <= 0 ? getString(R.string.filter_rating_any)
                : getString(R.string.filter_rating_value, value);
        binding.valueMinRating.setText(text);
    }

    private void setupMap() {
        androidx.fragment.app.Fragment fragment = getChildFragmentManager().findFragmentById(R.id.restaurant_map);
        if (fragment instanceof SupportMapFragment) {
            SupportMapFragment mapFragment = (SupportMapFragment) fragment;
            mapFragment.getMapAsync(map -> {
                googleMap = map;
                isMapReady = true;
                enableMyLocationOnMap();
                RestaurantViewModel.RestaurantUiState current = restaurantViewModel.getRestaurantState().getValue();
                if (current != null) {
                    renderMap(current);
                }
            });
        }
    }

    private void enableMyLocationOnMap() {
        if (googleMap == null) {
            return;
        }
        try {
            googleMap.setMyLocationEnabled(hasLocationPermission());
        } catch (SecurityException ignored) {
            // Permission may be revoked dynamically; safely ignore.
        }
    }

    private void renderMap(RestaurantViewModel.RestaurantUiState state) {
        if (!isMapReady || googleMap == null || state.getRestaurants() == null || state.getRestaurants().isEmpty()) {
            return;
        }
        googleMap.clear();
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Restaurant) {
                RestaurantDetailBottomSheet.show(getParentFragmentManager(), (Restaurant) tag);
                return true;
            }
            return false;
        });
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasBounds = false;

        for (Restaurant restaurant : state.getRestaurants()) {
            LatLng latLng = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(restaurant.getName())
                    .snippet(restaurant.getAddress()));
            if (marker != null) {
                marker.setTag(restaurant);
            }
            boundsBuilder.include(latLng);
            hasBounds = true;
        }

        if (state.getUserLatitude() != null && state.getUserLongitude() != null) {
            LatLng userLatLng = new LatLng(state.getUserLatitude(), state.getUserLongitude());
            boundsBuilder.include(userLatLng);
            hasBounds = true;
        }

        if (hasBounds) {
            try {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120));
            } catch (IllegalStateException ignored) {
                // Map view not laid out yet; skip animation.
            }
        }
    }

    private void updateResultsCount(RestaurantViewModel.RestaurantUiState state) {
        if (binding == null || state == null) {
            return;
        }
        int count = state.getRestaurants() != null ? state.getRestaurants().size() : 0;
        boolean showCount = state.getStatus() == RestaurantViewModel.Status.SUCCESS && count > 0;
        if (showCount) {
            String label = getResources().getQuantityString(R.plurals.results_found, count, count);
            binding.resultsCount.setText(label);
            binding.resultsCount.setVisibility(View.VISIBLE);
        } else {
            binding.resultsCount.setVisibility(View.GONE);
        }
    }

    private void recenterMap() {
        if (!isMapReady || googleMap == null || lastUiState == null) {
            return;
        }
        if (lastUiState.getUserLatitude() != null && lastUiState.getUserLongitude() != null) {
            LatLng userLatLng = new LatLng(lastUiState.getUserLatitude(), lastUiState.getUserLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f));
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void onPermissionResult(Map<String, Boolean> permissions) {
        boolean granted = false;
        for (Boolean value : permissions.values()) {
            if (Boolean.TRUE.equals(value)) {
                granted = true;
                break;
            }
        }
        Log.d("RestaurantListFragment", "onPermissionResult granted=" + granted + " values=" + permissions);
        if (granted) {
            enableMyLocationOnMap();
            restaurantViewModel.loadNearbyRestaurants();
        } else {
            restaurantViewModel.loadNearbyRestaurants();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
