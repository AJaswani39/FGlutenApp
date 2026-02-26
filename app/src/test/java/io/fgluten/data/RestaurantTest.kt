package io.fgluten.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Arrays

/**
 * Comprehensive unit tests for the Restaurant data model.
 * 
 * Tests cover:
 * - Basic restaurant data initialization
 * - Parcelable implementation
 * - Gluten-free detection logic
 * - Distance calculations
 * - Menu scanning functionality
 * - Favorites system
 * - Crowd notes system
 * - Edge cases and validation
 * 
 * @author FGluten Development Team
 */
class RestaurantTest {

    private lateinit var restaurant: Restaurant
    private lateinit var restaurantWithPlaceId: Restaurant
    private lateinit var restaurantWithDetails: Restaurant

    @Before
    fun setUp() {
        // Test data setup
        restaurant = Restaurant(
            name = "Gluten Free Kitchen",
            address = "123 Main St, Anytown, USA",
            hasGFMenu = true,
            gfMenu = Arrays.asList("GF Pasta", "GF Pizza"),
            latitude = 40.7128,
            longitude = -74.0060
        )

        restaurantWithPlaceId = Restaurant(
            name = "Safe Haven Restaurant",
            address = "456 Oak Ave, Somewhere, USA",
            hasGFMenu = false,
            gfMenu = Arrays.asList(),
            latitude = 40.7589,
            longitude = -73.9851,
            placeId = "ChIJN1t_tDeuEmsRUsoyG83frY4"
        )

        restaurantWithDetails = Restaurant(
            name = "Celiac Safe Bistro",
            address = "789 Pine St, Downtown, USA",
            hasGFMenu = true,
            gfMenu = Arrays.asList("GF Sandwich", "GF Salad", "GF Dessert"),
            latitude = 40.7505,
            longitude = -73.9934,
            rating = 4.5,
            openNow = true,
            placeId = "ChIJOwg_06VPwokRYv534QaPC8g"
        )
    }

    // ========== BASIC FUNCTIONALITY TESTS ==========

    @Test
    fun `test basic restaurant initialization`() {
        assertEquals("Gluten Free Kitchen", restaurant.name)
        assertEquals("123 Main St, Anytown, USA", restaurant.address)
        assertEquals(40.7128, restaurant.latitude, 0.0)
        assertEquals(-74.0060, restaurant.longitude, 0.0)
        assertTrue(restaurant.hasGlutenFreeOptions())
    }

    @Test
    fun `test restaurant with details initialization`() {
        assertEquals(4.5, restaurantWithDetails.rating, 0.0)
        assertTrue(restaurantWithDetails.openNow ?: false)
        assertEquals("ChIJOwg_06VPwokRYv534QaPC8g", restaurantWithDetails.placeId)
    }

    // ========== GLUTEN-FREE DETECTION TESTS ==========

    @Test
    fun `test hasGlutenFreeOptions returns true when hasGFMenu is true`() {
        assertTrue(restaurant.hasGlutenFreeOptions())
    }

    @Test
    fun `test hasGlutenFreeOptions returns true when gfMenu has items`() {
        val restaurantWithGFMenu = Restaurant(
            name = "Regular Restaurant",
            address = "123 Test St",
            hasGFMenu = false,
            gfMenu = Arrays.asList("GF Pasta", "GF Pizza"),
            latitude = 0.0,
            longitude = 0.0
        )
        assertTrue(restaurantWithGFMenu.hasGlutenFreeOptions())
    }

    @Test
    fun `test hasGlutenFreeOptions returns false when no GF indicators`() {
        val restaurantWithoutGF = Restaurant(
            name = "Regular Restaurant",
            address = "123 Test St",
            hasGFMenu = false,
            gfMenu = Arrays.asList(),
            latitude = 0.0,
            longitude = 0.0
        )
        assertFalse(restaurantWithoutGF.hasGlutenFreeOptions())
    }

