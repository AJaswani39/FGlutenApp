package com.example.fgluten.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestaurantRepository {

    public List<Restaurant> getRestaurants() {
        List<Restaurant> restaurants = new ArrayList<>();
        restaurants.add(new Restaurant("Cafe Good", "123 Main St, New York, NY", true,
                Arrays.asList("GF Burger", "Kale Caesar"), 40.741895, -73.989308));
        restaurants.add(new Restaurant("Pizza Place", "456 Elm St, Brooklyn, NY", false,
                new ArrayList<>(), 40.730610, -73.935242));
        restaurants.add(new Restaurant("Green Bowl", "789 Park Ave, New York, NY", true,
                Arrays.asList("Quinoa Bowl", "GF Avocado Toast"), 40.752726, -73.977229));
        restaurants.add(new Restaurant("Harbor Cafe", "10 Wharf Rd, Jersey City, NJ", true,
                Arrays.asList("GF Fish Tacos", "Grilled Veggies"), 40.728157, -74.035278));
        return restaurants;
    }
}
