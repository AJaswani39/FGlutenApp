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
import android.annotation.SuppressLint;

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
import java.net.URI;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ViewModel for restaurant-related functionality in the FGluten Android application.
 * 
 * This is the central business logic component that handles:
 * 
 * **Core Functionality:**
 * - Google Places API integration for restaurant discovery
 * - Location-based restaurant search with proximity filtering
 * - Real-time restaurant data fetching and caching
 * - Offline support through local data persistence
 * 
 * **Advanced Features:**
 * - Automated menu scanning and gluten-free evidence extraction
 * - Website scraping for restaurant menus and GF information
 * - User favorites system with persistent storage
 * - Crowd-sourced notes and reviews
 * - Robots.txt compliance for ethical web scraping
 * 
 * **Data Management:**
 * - Complex filtering (GF-only, open hours, rating, distance)
 * - Sorting capabilities (distance, name)
 * - SharedPreferences-based caching system
 * - Batch processing for performance optimization
 * 
 * **Architecture:**
 * - Uses AndroidViewModel for application-wide state management
 * - Implements reactive programming with LiveData
 * - Thread separation: UI work on main thread, I/O on background executor
 * - Error handling with graceful fallbacks and cached data usage
 * 
 * The ViewModel follows the Single Responsibility Principle, focusing entirely
 * on restaurant data operations while delegating UI concerns to fragments.
 * 
 * @author FGluten Development Team
 */
public class RestaurantViewModel extends AndroidViewModel {

    // ========== ENUMS FOR STATE MANAGEMENT ==========
    
    /**
     * Enumeration representing the current state of restaurant loading operations.
     * 
     * These states are used throughout the UI to show appropriate loading indicators,
     * error messages, and content. The state machine ensures consistent user experience
     * across different loading scenarios.
     */
    public enum Status {
        /** No operation in progress, ready for new requests */
        IDLE,
        
        /** Currently fetching restaurant data from APIs or cache */
        LOADING,
        
        /** Restaurant data successfully loaded and available for display */
        SUCCESS,
        
        /** Location permission required before proceeding with restaurant search */
        PERMISSION_REQUIRED,
        
        /** An error occurred during restaurant loading (network, API, or permission issues) */
        ERROR
    }

    /**
     * Enumeration defining available sorting methods for restaurant lists.
     * 
     * Determines how restaurants are ordered when displayed to users.
     * Different sorting modes serve different user needs and use cases.
     */
    public enum SortMode {
        /** Sort restaurants by distance from user's current location (default) */
        DISTANCE,
        
        /** Sort restaurants alphabetically by name */
        NAME
    }

    // ========== UI STATE DATA CLASS ==========
    
    /**
     * Immutable data class representing the complete state of restaurant data for UI consumption.
     * 
     * This class encapsulates all information needed by UI components to render the restaurant
     * interface, including loading states, data, messages, and user location. Using an immutable
     * design ensures thread safety and prevents accidental state corruption.
     * 
     * The state follows a Unidirectional Data Flow pattern where:
     * - ViewModel produces RestaurantUiState objects
     * - UI components observe and react to state changes
     * - User actions trigger ViewModel methods that produce new states
     * 
     * @see Status for possible loading states
     */
    public static class RestaurantUiState {
        // Core state data
        private final Status status;
        private final List<Restaurant> restaurants;
        private final String message;
        
        // User location for distance calculations and map centering
        private final Double userLatitude;
        private final Double userLongitude;

        /**
         * Private constructor to enforce use of factory methods.
         * Ensures all RestaurantUiState objects are created through controlled factory methods
         * that enforce business rules and state consistency.
         * 
         * @param status Current loading/processing status
         * @param restaurants List of restaurants (may be empty or null)
         * @param message Optional message for user display (errors, warnings, info)
         * @param userLatitude User's current latitude for distance calculations
         * @param userLongitude User's current longitude for distance calculations
         */
        private RestaurantUiState(Status status, List<Restaurant> restaurants, String message, Double userLatitude, Double userLongitude) {
            this.status = status;
            this.restaurants = restaurants;
            this.message = message;
            this.userLatitude = userLatitude;
            this.userLongitude = userLongitude;
        }

        // ========== FACTORY METHODS FOR DIFFERENT STATES ==========
        
        /** Creates an idle state with no active operations */
        public static RestaurantUiState idle() {
            return new RestaurantUiState(Status.IDLE, new ArrayList<>(), null, null, null);
        }

