package com.example.fgluten.ui.home;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.fgluten.R;
import com.example.fgluten.data.Restaurant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel for the HomeFragment dashboard and welcome screen.
 * 
 * This ViewModel manages the data and business logic for the app's main landing page:
 * 
 * **Core Responsibilities:**
 * - Cached restaurant data management and display
 * - Location permission status monitoring
 * - Text content management for dynamic UI
 * - SharedPreferences integration for offline data
 * 
 * **Cached Restaurant Management:**
 * - Loads previously found restaurants from local storage
 * - Maintains distance calculations from last known location
 * - Preserves restaurant metadata (favorites, notes, menu scan status)
 * - Provides LiveData for reactive UI updates
 * 
 * **Permission Monitoring:**
 * - Continuously checks location permission status
 * - Provides LiveData observable for UI permission banners
 * - Integrates with Android permission system
 * 
 * **Data Persistence:**
 * - Uses SharedPreferences for cached restaurant storage
 * - JSON serialization/deserialization for complex restaurant data
 * - Error handling for corrupted cache data
 * - Maintains data integrity across app sessions
 * 
 * **Architecture:**
 * - Extends AndroidViewModel for application-wide state management
 * - Uses LiveData for reactive programming patterns
 * - Follows MVVM architecture for clear separation of concerns
 * - Integrates with Restaurant data model for type safety
 * 
 * The ViewModel ensures smooth user experience by providing immediate access
 * to previously found restaurants while also monitoring permission status
 * to guide users through the app's location-dependent features.
 * 
 * @see HomeFragment for UI layer integration
 * @see Restaurant for data model structure
 * @see RestaurantViewModel for main restaurant search functionality
 * 
 * @author FGluten Development Team
 */
public class HomeViewModel extends AndroidViewModel {

    // ========== CONSTANTS & CONFIGURATION ==========
    
    /** SharedPreferences file name for cached restaurant data */
    private static final String PREFS_NAME = "restaurant_cache";
    
    /** SharedPreferences key for restaurant cache data */
    private static final String PREF_KEY_CACHE = "restaurant_cache";

    // ========== REACTIVE STATE MANAGEMENT ==========
    
    /** LiveData for dynamic text content (titles, subtitles) */
    private final MutableLiveData<String> mText;
    
    /** LiveData for cached restaurant list from previous sessions */
    private final MutableLiveData<List<Restaurant>> cachedRestaurants = new MutableLiveData<>(new ArrayList<>());
    
    /** LiveData for location permission grant status */
    private final MutableLiveData<Boolean> permissionGranted = new MutableLiveData<>(false);

    /**
     * Constructor that initializes all ViewModel state and data.
     * 
     * This constructor:
     * 1. Calls super constructor for AndroidViewModel initialization
     * 2. Sets up text content for the home screen
     * 3. Loads cached restaurant data from persistent storage
     * 4. Checks current location permission status
     * 
     * @param application Android Application context for ViewModel initialization
     */
    public HomeViewModel(@NonNull Application application) {
        super(application);
        
        // ========== TEXT CONTENT INITIALIZATION ==========
        mText = new MutableLiveData<>();
        mText.setValue(getApplication().getString(R.string.home_fragment_text));
        
        // ========== DATA LOADING ==========
        loadCachedRestaurants(); // Load from persistent storage
        checkPermission(); // Check current permission status
    }

    // ========== PUBLIC ACCESSORS ==========
    
    /**
     * Provides LiveData for dynamic text content.
     * 
     * This method returns a LiveData object that can be observed by the UI
     * for changes to text content such as fragment titles and subtitles.
     * Currently returns a fixed string from resources.
     * 
     * @return LiveData containing text content for the home fragment
     */
    public LiveData<String> getText() {
        return mText;
    }

    /**
     * Provides LiveData for cached restaurant data.
     * 
     * This method returns a LiveData object containing restaurants from
     * previous app sessions. The data is loaded asynchronously and cached
     * for immediate display when the fragment becomes active.
     * 
     * @return LiveData containing list of cached restaurants (may be empty)
     */
    public LiveData<List<Restaurant>> getCachedRestaurants() {
        return cachedRestaurants;
    }

    /**
     * Provides LiveData for location permission status.
     * 
     * This method returns a LiveData object that tracks whether location
     * permissions have been granted. The UI can observe this to show/hide
     * permission banners and guide users through the permission flow.
     * 
     * @return LiveData containing boolean permission status
     */
    public LiveData<Boolean> isPermissionGranted() {
        return permissionGranted;
    }

    // ========== PERMISSION MANAGEMENT ==========
    
