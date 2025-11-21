package com.example.fgluten.ui.restaurant;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.data.RestaurantRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestaurantViewModel extends AndroidViewModel {

    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        PERMISSION_REQUIRED,
        ERROR
    }

    public static class RestaurantUiState {
        private final Status status;
        private final List<Restaurant> restaurants;
        private final String message;

        private RestaurantUiState(Status status, List<Restaurant> restaurants, String message) {
            this.status = status;
            this.restaurants = restaurants;
            this.message = message;
        }

        public static RestaurantUiState idle() {
            return new RestaurantUiState(Status.IDLE, new ArrayList<>(), null);
        }

        public static RestaurantUiState loading(String message) {
            return new RestaurantUiState(Status.LOADING, new ArrayList<>(), message);
        }

        public static RestaurantUiState permissionRequired(String message) {
            return new RestaurantUiState(Status.PERMISSION_REQUIRED, new ArrayList<>(), message);
        }

        public static RestaurantUiState error(String message) {
            return new RestaurantUiState(Status.ERROR, new ArrayList<>(), message);
        }

        public static RestaurantUiState success(List<Restaurant> restaurants) {
            return new RestaurantUiState(Status.SUCCESS, restaurants, null);
        }

        public Status getStatus() {
            return status;
        }

        public List<Restaurant> getRestaurants() {
            return restaurants;
        }

        public String getMessage() {
            return message;
        }
    }

    private final RestaurantRepository repository;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final MutableLiveData<RestaurantUiState> restaurantState = new MutableLiveData<>(RestaurantUiState.idle());

    public RestaurantViewModel(@NonNull Application application) {
        super(application);
        repository = new RestaurantRepository();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application);
    }

    public LiveData<RestaurantUiState> getRestaurantState() {
        return restaurantState;
    }

    public void loadNearbyRestaurants() {
        if (!hasLocationPermission()) {
            String message = getApplication().getString(R.string.location_permission_needed);
            restaurantState.setValue(RestaurantUiState.permissionRequired(message));
            return;
        }

        restaurantState.setValue(RestaurantUiState.loading(getApplication().getString(R.string.loading_restaurants)));

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        publishRestaurantsForLocation(location);
                    } else {
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> postLocationError());
    }

    private boolean hasLocationPermission() {
        Application application = getApplication();
        int fine = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    private void requestFreshLocation() {
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        publishRestaurantsForLocation(location);
                    } else {
                        postLocationError();
                    }
                })
                .addOnFailureListener(e -> postLocationError());
    }

    private void publishRestaurantsForLocation(Location userLocation) {
        List<Restaurant> restaurants = repository.getRestaurants();
        for (Restaurant restaurant : restaurants) {
            float[] results = new float[1];
            Location.distanceBetween(
                    userLocation.getLatitude(),
                    userLocation.getLongitude(),
                    restaurant.getLatitude(),
                    restaurant.getLongitude(),
                    results
            );
            restaurant.setDistanceMeters(results[0]);
        }
        Collections.sort(restaurants, (first, second) -> Double.compare(first.getDistanceMeters(), second.getDistanceMeters()));

        if (restaurants.isEmpty()) {
            String message = getApplication().getString(R.string.no_restaurants_found);
            restaurantState.setValue(RestaurantUiState.error(message));
        } else {
            restaurantState.setValue(RestaurantUiState.success(restaurants));
        }
    }

    private void postLocationError() {
        String message = getApplication().getString(R.string.location_error);
        restaurantState.setValue(RestaurantUiState.error(message));
    }
}
