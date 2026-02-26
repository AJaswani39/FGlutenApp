package io.fgluten.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;

/**
 * Unit tests for the Restaurant data model.
 * 
 * This test class validates the core functionality of the Restaurant class,
 * focusing on gluten-free option detection and basic restaurant properties.
 * These tests ensure that the Restaurant model correctly identifies and
 * manages gluten-free dining options for users with celiac disease or
 * gluten sensitivity.
 * 
 * Test Coverage:
 * - Gluten-free option detection based on hasGFMenu flag
 * - Basic restaurant data initialization
 * - Edge cases for restaurant creation
 * 
 * @author FGluten Development Team
 */
public class RestaurantTest {

    // ========== GLUTEN-FREE DETECTION TESTS ==========
    
    /**
     * Test that hasGlutenFreeOptions returns true when hasGFMenu is true.
     * 
     * This test verifies that the Restaurant model correctly identifies
     * restaurants as having gluten-free options when the hasGFMenu flag
     * is explicitly set to true. This is one of the primary indicators
     * the app uses to determine if a restaurant is suitable for gluten-free dining.
     * 
     * Expected behavior: assertTrue(restaurant.hasGlutenFreeOptions())
     */
    @Test
    public void hasGlutenFreeOptions_hasGFMenuTrue_returnsTrue() {
        // Create a restaurant with hasGFMenu set to true
        // This simulates a restaurant that explicitly markets gluten-free options
        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                "123 Test Street",
                true,           // hasGFMenu = true
                Collections.emptyList(),
                0.0,
                0.0
        );
        
        // Verify that hasGlutenFreeOptions returns true
        assertTrue("Restaurant should have gluten-free options when hasGFMenu is true", 
                   restaurant.hasGlutenFreeOptions());
    }
    
    /**
     * Test that hasGlutenFreeOptions returns false when hasGFMenu is false.
     * 
     * This test verifies that the Restaurant model correctly identifies
     * restaurants as NOT having gluten-free options when the hasGFMenu flag
     * is set to false. This helps users avoid restaurants that don't
     * offer gluten-free alternatives.
     * 
     * Expected behavior: assertFalse(restaurant.hasGlutenFreeOptions())
     */
    @Test
    public void hasGlutenFreeOptions_hasGFMenuFalse_returnsFalse() {
        // Create a restaurant with hasGFMenu set to false
        // This simulates a restaurant that doesn't explicitly offer gluten-free options
        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                "123 Test Street",
                false,          // hasGFMenu = false
                Collections.emptyList(),
                0.0,
                0.0
        );
        
        // Verify that hasGlutenFreeOptions returns false
        assertFalse("Restaurant should not have gluten-free options when hasGFMenu is false", 
                    restaurant.hasGlutenFreeOptions());
    }
}
