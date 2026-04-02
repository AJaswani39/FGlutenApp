package io.fgluten.data.repository;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fgluten.data.Restaurant;

public class RestaurantCacheManager {

    private final SharedPreferences cachePrefs;
    private final SharedPreferences favoritesPrefs;
    
    private static final String PREF_KEY_CACHE = "restaurant_cache";
    private static final String PREF_KEY_FAVORITES = "favorites_map";

    public RestaurantCacheManager(SharedPreferences cachePrefs, SharedPreferences favoritesPrefs) {
        this.cachePrefs = cachePrefs;
        this.favoritesPrefs = favoritesPrefs;
    }

    public static class CachedData {
        public List<Restaurant> restaurants = new ArrayList<>();
        public double lat = Double.NaN;
        public double lng = Double.NaN;
        public long timestamp = 0L;
    }

    public void saveCache(List<Restaurant> restaurants, double lat, double lng) {
        try {
            JSONObject root = new JSONObject();
            root.put("lat", lat);
            root.put("lng", lng);
            root.put("timestamp", System.currentTimeMillis());
            JSONArray items = new JSONArray();
            for (Restaurant r : restaurants) {
                JSONObject obj = new JSONObject();
                obj.put("name", r.getName() != null ? r.getName() : "");
                obj.put("address", r.getAddress() != null ? r.getAddress() : "");
                obj.put("hasGf", r.hasGlutenFreeOptions());
                obj.put("lat", r.getLatitude());
                obj.put("lng", r.getLongitude());
                if (r.getRating() != null) obj.put("rating", r.getRating());
                if (r.getOpenNow() != null) obj.put("openNow", r.getOpenNow());
                
                JSONArray menu = new JSONArray();
                if (r.getGlutenFreeMenu() != null) {
                    for (String m : r.getGlutenFreeMenu()) menu.put(m);
                }
                if (r.getPlaceId() != null) obj.put("placeId", r.getPlaceId());
                if (r.getMenuUrl() != null) obj.put("menuUrl", r.getMenuUrl());
                
                if (r.getRawMenuText() != null) {
                    String raw = r.getRawMenuText();
                    if (raw.length() > 30000) raw = raw.substring(0, 30000);
                    obj.put("rawMenuText", raw);
                }
                if (r.getMenuScanStatus() != null) {
                    obj.put("menuScanStatus", r.getMenuScanStatus().name());
                }
                obj.put("menuScanTimestamp", r.getMenuScanTimestamp());
                if (r.getFavoriteStatus() != null) {
                    obj.put("favoriteStatus", r.getFavoriteStatus());
                }
                obj.put("menu", menu);
                items.put(obj);
            }
            root.put("items", items);
            cachePrefs.edit().putString(PREF_KEY_CACHE, root.toString()).apply();
        } catch (JSONException ignored) { }
    }

    public CachedData loadCached() {
        String cached = cachePrefs.getString(PREF_KEY_CACHE, null);
        if (cached == null) return null;
        CachedData data = new CachedData();
        try {
            JSONObject root = new JSONObject(cached);
            data.lat = root.optDouble("lat", Double.NaN);
            data.lng = root.optDouble("lng", Double.NaN);
            data.timestamp = root.optLong("timestamp", 0L);
            JSONArray items = root.optJSONArray("items");
            if (items == null || Double.isNaN(data.lat) || Double.isNaN(data.lng)) {
                return null;
            }
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
                    for (int j = 0; j < menuArray.length(); j++) menu.add(menuArray.optString(j, ""));
                }
                
                Restaurant r = new Restaurant(name, address, hasGf, menu, rLat, rLng, rating, openNow, placeId);
                if (!TextUtils.isEmpty(menuUrl)) r.setMenuUrl(menuUrl);
                String rawMenuText = obj.optString("rawMenuText", null);
                if (!TextUtils.isEmpty(rawMenuText)) r.setRawMenuText(rawMenuText);
                try {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.valueOf(scanStatusString));
                } catch (Exception ignored) {
                    r.setMenuScanStatus(Restaurant.MenuScanStatus.NOT_STARTED);
                }
                r.setMenuScanTimestamp(scanTimestamp);
                if (obj.has("favoriteStatus")) r.setFavoriteStatus(obj.optString("favoriteStatus", null));
                data.restaurants.add(r);
            }
            if (data.restaurants.isEmpty()) return null;
            return data;
        } catch (JSONException e) {
            return null;
        }
    }

    public Map<String, String> loadFavorites() {
        Map<String, String> map = new HashMap<>();
        String raw = favoritesPrefs.getString(PREF_KEY_FAVORITES, null);
        if (TextUtils.isEmpty(raw)) return map;
        try {
            JSONObject obj = new JSONObject(raw);
            JSONArray names = obj.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String key = names.optString(i, null);
                    if (key == null) continue;
                    String status = obj.optString(key, null);
                    if (!TextUtils.isEmpty(status)) map.put(key, status);
                }
            }
        } catch (JSONException ignored) { }
        return map;
    }

    public void saveFavorites(Map<String, String> favoriteMap) {
        try {
            JSONObject obj = new JSONObject();
            for (Map.Entry<String, String> entry : favoriteMap.entrySet()) {
                obj.put(entry.getKey(), entry.getValue());
            }
            favoritesPrefs.edit().putString(PREF_KEY_FAVORITES, obj.toString()).apply();
        } catch (JSONException ignored) { }
    }
}
