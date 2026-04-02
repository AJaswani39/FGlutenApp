package io.fgluten.ui.restaurant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.libraries.places.api.Places;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.fgluten.BuildConfig;
import io.fgluten.R;
import io.fgluten.data.Restaurant;
import io.fgluten.data.repository.MenuScannerRepository;
import io.fgluten.data.repository.PlacesRepository;
import io.fgluten.data.repository.RestaurantCacheManager;
import io.fgluten.util.SettingsManager;

public class RestaurantViewModel extends AndroidViewModel {

    public enum Status { IDLE, LOADING, SUCCESS, PERMISSION_REQUIRED, ERROR }
    public enum SortMode { DISTANCE, NAME }

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

        public static RestaurantUiState idle() { return new RestaurantUiState(Status.IDLE, new ArrayList<>(), null, null, null); }
        public static RestaurantUiState loading(String message) { return new RestaurantUiState(Status.LOADING, new ArrayList<>(), message, null, null); }
        public static RestaurantUiState permissionRequired(String message) { return new RestaurantUiState(Status.PERMISSION_REQUIRED, new ArrayList<>(), message, null, null); }
        public static RestaurantUiState error(String message) { return new RestaurantUiState(Status.ERROR, new ArrayList<>(), message, null, null); }
        public static RestaurantUiState success(List<Restaurant> restaurants, double userLatitude, double userLongitude) { return new RestaurantUiState(Status.SUCCESS, restaurants, null, userLatitude, userLongitude); }
        public static RestaurantUiState successWithMessage(List<Restaurant> restaurants, double userLatitude, double userLongitude, String message) { return new RestaurantUiState(Status.SUCCESS, restaurants, message, userLatitude, userLongitude); }

