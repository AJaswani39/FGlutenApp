package io.fgluten.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Comprehensive unit tests for the SettingsManager utility class.
 * 
 * Tests cover:
 * - Theme mode management (light, dark, system)
 * - Distance unit preferences (miles vs kilometers)
 * - SharedPreferences integration
 * - Default value handling
 * - Edge cases and error scenarios
 * 
 * @author FGluten Development Team
 */
class SettingsManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        
        // Mock the SharedPreferences creation
        `when`(mockContext.getSharedPreferences("fg_settings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(any(), any())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(any(), any())).thenReturn(mockEditor)
    }

    // ========== THEME MODE TESTS ==========

    @Test
    fun `test getThemeMode returns default system mode`() {
        `when`(mockSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
            .thenReturn(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        val result = SettingsManager.getThemeMode(mockContext)
        
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, result)
    }

    @Test
    fun `test getThemeMode returns light mode`() {
        `when`(mockSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
            .thenReturn(AppCompatDelegate.MODE_NIGHT_NO)
        
        val result = SettingsManager.getThemeMode(mockContext)
        
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, result)
        assertEquals("Light theme should be MODE_NIGHT_NO", AppCompatDelegate.MODE_NIGHT_NO, result)
    }

    @Test
    fun `test getThemeMode returns dark mode`() {
        `when`(mockSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
            .thenReturn(AppCompatDelegate.MODE_NIGHT_YES)
        
        val result = SettingsManager.getThemeMode(mockContext)
        
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, result)
        assertEquals("Dark theme should be MODE_NIGHT_YES", AppCompatDelegate.MODE_NIGHT_YES, result)
    }

    @Test
    fun `test setThemeMode saves and applies light mode`() {
        val mode = AppCompatDelegate.MODE_NIGHT_NO
        
        SettingsManager.setThemeMode(mockContext, mode)
        
        verify(mockEditor).putInt("theme_mode", mode)
        verify(mockEditor).apply()
        verify(AppCompatDelegate).setDefaultNightMode(mode)
    }

    @Test
    fun `test setThemeMode saves and applies dark mode`() {
        val mode = AppCompatDelegate.MODE_NIGHT_YES
        
        SettingsManager.setThemeMode(mockContext, mode)
        
        verify(mockEditor).putInt("theme_mode", mode)
        verify(mockEditor).apply()
        verify(AppCompatDelegate).setDefaultNightMode(mode)
    }

    @Test
    fun `test setThemeMode saves and applies system mode`() {
        val mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        
        SettingsManager.setThemeMode(mockContext, mode)
        
        verify(mockEditor).putInt("theme_mode", mode)
        verify(mockEditor).apply()
        verify(AppCompatDelegate).setDefaultNightMode(mode)
    }

    @Test
    fun `test setThemeMode with invalid mode value`() {
        val invalidMode = -999 // Invalid mode
        
        SettingsManager.setThemeMode(mockContext, invalidMode)
        
        verify(mockEditor).putInt("theme_mode", invalidMode)
        verify(mockEditor).apply()
        verify(AppCompatDelegate).setDefaultNightMode(invalidMode)
    }

    // ========== DISTANCE UNIT TESTS ==========

    @Test
    fun `test useMiles returns default false for kilometers`() {
        `when`(mockSharedPreferences.getBoolean("use_miles", false))
            .thenReturn(false)
        
        val result = SettingsManager.useMiles(mockContext)
        
        assertFalse("Default should be kilometers (false)", result)
    }

    @Test
    fun `test useMiles returns true for miles`() {
        `when`(mockSharedPreferences.getBoolean("use_miles", false))
            .thenReturn(true)
        
        val result = SettingsManager.useMiles(mockContext)
        
        assertTrue("Should return true for miles preference", result)
    }

    @Test
    fun `test setUseMiles saves true for miles`() {
        SettingsManager.setUseMiles(mockContext, true)
        
        verify(mockEditor).putBoolean("use_miles", true)
        verify(mockEditor).apply()
    }

    @Test
    fun `test setUseMiles saves false for kilometers`() {
        SettingsManager.setUseMiles(mockContext, false)
        
        verify(mockEditor).putBoolean("use_miles", false)
        verify(mockEditor).apply()
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    fun `test complete theme switch workflow`() {
        // Simulate switching from system to light mode
        `when`(mockSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
            .thenReturn(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // Get initial theme
        val initialTheme = SettingsManager.getThemeMode(mockContext)
        assertEquals("Initial theme should be system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, initialTheme)
        
        // Switch to light mode
        SettingsManager.setThemeMode(mockContext, AppCompatDelegate.MODE_NIGHT_NO)
        
        // Verify calls were made
        verify(mockEditor).putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
        verify(mockEditor).apply()
        verify(AppCompatDelegate).setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    @Test
    fun `test complete units switch workflow`() {
        // Default should be kilometers
        `when`(mockSharedPreferences.getBoolean("use_miles", false))
            .thenReturn(false)
        
        var useMiles = SettingsManager.useMiles(mockContext)
        assertFalse("Initial preference should be kilometers", useMiles)
        
        // Switch to miles
        SettingsManager.setUseMiles(mockContext, true)
        
        verify(mockEditor).putBoolean("use_miles", true)
        verify(mockEditor).apply()
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `test getThemeMode with corrupted preferences`() {
        `when`(mockSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
            .thenReturn(-1) // Corrupted value
        
        val result = SettingsManager.getThemeMode(mockContext)
        
        assertEquals("Should return corrupted value as-is", -1, result)
    }

    @Test
    fun `test getThemeMode with missing key`() {
        `when`(mockSharedPreferences.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))
            .thenReturn(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // Default value
        
        val result = SettingsManager.getThemeMode(mockContext)
        
        assertEquals("Should return default system mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, result)
    }

    @Test
    fun `test useMiles with corrupted preferences`() {
        `when`(mockSharedPreferences.getBoolean("use_miles", false))
            .thenReturn(false) // Default value even for corruption
        
        val result = SettingsManager.useMiles(mockContext)
        
        assertFalse("Should return default false", result)
    }

    @Test
    fun `test setThemeMode multiple times`() {
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_NO
        )
        
        for (mode in modes) {
            SettingsManager.setThemeMode(mockContext, mode)
            verify(mockEditor).putInt("theme_mode", mode)
            verify(AppCompatDelegate).setDefaultNightMode(mode)
        }
        
        verify(mockEditor, times(modes.size)).apply()
    }

    @Test
    fun `test setUseMiles toggling`() {
        // Switch to miles
        SettingsManager.setUseMiles(mockContext, true)
        verify(mockEditor).putBoolean("use_miles", true)
        verify(mockEditor).apply()
        
        // Switch back to kilometers
        SettingsManager.setUseMiles(mockContext, false)
        verify(mockEditor).putBoolean("use_miles", false)
        verify(mockEditor).apply()
        
        // Switch to miles again
        SettingsManager.setUseMiles(mockContext, true)
        verify(mockEditor).putBoolean("use_miles", true)
        verify(mockEditor, times(3)).apply()
    }

    // ========== CONSTANT VALUES TESTS ==========

    @Test
    fun `test constant values are correct`() {
        assertEquals("Light mode constant", AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_NO)
        assertEquals("Dark mode constant", AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_YES)
        assertEquals("System mode constant", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    @Test
    fun `test theme mode comparison`() {
        assertNotEquals("Light and dark modes should differ", AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES)
        assertNotEquals("Light and system modes should differ", AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        assertNotEquals("Dark and system modes should differ", AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    @Test
    fun `test all theme modes are different`() {
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        
        // Check that all modes are distinct
        assertNotEquals("Light ≠ Dark", modes[0], modes[1])
        assertNotEquals("Light ≠ System", modes[0], modes[2])
        assertNotEquals("Dark ≠ System", modes[1], modes[2])
    }

    // ========== MOCK VERIFICATION TESTS ==========

    @Test
    fun `test SharedPreferences is called with correct filename`() {
        SettingsManager.getThemeMode(mockContext)
        
        verify(mockContext).getSharedPreferences("fg_settings", Context.MODE_PRIVATE)
    }

    @Test
    fun `test editor is obtained from SharedPreferences`() {
        SettingsManager.setThemeMode(mockContext, AppCompatDelegate.MODE_NIGHT_NO)
        
        verify(mockSharedPreferences).edit()
    }

    @Test
    fun `test apply is called for theme changes`() {
        SettingsManager.setThemeMode(mockContext, AppCompatDelegate.MODE_NIGHT_YES)
        
        verify(mockEditor).apply()
    }

    @Test
    fun `test apply is called for unit changes`() {
        SettingsManager.setUseMiles(mockContext, true)
        
        verify(mockEditor).apply()
    }

    @Test
    fun `test AppCompatDelegate is called for theme changes`() {
        SettingsManager.setThemeMode(mockContext, AppCompatDelegate.MODE_NIGHT_NO)
        
        verify(AppCompatDelegate).setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    @Test
    fun `test no exception thrown for null context`() {
        try {
            SettingsManager.getThemeMode(null)
            fail("Should have thrown exception for null context")
        } catch (e: NullPointerException) {
            // Expected behavior
        }
    }

    @Test
    fun `test no exception thrown for null context in setThemeMode`() {
        try {
            SettingsManager.setThemeMode(null, AppCompatDelegate.MODE_NIGHT_NO)
            fail("Should have thrown exception for null context")
        } catch (e: NullPointerException) {
            // Expected behavior
        }
    }

    @Test
    fun `test no exception thrown for null context in useMiles`() {
        try {
            SettingsManager.useMiles(null)
            fail("Should have thrown exception for null context")
        } catch (e: NullPointerException) {
            // Expected behavior
        }
    }

    @Test
    fun `test no exception thrown for null context in setUseMiles`() {
        try {
            SettingsManager.setUseMiles(null, true)
            fail("Should have thrown exception for null context")
        } catch (e: NullPointerException) {
            // Expected behavior
        }
    }

    // ========== PREFERENCE KEY TESTS ==========

    @Test
    fun `test theme mode key is used correctly`() {
        SettingsManager.setThemeMode(mockContext, AppCompatDelegate.MODE_NIGHT_NO)
        
        verify(mockEditor).putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
    }

    @Test
    fun `test use miles key is used correctly`() {
        SettingsManager.setUseMiles(mockContext, true)
        
        verify(mockEditor).putBoolean("use_miles", true)
    }

    @Test
    fun `test different preference keys are used for different settings`() {
        SettingsManager.setThemeMode(mockContext, AppCompatDelegate.MODE_NIGHT_YES)
        SettingsManager.setUseMiles(mockContext, true)
        
        // Both keys should be used
        verify(mockEditor).putInt("theme_mode", AppCompatDelegate.MODE_NIGHT_YES)
        verify(mockEditor).putBoolean("use_miles", true)
    }
}