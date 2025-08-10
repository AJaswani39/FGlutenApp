package com.example.fgluten.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;


public class RestaurantTest {
    @Test
    public void hasGlutenFreeOptions_hasGFMenuTrue_returnsTrue() {
        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                "123 Test Street",
                true,
                Collections.emptyList(),
                0.0,
                0.0
        );
        assertTrue(restaurant.hasGlutenFreeOptions());
    }
    @Test
    public void hasGlutenFreeOptions_hasGFMenuFalse_returnsFalse() {
        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                "123 Test Street",
                false,
                Collections.emptyList(),
                0.0,
                0.0
        );
        assertFalse(restaurant.hasGlutenFreeOptions());
    }
}
