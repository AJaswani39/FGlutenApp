package com.example.FGluten.data;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.assertEquals;


public class RestaurantTest {
    @Test
    public void determineIfGlutenFree_hasGFMenuTrue_returnsPositiveMessage() {
        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                "123 Test Street",
                true,
                Collections.emptyList(),
                0.0,
                0.0
        );

        String expected = "This restaurant has gluten free options";
        assertEquals(expected, restaurant.determineIfGlutenFree());

    }
    @Test
    public void determineIfGlutenFree_noGFMenuTrue_returnsNegativeMessage() {
        Restaurant restaurant = new Restaurant(
                "Test Restaurant",
                "123 Test Street",
                false,
                Collections.emptyList(),
                0.0,
                0.0
        );
        String expected = "This restaurant does not have gluten free options";
        assertEquals(expected, restaurant.determineIfGlutenFree());
    }
}
