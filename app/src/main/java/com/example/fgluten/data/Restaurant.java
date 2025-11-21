package com.example.fgluten.data;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
import java.util.ArrayList;


//GF = Gluten free
public class Restaurant implements Parcelable {

    public enum MenuScanStatus {
        NOT_STARTED,
        FETCHING,
        SUCCESS,
        NO_WEBSITE,
        FAILED
    }

    private final String name;
    private final String address;
    private final boolean hasGFMenu;
    private final List<String> gfMenu;

    private final double latitude;
    private final double longitude;
    private double distanceMeters;
    private final Double rating;
    private final Boolean openNow;
    private final String placeId;
    private String menuUrl;
    private MenuScanStatus menuScanStatus;
    private long menuScanTimestamp;

    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu, double latitude, double longitude) {
        this(name, address, hasGFMenu, gfMenu, latitude, longitude, null, null, null);
    }

    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu,
                      double latitude, double longitude, Double rating, Boolean openNow) {
        this(name, address, hasGFMenu, gfMenu, latitude, longitude, rating, openNow, null);
    }

    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu,
                      double latitude, double longitude, Double rating, Boolean openNow, String placeId) {
        this.name = name;
        this.address = address;
        this.hasGFMenu = hasGFMenu;
        this.gfMenu = gfMenu != null ? gfMenu : new ArrayList<>();
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = 0.0;
        this.rating = rating;
        this.openNow = openNow;
        this.placeId = placeId;
        this.menuUrl = null;
        this.menuScanStatus = MenuScanStatus.NOT_STARTED;
        this.menuScanTimestamp = 0L;
    }

    protected Restaurant(Parcel in) {
        name = in.readString();
        address = in.readString();
        hasGFMenu = in.readByte() != 0;
        gfMenu = in.createStringArrayList();
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

    // toString method
    @Override
    public String toString(){
        return String.format(
                "Restaurant{name=%s, address=%s, hasGFMenu=%s, gfMenu=%s, latitude=%s, longitude=%s, distanceMeters=%s, placeId=%s, menuScanStatus=%s}",
                name,
                address,
                hasGFMenu,
                gfMenu,
                latitude,
                longitude,
                distanceMeters,
                placeId,
                menuScanStatus

        );
    }

}