        /** Creates a loading state with optional message */
        public static RestaurantUiState loading(String message) {
            return new RestaurantUiState(Status.LOADING, new ArrayList<>(), message, null, null);
        }

        /** Creates a permission-required state (typically for location access) */
        public static RestaurantUiState permissionRequired(String message) {
            return new RestaurantUiState(Status.PERMISSION_REQUIRED, new ArrayList<>(), message, null, null);
        }

        /** Creates an error state with error message */
        public static RestaurantUiState error(String message) {
            return new RestaurantUiState(Status.ERROR, new ArrayList<>(), message, null, null);
        }

        /** Creates a success state with restaurant data and user location */
        public static RestaurantUiState success(List<Restaurant> restaurants, double userLatitude, double userLongitude) {
            return new RestaurantUiState(Status.SUCCESS, restaurants, null, userLatitude, userLongitude);
        }

        /** Creates a success state with additional message (e.g., cached data notice) */
        public static RestaurantUiState successWithMessage(List<Restaurant> restaurants, double userLatitude, double userLongitude, String message) {
            return new RestaurantUiState(Status.SUCCESS, restaurants, message, userLatitude, userLongitude);
        }

        // ========== ACCESSOR METHODS ==========
        
        /** @return Current status of the restaurant loading operation */
        public Status getStatus() {
            return status;
        }

        /** @return List of restaurants (never null, may be empty) */
        public List<Restaurant> getRestaurants() {
            return restaurants;
        }

        /** @return Optional message for user display (error, warning, info) */
        public String getMessage() {
            return message;
        }

        /** @return User's latitude for distance calculations, null if unavailable */
        public Double getUserLatitude() {
            return userLatitude;
        }

        /** @return User's longitude for distance calculations, null if unavailable */
        public Double getUserLongitude() {
            return userLongitude;
        }
    }

    // ========== CORE DEPENDENCIES ==========
    /** Repository for restaurant data operations */
    private final RestaurantRepository repository;
    
    /** Google Play Services client for location services */
    private final FusedLocationProviderClient fusedLocationProviderClient;
    
    /** Google Places API client for restaurant search functionality */
    private final PlacesClient placesClient;

    // ========== REACTIVE STATE MANAGEMENT ==========
    /** LiveData observable for UI components to observe restaurant state changes */
    private final MutableLiveData<RestaurantUiState> restaurantState = new MutableLiveData<>(RestaurantUiState.idle());

    // ========== DATA STORAGE FOR FILTERING & CACHING ==========
    /** Raw restaurant data from the last successful API call (before filtering) */
    private final List<Restaurant> lastRestaurantsRaw = new ArrayList<>();
    
    /** Filtered and sorted restaurants ready for UI display */
    private final List<Restaurant> lastSuccessfulRestaurants = new ArrayList<>();
    
    /** Cached user location for distance calculations */
    private Double lastUserLat = null;
    private Double lastUserLng = null;

    // ========== FILTER PREFERENCES ==========
    /** User preference: show only restaurants with gluten-free options */
    private boolean gfOnly = false;
    
    /** Current sorting mode for restaurant display */
    private SortMode sortMode = SortMode.DISTANCE;
    
    /** User preference: show only currently open restaurants */
    private boolean openNowOnly = false;
    
    /** Maximum distance filter in meters (0 = no limit) */
    private double maxDistanceMeters = 0.0; // 0 means no limit
    
    /** Minimum rating filter (0.0 = no minimum) */
    private double minRating = 0.0; // 0 means none

    // ========== PERSISTENT STORAGE ==========
    /** SharedPreferences for caching restaurant data */
    private final SharedPreferences cachePrefs;
    
    /** SharedPreferences for storing user favorites */
    private final SharedPreferences favoritesPrefs;
    
    /** SharedPreferences for storing crowd-sourced notes */
    private final SharedPreferences notesPrefs;
    
    /** Flag to prevent multiple cache load attempts */
    private boolean cacheAttempted = false;
    
    /** Indicates if Google Maps API key is configured */
    private final boolean hasMapsKey;

    // ========== CONSTANTS & CONFIGURATION ==========
    /** Log tag for debugging and logging purposes */
    private static final String TAG = "RestaurantViewModel";
    
    /** Search radius for nearby restaurants in meters (120km = 120,000m) */
    private static final int NEARBY_RADIUS_METERS = 50_000;
    
