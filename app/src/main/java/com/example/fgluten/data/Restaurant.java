package com.example.fgluten.data;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;


//GF = Gluten free
public class Restaurant implements Parcelable {
    private final String name;
    private final String address;
    private final boolean hasGFMenu;
    private final List<String> gfMenu;

    private final double latitude;
    private final double longitude;
    private double distanceMeters;
    private final Double rating;
    private final Boolean openNow;

    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu, double latitude, double longitude) {
        this(name, address, hasGFMenu, gfMenu, latitude, longitude, null, null);
    }

    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu,
                      double latitude, double longitude, Double rating, Boolean openNow) {
        this.name = name;
        this.address = address;
        this.hasGFMenu = hasGFMenu;
        this.gfMenu = gfMenu;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = 0.0;
        this.rating = rating;
        this.openNow = openNow;
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
        return this.hasGFMenu;
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
                "Restaurant{name=%s, address=%s, hasGFMenu=%s, gfMenu=%s, latitude=%s, longitude=%s, distanceMeters=%s}",
                name,
                address,
                hasGFMenu,
                gfMenu,
                latitude,
                longitude,
                distanceMeters

        );
    }

}
