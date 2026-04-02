package io.fgluten.data.repository;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.fgluten.data.Restaurant;

public class PlacesRepository {
    private static final String TAG = "PlacesRepository";
    private static final int NEARBY_RADIUS_METERS = 50_000;

    public List<Restaurant> fetchNearbyRestaurants(double lat, double lng, String apiKey) throws Exception {
        List<Restaurant> results = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            String urlStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                    + "?location=" + lat + "," + lng
                    + "&radius=" + NEARBY_RADIUS_METERS
                    + "&type=restaurant"
                    + "&keyword=gluten%20free"
                    + "&key=" + apiKey;
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
            return results;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