    /** SharedPreferences key for restaurant cache data */
    private static final String PREF_KEY_CACHE = "restaurant_cache";
    
    /** SharedPreferences key for favorites data */
    private static final String PREF_KEY_FAVORITES = "favorites_map";
    
    /** SharedPreferences key for notes data */
    private static final String PREF_KEY_NOTES = "notes_map";
    
    /** Time-to-live for menu scan results in milliseconds (3 days) */
    private static final long MENU_SCAN_TTL_MS = 3L * 24 * 60 * 60 * 1000; // 3 days
    
    /** Maximum bytes to fetch when scraping restaurant websites */
    private static final int MENU_MAX_BYTES = 200_000;
    
    /** Maximum number of menu scans to perform in a single batch */
    private static final int MAX_SCANS_PER_BATCH = 5;
    
    /** User agent string for HTTP requests to restaurant websites */
    private static final String USER_AGENT = "FGlutenApp/1.0";

    // ========== THREADING & EXECUTION ==========
    /** Background executor for I/O operations (API calls, web scraping, file operations) */
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    
    /** Main thread handler for UI updates */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ========== WEB SCRAPING INFRASTRUCTURE ==========
    /** Cache for robots.txt disallow rules to avoid repeated network requests */
    private final Map<String, List<String>> robotsDisallowCache = new HashMap<>();
    
    /** In-memory cache of user favorites for fast access */
    private Map<String, String> favoriteMap = new HashMap<>();
    
    /** In-memory cache of crowd-sourced notes for fast access */
    private Map<String, List<String>> notesMap = new HashMap<>();

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
        favoritesPrefs = application.getSharedPreferences("restaurant_favorites", Context.MODE_PRIVATE);
        notesPrefs = application.getSharedPreferences("restaurant_notes", Context.MODE_PRIVATE);
        favoriteMap = loadFavorites();
        notesMap = loadNotes();
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

    public void setOpenNowOnly(boolean openNowOnly) {
        this.openNowOnly = openNowOnly;
        emitFilteredState(null);
    }

    public void setMaxDistanceMeters(double maxDistanceMeters) {
        this.maxDistanceMeters = Math.max(0.0, maxDistanceMeters);
        emitFilteredState(null);
    }

    public void setMinRating(double minRating) {
        this.minRating = Math.max(0.0, minRating);
        emitFilteredState(null);
    }

