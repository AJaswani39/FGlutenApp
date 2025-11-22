package com.example.fgluten.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

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

    /** Preference key for contributor nickname (crowd note attribution) */
    private static final String KEY_CONTRIBUTOR_NAME = "contributor_name";

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

    // ========== CONTRIBUTOR ATTRIBUTION ==========

    /**
     * Retrieves the contributor nickname that will be appended to crowd notes.
     *
     * @param context Application context
     * @return Nickname string or empty string if not set
     */
    public static String getContributorName(Context context) {
        return prefs(context).getString(KEY_CONTRIBUTOR_NAME, "");
    }

    /**
     * Stores the contributor nickname used for crowd note attribution.
     *
     * @param context Application context
     * @param name Nickname to store (null clears the value)
     */
    public static void setContributorName(Context context, String name) {
        prefs(context).edit().putString(KEY_CONTRIBUTOR_NAME, name != null ? name : "").apply();
    }
}
