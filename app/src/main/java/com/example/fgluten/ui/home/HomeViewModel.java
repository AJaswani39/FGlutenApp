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

public class HomeViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "restaurant_cache";
    private static final String PREF_KEY_CACHE = "restaurant_cache";
    private final MutableLiveData<String> mText;
    private final MutableLiveData<List<Restaurant>> cachedRestaurants = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> permissionGranted = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        mText = new MutableLiveData<>();
        mText.setValue(getApplication().getString(R.string.home_fragment_text));
        loadCachedRestaurants();
        checkPermission();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<List<Restaurant>> getCachedRestaurants() {
        return cachedRestaurants;
    }

    public LiveData<Boolean> isPermissionGranted() {
        return permissionGranted;
    }

    private void checkPermission() {
        boolean granted = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        permissionGranted.setValue(granted);
    }

    private void loadCachedRestaurants() {
        SharedPreferences prefs = getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String cached = prefs.getString(PREF_KEY_CACHE, null);
        if (cached == null) {
            return;
        }
        try {
            JSONObject root = new JSONObject(cached);
            double lat = root.optDouble("lat", Double.NaN);
            double lng = root.optDouble("lng", Double.NaN);
            JSONArray items = root.optJSONArray("items");
            if (items == null) {
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
                JSONArray notesArray = obj.optJSONArray("notes");
                List<String> notes = new ArrayList<>();
                if (notesArray != null) {
                    for (int j = 0; j < notesArray.length(); j++) {
                        notes.add(notesArray.optString(j, ""));
                    }
                }
                JSONArray menuArray = obj.optJSONArray("menu");
                List<String> menu = new ArrayList<>();
                if (menuArray != null) {
                    for (int j = 0; j < menuArray.length(); j++) {
                        menu.add(menuArray.optString(j, ""));
                    }
                }
                Restaurant r = new Restaurant(name, address, hasGf, menu, rLat, rLng, null, null, placeId);
                String fav = obj.optString("favoriteStatus", null);
                if (fav != null && !fav.isEmpty()) {
                    r.setFavoriteStatus(fav);
                }
                if (!notes.isEmpty()) {
                    r.setCrowdNotes(notes);
                }
                if (menuUrl != null && !menuUrl.isEmpty()) {
                    r.setMenuUrl(menuUrl);
                }
                try {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.valueOf(scanStatusString));
                } catch (Exception ignored) {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.NOT_STARTED);
                }
                r.setMenuScanTimestamp(scanTimestamp);
                if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
                    float[] results = new float[1];
                    Location.distanceBetween(lat, lng, rLat, rLng, results);
                    r.setDistanceMeters(results[0]);
                }
                restored.add(r);
            }
            cachedRestaurants.setValue(restored);
        } catch (JSONException ignored) {
            // ignore malformed cache
        }
    }
}
