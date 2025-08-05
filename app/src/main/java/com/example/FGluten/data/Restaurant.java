package com.example.FGluten.data;

import java.util.List;
import java.util.Objects;

//GF = Gluten free
public class Restaurant {
    private final String name;
    private final String address;
    private final boolean hasGFMenu;
    private final List<String> gfMenu;

    private final double latitude;
    private final double longitude;


    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public List<String> getGlutenFreeMenu() {
        return gfMenu;
    }

    public boolean gethasGFMenu() {
        return hasGFMenu;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public String determineIfGlutenFree(Object gf) {
        if (!this.hasGFMenu) {
            return "This restaurant has no gluten free options";
        } else {
            return "This restaurant has gluten free options";
        }
    }


    // toString method
    @Override
    public String toString(){
        return "Restaurant{" +
                "name=" + name
                + " address=" + address
                + " hasGlutenFreeOption=" + hasGFMenu
                + " Gluten Free Menu=" + gfMenu
                + " latitude=" + latitude
                + " longitude=" + longitude
                + "}";
    }

}
