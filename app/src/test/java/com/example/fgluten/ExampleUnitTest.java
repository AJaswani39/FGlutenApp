package com.example.fgluten;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * This test class serves as a template and demonstration of basic JUnit testing
 * practices for the FGluten Android application. While simple, it demonstrates
 * fundamental testing concepts that are used throughout the application's test suite.
 *
 * **Purpose:**
 * - Provides a basic example of unit testing structure
 * - Demonstrates JUnit @Test annotation usage
 * - Shows simple assertion patterns with assertEquals
 * - Serves as a "smoke test" to verify the testing framework is working
 *
 * **Testing Best Practices Demonstrated:**
 * - Clear test method naming that describes the expected behavior
 * - Simple, focused test cases that verify one specific behavior
 * - Use of standard JUnit assertions for validation
 * - Minimal setup and teardown for simple test cases
 *
 * **Integration with Android Testing:**
 * - This test runs on the local JVM (not on Android device/emulator)
 * - Uses standard JUnit testing framework without Android dependencies
 * - Fast execution suitable for continuous integration and rapid development
 * - Can be extended with more complex testing scenarios as needed
 *
 * @see <a href="http://d.android.com/tools/testing">Android Testing Documentation</a>
 * @see <a href="https://junit.org/junit4/">JUnit 4 Documentation</a>
 */
public class ExampleUnitTest {
    
    /**
     * Test basic arithmetic operation to verify testing framework functionality.
     * 
     * This simple test validates that:
     * 1. The JUnit testing framework is properly configured
     * 2. The assertEquals assertion method works correctly
     * 3. Basic Java arithmetic operations function as expected
     * 
     * While trivial, this test serves as a "sanity check" to ensure the testing
     * environment is set up correctly before running more complex application tests.
     * 
     * **Test Scenario:**
     * - Input: Simple arithmetic expression (2 + 2)
     * - Expected Result: 4
     * - Validation: assertEquals verifies the actual result matches expected result
     * 
     * **Real-World Application:**
     * In the context of the FGluten app, similar simple tests might verify:
     * - Distance calculation utilities
     * - String formatting functions
     * - Data validation helper methods
     * - Configuration value parsing
     * 
     * @since 1.0
     */
    @Test
    public void addition_isCorrect() {
        // Verify that 2 + 2 equals 4
        // This tests both the JUnit framework and basic arithmetic functionality
        assertEquals("Basic arithmetic should work correctly", 4, 2 + 2);
    }
}