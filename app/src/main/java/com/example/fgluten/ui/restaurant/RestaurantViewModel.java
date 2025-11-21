package com.example.fgluten.ui.restaurant;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.text.TextUtils;
import android.os.Handler;
import android.os.Looper;

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
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantViewModel extends AndroidViewModel {

    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        PERMISSION_REQUIRED,
        ERROR
    }

    public enum SortMode {
        DISTANCE,
        NAME
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
    private final List<Restaurant> lastRestaurantsRaw = new ArrayList<>();
    private final List<Restaurant> lastSuccessfulRestaurants = new ArrayList<>();
    private Double lastUserLat = null;
    private Double lastUserLng = null;
    private boolean gfOnly = false;
    private SortMode sortMode = SortMode.DISTANCE;
    private final SharedPreferences cachePrefs;
    private boolean cacheAttempted = false;
    private final boolean hasMapsKey;
    private static final String TAG = "RestaurantViewModel";
    private static final String PREF_KEY_CACHE = "restaurant_cache";
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public RestaurantViewModel(@NonNull Application application) {
        super(application);
        repository = new RestaurantRepository();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application);
        String apiKey = BuildConfig.MAPS_API_KEY;
        hasMapsKey = !TextUtils.isEmpty(apiKey);
        if (!Places.isInitialized() && hasMapsKey) {
            Places.initialize(application, apiKey);
        }
        placesClient = Places.createClient(application);
        cachePrefs = application.getSharedPreferences("restaurant_cache", Context.MODE_PRIVATE);
    }

    public LiveData<RestaurantUiState> getRestaurantState() {
        return restaurantState;
    }

    public void setGfOnly(boolean gfOnly) {
        this.gfOnly = gfOnly;
        emitFilteredState(null);
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
        emitFilteredState(null);
    }

    public void loadNearbyRestaurants() {
        Log.d(TAG, "loadNearbyRestaurants called");
        if (!hasMapsKey) {
            restaurantState.setValue(RestaurantUiState.error(getApplication().getString(R.string.fgluten_missing_maps_key)));
            return;
        }

        loadCachedIfAvailable();

        boolean hasPermission = hasLocationPermission();
        Log.d(TAG, "hasLocationPermission=" + hasPermission);
        if (!hasPermission) {
            String message = getApplication().getString(R.string.location_permission_needed);
            restaurantState.setValue(RestaurantUiState.permissionRequired(message));
            return;
        }

        restaurantState.setValue(RestaurantUiState.loading(getApplication().getString(R.string.loading_restaurants)));

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    Log.d(TAG, "getLastLocation success, location=" + (location != null ? location.getLatitude() + "," + location.getLongitude() : "null"));
                    if (location != null) {
                        publishRestaurantsForLocation(location);
                    } else {
                        requestFreshLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getLastLocation failure", e);
                    postLocationError();
                });
    }

    private boolean hasLocationPermission() {
        Application application = getApplication();
        int fine = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(application, Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    private void requestFreshLocation() {
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_LOW_POWER, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    Log.d(TAG, "getCurrentLocation success, location=" + (location != null ? location.getLatitude() + "," + location.getLongitude() : "null"));
                    if (location != null) {
                        publishRestaurantsForLocation(location);
                    } else {
                        postLocationError();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getCurrentLocation failure", e);
                    postLocationError();
                });
    }

    private void publishRestaurantsForLocation(Location userLocation) {
        fetchRestaurantsViaNearbySearch(userLocation);
    }

    private void fetchRestaurantsFromPlaces(Location userLocation) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES,
                Place.Field.RATING,
                Place.Field.OPENING_HOURS
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
                    Log.d(TAG, "Places success, restaurant candidates=" + results.size());
                    if (results.isEmpty()) {
                        results.addAll(repository.getRestaurants());
                    }
                    if (results.isEmpty()) {
                        Log.w(TAG, "No restaurants returned from Places; attempting Nearby Search REST fallback");
                        fetchRestaurantsViaNearbySearch(userLocation);
                        return;
                    }
                    publishWithDistances(userLocation, results);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Places request failed", e);
                    handlePlacesFailure(e, userLocation);
                });
    }

    private void fetchRestaurantsViaNearbySearch(Location userLocation) {
        ioExecutor.execute(() -> {
            List<Restaurant> results = new ArrayList<>();
            HttpURLConnection connection = null;
            try {
                double lat = userLocation.getLatitude();
                double lng = userLocation.getLongitude();
                String urlStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + lat + "," + lng
                        + "&radius=5000"
                        + "&type=restaurant"
                        + "&keyword=gluten%20free"
                        + "&key=" + BuildConfig.MAPS_API_KEY;
                URL url = new URL(urlStr);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);
                int code = connection.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP " + code);
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                JSONObject root = new JSONObject(sb.toString());
                String status = root.optString("status", "");
                if (!"OK".equalsIgnoreCase(status) && !"ZERO_RESULTS".equalsIgnoreCase(status)) {
                    throw new Exception("Places nearby search status=" + status);
                }
                JSONArray arr = root.optJSONArray("results");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.optJSONObject(i);
                        if (obj == null) continue;
                        String name = obj.optString("name", "");
                        String address = obj.optString("vicinity", "");
                        JSONObject geometry = obj.optJSONObject("geometry");
                        JSONObject loc = geometry != null ? geometry.optJSONObject("location") : null;
                        double rLat = loc != null ? loc.optDouble("lat", Double.NaN) : Double.NaN;
                        double rLng = loc != null ? loc.optDouble("lng", Double.NaN) : Double.NaN;
                        Double rating = obj.has("rating") ? obj.optDouble("rating") : null;
                        Boolean openNow = null;
                        JSONObject opening = obj.optJSONObject("opening_hours");
                        if (opening != null && opening.has("open_now")) {
                            openNow = opening.optBoolean("open_now");
                        }
                        if (Double.isNaN(rLat) || Double.isNaN(rLng)) continue;
                        boolean likelyHasGf = name.toLowerCase().contains("gluten") || name.toLowerCase().contains("gf");
                        results.add(new Restaurant(name, address, likelyHasGf, new ArrayList<>(), rLat, rLng, rating, openNow));
                    }
                }
            } catch (Exception ex) {
                Log.e(TAG, "Nearby Search fallback failed", ex);
                String message = getApplication().getString(R.string.fgluten_places_error_message) + " [fallback:" + ex.getMessage() + "]";
                mainHandler.post(() -> {
                    if (!lastSuccessfulRestaurants.isEmpty() && lastUserLat != null && lastUserLng != null) {
                        restaurantState.setValue(RestaurantUiState.successWithMessage(
                                new ArrayList<>(lastSuccessfulRestaurants),
                                lastUserLat,
                                lastUserLng,
                                message
                        ));
                    } else {
                        restaurantState.setValue(RestaurantUiState.error(message));
                    }
                });
                if (connection != null) {
                    connection.disconnect();
                }
                return;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            if (results.isEmpty()) {
                Log.w(TAG, "Nearby Search returned 0 results");
                mainHandler.post(() -> restaurantState.setValue(
                        RestaurantUiState.error(getApplication().getString(R.string.no_restaurants_found))));
                return;
            }
            // Publish on main thread with distances
            mainHandler.post(() -> publishWithDistances(userLocation, results));
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
        Double rating = place.getRating();
        Boolean openNow = null;
        if (place.getOpeningHours() != null) {
            // OpeningHours does not expose open-now directly here; leave null.
            openNow = null;
        }
        return new Restaurant(name, address, likelyHasGf, new ArrayList<>(), latLng.latitude, latLng.longitude, rating, openNow);
    }

    private void handlePlacesFailure(Throwable throwable, Location userLocation) {
        String message = buildDetailedError(throwable);
        Log.e(TAG, "Places failure: " + message, throwable);
        if (userLocation != null) {
            lastUserLat = userLocation.getLatitude();
            lastUserLng = userLocation.getLongitude();
        }
        if (!lastSuccessfulRestaurants.isEmpty() && lastUserLat != null && lastUserLng != null) {
            restaurantState.setValue(RestaurantUiState.successWithMessage(
                    new ArrayList<>(lastSuccessfulRestaurants),
                    lastUserLat,
                    lastUserLng,
                    message
            ));
        } else {
            restaurantState.setValue(RestaurantUiState.error(message));
        }
    }

    private String buildDetailedError(Throwable throwable) {
        String base = getApplication().getString(R.string.fgluten_places_error_message);
        if (throwable == null) {
            return base;
        }
        StringBuilder sb = new StringBuilder(base);
        if (throwable instanceof ApiException) {
            ApiException api = (ApiException) throwable;
            sb.append(" [statusCode=").append(api.getStatusCode());
            if (api.getStatusMessage() != null) {
                sb.append(", statusMessage=").append(api.getStatusMessage());
            }
            sb.append("]");
        } else {
            if (throwable.getClass() != null) {
                sb.append(" [").append(throwable.getClass().getSimpleName()).append("]");
            }
            if (throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                sb.append(" ").append(throwable.getMessage());
            }
        }
        if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
            sb.append(" Cause: ").append(throwable.getCause().getMessage());
        }
        return sb.toString();
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
        lastRestaurantsRaw.clear();
        lastRestaurantsRaw.addAll(restaurants);
        lastUserLat = userLocation.getLatitude();
        lastUserLng = userLocation.getLongitude();
        emitFilteredState(null);
        saveCache(restaurants, lastUserLat, lastUserLng);
    }

    private void postLocationError() {
        String message = getApplication().getString(R.string.location_error);
        Log.e(TAG, "Location error: " + message);
        if (!lastRestaurantsRaw.isEmpty() && lastUserLat != null && lastUserLng != null) {
            emitFilteredState(getApplication().getString(R.string.using_cached_results));
        } else {
            restaurantState.setValue(RestaurantUiState.error(message));
        }
    }

    private void emitFilteredState(String messageIfAny) {
        if (lastRestaurantsRaw.isEmpty() || lastUserLat == null || lastUserLng == null) {
            return;
        }
        List<Restaurant> filtered = new ArrayList<>();
        for (Restaurant restaurant : lastRestaurantsRaw) {
            if (!gfOnly || restaurant.hasGlutenFreeOptions()) {
                filtered.add(restaurant);
            }
        }
        if (sortMode == SortMode.DISTANCE) {
            Collections.sort(filtered, (first, second) -> Double.compare(first.getDistanceMeters(), second.getDistanceMeters()));
        } else {
            Collections.sort(filtered, (first, second) -> {
                String left = first.getName() != null ? first.getName().toLowerCase() : "";
                String right = second.getName() != null ? second.getName().toLowerCase() : "";
                return left.compareTo(right);
            });
        }

        if (filtered.isEmpty()) {
            String message = getApplication().getString(R.string.no_restaurants_found);
            restaurantState.setValue(RestaurantUiState.error(message));
            return;
        }

        lastSuccessfulRestaurants.clear();
        lastSuccessfulRestaurants.addAll(filtered);
        if (messageIfAny != null) {
            restaurantState.setValue(RestaurantUiState.successWithMessage(filtered, lastUserLat, lastUserLng, messageIfAny));
        } else {
            restaurantState.setValue(RestaurantUiState.success(filtered, lastUserLat, lastUserLng));
        }
    }

    private void saveCache(List<Restaurant> restaurants, double lat, double lng) {
        try {
            JSONObject root = new JSONObject();
            root.put("lat", lat);
            root.put("lng", lng);
            root.put("timestamp", System.currentTimeMillis());
            JSONArray items = new JSONArray();
            for (Restaurant r : restaurants) {
                JSONObject obj = new JSONObject();
                obj.put("name", r.getName());
                obj.put("address", r.getAddress());
                obj.put("hasGf", r.hasGlutenFreeOptions());
                obj.put("lat", r.getLatitude());
                obj.put("lng", r.getLongitude());
                if (r.getRating() != null) {
                    obj.put("rating", r.getRating());
                }
                if (r.getOpenNow() != null) {
                    obj.put("openNow", r.getOpenNow());
                }
                JSONArray menu = new JSONArray();
                if (r.getGlutenFreeMenu() != null) {
                    for (String m : r.getGlutenFreeMenu()) {
                        menu.put(m);
                    }
                }
                obj.put("menu", menu);
                items.put(obj);
            }
            root.put("items", items);
            cachePrefs.edit().putString(PREF_KEY_CACHE, root.toString()).apply();
        } catch (JSONException e) {
            Log.w(TAG, "Failed to cache restaurants", e);
        }
    }

    private void loadCachedIfAvailable() {
        if (cacheAttempted) {
            return;
        }
        cacheAttempted = true;
        String cached = cachePrefs.getString(PREF_KEY_CACHE, null);
        if (cached == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject(cached);
            double lat = root.optDouble("lat", Double.NaN);
            double lng = root.optDouble("lng", Double.NaN);
            long timestamp = root.optLong("timestamp", 0L);
            JSONArray items = root.optJSONArray("items");
            if (items == null || Double.isNaN(lat) || Double.isNaN(lng)) {
                return;
            }
            List<Restaurant> restored = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.optJSONObject(i);
                if (obj == null) continue;
                String name = obj.optString("name", "");
                String address = obj.optString("address", "");
                boolean hasGf = obj.optBoolean("hasGf", false);
                double rLat = obj.optDouble("lat", 0);
                double rLng = obj.optDouble("lng", 0);
                Double rating = obj.has("rating") ? obj.optDouble("rating") : null;
                Boolean openNow = obj.has("openNow") ? obj.optBoolean("openNow") : null;
                JSONArray menuArray = obj.optJSONArray("menu");
                List<String> menu = new ArrayList<>();
                if (menuArray != null) {
                    for (int j = 0; j < menuArray.length(); j++) {
                        menu.add(menuArray.optString(j, ""));
                    }
                }
                Restaurant r = new Restaurant(name, address, hasGf, menu, rLat, rLng, rating, openNow);
                restored.add(r);
            }
            if (restored.isEmpty()) {
                return;
            }
            lastRestaurantsRaw.clear();
            lastRestaurantsRaw.addAll(restored);
            lastUserLat = lat;
            lastUserLng = lng;
            String message = getApplication().getString(R.string.using_cached_results);
            if (timestamp > 0) {
                message = message + " (" + android.text.format.DateFormat.format("MMM d, h:mm a", timestamp) + ")";
            }
            emitFilteredState(message);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse cached restaurants", e);
        }
    }
}
