package com.example.fgluten.ui.home

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.fgluten.data.Restaurant
import com.example.fgluten.util.SettingsManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

/**
 * Unit tests for HomeViewModel.
 * 
 * Tests cover:
 * - Cached restaurant display
 * - Location permission observation
 * - Text content management
 * - Settings integration
 * - ViewModel lifecycle
 * 
 * @author FGluten Development Team
 */
@RunWith(MockitoJUnitRunner::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    private lateinit var homeViewModel: HomeViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock Context and SharedPreferences
        `when`(mockContext.getSharedPreferences("restaurant_cache", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("restaurant_favorites", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("restaurant_notes", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockContext.getSharedPreferences("fg_settings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
    }

    // ========== TEXT CONTENT TESTS ==========

    @Test
    fun `test text LiveData initialization`() {
        homeViewModel = HomeViewModel()
        val textLiveData = homeViewModel.text
        
        assertNotNull("Text LiveData should not be null", textLiveData)
    }

    @Test
    fun `test text LiveData provides expected content`() {
        homeViewModel = HomeViewModel()
        val textObserver = mock<Observer<String>>()
        val textLiveData = homeViewModel.text
        
        textLiveData.observeForever(textObserver)
        
        verify(textObserver).onChanged("This is home fragment")
    }

    // ========== CACHED RESTAURANTS TESTS ==========

    @Test
    fun `test cachedRestaurants LiveData initialization`() {
        homeViewModel = HomeViewModel()
        val cachedRestaurantsLiveData = homeViewModel.cachedRestaurants
        
        assertNotNull("Cached restaurants LiveData should not be null", cachedRestaurantsLiveData)
    }

    @Test
    fun `test cachedRestaurants returns empty list when no cache`() {
        `when`(mockSharedPreferences.getString("restaurant_cache", null))
            .thenReturn(null)
        
        homeViewModel = HomeViewModel()
        val cachedRestaurantsLiveData = homeViewModel.cachedRestaurants
        
        val observer = mock<Observer<List<Restaurant>>>()
        cachedRestaurantsLiveData.observeForever(observer)
        
        verify(observer).onChanged(eq(emptyList()))
    }

    @Test
    fun `test cachedRestaurants with valid restaurant data`() {
        val restaurantJson = """
        {
            "lat": 40.7128,
            "lng": -74.0060,
            "timestamp": ${System.currentTimeMillis()},
            "items": [
                {
                    "name": "Test Restaurant",
                    "address": "123 Test St",
                    "hasGf": true,
                    "lat": 40.7128,
                    "lng": -74.0060,
                    "rating": 4.5,
                    "openNow": true,
                    "placeId": "test_place_id",
                    "menu": ["GF Pasta", "GF Pizza"],
                    "notes": [],
                    "menuScanStatus": "SUCCESS",
                    "menuScanTimestamp": 1000000,
                    "favoriteStatus": null
                }
            ]
        }
        """.trimIndent()
        
        `when`(mockSharedPreferences.getString("restaurant_cache", null))
            .thenReturn(restaurantJson)
        
        homeViewModel = HomeViewModel()
        val cachedRestaurantsLiveData = homeViewModel.cachedRestaurants
        
        val observer = mock<Observer<List<Restaurant>>>()
        cachedRestaurantsLiveData.observeForever(observer)
        
        verify(observer).onChanged(not(eq(emptyList())))
    }

    @Test
    fun `test cachedRestaurants with malformed JSON`() {
        `when`(mockSharedPreferences.getString("restaurant_cache", null))
            .thenReturn("invalid json")
        
        homeViewModel = HomeViewModel()
        val cachedRestaurantsLiveData = homeViewModel.cachedRestaurants
        
        val observer = mock<Observer<List<Restaurant>>>()
        cachedRestaurantsLiveData.observeForever(observer)
        
        verify(observer).onChanged(eq(emptyList()))
    }

    @Test
    fun `test cachedRestaurants with empty items array`() {
        val emptyRestaurantJson = """
        {
            "lat": 40.7128,
            "lng": -74.0060,
            "timestamp": ${System.currentTimeMillis()},
            "items": []
        }
        """.trimIndent()
        
        `when`(mockSharedPreferences.getString("restaurant_cache", null))
            .thenReturn(emptyRestaurantJson)
        
        homeViewModel = HomeViewModel()
        val cachedRestaurantsLiveData = homeViewModel.cachedRestaurants
        
        val observer = mock<Observer<List<Restaurant>>>()
        cachedRestaurantsLiveData.observeForever(observer)
        
        verify(observer).onChanged(eq(emptyList()))
    }

    // ========== LOCATION PERMISSION TESTS ==========

    @Test
    fun `test isPermissionGranted LiveData initialization`() {
        homeViewModel = HomeViewModel()
        val permissionLiveData = homeViewModel.isPermissionGranted
        
        assertNotNull("Permission LiveData should not be null", permissionLiveData)
    }

    @Test
    fun `test isPermissionGranted returns false when permission denied`() {
        // Mock permission denied
        `when`(mockSharedPreferences.getBoolean("location_permission_granted", false))
            .thenReturn(false)
        
        homeViewModel = HomeViewModel()
        val permissionLiveData = homeViewModel.isPermissionGranted
        
        val observer = mock<Observer<Boolean>>()
        permissionLiveData.observeForever(observer)
        
        verify(observer).onChanged(eq(false))
    }

    @Test
    fun `test isPermissionGranted returns true when permission granted`() {
        // Mock permission granted
        `when`(mockSharedPreferences.getBoolean("location_permission_granted", false))
            .thenReturn(true)
        
        homeViewModel = HomeViewModel()
        val permissionLiveData = homeViewModel.isPermissionGranted
        
        val observer = mock<Observer<Boolean>>()
        permissionLiveData.observeForever(observer)
        
        verify(observer).onChanged(eq(true))
    }

    // ========== RESTAURANT DATA VALIDATION TESTS ==========

    @Test
    fun `test restaurant data validation - valid restaurant`() {
        val validRestaurantJson = """
        {
            "name": "Gluten Free Kitchen",
            "address": "123 Main St",
            "hasGf": true,
            "lat": 40.7128,
            "lng": -74.0060,
            "rating": 4.5,
            "openNow": true,
            "placeId": "test_place_id",
            "menu": ["GF Pasta"],
            "notes": ["Great food!"],
            "menuScanStatus": "SUCCESS",
            "menuScanTimestamp": 1000000,
            "favoriteStatus": "safe"
        }
        """.trimIndent()
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(validRestaurantJson)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertEquals("Should have one restaurant", 1, restaurants.size)
        
        val restaurant = restaurants[0]
        assertEquals("Gluten Free Kitchen", restaurant.name)
        assertEquals("123 Main St", restaurant.address)
        assertTrue("Should have gluten-free options", restaurant.hasGlutenFreeOptions())
        assertEquals("Safe", restaurant.favoriteStatus)
        assertTrue("Should have crowd notes", restaurant.crowdNotes.isNotEmpty())
    }

    @Test
    fun `test restaurant data validation - missing required fields`() {
        val invalidRestaurantJson = """
        {
            "name": "",
            "address": "",
            "hasGf": false,
            "lat": 0.0,
            "lng": 0.0
        }
        """.trimIndent()
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(invalidRestaurantJson)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should handle invalid data gracefully", restaurants.isEmpty())
    }

    @Test
    fun `test restaurant data validation - null values`() {
        val nullRestaurantJson = """
        {
            "name": null,
            "address": null,
            "hasGf": null,
            "lat": null,
            "lng": null
        }
        """.trimIndent()
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(nullRestaurantJson)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should handle null values gracefully", restaurants.isEmpty())
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `test empty JSON`() {
        val emptyJson = "{}"
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(emptyJson)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should return empty list for empty JSON", restaurants.isEmpty())
    }

    @Test
    fun `test null JSON`() {
        val restaurants = homeViewModel.parseRestaurantsFromJson(null)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should return empty list for null JSON", restaurants.isEmpty())
    }

    @Test
    fun `test malformed JSON string`() {
        val malformedJson = "{ invalid json }"
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(malformedJson)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should return empty list for malformed JSON", restaurants.isEmpty())
    }

    @Test
    fun `test JSON with missing items array`() {
        val jsonWithoutItems = """
        {
            "lat": 40.7128,
            "lng": -74.0060
        }
        """.trimIndent()
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(jsonWithoutItems)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should return empty list when items array is missing", restaurants.isEmpty())
    }

    @Test
    fun `test JSON with non-array items`() {
        val jsonWithNonArrayItems = """
        {
            "items": "not an array"
        }
        """.trimIndent()
        
        val restaurants = homeViewModel.parseRestaurantsFromJson(jsonWithNonArrayItems)
        
        assertNotNull("Restaurants should not be null", restaurants)
        assertTrue("Should return empty list when items is not an array", restaurants.isEmpty())
    }

    // ========== SETTINGS INTEGRATION TESTS ==========

    @Test
    fun `test settings integration for distance units`() {
        // Mock miles preference
        `when`(mockSharedPreferences.getBoolean("use_miles", false))
            .thenReturn(true)
        
        homeViewModel = HomeViewModel()
        
        val useMiles = SettingsManager.useMiles(mockContext)
        assertTrue("Should use miles when preference is true", useMiles)
    }

    @Test
    fun `test settings integration for theme mode`() {
        // Mock theme preference
        `when`(mockSharedPreferences.getInt("theme_mode", 1))
            .thenReturn(2) // Dark theme
        
        homeViewModel = HomeViewModel()
        
        val themeMode = SettingsManager.getThemeMode(mockContext)
        assertEquals("Should return dark theme", 2, themeMode)
    }

    // ========== LIFECYCLE TESTS ==========

    @Test
    fun `test ViewModel creation`() {
        homeViewModel = HomeViewModel()
        
        assertNotNull("HomeViewModel should be created successfully", homeViewModel)
    }

    @Test
    fun `test ViewModel lifecycle - onCleared`() {
        homeViewModel = HomeViewModel()
        
        // Simulate ViewModel clearing
        homeViewModel.onCleared()
        
        // No specific assertion needed, just ensure no exceptions are thrown
        assertTrue("ViewModel cleanup should complete without errors", true)
    }

    @Test
    fun `test multiple observers on same LiveData`() {
        homeViewModel = HomeViewModel()
        val textLiveData = homeViewModel.text
        
        val observer1 = mock<Observer<String>>()
        val observer2 = mock<Observer<String>>()
        
        textLiveData.observeForever(observer1)
        textLiveData.observeForever(observer2)
        
        verify(observer1).onChanged("This is home fragment")
        verify(observer2).onChanged("This is home fragment")
    }

    @Test
    fun `test observer removal`() {
        homeViewModel = HomeViewModel()
        val textLiveData = homeViewModel.text
        
        val observer = mock<Observer<String>>()
        textLiveData.observeForever(observer)
        
        // Remove observer
        textLiveData.removeObserver(observer)
        
        // Observer should not be called again
        verifyNoMoreInteractions(observer)
    }

    // ========== HELPER METHODS TESTS ==========

    @Test
    fun `test isValidRestaurant with valid restaurant`() {
        val validRestaurant = Restaurant(
            name = "Valid Restaurant",
            address = "123 Valid St",
            hasGFMenu = true,
            gfMenu = Arrays.asList("GF Item"),
            latitude = 40.7128,
            longitude = -74.0060
        )
        
        val isValid = homeViewModel.isValidRestaurant(validRestaurant)
        
        assertTrue("Valid restaurant should pass validation", isValid)
    }

    @Test
    fun `test isValidRestaurant with invalid restaurant - empty name`() {
        val invalidRestaurant = Restaurant(
            name = "",
            address = "123 Test St",
            hasGFMenu = false,
            gfMenu = Arrays.asList(),
            latitude = 0.0,
            longitude = 0.0
        )
        
        val isValid = homeViewModel.isValidRestaurant(invalidRestaurant)
        
        assertFalse("Restaurant with empty name should be invalid", isValid)
    }

    @Test
    fun `test isValidRestaurant with invalid restaurant - null name`() {
        val invalidRestaurant = Restaurant(
            name = null,
            address = "123 Test St",
            hasGFMenu = false,
            gfMenu = Arrays.asList(),
            latitude = 0.0,
            longitude = 0.0
        )
        
        val isValid = homeViewModel.isValidRestaurant(invalidRestaurant)
        
        assertFalse("Restaurant with null name should be invalid", isValid)
    }

    @Test
    fun `test formatDistance with miles preference`() {
        val distanceMeters = 1609.34 // 1 mile
        
        val formattedDistance = homeViewModel.formatDistance(distanceMeters, true)
        
        assertEquals("Should format as miles", "1.0 mi", formattedDistance)
    }

    @Test
    fun `test formatDistance with kilometers preference`() {
        val distanceMeters = 1000.0 // 1 kilometer
        
        val formattedDistance = homeViewModel.formatDistance(distanceMeters, false)
        
        assertEquals("Should format as kilometers", "1.0 km", formattedDistance)
    }

    @Test
    fun `test formatDistance with feet`() {
        val distanceMeters = 304.8 // 1000 feet
        
        val formattedDistance = homeViewModel.formatDistance(distanceMeters, true)
        
        assertEquals("Should format as feet for short distances", "1000 ft", formattedDistance)
    }

    @Test
    fun `test formatDistance with meters`() {
        val distanceMeters = 500.0 // 500 meters
        
        val formattedDistance = homeViewModel.formatDistance(distanceMeters, false)
        
        assertEquals("Should format as meters for short distances", "500 m", formattedDistance)
    }

    @After
    fun tearDown() {
        // Clean up any mocks
        try {
            // Additional cleanup if needed
        } catch (e: Exception) {
            // Ignore cleanup exceptions
        }
    }
}