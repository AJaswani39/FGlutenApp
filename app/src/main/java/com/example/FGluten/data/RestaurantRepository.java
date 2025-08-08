package com.example.FGluten.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestaurantRepository {

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<>();
        restaurants.add(new Restaurant("Cafe Good", "123 Main St", true,
                Arrays.asList("GF Burger", "Salad"), 0.0, 0.0));
        restaurants.add(new Restaurant("Pizza Place", "456 Elm St", false,
                new ArrayList<>(), 0.0, 0.0));
        return restaurants;
    }
}
