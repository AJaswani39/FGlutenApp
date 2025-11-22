package com.example.fgluten.data;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
import java.util.ArrayList;

/**
 * Data model representing a restaurant with gluten-free information.
 * 
 * This class stores comprehensive information about restaurants, including basic details
 * (name, address, location), gluten-free specific data (menu items, status), user-generated
 * content (favorites, crowd notes), and caching metadata. Implements Parcelable to allow
 * passing Restaurant objects between Android components (fragments, activities, etc.).
 * 
 * GF = Gluten Free - this abbreviation is used throughout the class for brevity.
 * 
 * @author FGluten Development Team
 */
public class Restaurant implements Parcelable {

    /**
     * Enumeration representing the current status of gluten-free menu scanning for a restaurant.
     * 
     * This status helps track whether the app has attempted to find and analyze gluten-free
     * menu information from the restaurant's website. Different states enable different
     * UI behaviors and user actions.
     */
    public enum MenuScanStatus {
        /** Menu scan has not been initiated yet for this restaurant */
        NOT_STARTED,
        
        /** Menu scan is currently in progress (web scraping/analysis happening) */
        FETCHING,
        
        /** Menu scan completed successfully - either found GF items or confirmed none exist */
        SUCCESS,
        
        /** Restaurant has no website or website could not be found */
        NO_WEBSITE,
        
        /** Menu scan failed due to network error, parsing issues, or other technical problems */
        FAILED
    }

    // ========== CORE RESTAURANT INFORMATION ==========
    /** Restaurant name - required field from Google Places API or user input */
    private final String name;
    
    /** Restaurant address - required field from Google Places API */
    private final String address;
    
    /** 
     * Indicates if restaurant is known to have gluten-free options based on name analysis.
     * Set to true if restaurant name contains "gluten" or "gf" keywords.
     * This is a heuristic-based initial assessment, not definitive proof.
     */
    private final boolean hasGFMenu;
    
    // ========== USER-GENERATED CONTENT ==========
    /** 
     * List of gluten-free menu items discovered through web scraping.
     * Populated by RestaurantViewModel's menu scanning functionality.
     * Each string represents a menu item or description containing GF indicators.
     */
    private final List<String> gfMenu;
    
    /** 
     * Crowd-sourced notes about the restaurant from other users.
     * Used for sharing experiences, warnings, or positive feedback about gluten-free options.
     */
    private final List<String> crowdNotes;

    // ========== LOCATION & PROXIMITY ==========
    /** Restaurant latitude coordinate for mapping and distance calculations */
    private final double latitude;
    
    /** Restaurant longitude coordinate for mapping and distance calculations */
    private final double longitude;
    
    /** 
     * Distance from user's current location in meters.
     * Calculated dynamically using Location.distanceBetween() method.
     * Updated whenever user location changes or restaurant list is refreshed.
     */
    private double distanceMeters;

    // ========== GOOGLE PLACES DATA ==========
    /** Restaurant rating from Google Places (0.0-5.0 scale, null if unavailable) */
    private final Double rating;
    
    /** 
     * Indicates if restaurant is currently open based on Google Places hours data.
     * null if hours data unavailable, true/false for open/closed status.
     */
    private final Boolean openNow;
    
    /** 
     * Unique Google Places identifier for this restaurant.
     * Used for API calls, caching, and consistent identification across app sessions.
     * Critical for reliable menu scanning and favorite/notes association.
     */
    private final String placeId;

    // ========== MENU SCANNING & ANALYSIS ==========
    /** 
     * URL to restaurant's website or specific menu page discovered during scanning.
     * Populated by RestaurantViewModel when menu scanning functionality finds a website.
     * Used for opening restaurant website in external browser.
     */
    private String menuUrl;
    
    /** Current status of gluten-free menu scanning process for this restaurant */
    private MenuScanStatus menuScanStatus;
    
    /** 
     * Timestamp (milliseconds since epoch) when menu scan was last performed.
     * Used for determining if re-scanning is needed (3-day TTL).
     * Enables efficient caching and avoids unnecessary repeated scans.
     */
    private long menuScanTimestamp;

