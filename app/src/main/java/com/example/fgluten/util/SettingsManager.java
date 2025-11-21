package com.example.fgluten.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class SettingsManager {
    private static final String PREFS_NAME = "fg_settings";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_USE_MILES = "use_miles";

    private SettingsManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getThemeMode(Context context) {
        return prefs(context).getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context context, int mode) {
        prefs(context).edit().putInt(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean useMiles(Context context) {
        return prefs(context).getBoolean(KEY_USE_MILES, false);
    }

    public static void setUseMiles(Context context, boolean useMiles) {
        prefs(context).edit().putBoolean(KEY_USE_MILES, useMiles).apply();
    }
}