        public Status getStatus() { return status; }
        public List<Restaurant> getRestaurants() { return restaurants; }
        public String getMessage() { return message; }
        public Double getUserLatitude() { return userLatitude; }
        public Double getUserLongitude() { return userLongitude; }
    }

    private static final String TAG = "RestaurantViewModel";
    private static final long MENU_SCAN_TTL_MS = 3L * 24 * 60 * 60 * 1000;
    private static final int MAX_SCANS_PER_BATCH = 5;

    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final MutableLiveData<RestaurantUiState> restaurantState = new MutableLiveData<>(RestaurantUiState.idle());
    private final List<Restaurant> lastRestaurantsRaw = new ArrayList<>();
    private final List<Restaurant> lastSuccessfulRestaurants = new ArrayList<>();
    
    private Double lastUserLat = null;
    private Double lastUserLng = null;
    
    private boolean gfOnly = false;
    private SortMode sortMode = SortMode.DISTANCE;
    private boolean openNowOnly = false;
    private double maxDistanceMeters = 0.0;
    private double minRating = 0.0;
    private String searchQuery = "";
    
    private final SharedPreferences filterPrefs;
    private static final String PREFS_FILTERS = "restaurant_filters";
    private static final String PREF_KEY_GF_ONLY = "gf_only";
    private static final String PREF_KEY_OPEN_NOW = "open_now";
    private static final String PREF_KEY_SORT_MODE = "sort_mode";
    private static final String PREF_KEY_MAX_DISTANCE = "max_distance";
    private static final String PREF_KEY_MIN_RATING = "min_rating";
    
    private boolean cacheAttempted = false;
    private final boolean hasMapsKey;
    
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private final ConcurrentHashMap<String, String> favoriteMap = new ConcurrentHashMap<>();

    private final PlacesRepository placesRepository;
    private final MenuScannerRepository menuScannerRepository;
    private final RestaurantCacheManager cacheManager;

    public RestaurantViewModel(@NonNull Application application) {
        super(application);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application);
        String apiKey = BuildConfig.MAPS_API_KEY;
        hasMapsKey = !TextUtils.isEmpty(apiKey);
        if (!Places.isInitialized() && hasMapsKey) {
            Places.initialize(application, apiKey);
        }

        placesRepository = new PlacesRepository();
        menuScannerRepository = new MenuScannerRepository();
        cacheManager = new RestaurantCacheManager(
                application.getSharedPreferences("restaurant_cache", Context.MODE_PRIVATE),
                application.getSharedPreferences("restaurant_favorites", Context.MODE_PRIVATE)
        );

        favoriteMap.putAll(cacheManager.loadFavorites());

        filterPrefs = application.getSharedPreferences(PREFS_FILTERS, Context.MODE_PRIVATE);
        gfOnly = filterPrefs.getBoolean(PREF_KEY_GF_ONLY, false);
        openNowOnly = filterPrefs.getBoolean(PREF_KEY_OPEN_NOW, false);
        String sortModeStr = filterPrefs.getString(PREF_KEY_SORT_MODE, SortMode.DISTANCE.name());
        try { sortMode = SortMode.valueOf(sortModeStr); } catch (Exception e) { sortMode = SortMode.DISTANCE; }
        maxDistanceMeters = filterPrefs.getFloat(PREF_KEY_MAX_DISTANCE, 0.0f);
        minRating = filterPrefs.getFloat(PREF_KEY_MIN_RATING, 0.0f);
    }

    public LiveData<RestaurantUiState> getRestaurantState() { return restaurantState; }
    
    public void setGfOnly(boolean gfOnly) {
        this.gfOnly = gfOnly;
        filterPrefs.edit().putBoolean(PREF_KEY_GF_ONLY, gfOnly).apply();
        emitFilteredState(null);
    }
    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
        filterPrefs.edit().putString(PREF_KEY_SORT_MODE, sortMode.name()).apply();
        emitFilteredState(null);
    }
    public void setOpenNowOnly(boolean openNowOnly) {
        this.openNowOnly = openNowOnly;
        filterPrefs.edit().putBoolean(PREF_KEY_OPEN_NOW, openNowOnly).apply();
        emitFilteredState(null);
    }
    public void setMaxDistanceMeters(double maxDistanceMeters) {
        this.maxDistanceMeters = Math.max(0.0, maxDistanceMeters);
        filterPrefs.edit().putFloat(PREF_KEY_MAX_DISTANCE, (float) this.maxDistanceMeters).apply();
        emitFilteredState(null);
    }
    public void setMinRating(double minRating) {
        this.minRating = Math.max(0.0, minRating);
        filterPrefs.edit().putFloat(PREF_KEY_MIN_RATING, (float) this.minRating).apply();
        emitFilteredState(null);
    }
    public boolean isGfOnly() { return gfOnly; }
    public boolean isOpenNowOnly() { return openNowOnly; }
    public SortMode getSortMode() { return sortMode; }
    public double getMaxDistanceMeters() { return maxDistanceMeters; }
    public double getMinRating() { return minRating; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query.trim().toLowerCase();
        emitFilteredState(null);
    }

    @SuppressLint("MissingPermission")
    public void loadNearbyRestaurants() {
        if (!hasMapsKey) {
            restaurantState.setValue(RestaurantUiState.error(getApplication().getString(R.string.fgluten_missing_maps_key)));
            return;
        }

        loadCachedIfAvailable();

        if (!hasLocationPermission()) {
            restaurantState.setValue(RestaurantUiState.permissionRequired(getApplication().getString(R.string.location_permission_needed)));
            return;
        }

        restaurantState.setValue(RestaurantUiState.loading(getApplication().getString(R.string.loading_restaurants)));

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) publishRestaurantsForLocation(location);
                    else requestFreshLocation();
                })
                .addOnFailureListener(e -> postLocationError());
    }

    private boolean hasLocationPermission() {
        int fine = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_COARSE_LOCATION);
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void requestFreshLocation() {
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_LOW_POWER, tokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) publishRestaurantsForLocation(location);
                    else postLocationError();
                })
                .addOnFailureListener(e -> postLocationError());
    }

    private void publishRestaurantsForLocation(Location userLocation) {
        fetchRestaurantsViaNearbySearch(userLocation);
    }

    private void fetchRestaurantsViaNearbySearch(Location userLocation) {
        ioExecutor.execute(() -> {
            try {
                List<Restaurant> results = placesRepository.fetchNearbyRestaurants(userLocation.getLatitude(), userLocation.getLongitude(), BuildConfig.MAPS_API_KEY);
                if (results.isEmpty()) {
                    mainHandler.post(() -> restaurantState.setValue(RestaurantUiState.error(getApplication().getString(R.string.no_restaurants_found))));
                    return;
                }
                mainHandler.post(() -> publishWithDistances(userLocation, results));
            } catch (Exception ex) {
                String message = getApplication().getString(R.string.fgluten_places_error_message) + " [fallback:" + ex.getMessage() + "]";
                mainHandler.post(() -> {
                    if (!lastSuccessfulRestaurants.isEmpty() && lastUserLat != null && lastUserLng != null) {
                        restaurantState.setValue(RestaurantUiState.successWithMessage(
                                new ArrayList<>(lastSuccessfulRestaurants), lastUserLat, lastUserLng, message));
                    } else {
                        restaurantState.setValue(RestaurantUiState.error(message));
                    }
                });
            }
        });
    }

    private void publishWithDistances(Location userLocation, List<Restaurant> restaurants) {
        if (restaurants == null) restaurants = new ArrayList<>();
        applyFavorites(restaurants);
        for (Restaurant restaurant : restaurants) {
            float[] results = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(), restaurant.getLatitude(), restaurant.getLongitude(), results);
            restaurant.setDistanceMeters(results[0]);
        }
        lastRestaurantsRaw.clear();
        lastRestaurantsRaw.addAll(restaurants);
        lastUserLat = userLocation.getLatitude();
        lastUserLng = userLocation.getLongitude();
        emitFilteredState(null);
        cacheManager.saveCache(restaurants, lastUserLat, lastUserLng);
        kickOffMenuScans(restaurants);
    }

    private void postLocationError() {
        String message = getApplication().getString(R.string.location_error);
        if (!lastRestaurantsRaw.isEmpty() && lastUserLat != null && lastUserLng != null) {
            emitFilteredState(getApplication().getString(R.string.using_cached_results));
        } else {
            restaurantState.setValue(RestaurantUiState.error(message));
        }
    }

    private void emitFilteredState(String messageIfAny) {
        if (lastRestaurantsRaw.isEmpty() || lastUserLat == null || lastUserLng == null) return;
        
        boolean isStrictCeliac = SettingsManager.isStrictCeliac(getApplication());
        List<Restaurant> filtered = new ArrayList<>();
        for (Restaurant restaurant : lastRestaurantsRaw) {
            boolean fitsGF = (!gfOnly || restaurant.hasGlutenFreeOptions());
            
            if (isStrictCeliac) {
                boolean hasEvidence = restaurant.getGlutenFreeMenu() != null && !restaurant.getGlutenFreeMenu().isEmpty();
                boolean isHighlyRatedGF = restaurant.hasGlutenFreeOptions() && restaurant.getRating() != null && restaurant.getRating() >= 4.0;
                fitsGF = hasEvidence || isHighlyRatedGF;
            }
            
            boolean passesSearch = true;
            if (!TextUtils.isEmpty(searchQuery)) {
                boolean matchesName = restaurant.getName() != null && restaurant.getName().toLowerCase().contains(searchQuery);
                boolean matchesMenu = false;
                if (restaurant.getGlutenFreeMenu() != null) {
                    for (String item : restaurant.getGlutenFreeMenu()) {
                        if (item.toLowerCase().contains(searchQuery)) { matchesMenu = true; break; }
                    }
                }
                passesSearch = matchesName || matchesMenu;
            }
            
            if (fitsGF && passesSearch) {
                boolean passesOpen = !openNowOnly || (restaurant.getOpenNow() != null && restaurant.getOpenNow());
                boolean passesRating = minRating <= 0.0 || (restaurant.getRating() != null && restaurant.getRating() >= minRating);
                boolean passesDistance = maxDistanceMeters <= 0.0 || restaurant.getDistanceMeters() <= maxDistanceMeters;
                if (passesOpen && passesRating && passesDistance) {
                    filtered.add(restaurant);
                }
            }
        }
        if (sortMode == SortMode.DISTANCE) {
            Collections.sort(filtered, (f, s) -> Double.compare(f.getDistanceMeters(), s.getDistanceMeters()));
        } else {
            Collections.sort(filtered, (f, s) -> {
                String left = f.getName() != null ? f.getName().toLowerCase() : "";
                String right = s.getName() != null ? s.getName().toLowerCase() : "";
                return left.compareTo(right);
            });
        }

        if (filtered.isEmpty()) {
            restaurantState.setValue(RestaurantUiState.error(getApplication().getString(R.string.no_restaurants_found)));
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
        if (restaurants == null || restaurants.isEmpty()) return;
        long now = System.currentTimeMillis();
        boolean anyStarted = false;
        int launched = 0;
        for (Restaurant restaurant : restaurants) {
            if (TextUtils.isEmpty(restaurant.getPlaceId())) continue;
            Restaurant.MenuScanStatus status = restaurant.getMenuScanStatus();
            if (status == Restaurant.MenuScanStatus.FETCHING) continue;
            long age = restaurant.getMenuScanTimestamp() > 0 ? (now - restaurant.getMenuScanTimestamp()) : Long.MAX_VALUE;
            if (age < MENU_SCAN_TTL_MS && status != Restaurant.MenuScanStatus.NOT_STARTED) continue;
            
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.FETCHING);
            anyStarted = true;
            ioExecutor.execute(() -> scanMenu(restaurant));
            launched++;
            if (launched >= MAX_SCANS_PER_BATCH) break;
        }
        if (anyStarted) mainHandler.post(() -> emitFilteredState(null));
    }

    public void requestMenuRescan(Restaurant restaurant) {
        if (restaurant == null || TextUtils.isEmpty(restaurant.getPlaceId())) return;
        Restaurant target = findMatchingInLast(restaurant);
        if (target == null) target = restaurant;
        final Restaurant scanTarget = target;
        target.setMenuScanStatus(Restaurant.MenuScanStatus.FETCHING);
        target.setMenuScanTimestamp(System.currentTimeMillis());
        target.setGlutenFreeMenuItems(new ArrayList<>());
        mainHandler.post(() -> emitFilteredState(null));
        ioExecutor.execute(() -> scanMenu(scanTarget));
    }

    private Restaurant findMatchingInLast(Restaurant candidate) {
        if (candidate == null || lastRestaurantsRaw.isEmpty()) return null;
        for (Restaurant r : lastRestaurantsRaw) {
            if (!TextUtils.isEmpty(candidate.getPlaceId()) && candidate.getPlaceId().equals(r.getPlaceId())) return r;
            if (TextUtils.isEmpty(candidate.getPlaceId()) && candidate.getName() != null && candidate.getAddress() != null
                    && candidate.getName().equals(r.getName()) && candidate.getAddress().equals(r.getAddress())) {
                return r;
            }
        }
        return null;
    }

    private void scanMenu(Restaurant restaurant) {
        String website = menuScannerRepository.fetchWebsiteForPlace(restaurant.getPlaceId(), BuildConfig.MAPS_API_KEY);
        if (TextUtils.isEmpty(website)) {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.NO_WEBSITE);
            restaurant.setMenuScanTimestamp(System.currentTimeMillis());
            mainHandler.post(() -> emitFilteredState(null));
            return;
        }

        restaurant.setMenuUrl(website);
        String html = menuScannerRepository.fetchHtml(website);
        if (html != null) {
            String menuUrl = menuScannerRepository.findMenuLink(html, website);
            if (!TextUtils.isEmpty(menuUrl) && !menuUrl.equals(website)) {
                restaurant.setMenuUrl(menuUrl);
                String menuHtml = menuScannerRepository.fetchHtml(menuUrl);
                if (menuHtml != null) html = menuHtml;
            }
        }

        List<String> evidence = html != null ? menuScannerRepository.extractGfEvidence(html) : new ArrayList<>();
        restaurant.setGlutenFreeMenuItems(evidence);

        if (html != null) {
            String rawText = menuScannerRepository.extractRawMenuText(html);
            if (rawText != null) restaurant.setRawMenuText(rawText);
        }

        restaurant.setMenuScanTimestamp(System.currentTimeMillis());
        if (TextUtils.isEmpty(restaurant.getMenuUrl())) {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.NO_WEBSITE);
        } else if (html == null) {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.FAILED);
        } else {
            restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.SUCCESS);
        }
        mainHandler.post(() -> emitFilteredState(null));
    }

    public void setFavoriteStatus(Restaurant restaurant, String status) {
        if (restaurant == null) return;
        String key = favoriteKey(restaurant);
        if (TextUtils.isEmpty(key)) return;
        if (TextUtils.isEmpty(status)) favoriteMap.remove(key);
        else favoriteMap.put(key, status);
        restaurant.setFavoriteStatus(status);
        cacheManager.saveFavorites(favoriteMap);
        
        Restaurant match = findMatchingInLast(restaurant);
        if (match != null) match.setFavoriteStatus(status);
        emitFilteredState(null);
    }

    private void applyFavorites(List<Restaurant> restaurants) {
        if (restaurants == null || restaurants.isEmpty() || favoriteMap.isEmpty()) return;
        for (Restaurant r : restaurants) {
            String key = favoriteKey(r);
            if (!TextUtils.isEmpty(key) && favoriteMap.containsKey(key)) {
                r.setFavoriteStatus(favoriteMap.get(key));
            }
        }
    }

    private String favoriteKey(Restaurant restaurant) {
        if (restaurant == null) return null;
        if (!TextUtils.isEmpty(restaurant.getPlaceId())) return "pid:" + restaurant.getPlaceId();
        if (!TextUtils.isEmpty(restaurant.getName()) && !TextUtils.isEmpty(restaurant.getAddress())) {
            return "na:" + restaurant.getName() + "|" + restaurant.getAddress();
        }
        return null;
    }

    private void loadCachedIfAvailable() {
        if (cacheAttempted) return;
        cacheAttempted = true;
        RestaurantCacheManager.CachedData cachedData = cacheManager.loadCached();
        if (cachedData == null) return;
        
        lastRestaurantsRaw.clear();
        lastRestaurantsRaw.addAll(cachedData.restaurants);
        applyFavorites(lastRestaurantsRaw);
        lastUserLat = cachedData.lat;
        lastUserLng = cachedData.lng;
        
        String message = getApplication().getString(R.string.using_cached_results);
        if (cachedData.timestamp > 0) {
            message = message + " (" + android.text.format.DateFormat.format("MMM d, h:mm a", cachedData.timestamp) + ")";
        }
        emitFilteredState(message);
        kickOffMenuScans(cachedData.restaurants);
    }
}
