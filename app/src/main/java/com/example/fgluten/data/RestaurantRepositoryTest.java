package com.example.fgluten.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class RestaurantRepositoryTest {
    @Test 
    public void getRestaurants_returnsExpectedList() {
        RestaurantRepository repository = new RestaurantRepository();
        List<Restaurant> restaurants = repository.getRestaurants();
        assertEquals(2, restaurants.size());
        assertTrue(restaurants.contains(new Restaurant("Restaurant 1", "Description 1")));
        
        Restaurant first = restaurants.get(0);
        assertEquals("Cafe Good", first.getName());
        assertTrue(first.hasGlutenFreeOptions());

        Restaurant second = restaurants.get(1);
        assertEquals("Pizza Place", second.getName());
        assertFalse(second.hasGlutenFreeOptions());

    }
}