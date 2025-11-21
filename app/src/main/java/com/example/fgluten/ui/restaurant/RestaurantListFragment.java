package com.example.fgluten.ui.restaurant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.databinding.FragmentRestaurantListBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Map;

public class RestaurantListFragment extends Fragment {

    private FragmentRestaurantListBinding binding;
    private RestaurantAdapter adapter;
    private RestaurantViewModel restaurantViewModel;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private GoogleMap googleMap;
    private boolean isMapReady = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::onPermissionResult
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentRestaurantListBinding.inflate(inflater, container, false);
        RecyclerView recyclerView = binding.restaurantRecycler;

        adapter = new RestaurantAdapter(new ArrayList<>(), restaurant -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("restaurant", restaurant);
            NavHostFragment.findNavController(RestaurantListFragment.this)
                    .navigate(R.id.action_restaurantListFragment_to_restaurantDetailFragment, bundle);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        restaurantViewModel = new ViewModelProvider(this).get(RestaurantViewModel.class);
        restaurantViewModel.getRestaurantState().observe(getViewLifecycleOwner(), this::renderUiState);

        binding.stateAction.setOnClickListener(v -> requestLocationFlow());
        setupToggleButtons();
        setupMap();

        // kick off first load to show permission state or fetch if already granted
        restaurantViewModel.loadNearbyRestaurants();

        return binding.getRoot();
    }

    private void requestLocationFlow() {
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
        boolean isLoading = state.getStatus() == RestaurantViewModel.Status.LOADING;
        binding.loadingProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (state.getRestaurants() != null) {
            adapter.setRestaurants(state.getRestaurants());
        }
        boolean hasData = state.getRestaurants() != null && !state.getRestaurants().isEmpty();
        applyContentVisibility(hasData);

        boolean shouldShowMessage = !hasData && state.getMessage() != null;
        binding.stateContainer.setVisibility(shouldShowMessage ? View.VISIBLE : View.GONE);
        binding.stateAction.setVisibility(View.GONE);

        if (shouldShowMessage) {
            binding.stateMessage.setText(state.getMessage());
            if (state.getStatus() == RestaurantViewModel.Status.PERMISSION_REQUIRED) {
                binding.stateAction.setText(R.string.enable_location);
                binding.stateAction.setVisibility(View.VISIBLE);
            } else if (state.getStatus() == RestaurantViewModel.Status.ERROR) {
                binding.stateAction.setText(R.string.retry);
                binding.stateAction.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else if (state.getStatus() == RestaurantViewModel.Status.SUCCESS && state.getMessage() != null) {
            Toast.makeText(requireContext(), state.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
                // Permission revoked while map is active.
            }
        } else {
            googleMap.setMyLocationEnabled(false);
        }
    }

    private void renderMap(RestaurantViewModel.RestaurantUiState state) {
        if (!isMapReady || googleMap == null || state.getRestaurants() == null || state.getRestaurants().isEmpty()) {
            return;
        }
        googleMap.clear();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasBounds = false;

        for (Restaurant restaurant : state.getRestaurants()) {
            LatLng latLng = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(restaurant.getName())
                    .snippet(restaurant.getAddress()));
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
