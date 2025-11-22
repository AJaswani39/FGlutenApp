# Android Testing Guide for FGluten App

## Test Types in Project

### Unit Tests (JVM Tests)
- **Location**: `app/src/test/`
- **Examples**: 
  - `ExampleUnitTest.java`
  - `RestaurantTest.java`
  - `HomeViewModelTest.kt`
  - `SettingsManagerTest.kt`
- **Run on**: Development machine (JVM)

### Instrumentation Tests (Android Tests)
- **Location**: `app/src/androidTest/`
- **Examples**: 
  - `ExampleInstrumentedTest.java`
  - `MainActivityTest.kt`
  - `RestaurantNavigationTest.java`
- **Run on**: Android device or emulator

## Running Tests in Android Studio

### Method 1: Run All Tests
1. Right-click on the `test` folder or `androidTest` folder
2. Select **"Run Tests"** or use the gutter icons next to test methods

### Method 2: Run Specific Test Class
1. Open the test file you want to run
2. Right-click on the test class name or method
3. Select **"Run 'ClassName'"**

### Method 3: Run Single Test Method
1. In an open test file, click the green ▶️ icon next to the test method
2. Or right-click the method and select **"Run 'methodName'"**

## Running Tests via Command Line

### Gradle Commands for Testing

#### Run All Unit Tests
```bash
./gradlew test
# or on Windows:
gradlew.bat test
```

#### Run All Instrumentation Tests
```bash
./gradlew connectedAndroidTest
# or short version:
./gradlew cAT
```

#### Run Specific Test Package
```bash
# Unit tests for specific package
./gradlew testDebugUnitTest

# Instrumentation tests for specific package
./gradlew connectedAndroidTestDebug
```

#### Run Specific Test Class
```bash
# Unit test
./gradlew test --tests "com.example.fgluten.data.RestaurantTest"

# Instrumentation test
./gradlew connectedAndroidTest --tests "com.example.fgluten.ExampleInstrumentedTest"
```

#### Run Specific Test Method
```bash
# Unit test method
./gradlew test --tests "com.example.fgluten.data.RestaurantTest.hasGlutenFreeOptions_hasGFMenuTrue_returnsTrue"

# Instrumentation test method
./gradlew connectedAndroidTest --tests "com.example.fgluten.ExampleInstrumentedTest.useAppContext"
```

## Testing Configuration

### Current Test Dependencies (from build.gradle.kts)

#### Unit Testing Dependencies:
- JUnit 4.13.2
- Mockito 5.1.1
- Robolectric 4.10.3
- Truth (assertion library)
- Coroutines Test

#### Instrumentation Testing Dependencies:
- Espresso 3.7.0
- AndroidX Test Runner
- AndroidX Test Rules

### Test Configuration in build.gradle.kts:
```kotlin
defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}
```

## Viewing Test Results

### In Android Studio:
1. **Run Tab**: Shows real-time test execution
2. **Run** → **Edit Configurations** to see test results
3. **Build** → **Build Output** shows test summaries

### In Command Line:
- Test results are saved in: `app/build/reports/tests/`
- HTML reports available at: `app/build/reports/tests/testDebugUnitTest/index.html`

## Best Practices for Your Project

### 1. Use Consistent Test Naming
Your current tests follow good patterns:
- `testName_StateUnderTest_ExpectedResult()`

### 2. Organize Tests by Feature
Your current structure is good:
```
src/
├── test/ (Unit tests)
│   └── com/example/fgluten/
│       ├── ExampleUnitTest.java
│       ├── data/ (Data layer tests)
│       ├── ui/ (UI layer tests)
│       └── util/ (Utility tests)
└── androidTest/ (Integration tests)
    └── com/example/fgluten/
        ├── ExampleInstrumentedTest.java
        ├── MainActivityTest.kt
        └── RestaurantNavigationTest.java
```

### 3. Add More Specific Tests
Consider adding tests for:
- `RestaurantRepository` - Mock Firebase interactions
- `RestaurantAdapter` - RecyclerView adapter testing
- `SettingsManager` - Shared preferences testing
- `MainActivity` - UI integration tests

## Quick Test Commands Reference

```bash
# Run all tests
./gradlew test connectedAndroidTest

# Run only unit tests
./gradlew test

# Run only instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run tests with coverage (if configured)
./gradlew jacocoTestReport

# Clean and run tests
./gradlew clean test