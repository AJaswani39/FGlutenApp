package com.example.fgluten.ui.restaurant;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.BuildConfig;
import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;
import com.example.fgluten.data.RestaurantRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
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
        private final Double userLatitude;
        private final Double userLongitude;

        private RestaurantUiState(Status status, List<Restaurant> restaurants, String message, Double userLatitude, Double userLongitude) {
            this.status = status;
            this.restaurants = restaurants;
            this.message = message;
            this.userLatitude = userLatitude;
            this.userLongitude = userLongitude;
        }

        public static RestaurantUiState idle() {
            return new RestaurantUiState(Status.IDLE, new ArrayList<>(), null, null, null);
        }

        public static RestaurantUiState loading(String message) {
            return new RestaurantUiState(Status.LOADING, new ArrayList<>(), message, null, null);
        }

        public static RestaurantUiState permissionRequired(String message) {
            return new RestaurantUiState(Status.PERMISSION_REQUIRED, new ArrayList<>(), message, null, null);
        }

        public static RestaurantUiState error(String message) {
            return new RestaurantUiState(Status.ERROR, new ArrayList<>(), message, null, null);
        }

        public static RestaurantUiState success(List<Restaurant> restaurants, double userLatitude, double userLongitude) {
            return new RestaurantUiState(Status.SUCCESS, restaurants, null, userLatitude, userLongitude);
        }

        public static RestaurantUiState successWithMessage(List<Restaurant> restaurants, double userLatitude, double userLongitude, String message) {
            return new RestaurantUiState(Status.SUCCESS, restaurants, message, userLatitude, userLongitude);
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

        public Double getUserLatitude() {
            return userLatitude;
        }

        public Double getUserLongitude() {
            return userLongitude;
        }
    }

    private final RestaurantRepository repository;
    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final PlacesClient placesClient;
    private final MutableLiveData<RestaurantUiState> restaurantState = new MutableLiveData<>(RestaurantUiState.idle());
    private final List<Restaurant> lastSuccessfulRestaurants = new ArrayList<>();
    private static final String TAG = "RestaurantViewModel";

    public RestaurantViewModel(@NonNull Application application) {
        super(application);
        repository = new RestaurantRepository();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application);
        String apiKey = BuildConfig.MAPS_API_KEY;
        if (!Places.isInitialized() && apiKey != null && !apiKey.isEmpty()) {
            Places.initialize(application, apiKey);
        }
        placesClient = Places.createClient(application);
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
        fetchRestaurantsFromPlaces(userLocation);
    }

    private void fetchRestaurantsFromPlaces(Location userLocation) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
        );
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(fields);

        placesClient.findCurrentPlace(request)
                .addOnSuccessListener(response -> {
                    List<Restaurant> results = new ArrayList<>();
                    for (PlaceLikelihood likelihood : response.getPlaceLikelihoods()) {
                        Place place = likelihood.getPlace();
                        if (place.getTypes() != null && place.getTypes().contains(Place.Type.RESTAURANT)) {
                            LatLng latLng = place.getLatLng();
                            if (latLng != null) {
                                results.add(mapPlaceToRestaurant(place, latLng));
                            }
                        }
                    }
                    if (results.isEmpty()) {
                        results.addAll(repository.getRestaurants());
                    }
                    publishWithDistances(userLocation, results);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Places request failed", e);
                    publishWithDistances(userLocation, repository.getRestaurants());
                });
    }

    private Restaurant mapPlaceToRestaurant(Place place, LatLng latLng) {
        String name = place.getName() != null ? place.getName() : getApplication().getString(R.string.missing_data);
        String address = place.getAddress() != null ? place.getAddress() : "";
        boolean likelyHasGf = false;
        if (name != null) {
            String lower = name.toLowerCase();
            likelyHasGf = lower.contains("gluten") || lower.contains("gf");
        }
        return new Restaurant(name, address, likelyHasGf, new ArrayList<>(), latLng.latitude, latLng.longitude);
    }

    private void publishWithDistances(Location userLocation, List<Restaurant> restaurants) {
        if (restaurants == null) {
            restaurants = new ArrayList<>();
        }
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
            lastSuccessfulRestaurants.clear();
            lastSuccessfulRestaurants.addAll(restaurants);
            restaurantState.setValue(RestaurantUiState.success(restaurants, userLocation.getLatitude(), userLocation.getLongitude()));
        }
    }

    private void postLocationError() {
        String message = getApplication().getString(R.string.location_error);
        if (!lastSuccessfulRestaurants.isEmpty()) {
            restaurantState.setValue(RestaurantUiState.successWithMessage(
                    lastSuccessfulRestaurants,
                    lastSuccessfulRestaurants.get(0).getLatitude(),
                    lastSuccessfulRestaurants.get(0).getLongitude(),
                    getApplication().getString(R.string.using_cached_results)
            ));
        } else {
            restaurantState.setValue(RestaurantUiState.error(message));
        }
    }
}