    /**
     * Initiates the main restaurant discovery workflow.
     * 
     * This is the primary entry point for finding restaurants near the user's location.
     * The method implements a robust error-handling and fallback strategy:
     * 
     * 1. **API Key Validation**: Checks if Google Maps API key is configured
     * 2. **Cache Loading**: Attempts to load cached restaurant data for instant display
     * 3. **Permission Check**: Verifies location permissions before proceeding
     * 4. **Location Fetching**: Gets user location with fallback strategies
     * 5. **Restaurant Search**: Uses Google Places API to find nearby restaurants
     * 6. **Data Processing**: Applies filtering, sorting, and distance calculations
     * 7. **Menu Scanning**: Automatically scans restaurant websites for GF information
     * 8. **Caching**: Saves results for offline use and future sessions
     * 
     * The method is designed to be resilient, providing cached data when live data
     * is unavailable and clear error messages for permission or API issues.
     * 
     * @see #loadCachedIfAvailable() for offline data loading
     * @see #fetchRestaurantsViaNearbySearch(Location) for API integration
     * @see #handlePlacesFailure(Throwable, Location) for error handling
     */
    @SuppressLint("MissingPermission")
    public void loadNearbyRestaurants() {
        Log.d(TAG, "loadNearbyRestaurants called");
        
        // Step 1: Validate API key configuration
        if (!hasMapsKey) {
            restaurantState.setValue(RestaurantUiState.error(getApplication().getString(R.string.fgluten_missing_maps_key)));
            return;
        }

        // Step 2: Load cached data for immediate display (offline support)
        loadCachedIfAvailable();

        // Step 3: Check location permissions
        boolean hasPermission = hasLocationPermission();
        Log.d(TAG, "hasLocationPermission=" + hasPermission);
        if (!hasPermission) {
            String message = getApplication().getString(R.string.location_permission_needed);
            restaurantState.setValue(RestaurantUiState.permissionRequired(message));
            return;
        }

        // Step 4: Show loading state while fetching fresh data
        restaurantState.setValue(RestaurantUiState.loading(getApplication().getString(R.string.loading_restaurants)));

        // Step 5: Fetch user location with graceful fallback
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    Log.d(TAG, "getLastLocation success, location=" + (location != null ? location.getLatitude() + "," + location.getLongitude() : "null"));
                    if (location != null) {
                        publishRestaurantsForLocation(location);
                    } else {
                        // Fallback: request fresh location if cached location is unavailable
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

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
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
                        + "&radius=" + NEARBY_RADIUS_METERS
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
                        String placeId = obj.optString("place_id", null);
                        JSONObject opening = obj.optJSONObject("opening_hours");
                        if (opening != null && opening.has("open_now")) {
                            openNow = opening.optBoolean("open_now");
                        }
                        if (Double.isNaN(rLat) || Double.isNaN(rLng)) continue;
                        boolean likelyHasGf = name.toLowerCase().contains("gluten") || name.toLowerCase().contains("gf");
                        results.add(new Restaurant(name, address, likelyHasGf, new ArrayList<>(), rLat, rLng, rating, openNow, placeId));
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
        String placeId = place.getId();
        return new Restaurant(name, address, likelyHasGf, new ArrayList<>(), latLng.latitude, latLng.longitude, rating, openNow, placeId);
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
        applyFavorites(restaurants);
        applyNotes(restaurants);
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
        kickOffMenuScans(restaurants);
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
                boolean passesOpen = !openNowOnly || (restaurant.getOpenNow() != null && restaurant.getOpenNow());
                boolean passesRating = minRating <= 0.0 || (restaurant.getRating() != null && restaurant.getRating() >= minRating);
                boolean passesDistance = maxDistanceMeters <= 0.0 || restaurant.getDistanceMeters() <= maxDistanceMeters;
                if (passesOpen && passesRating && passesDistance) {
                    filtered.add(restaurant);
                }
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

    private void kickOffMenuScans(List<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean anyStarted = false;
        int launched = 0;
        for (Restaurant restaurant : restaurants) {
            if (TextUtils.isEmpty(restaurant.getPlaceId())) {
                continue;
            }
            Restaurant.MenuScanStatus status = restaurant.getMenuScanStatus();
            if (status == Restaurant.MenuScanStatus.FETCHING) {
                continue;
            }
            long age = restaurant.getMenuScanTimestamp() > 0 ? (now - restaurant.getMenuScanTimestamp()) : Long.MAX_VALUE;
            if (age < MENU_SCAN_TTL_MS && status != Restaurant.MenuScanStatus.NOT_STARTED) {
                continue; // recently checked
            }
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.FETCHING);
            anyStarted = true;
            ioExecutor.execute(() -> scanMenu(restaurant));
            launched++;
            if (launched >= MAX_SCANS_PER_BATCH) {
                break; // avoid hammering servers on burst
            }
        }
        if (anyStarted) {
            mainHandler.post(() -> emitFilteredState(null));
        }
    }

    public void requestMenuRescan(Restaurant restaurant) {
        if (restaurant == null || TextUtils.isEmpty(restaurant.getPlaceId())) {
            return;
        }
        Restaurant target = findMatchingInLast(restaurant);
        if (target == null) {
            target = restaurant;
        }
        final Restaurant scanTarget = target;
        target.setMenuScanStatus(Restaurant.MenuScanStatus.FETCHING);
        target.setMenuScanTimestamp(System.currentTimeMillis());
        target.setGlutenFreeMenuItems(new ArrayList<>());
        mainHandler.post(() -> emitFilteredState(null));
        ioExecutor.execute(() -> scanMenu(scanTarget));
    }

    private Restaurant findMatchingInLast(Restaurant candidate) {
        if (candidate == null || lastRestaurantsRaw.isEmpty()) {
            return null;
        }
        for (Restaurant r : lastRestaurantsRaw) {
            if (!TextUtils.isEmpty(candidate.getPlaceId()) && candidate.getPlaceId().equals(r.getPlaceId())) {
                return r;
            }
            if (TextUtils.isEmpty(candidate.getPlaceId())
                    && candidate.getName() != null && candidate.getAddress() != null
                    && candidate.getName().equals(r.getName())
                    && candidate.getAddress().equals(r.getAddress())) {
                return r;
            }
        }
        return null;
    }

    private void scanMenu(Restaurant restaurant) {
        String website = fetchWebsiteForPlace(restaurant.getPlaceId());
        if (TextUtils.isEmpty(website)) {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.NO_WEBSITE);
            restaurant.setMenuScanTimestamp(System.currentTimeMillis());
            mainHandler.post(() -> emitFilteredState(null));
            return;
        }

        restaurant.setMenuUrl(website);
        String html = fetchHtml(website);
        if (html != null) {
            String menuUrl = findMenuLink(html, website);
            if (!TextUtils.isEmpty(menuUrl) && !menuUrl.equals(website)) {
                restaurant.setMenuUrl(menuUrl);
                String menuHtml = fetchHtml(menuUrl);
                if (menuHtml != null) {
                    html = menuHtml;
                }
            }
        }

        List<String> evidence = html != null ? extractGfEvidence(html) : new ArrayList<>();
        restaurant.setGlutenFreeMenuItems(evidence);
        restaurant.setMenuScanTimestamp(System.currentTimeMillis());
        if (TextUtils.isEmpty(restaurant.getMenuUrl())) {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.NO_WEBSITE);
        } else if (html == null) {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.FAILED);
        } else {
            // SUCCESS simply means we scanned; evidence may be empty if no GF items were found.
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.SUCCESS);
        }
        mainHandler.post(() -> emitFilteredState(null));
    }

    private String fetchWebsiteForPlace(String placeId) {
        if (TextUtils.isEmpty(placeId) || !hasMapsKey) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            String encodedPlaceId = placeId;
            try {
                encodedPlaceId = URLEncoder.encode(placeId, "UTF-8");
            } catch (Exception ignored) {
                // best-effort; continue with raw value
            }
            String urlStr = "https://maps.googleapis.com/maps/api/place/details/json"
                    + "?place_id=" + encodedPlaceId
                    + "&fields=website"
                    + "&key=" + BuildConfig.MAPS_API_KEY;
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            JSONObject root = new JSONObject(sb.toString());
            JSONObject result = root.optJSONObject("result");
            if (result != null) {
                String website = result.optString("website", null);
                if (!TextUtils.isEmpty(website)) {
                    return website;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "fetchWebsiteForPlace failed for placeId=" + placeId, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private String fetchHtml(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            if (!isAllowedByRobots(urlStr)) {
                Log.d(TAG, "Blocked by robots.txt for url=" + urlStr);
                return null;
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return null;
            }
            String contentType = connection.getHeaderField("Content-Type");
            if (contentType != null && !contentType.toLowerCase().contains("text")) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[2048];
            int total = 0;
            int read;
            while ((read = reader.read(buffer)) != -1 && total < MENU_MAX_BYTES) {
                sb.append(buffer, 0, read);
                total += read;
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            Log.w(TAG, "fetchHtml failed for url=" + urlStr, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String findMenuLink(String html, String baseUrl) {
        if (TextUtils.isEmpty(html)) {
            return null;
        }
        Pattern menuPattern = Pattern.compile("href\\s*=\\s*\"([^\"]*menu[^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = menuPattern.matcher(html);
        while (matcher.find()) {
            String link = matcher.group(1);
            String resolved = resolveUrl(baseUrl, link);
            if (!TextUtils.isEmpty(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    private String resolveUrl(String baseUrl, String candidate) {
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(candidate);
            return resolved.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    // Favorites
    public void setFavoriteStatus(Restaurant restaurant, String status) {
        if (restaurant == null) return;
        String key = favoriteKey(restaurant);
        if (TextUtils.isEmpty(key)) return;
        if (TextUtils.isEmpty(status)) {
            favoriteMap.remove(key);
        } else {
            favoriteMap.put(key, status);
        }
        restaurant.setFavoriteStatus(status);
        saveFavorites();
        // update matching entry in list
        Restaurant match = findMatchingInLast(restaurant);
        if (match != null) {
            match.setFavoriteStatus(status);
        }
        emitFilteredState(null);
    }

    private void applyFavorites(List<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty() || favoriteMap.isEmpty()) {
            return;
        }
        for (Restaurant r : restaurants) {
            String key = favoriteKey(r);
            if (!TextUtils.isEmpty(key) && favoriteMap.containsKey(key)) {
                r.setFavoriteStatus(favoriteMap.get(key));
            }
        }
    }

    private String favoriteKey(Restaurant restaurant) {
        if (restaurant == null) return null;
        if (!TextUtils.isEmpty(restaurant.getPlaceId())) {
            return "pid:" + restaurant.getPlaceId();
        }
        if (!TextUtils.isEmpty(restaurant.getName()) && !TextUtils.isEmpty(restaurant.getAddress())) {
            return "na:" + restaurant.getName() + "|" + restaurant.getAddress();
        }
        return null;
    }

    private Map<String, String> loadFavorites() {
        Map<String, String> map = new HashMap<>();
        String raw = favoritesPrefs.getString(PREF_KEY_FAVORITES, null);
        if (TextUtils.isEmpty(raw)) {
            return map;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray names = obj.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.optString(i, null);
                    if (key == null) continue;
                    String status = obj.optString(key, null);
                    if (!TextUtils.isEmpty(status)) {
                        map.put(key, status);
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse favorites map", e);
        }
        return map;
    }

    private void saveFavorites() {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, String> entry : favoriteMap.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
            favoritesPrefs.edit().putString(PREF_KEY_FAVORITES, obj.toString()).apply();
        } catch (JSONException e) {
            Log.w(TAG, "Failed to save favorites map", e);
        }
    }

    public void addCrowdNote(Restaurant restaurant, String note) {
        if (restaurant == null || TextUtils.isEmpty(note)) return;
        String key = favoriteKey(restaurant); // reuse key logic
        if (TextUtils.isEmpty(key)) return;
        List<String> notes = notesMap.containsKey(key) ? notesMap.get(key) : new ArrayList<>();
        notes.add(note.trim());
        notesMap.put(key, notes);
        restaurant.addCrowdNote(note);
        Restaurant match = findMatchingInLast(restaurant);
        if (match != null) {
            if (match != restaurant) {
                match.addCrowdNote(note);
            }
        }
        saveNotes();
        emitFilteredState(null);
    }

    private void applyNotes(List<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty() || notesMap.isEmpty()) {
            return;
        }
        for (Restaurant r : restaurants) {
            String key = favoriteKey(r);
            if (!TextUtils.isEmpty(key) && notesMap.containsKey(key)) {
                r.setCrowdNotes(notesMap.get(key));
            }
        }
    }

    private Map<String, List<String>> loadNotes() {
        Map<String, List<String>> map = new HashMap<>();
        String raw = notesPrefs.getString(PREF_KEY_NOTES, null);
        if (TextUtils.isEmpty(raw)) {
            return map;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray names = obj.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.optString(i, null);
                    if (key == null) continue;
                    JSONArray arr = obj.optJSONArray(key);
                    if (arr == null) continue;
                    List<String> notes = new ArrayList<>();
                    for (int j = 0; j < arr.length(); j++) {
                        String n = arr.optString(j, null);
                        if (!TextUtils.isEmpty(n)) {
                            notes.add(n);
                        }
                    }
                    if (!notes.isEmpty()) {
                        map.put(key, notes);
                    }
                }
            }
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse notes map", e);
        }
        return map;
    }

    private void saveNotes() {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, List<String>> entry : notesMap.entrySet()) {
                JSONArray arr = new JSONArray();
                for (String n : entry.getValue()) {
                    arr.put(n);
                }
                root.put(entry.getKey(), arr);
            }
            notesPrefs.edit().putString(PREF_KEY_NOTES, root.toString()).apply();
        } catch (JSONException e) {
            Log.w(TAG, "Failed to save notes map", e);
        }
    }

    private boolean isAllowedByRobots(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String host = url.getHost();
            String path = url.getPath();
            List<String> disallows = robotsDisallowCache.get(host);
            if (disallows == null) {
                disallows = fetchRobots(host, url.getProtocol());
                robotsDisallowCache.put(host, disallows);
            }
            for (String rule : disallows) {
                if (path.startsWith(rule)) {
                    return false;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "robots check failed for " + urlStr, e);
            return true; // fail open to avoid blocking everything
        }
        return true;
    }

    private List<String> fetchRobots(String host, String scheme) {
        List<String> disallows = new ArrayList<>();
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL robotsUrl = new URL(scheme + "://" + host + "/robots.txt");
            connection = (HttpURLConnection) robotsUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                return disallows;
            }
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            boolean inStarSection = false;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.toLowerCase().startsWith("user-agent:")) {
                    String agent = line.substring("user-agent:".length()).trim();
                    inStarSection = "*".equals(agent);
                } else if (inStarSection && line.toLowerCase().startsWith("disallow:")) {
                    String rule = line.substring("disallow:".length()).trim();
                    if (!rule.isEmpty()) {
                        disallows.add(rule);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore robots failures
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (Exception ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return disallows;
    }

    private List<String> extractGfEvidence(String html) {
        List<String> results = new ArrayList<>();
        if (TextUtils.isEmpty(html)) {
            return results;
        }
        String noTags = html.replaceAll("(?s)<script.*?>.*?</script>", " ")
                .replaceAll("(?s)<style.*?>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ");
        String[] lines = noTags.split("\n");
        Pattern gfPattern = Pattern.compile("(?i)(gluten[-\\s]?free|\\bgf\\b|celiac|coeliac|no gluten)");
        for (String rawLine : lines) {
            String line = rawLine.trim().replaceAll("\\s{2,}", " ");
            if (line.length() < 4) {
                continue;
            }
            Matcher m = gfPattern.matcher(line);
            if (m.find()) {
                String snippet = line;
                if (snippet.length() > 140) {
                    snippet = snippet.substring(0, 140);
                }
                if (!results.contains(snippet)) {
                    results.add(snippet);
                    if (results.size() >= 8) {
                        break;
                    }
                }
            }
        }
        return results;
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
                if (r.getPlaceId() != null) {
                    obj.put("placeId", r.getPlaceId());
                }
                if (r.getMenuUrl() != null) {
                    obj.put("menuUrl", r.getMenuUrl());
                }
                if (r.getMenuScanStatus() != null) {
                    obj.put("menuScanStatus", r.getMenuScanStatus().name());
                }
                obj.put("menuScanTimestamp", r.getMenuScanTimestamp());
                if (r.getFavoriteStatus() != null) {
                    obj.put("favoriteStatus", r.getFavoriteStatus());
                }
                JSONArray notesArr = new JSONArray();
                if (r.getCrowdNotes() != null) {
                    for (String note : r.getCrowdNotes()) {
                        notesArr.put(note);
                    }
                }
                obj.put("notes", notesArr);
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
                String placeId = obj.optString("placeId", null);
                String menuUrl = obj.optString("menuUrl", null);
                String scanStatusString = obj.optString("menuScanStatus", Restaurant.MenuScanStatus.NOT_STARTED.name());
                long scanTimestamp = obj.optLong("menuScanTimestamp", 0L);
                Double rating = obj.has("rating") ? obj.optDouble("rating") : null;
                Boolean openNow = obj.has("openNow") ? obj.optBoolean("openNow") : null;
                JSONArray menuArray = obj.optJSONArray("menu");
                List<String> menu = new ArrayList<>();
                if (menuArray != null) {
                    for (int j = 0; j < menuArray.length(); j++) {
                        menu.add(menuArray.optString(j, ""));
                    }
                }
                JSONArray notesArray = obj.optJSONArray("notes");
                List<String> notes = new ArrayList<>();
                if (notesArray != null) {
                    for (int j = 0; j < notesArray.length(); j++) {
                        notes.add(notesArray.optString(j, ""));
                    }
                }
                Restaurant r = new Restaurant(name, address, hasGf, menu, rLat, rLng, rating, openNow, placeId);
                if (!TextUtils.isEmpty(menuUrl)) {
                    r.setMenuUrl(menuUrl);
                }
                try {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.valueOf(scanStatusString));
                } catch (Exception ignored) {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.NOT_STARTED);
                }
                r.setMenuScanTimestamp(scanTimestamp);
                r.setCrowdNotes(notes);
                if (obj.has("favoriteStatus")) {
                    r.setFavoriteStatus(obj.optString("favoriteStatus", null));
                }
                restored.add(r);
            }
            if (restored.isEmpty()) {
                return;
            }
            lastRestaurantsRaw.clear();
            lastRestaurantsRaw.addAll(restored);
            applyFavorites(lastRestaurantsRaw);
            applyNotes(lastRestaurantsRaw);
            lastUserLat = lat;
            lastUserLng = lng;
            String message = getApplication().getString(R.string.using_cached_results);
            if (timestamp > 0) {
                message = message + " (" + android.text.format.DateFormat.format("MMM d, h:mm a", timestamp) + ")";
            }
            emitFilteredState(message);
            kickOffMenuScans(restored);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse cached restaurants", e);
        }
    }
}
