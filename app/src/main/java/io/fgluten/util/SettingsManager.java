package io.fgluten.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;

import io.fgluten.R;
import io.fgluten.data.Restaurant;

/**
 * Centralized settings management for the FGluten Android application.
 * 
 * This utility class handles all application-level preferences including:
 * - Theme preferences (light, dark, or system-default)
 * - Distance unit preferences (kilometers vs miles)
 * 
 * Uses Android SharedPreferences for persistent storage across app sessions.
 * Provides a clean API for other components to access and modify settings without
 * directly interacting with SharedPreferences.
 * 
 * Design follows the Singleton pattern with a private constructor and static methods,
 * ensuring consistent settings access throughout the application lifecycle.
 * 
 * @author FGluten Development Team
 */
public class SettingsManager {
    // ========== SHARED PREFERENCES CONFIGURATION ==========
    /** SharedPreferences file name for storing FGluten app settings */
    private static final String PREFS_NAME = "fg_settings";
    
    /** Preference key for theme mode setting */
    private static final String KEY_THEME_MODE = "theme_mode";
    
    /** Preference key for distance unit preference (miles vs kilometers) */
    private static final String KEY_USE_MILES = "use_miles";
    
    /** Preference key for dietary profile (strict celiac vs preference) */
    private static final String KEY_STRICT_CELIAC = "strict_celiac";


    /**
     * Private constructor to enforce static usage and prevent instantiation.
     * This is a utility class that should not be instantiated.
     */
    private SettingsManager() {}

    /**
     * Internal helper method to obtain SharedPreferences instance.
     * 
     * This method provides a centralized way to access the app's preference storage.
     * Uses Context.MODE_PRIVATE to ensure preferences are only accessible to this app.
     * 
     * @param context Application context for accessing SharedPreferences
     * @return SharedPreferences instance for the FGluten app
     */
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ========== THEME MANAGEMENT ==========
    
    /**
     * Retrieves the current theme mode preference.
     * 
     * Returns the user's selected theme mode, which can be:
     * - AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM (default)
     * - AppCompatDelegate.MODE_NIGHT_NO (light theme)
     * - AppCompatDelegate.MODE_NIGHT_YES (dark theme)
     * 
     * @param context Application context for accessing SharedPreferences
     * @return Theme mode integer constant from AppCompatDelegate
     */
    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Sets the theme mode and applies it immediately to the application.
     * 
     * When the theme mode is changed, this method:
     * 1. Persists the new theme preference to SharedPreferences
     * 2. Immediately applies the theme by calling AppCompatDelegate.setDefaultNightMode()
     * 3. The activity will recreate to apply the new theme
     * 
     * @param context Application context
     * @param mode Theme mode constant from AppCompatDelegate (e.g., MODE_NIGHT_FOLLOW_SYSTEM)
     */
    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // ========== DISTANCE UNIT PREFERENCES ==========
    
    /**
     * Determines if the user prefers distance measurements in miles.
     * 
     * This setting affects how distances are displayed throughout the app:
     * - true: Distances shown in miles/feet
     * - false: Distances shown in kilometers/meters (default)
     * 
     * @param context Application context for accessing SharedPreferences
     * @return true if user prefers miles, false for kilometers
     */
    public static boolean useMiles(Context context) {
        return prefs(context).getBoolean(KEY_USE_MILES, false);
    }

    /**
     * Sets the user's distance unit preference.
     * 
     * This preference affects all distance displays in the app including:
     * - Restaurant list distances
     * - Map marker information
     * - Filter distance limits
     * 
     * @param context Application context
     * @param useMiles true for miles, false for kilometers
     */
    public static void setUseMiles(Context context, boolean useMiles) {
        prefs(context).edit().putBoolean(KEY_USE_MILES, useMiles).apply();
    }

    // ========== DIETARY PROFILE ==========
    
    /**
     * Determines if the user has selected a Strict Celiac dietary profile.
     * 
     * @param context Application context
     * @return true if user is strict celiac, false if general preference
     */
    public static boolean isStrictCeliac(Context context) {
        return prefs(context).getBoolean(KEY_STRICT_CELIAC, false);
    }

    /**
     * Sets the user's dietary profile.
     * 
     * @param context Application context
     * @param strict true for Strict Celiac, false for Preference
     */
    public static void setStrictCeliac(Context context, boolean strict) {
        prefs(context).edit().putBoolean(KEY_STRICT_CELIAC, strict).apply();
    }

    public static String formatDistance(Context context, double meters) {
        if (meters <= 0) {
            return null;
        }
        boolean useMiles = useMiles(context);
        if (useMiles) {
            double miles = meters / 1609.34;
            if (miles >= 0.1) {
                return context.getString(R.string.distance_miles_away, miles);
            } else {
                int feet = (int) Math.round(meters * 3.28084);
                return context.getString(R.string.distance_feet_away, feet);
            }
        } else {
            if (meters >= 1000) {
                double km = meters / 1000.0;
                return context.getString(R.string.distance_km_away, km);
            }
            int roundedMeters = (int) Math.round(meters);
            return context.getString(R.string.distance_m_away, roundedMeters);
        }
    }

    public static void openInMaps(Context context, Restaurant restaurant) {
        if (restaurant == null) return;
        try {
            Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude() +
                    "?q=" + Uri.encode(restaurant.getName()));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setPackage("com.google.android.apps.maps");
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Uri uri = Uri.parse("geo:" + restaurant.getLatitude() + "," + restaurant.getLongitude());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                context.startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

}