    // ========== USER PREFERENCES & SOCIAL FEATURES ==========
    /** 
     * User's personal rating/preference for this restaurant.
     * Possible values: "safe" (confirmed GF-friendly), "try" (want to test), 
     * "avoid" (had bad experience), or null (no preference set).
     * Stored locally and associated with placeId for consistency.
     */
    private String favoriteStatus; // "safe", "avoid", "try", or null

    /**
     * Simplified constructor for basic restaurant creation with essential location data.
     * 
     * @param name Restaurant name (required)
     * @param address Restaurant address (required)
     * @param hasGFMenu Initial assessment of gluten-free options based on name analysis
     * @param gfMenu List of gluten-free menu items (can be empty)
     * @param latitude Restaurant latitude coordinate
     * @param longitude Restaurant longitude coordinate
     */
    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu, double latitude, double longitude) {
        this(name, address, hasGFMenu, gfMenu, latitude, longitude, null, null, null);
    }

    /**
     * Extended constructor adding rating and hours information from Google Places.
     * 
     * @param name Restaurant name (required)
     * @param address Restaurant address (required)
     * @param hasGFMenu Initial assessment of gluten-free options
     * @param gfMenu List of gluten-free menu items
     * @param latitude Restaurant latitude coordinate
     * @param longitude Restaurant longitude coordinate
     * @param rating Google Places rating (0.0-5.0, null if unavailable)
     * @param openNow Current open status (null if unknown, true/false for hours)
     */
    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu,
                      double latitude, double longitude, Double rating, Boolean openNow) {
        this(name, address, hasGFMenu, gfMenu, latitude, longitude, rating, openNow, null);
    }

    /**
     * Full constructor with complete restaurant information including Google Places ID.
     * 
     * This is the primary constructor used by RestaurantViewModel when creating Restaurant
     * objects from Google Places API responses. The placeId is crucial for:
     * - Menu scanning functionality
     * - Caching and offline support
     * - Favorites and notes association
     * - Reliable restaurant identification across sessions
     * 
     * @param name Restaurant name (required)
     * @param address Restaurant address (required)
     * @param hasGFMenu Initial assessment of gluten-free options
     * @param gfMenu List of gluten-free menu items (copied to prevent external modification)
     * @param latitude Restaurant latitude coordinate
     * @param longitude Restaurant longitude coordinate
     * @param rating Google Places rating (null if unavailable)
     * @param openNow Current open status (null if unknown)
     * @param placeId Google Places unique identifier (required for menu scanning)
     */
    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu,
                      double latitude, double longitude, Double rating, Boolean openNow, String placeId) {
        this.name = name;
        this.address = address;
        this.hasGFMenu = hasGFMenu;
        this.gfMenu = gfMenu != null ? gfMenu : new ArrayList<>();
        this.crowdNotes = new ArrayList<>();
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = 0.0;
        this.rating = rating;
        this.openNow = openNow;
        this.placeId = placeId;
        this.menuUrl = null;
        this.menuScanStatus = MenuScanStatus.NOT_STARTED;
        this.menuScanTimestamp = 0L;
        this.favoriteStatus = null;
    }

    /**
     * Constructor for recreating Restaurant objects from Parcel data.
     * 
     * This is required for the Parcelable implementation to allow Restaurant objects
     * to be passed between Android components (activities, fragments, etc.) without
     * serialization overhead. The parcel data contains all fields in the same order
     * as written by writeToParcel().
     * 
     * @param in Parcel containing serialized Restaurant data
     */
    protected Restaurant(Parcel in) {
        name = in.readString();
        address = in.readString();
        hasGFMenu = in.readByte() != 0;
        gfMenu = in.createStringArrayList();
        crowdNotes = in.createStringArrayList();
        latitude = in.readDouble();
        longitude = in.readDouble();
        distanceMeters = in.readDouble();
        if (in.readByte() == 0) {
            rating = null;
        } else {
            rating = in.readDouble();
        }
        byte openByte = in.readByte();
        if (openByte == 0) {
            openNow = null;
        } else {
            openNow = openByte == 1;
        }
        placeId = in.readString();
        menuUrl = in.readString();
        int statusOrdinal = in.readInt();
        menuScanStatus = statusOrdinal >= 0 && statusOrdinal < MenuScanStatus.values().length
                ? MenuScanStatus.values()[statusOrdinal]
                : MenuScanStatus.NOT_STARTED;
        menuScanTimestamp = in.readLong();
        favoriteStatus = in.readString();
    }


    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public List<String> getGlutenFreeMenu() {
        return gfMenu;
    }

    /**
     * Determines if this restaurant offers gluten-free options.
     * 
     * This is the primary method used throughout the app to filter and display
     * restaurants with gluten-free capabilities. A restaurant is considered to have
     * GF options if either:
     * 1. The initial name analysis suggested GF options (hasGFMenu = true), OR
     * 2. Menu scanning discovered actual GF menu items (gfMenu list not empty)
     * 
     * This dual-criteria approach ensures we don't miss restaurants that may have
     * GF options but weren't initially identified by name analysis.
     * 
     * @return true if restaurant has gluten-free options, false otherwise
     */
    public boolean hasGlutenFreeOptions() {
        return this.hasGFMenu || (gfMenu != null && !gfMenu.isEmpty());
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Double getRating() {
        return rating;
    }

    public Boolean getOpenNow() {
        return openNow;
    }

    public String getPlaceId() {
        return placeId;
    }

    public String getMenuUrl() {
        return menuUrl;
    }

    public void setMenuUrl(String menuUrl) {
        this.menuUrl = menuUrl;
    }

    public MenuScanStatus getMenuScanStatus() {
        return menuScanStatus;
    }

    public void setMenuScanStatus(MenuScanStatus status) {
        this.menuScanStatus = status;
    }

    public void setMenuScanTimestamp(long timestamp) {
        this.menuScanTimestamp = timestamp;
    }

    public long getMenuScanTimestamp() {
        return menuScanTimestamp;
    }

    public void setGlutenFreeMenuItems(List<String> items) {
        gfMenu.clear();
        if (items != null) {
            gfMenu.addAll(items);
        }
    }

    public List<String> getCrowdNotes() {
        return crowdNotes;
    }

    public void setCrowdNotes(List<String> notes) {
        crowdNotes.clear();
        if (notes != null) {
            crowdNotes.addAll(notes);
        }
    }

    /**
     * Adds a crowd-sourced note about this restaurant with validation.
     * 
     * This method allows users to contribute their experiences about the restaurant's
     * gluten-free options. Notes are stored locally and can help other users make
     * informed decisions. Empty or null notes are rejected to maintain data quality.
     * 
     * @param note The note text to add (will be trimmed and validated)
     */
    public void addCrowdNote(String note) {
        if (note != null && !note.trim().isEmpty()) {
            crowdNotes.add(note.trim());
        }
    }

    public String getFavoriteStatus() {
        return favoriteStatus;
    }

    public void setFavoriteStatus(String favoriteStatus) {
        this.favoriteStatus = favoriteStatus;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeByte((byte) (hasGFMenu ? 1 : 0));
        dest.writeStringList(gfMenu);
        dest.writeStringList(crowdNotes);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(distanceMeters);
        if (rating == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(rating);
        }
        if (openNow == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) (openNow ? 1 : 2));
        }
        dest.writeString(placeId);
        dest.writeString(menuUrl);
        dest.writeInt(menuScanStatus != null ? menuScanStatus.ordinal() : MenuScanStatus.NOT_STARTED.ordinal());
        dest.writeLong(menuScanTimestamp);
        dest.writeString(favoriteStatus);
    }

    public static final Creator<Restaurant> CREATOR = new Creator<Restaurant>() {
        @Override
        public Restaurant createFromParcel(Parcel in) {
            return new Restaurant(in);
        }

        @Override
        public Restaurant[] newArray(int size) {
            return new Restaurant[size];
        }
    };

    /**
     * String representation of Restaurant for debugging and logging purposes.
     * 
     * This method provides a comprehensive string representation that includes
     * all key restaurant information. Note that it intentionally excludes some
     * sensitive or verbose data (like crowd notes list) to keep log output manageable.
     * 
     * @return Formatted string containing essential restaurant information
     */
    @Override
    public String toString(){
        return String.format(
                "Restaurant{name=%s, address=%s, hasGFMenu=%s, gfMenu=%s, latitude=%s, longitude=%s, distanceMeters=%s, placeId=%s, menuScanStatus=%s, favoriteStatus=%s}",
                name,
                address,
                hasGFMenu,
                gfMenu,
                latitude,
                longitude,
                distanceMeters,
                placeId,
                menuScanStatus,
                favoriteStatus

        );
    }

}