    /**
     * Checks current location permission status and updates LiveData.
     * 
     * This method examines the current permission state for both fine and coarse
     * location permissions. It updates the permissionGranted LiveData to reflect
     * the current status, which the UI can observe to show appropriate messaging.
     * 
     * The method checks both:
     * - ACCESS_FINE_LOCATION for precise location data
     * - ACCESS_COARSE_LOCATION for approximate location data
     * 
     * Either permission is sufficient for the app's functionality.
     */
    private void checkPermission() {
        boolean granted = ContextCompat.checkSelfPermission(getApplication(), 
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplication(), 
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionGranted.setValue(granted);
    }

    // ========== CACHED DATA MANAGEMENT ==========
    
    /**
     * Loads cached restaurant data from persistent storage.
     * 
     * This method:
     * 1. Retrieves cached data from SharedPreferences
     * 2. Parses JSON data containing restaurant information
     * 3. Reconstructs Restaurant objects with all metadata
     * 4. Calculates distances from last known location
     * 5. Updates cachedRestaurants LiveData for UI display
     * 
     * The method includes robust error handling for:
     * - Missing cached data (first app run)
     * - JSON parsing errors (corrupted cache)
     * - Invalid restaurant data
     * - Missing location coordinates
     * 
     * Successfully loaded restaurants maintain all their original metadata including:
     * - Favorite status
     * - Crowd-sourced notes
     * - Menu scan results and timestamps
     * - Gluten-free menu items
     */
    private void loadCachedRestaurants() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cached = prefs.getString(PREF_KEY_CACHE, null);
        
        // No cached data available (first app run or cache cleared)
        if (cached == null) {
            return;
        }
        
        try {
            // ========== JSON PARSING ==========
            JSONObject root = new JSONObject(cached);
            double lat = root.optDouble("lat", Double.NaN);
            double lng = root.optDouble("lng", Double.NaN);
            JSONArray items = root.optJSONArray("items");
            
            // Validate cache structure
            if (items == null) {
                return;
            }
            
            // ========== RESTAURANT RECONSTRUCTION ==========
            List<Restaurant> restored = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.optJSONObject(i);
                if (obj == null) continue;
                
                // Extract basic restaurant information
                String name = obj.optString("name", "");
                String address = obj.optString("address", "");
                boolean hasGf = obj.optBoolean("hasGf", false);
                double rLat = obj.optDouble("lat", 0);
                double rLng = obj.optDouble("lng", 0);
                String placeId = obj.optString("placeId", null);
                String menuUrl = obj.optString("menuUrl", null);
                String scanStatusString = obj.optString("menuScanStatus", Restaurant.MenuScanStatus.NOT_STARTED.name());
                long scanTimestamp = obj.optLong("menuScanTimestamp", 0L);
                
                // ========== EXTRACT LISTS ==========
                // Parse crowd-sourced notes
                JSONArray notesArray = obj.optJSONArray("notes");
                List<String> notes = new ArrayList<>();
                if (notesArray != null) {
                    for (int j = 0; j < notesArray.length(); j++) {
                        notes.add(notesArray.optString(j, ""));
                    }
                }
                
                // Parse gluten-free menu items
                JSONArray menuArray = obj.optJSONArray("menu");
                List<String> menu = new ArrayList<>();
                if (menuArray != null) {
                    for (int j = 0; j < menuArray.length(); j++) {
                        menu.add(menuArray.optString(j, ""));
                    }
                }
                
                // ========== RESTAURANT CREATION ==========
                // Create Restaurant object with cached data
                Restaurant r = new Restaurant(name, address, hasGf, menu, rLat, rLng, null, null, placeId);
                
                // ========== RESTORE METADATA ==========
                // Restore favorite status
                String fav = obj.optString("favoriteStatus", null);
                if (fav != null && !fav.isEmpty()) {
                    r.setFavoriteStatus(fav);
                }
                
                // Restore crowd-sourced notes
                if (!notes.isEmpty()) {
                    r.setCrowdNotes(notes);
                }
                
                // Restore menu URL
                if (menuUrl != null && !menuUrl.isEmpty()) {
                    r.setMenuUrl(menuUrl);
                }
                
                // Restore menu scan status
                try {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.valueOf(scanStatusString));
                } catch (Exception ignored) {
                    // Invalid scan status, reset to default
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.NOT_STARTED);
                }
                
                // Restore scan timestamp
                r.setMenuScanTimestamp(scanTimestamp);
                
                // ========== DISTANCE CALCULATION ==========
                // Calculate distance from cached user location
                if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                    float[] results = new float[1];
                    Location.distanceBetween(lat, lng, rLat, rLng, results);
                    r.setDistanceMeters(results[0]);
                }
                
                restored.add(r);
            }
            
            // ========== UPDATE LIVEDATA ==========
            cachedRestaurants.setValue(restored);
            
        } catch (JSONException ignored) {
            // Handle corrupted cache data gracefully
            // Log error for debugging but don't crash the app
        }
    }
}
