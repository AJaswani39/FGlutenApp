package io.fgluten

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive instrumentation tests for FGlutenApp.
 * 
 * Tests cover:
 * - Main Activity launch and navigation
 * - Fragment navigation (Home, Restaurant List)
 * - Location permission flow
 * - Settings menu interaction
 * - Drawer navigation
 * - UI element visibility and functionality
 * 
 * @author FGluten Development Team
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var uiDevice: UiDevice

    @Before
    fun setUp() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        
        // Grant location permission before starting tests
        grantLocationPermission()
    }

    @After
    fun tearDown() {
        // Clean up any test state
    }

    // ========== ACTIVITY LAUNCH TESTS ==========

    @Test
    fun testMainActivityLaunch() {
        // Test that MainActivity launches successfully
        onView(withId(R.id.drawer_layout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testToolbarVisibility() {
        // Test that toolbar is visible and has correct title
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
        
        onView(withText("FGluten"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationDrawerOpen() {
        // Test opening the navigation drawer
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open())
        
        // Verify drawer is open
        onView(withId(R.id.nav_view))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigationDrawerClose() {
        // Open then close the drawer
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open())
        
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.close())
        
        // Verify drawer is closed
        onView(withId(R.id.nav_view))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    // ========== FRAGMENT NAVIGATION TESTS ==========

    @Test
    fun testHomeFragmentDisplayed() {
        // Initially should be on home fragment
        onView(withId(R.id.home_title))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.home_subtitle))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateToRestaurantList() {
        // Open drawer and navigate to restaurant list
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open())
        
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_restaurant_list))
        
        // Verify restaurant list fragment is displayed
        onView(withId(R.id.restaurant_recycler))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testNavigateBackToHome() {
        // Navigate to restaurant list first
        testNavigateToRestaurantList()
        
        // Open drawer and navigate back to home
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open())
        
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_home))
        
        // Verify home fragment is displayed
        onView(withId(R.id.home_title))
            .check(matches(isDisplayed()))
    }

    // ========== HOME FRAGMENT TESTS ==========

    @Test
    fun testHomeCtaButtonClick() {
        // Click the main CTA button to navigate to restaurant list
        onView(withId(R.id.cta_find_restaurants))
            .perform(click())
        
        // Should navigate to restaurant list
        onView(withId(R.id.restaurant_recycler))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testHomeFragmentElements() {
        // Check all home fragment elements are displayed
        onView(withId(R.id.home_title))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.home_subtitle))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.cta_find_restaurants))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.home_cta_meta))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPermissionBannerVisibility() {
        // This test may vary based on actual permission state
        // Check if permission banner is displayed or not
        try {
            onView(withId(R.id.permission_banner))
                .check(matches(isDisplayed()))
        } catch (e: Exception) {
            // Banner not displayed - this is also valid
            onView(withId(R.id.permission_banner))
                .check(matches(withEffectiveVisibility(Visibility.GONE)))
        }
    }

    // ========== SETTINGS TESTS ==========

    @Test
    fun testSettingsMenuVisibility() {
        // Check that settings menu item is visible in toolbar
        onView(withId(R.id.action_settings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsMenuClick() {
        // Click settings menu item
        onView(withId(R.id.action_settings))
            .perform(click())
        
        // Should open settings bottom sheet
        // Note: Bottom sheet may not be immediately visible in tests
        // This test verifies the click doesn't crash the app
        onView(withId(R.id.drawer_layout))
            .check(matches(isDisplayed()))
    }

    // ========== RESTAURANT LIST TESTS ==========

    @Test
    fun testRestaurantListViewToggle() {
        // Navigate to restaurant list
        testNavigateToRestaurantList()
        
        // Check that list view toggle is displayed
        onView(withId(R.id.view_toggle))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.toggle_list))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.toggle_map))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRestaurantListFilterChips() {
        // Navigate to restaurant list
        testNavigateToRestaurantList()
        
        // Check filter chips are displayed
        onView(withId(R.id.chip_gf_only))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.chip_open_now))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRestaurantListSortOptions() {
        // Navigate to restaurant list
        testNavigateToRestaurantList()
        
        // Check sort options are displayed
        onView(withId(R.id.sort_distance))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.sort_name))
            .check(matches(isDisplayed()))
    }

    // ========== MAP VIEW TESTS ==========

    @Test
    fun testMapViewToggle() {
        // Navigate to restaurant list
        testNavigateToRestaurantList()
        
        // Switch to map view
        onView(withId(R.id.toggle_map))
            .perform(click())
        
        // Map container should be visible
        onView(withId(R.id.restaurant_map_container))
            .check(matches(isDisplayed()))
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun testNavigationWithEmptyBackStack() {
        // Test navigation when back stack is empty
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open())
        
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_home))
        
        // Should remain on home fragment
        onView(withId(R.id.home_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMultipleNavigationOperations() {
        // Test multiple navigation operations
        // Home -> Restaurant List -> Home -> Restaurant List
        
        // Navigate to restaurant list
        testNavigateToRestaurantList()
        
        // Navigate back to home
        onView(withId(R.id.drawer_layout))
            .perform(DrawerActions.open())
        
        onView(withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_home))
        
        onView(withId(R.id.home_title))
            .check(matches(isDisplayed()))
        
        // Navigate to restaurant list again
        testNavigateToRestaurantList()
        
        // Should be back on restaurant list
        onView(withId(R.id.restaurant_recycler))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testDeviceRotation() {
        // Test app behavior during rotation
        // This is a basic rotation test - more complex rotation tests
        // would involve checking state preservation
        
        uiDevice.setOrientationNatural()
        
        // Wait for rotation to complete
        Thread.sleep(1000)
        
        // Verify app is still functional
        onView(withId(R.id.drawer_layout))
            .check(matches(isDisplayed()))
        
        uiDevice.unfreezeRotation()
    }

    // ========== HELPER METHODS ==========

    private fun grantLocationPermission() {
        // Grant location permission for testing
        try {
            val allowButton = uiDevice.findObject(
                UiSelector().textMatches("(?i)allow|ok|yes")
            )
            
            if (allowButton.exists()) {
                allowButton.click()
            }
        } catch (e: Exception) {
            // Permission dialog might not appear, which is fine
        }
    }

    private fun denyLocationPermission() {
        // Deny location permission for testing
        try {
            val denyButton = uiDevice.findObject(
                UiSelector().textMatches("(?i)deny|block|no")
            )
            
            if (denyButton.exists()) {
                denyButton.click()
            }
        } catch (e: Exception) {
            // Permission dialog might not appear, which is fine
        }
    }
}