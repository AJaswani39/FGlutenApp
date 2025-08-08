package com.example.FGluten.data;

import java.io.Serializable;
import java.util.List;


//GF = Gluten free
public class Restaurant implements Serializable {
    private final String name;
    private final String address;
    private final boolean hasGFMenu;
    private final List<String> gfMenu;

    private final double latitude;
    private final double longitude;

    public Restaurant(String name, String address, boolean hasGFMenu, List<String> gfMenu, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.hasGFMenu = hasGFMenu;
        this.gfMenu = gfMenu;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public boolean hasGFMenu() {
        return this.hasGFMenu;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


    public String determineIfGlutenFree() {
        if (!this.hasGFMenu) {
            return "This restaurant has no gluten free options";
        } else {
            return "This restaurant has gluten free options";
        }
    }


    // toString method
    @Override
    public String toString(){
        return String.format(
                "Restaurant{name=%s, address=%s, hasGFMenu=%s, gfMenu=%s, latitude=%s, longitude=%s}",
                name,
                address,
                hasGFMenu,
                gfMenu,
                latitude,
                longitude

        );
    }

}