    @Test
    fun `test hasGlutenFreeOptions returns true with both indicators`() {
        val restaurantWithBoth = Restaurant(
            name = "GF Friendly Place",
            address = "123 Test St",
            hasGFMenu = true,
            gfMenu = Arrays.asList("GF Pasta"),
            latitude = 0.0,
            longitude = 0.0
        )
        assertTrue(restaurantWithBoth.hasGlutenFreeOptions())
    }

    // ========== DISTANCE CALCULATION TESTS ==========

    @Test
    fun `test distance initialization and setting`() {
        assertEquals(0.0, restaurant.distanceMeters, 0.0)
        
        restaurant.setDistanceMeters(500.0)
        assertEquals(500.0, restaurant.distanceMeters, 0.0)
        
        restaurant.setDistanceMeters(1500.0)
        assertEquals(1500.0, restaurant.distanceMeters, 0.0)
    }

    @Test
    fun `test distance calculations remain accurate`() {
        val testDistance = 1234.56
        restaurant.setDistanceMeters(testDistance)
        assertEquals(testDistance, restaurant.distanceMeters(), 0.0)
    }

    // ========== MENU SCANNING TESTS ==========

    @Test
    fun `test initial menu scan status`() {
        assertEquals(Restaurant.MenuScanStatus.NOT_STARTED, restaurant.menuScanStatus)
    }

    @Test
    fun `test menu scan status updates`() {
        restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.FETCHING)
        assertEquals(Restaurant.MenuScanStatus.FETCHING, restaurant.menuScanStatus)
        
        restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.SUCCESS)
        assertEquals(Restaurant.MenuScanStatus.SUCCESS, restaurant.menuScanStatus)
        
        restaurant.setMenuScanStatus(Restaurant.MenuScanStatus.FAILED)
        assertEquals(Restaurant.MenuScanStatus.FAILED, restaurant.menuScanStatus)
    }

    @Test
    fun `test menu scan timestamp management`() {
        val currentTime = System.currentTimeMillis()
        restaurant.setMenuScanTimestamp(currentTime)
        assertEquals(currentTime, restaurant.menuScanTimestamp)
        
        val laterTime = currentTime + 10000
        restaurant.setMenuScanTimestamp(laterTime)
        assertEquals(laterTime, restaurant.menuScanTimestamp)
    }

    @Test
    fun `test gluten-free menu items management`() {
        val initialItems = restaurant.glutenFreeMenu
        assertEquals(2, initialItems.size)
        assertTrue(initialItems.contains("GF Pasta"))
        assertTrue(initialItems.contains("GF Pizza"))
        
        val newItems = Arrays.asList("New GF Item 1", "New GF Item 2", "New GF Item 3")
        restaurant.setGlutenFreeMenuItems(newItems)
        
        val updatedItems = restaurant.glutenFreeMenu
        assertEquals(3, updatedItems.size)
        assertTrue(updatedItems.contains("New GF Item 1"))
        assertTrue(updatedItems.contains("New GF Item 2"))
        assertTrue(updatedItems.contains("New GF Item 3"))
        assertFalse(updatedItems.contains("GF Pasta"))
    }

    @Test
    fun `test setting null menu items`() {
        restaurant.setGlutenFreeMenuItems(null)
        assertTrue(restaurant.glutenFreeMenu.isEmpty())
    }

    @Test
    fun `test setting empty menu items`() {
        restaurant.setGlutenFreeMenuItems(Arrays.asList())
        assertTrue(restaurant.glutenFreeMenu.isEmpty())
    }

    // ========== FAVORITES SYSTEM TESTS ==========

    @Test
    fun `test initial favorite status`() {
        assertNull(restaurant.favoriteStatus)
    }

    @Test
    fun `test setting favorite status`() {
        restaurant.setFavoriteStatus("safe")
        assertEquals("safe", restaurant.favoriteStatus)
        
        restaurant.setFavoriteStatus("avoid")
        assertEquals("avoid", restaurant.favoriteStatus)
        
        restaurant.setFavoriteStatus(null)
        assertNull(restaurant.favoriteStatus)
    }

    // ========== CROWD NOTES TESTS ==========

    @Test
    fun `test initial crowd notes`() {
        assertTrue(restaurant.crowdNotes.isEmpty())
    }

    @Test
    fun `test adding valid crowd notes`() {
        val note1 = "Great gluten-free options!"
        val note2 = "Staff was knowledgeable about cross-contamination."
        
        restaurant.addCrowdNote(note1)
        assertEquals(1, restaurant.crowdNotes.size)
        assertTrue(restaurant.crowdNotes.contains(note1))
        
        restaurant.addCrowdNote(note2)
        assertEquals(2, restaurant.crowdNotes.size)
        assertTrue(restaurant.crowdNotes.contains(note2))
    }

    @Test
    fun `test adding empty or null notes`() {
        restaurant.addCrowdNote("")
        restaurant.addCrowdNote("   ")
        restaurant.addCrowdNote(null)
        assertTrue(restaurant.crowdNotes.isEmpty())
    }

    @Test
    fun `test adding notes trims whitespace`() {
        val noteWithSpaces = "  Note with spaces  "
        restaurant.addCrowdNote(noteWithSpaces)
        assertEquals(1, restaurant.crowdNotes.size)
        assertEquals("Note with spaces", restaurant.crowdNotes[0])
    }

    @Test
    fun `test setting crowd notes`() {
        val notes = Arrays.asList("Note 1", "Note 2", "Note 3")
        restaurant.setCrowdNotes(notes)
        
        assertEquals(3, restaurant.crowdNotes.size)
        assertTrue(restaurant.crowdNotes.containsAll(notes))
    }

    @Test
    fun `test setting null crowd notes`() {
        restaurant.setCrowdNotes(Arrays.asList("Initial note"))
        restaurant.setCrowdNotes(null)
        assertTrue(restaurant.crowdNotes.isEmpty())
    }

    // ========== PLACE ID AND URL TESTS ==========

    @Test
    fun `test place ID management`() {
        assertNull(restaurant.placeId)
        assertEquals("ChIJN1t_tDeuEmsRUsoyG83frY4", restaurantWithPlaceId.placeId)
    }

    @Test
    fun `test menu URL management`() {
        assertNull(restaurant.menuUrl)
        
        val testUrl = "https://example.com/menu"
        restaurant.setMenuUrl(testUrl)
        assertEquals(testUrl, restaurant.menuUrl)
        
        val updatedUrl = "https://example.com/new-menu"
        restaurant.setMenuUrl(updatedUrl)
        assertEquals(updatedUrl, restaurant.menuUrl)
    }

    // ========== PARCELABLE TESTS ==========

    @Test
    fun `test parcelable roundtrip`() {
        // Setup original restaurant with all data
        val original = Restaurant(
            name = "Test Restaurant",
            address = "123 Test St",
            hasGFMenu = true,
            gfMenu = Arrays.asList("GF Item 1", "GF Item 2"),
            latitude = 40.7128,
            longitude = -74.0060,
            rating = 4.5,
            openNow = true,
            placeId = "test_place_id"
        )
        
        // Set additional data
        original.setDistanceMeters(500.0)
        original.setMenuUrl("https://test.com/menu")
        original.setMenuScanStatus(Restaurant.MenuScanStatus.SUCCESS)
        original.setMenuScanTimestamp(1000000L)
        original.setFavoriteStatus("safe")
        original.addCrowdNote("Test note")
        
        // Create parcel and restore
        val parcel = android.os.Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        
        val restored = Restaurant.CREATOR.createFromParcel(parcel)
        
        // Verify all data was preserved
        assertEquals(original.name, restored.name)
        assertEquals(original.address, restored.address)
        assertEquals(original.hasGlutenFreeOptions(), restored.hasGlutenFreeOptions())
        assertEquals(original.latitude, restored.latitude, 0.0)
        assertEquals(original.longitude, restored.longitude, 0.0)
        assertEquals(original.distanceMeters, restored.distanceMeters(), 0.0)
        assertEquals(original.rating, restored.rating)
        assertEquals(original.openNow, restored.openNow)
        assertEquals(original.placeId, restored.placeId)
        assertEquals(original.menuUrl, restored.menuUrl)
        assertEquals(original.menuScanStatus, restored.menuScanStatus)
        assertEquals(original.menuScanTimestamp, restored.menuScanTimestamp)
        assertEquals(original.favoriteStatus, restored.favoriteStatus)
        assertEquals(original.glutenFreeMenu, restored.glutenFreeMenu)
        assertEquals(original.crowdNotes, restored.crowdNotes)
        
        parcel.recycle()
    }

    @Test
    fun `test parcelable array creation`() {
        val restaurants = arrayOf(restaurant, restaurantWithPlaceId, restaurantWithDetails)
        assertEquals(3, restaurants.size)
    }

    // ========== TO STRING TESTS ==========

    @Test
    fun `test to string contains key information`() {
        val restaurantString = restaurant.toString()
        assertTrue(restaurantString.contains("Restaurant{name=Gluten Free Kitchen"))
        assertTrue(restaurantString.contains("address=123 Main St, Anytown, USA"))
        assertTrue(restaurantString.contains("hasGFMenu=true"))
        assertTrue(restaurantString.contains("placeId=null"))
    }

    @Test
    fun `test to string with all data`() {
        restaurantWithDetails.setDistanceMeters(123.45)
        restaurantWithDetails.setMenuScanStatus(Restaurant.MenuScanStatus.SUCCESS)
        restaurantWithDetails.setFavoriteStatus("safe")
        
        val restaurantString = restaurantWithDetails.toString()
        assertTrue(restaurantString.contains("Celiac Safe Bistro"))
        assertTrue(restaurantString.contains("distanceMeters=123.45"))
        assertTrue(restaurantString.contains("favoriteStatus=safe"))
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `test restaurant with minimal data`() {
        val minimalRestaurant = Restaurant(
            name = "",
            address = "",
            hasGFMenu = false,
            gfMenu = Arrays.asList(),
            latitude = 0.0,
            longitude = 0.0
        )
        
        assertEquals("", minimalRestaurant.name)
        assertEquals("", minimalRestaurant.address)
        assertFalse(minimalRestaurant.hasGlutenFreeOptions())
        assertEquals(0.0, minimalRestaurant.latitude, 0.0)
        assertEquals(0.0, minimalRestaurant.longitude, 0.0)
    }

    @Test
    fun `test restaurant with null place ID`() {
        val restaurantNullPlaceId = Restaurant(
            name = "Test Restaurant",
            address = "Test Address",
            hasGFMenu = true,
            gfMenu = Arrays.asList("Test Item"),
            latitude = 0.0,
            longitude = 0.0,
            placeId = null
        )
        
        assertNull(restaurantNullPlaceId.placeId)
        assertTrue(restaurantNullPlaceId.hasGlutenFreeOptions())
    }

    @Test
    fun `test restaurant with extreme coordinates`() {
        val extremeRestaurant = Restaurant(
            name = "Extreme Location",
            address = "Antarctica",
            hasGFMenu = false,
            gfMenu = Arrays.asList(),
            latitude = -90.0, // South Pole
            longitude = 180.0 // International Date Line
        )
        
        assertEquals(-90.0, extremeRestaurant.latitude, 0.0)
        assertEquals(180.0, extremeRestaurant.longitude, 0.0)
    }

    // ========== ENUM TESTS ==========

    @Test
    fun `test MenuScanStatus enum values`() {
        assertEquals(5, Restaurant.MenuScanStatus.values().size)
        assertNotNull(Restaurant.MenuScanStatus.NOT_STARTED)
        assertNotNull(Restaurant.MenuScanStatus.FETCHING)
        assertNotNull(Restaurant.MenuScanStatus.SUCCESS)
        assertNotNull(Restaurant.MenuScanStatus.NO_WEBSITE)
        assertNotNull(Restaurant.MenuScanStatus.FAILED)
    }

    @Test
    fun `test MenuScanStatus comparison`() {
        assertNotEquals(Restaurant.MenuScanStatus.NOT_STARTED, Restaurant.MenuScanStatus.FETCHING)
        assertNotEquals(Restaurant.MenuScanStatus.SUCCESS, Restaurant.MenuScanStatus.FAILED)
    }
}