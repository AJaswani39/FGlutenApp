# CLAUDE.md — FGlutenApp

This file provides guidance for AI assistants working on the FGlutenApp codebase.

---

## Project Overview

**FGlutenApp** is an Android application that helps users find and evaluate gluten-free restaurants. Core capabilities include:

- Location-based restaurant discovery via Google Places API
- AI-powered menu analysis (ML Kit + TensorFlow Lite)
- User authentication (email/password and Google Sign-In via Firebase)
- User profiles with contribution and reputation tracking
- Crowd-sourced notes and reviews stored in Firestore
- Favorites management and offline caching via SharedPreferences
- Theme preferences (light/dark/system) and distance unit preferences (km/miles)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin + Java (hybrid) |
| Architecture | MVVM, single-activity, Navigation Component |
| UI | Android Fragments, ConstraintLayout, Material Design |
| Database (remote) | Firebase Firestore |
| Auth | Firebase Authentication |
| Location & maps | Google Play Services Location, Google Maps SDK, Google Places API |
| AI/ML | ML Kit (Translate, Text Recognition, Entity Extraction), TensorFlow Lite |
| Testing | JUnit 4, Mockito, Robolectric, Espresso |
| Build | Gradle (Kotlin DSL), AGP 8.12.0, Kotlin 1.9.24 |
| CI | GitHub Actions |

---

## Repository Layout

```
FGlutenApp/
├── app/
│   ├── build.gradle.kts              # App-level build config
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── google-services.json
│       │   ├── java/com/example/fgluten/
│       │   │   ├── FGlutenApp.kt         # Application class, Firebase init
│       │   │   ├── MainActivity.java     # Single-activity host
│       │   │   ├── data/                 # Data layer
│       │   │   │   ├── Restaurant.java
│       │   │   │   ├── RestaurantRepository.java
│       │   │   │   ├── ai/
│       │   │   │   │   └── MenuAnalysisResult.kt
│       │   │   │   ├── repository/
│       │   │   │   │   ├── AuthRepository.kt
│       │   │   │   │   └── AIRepository.kt
│       │   │   │   └── user/
│       │   │   │       ├── UserProfile.kt
│       │   │   │       ├── RestaurantReview.kt
│       │   │   │       └── CrowdNote.kt
│       │   │   ├── ui/                   # UI layer (fragments + ViewModels)
│       │   │   │   ├── auth/
│       │   │   │   ├── home/
│       │   │   │   ├── restaurant/
│       │   │   │   ├── profile/
│       │   │   │   ├── ai/
│       │   │   │   ├── gallery/
│       │   │   │   └── settings/
│       │   │   └── util/
│       │   │       └── SettingsManager.java
│       │   └── res/
│       │       ├── layout/               # 18+ layout XML files
│       │       ├── navigation/
│       │       │   └── mobile_navigation.xml
│       │       ├── values/               # strings, colors, themes
│       │       └── values-night/         # dark-mode overrides
│       ├── test/                         # JVM unit tests
│       │   └── java/com/example/fgluten/
│       │       ├── data/
│       │       ├── ui/home/
│       │       └── util/
│       └── androidTest/                  # Instrumentation tests
│           └── java/com/example/fgluten/
├── build.gradle.kts                      # Root build config
├── gradle.properties
├── settings.gradle.kts
├── .github/workflows/android-ci.yml
├── README_TESTS.md
├── FGlutenApp_Architecture_Improvements.md
└── FGlutenApp_AI_Enhancement_Plan.md
```

---

## Architecture

### Pattern: MVVM + Repository

```
UI (Fragments) ──LiveData──► ViewModels ──► Repositories ──► Data Sources
```

- **Fragments** observe LiveData from ViewModels and update the UI reactively.
- **ViewModels** hold business logic and mutable state. They survive configuration changes.
- **Repositories** abstract data sources. `AuthRepository.kt` uses Kotlin coroutines and returns `Result<T>`. `RestaurantRepository.java` is a placeholder pending full implementation.
- **Data sources**: Firebase Firestore (remote), SharedPreferences (local cache), Google Places API (restaurant search).

### Navigation

- Single activity: `MainActivity.java` hosts all fragments via the Navigation Component.
- Bottom navigation bar switches between **Home** (`nav_home`) and **Restaurant List** (`nav_restaurant_list`).
- Deep links and back-stack are managed by `mobile_navigation.xml`.

### Application Initialization

`FGlutenApp.kt` extends `Application` and initialises Firebase before any ViewModel or Repository is instantiated.

---

## Key Source Files

| File | Purpose |
|---|---|
| `MainActivity.java` | Activity host; splash screen, theme setup, bottom nav |
| `FGlutenApp.kt` | Application class; Firebase init |
| `Restaurant.java` | Core data model; Parcelable; `MenuScanStatus` enum |
| `RestaurantViewModel.java` | Central restaurant logic; Places API, menu scanning, favorites |
| `HomeViewModel.java` | Home screen state; cached restaurants, location permissions |
| `AuthRepository.kt` | Firebase Auth + Firestore CRUD; email & Google sign-in; coroutines |
| `AIRepository.kt` | AI/ML service integration for menu analysis |
| `MenuAnalysisResult.kt` | Enums and data classes for AI analysis output |
| `UserProfile.kt` | Firestore user profile model; trust level, attribution name |
| `SettingsManager.java` | Singleton; SharedPreferences wrapper for theme, units, contributor name |

---

## Development Workflows

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Run Tests

```bash
# All JVM unit tests
./gradlew testDebugUnitTest

# Single test class
./gradlew test --tests "com.example.fgluten.ui.home.HomeViewModelTest"

# Instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Coverage report
./gradlew jacocoTestReport
```

Test reports are written to `app/build/reports/tests/`.

### Lint

```bash
./gradlew lint
```

Lint reports are written to `app/build/reports/lint/`.

### Clean

```bash
./gradlew clean
```

---

## Environment & Secrets

| Variable | Where it lives | How it is used |
|---|---|---|
| `MAPS_API_KEY` | `local.properties` (git-ignored) | Injected via `manifestPlaceholders` and `buildConfigField` |
| Firebase config | `app/assets/google-services.json` | Parsed automatically by the Google Services Gradle plugin |

**Never commit `local.properties`** — it is listed in `.gitignore`.

Required `local.properties` entry:

```
MAPS_API_KEY=YOUR_KEY_HERE
```

---

## Firebase / Firestore Collections

| Collection | Description |
|---|---|
| `users` | User profiles (`UserProfile.kt`) |
| `crowd_notes` | Crowd-sourced restaurant notes (`CrowdNote.kt`) |
| `restaurant_reviews` | User reviews (`RestaurantReview.kt`) |

---

## SharedPreferences Keys

Managed entirely through `SettingsManager.java`:

| Preference file | Key examples |
|---|---|
| `fg_settings` | theme mode, distance units, contributor name |
| `restaurant_cache` | JSON-serialised cached restaurant list |
| `restaurant_favorites` | Set of favourite place IDs |
| `restaurant_notes` | Cached crowd notes |

---

## Code Conventions

### Mixed Kotlin / Java

The codebase uses both languages. Prefer **Kotlin** for new files. When editing existing Java files, stay in Java unless converting the whole file.

### Null Safety

- In Kotlin files, use nullable types (`?`) and safe-call operators (`?.`) rather than `!!` where possible.
- In Java files, add `@Nullable` / `@NonNull` annotations for interop with Kotlin.

### Threading

- `AuthRepository.kt` uses Kotlin coroutines (`suspend` functions, `Result<T>`).
- `RestaurantViewModel.java` uses an `Executor` for background I/O.
- Do **not** perform network or disk I/O on the main thread.

### LiveData

- Expose `LiveData<T>` (immutable) from ViewModels; keep `MutableLiveData<T>` private.
- Observe LiveData in `onViewCreated`, not `onCreate`, to avoid stale observers after fragment view recreation.

### Repository Pattern

- ViewModels must not directly access Firebase, SharedPreferences, or network APIs — route everything through a Repository.

### Resource Naming

Follow standard Android conventions:

| Resource type | Prefix |
|---|---|
| Layouts | `fragment_`, `activity_`, `item_`, `dialog_` |
| IDs | `camelCase` (e.g., `tvRestaurantName`) |
| Drawables | `ic_` (icons), `bg_` (backgrounds) |
| String keys | `snake_case` |

---

## CI/CD

Defined in `.github/workflows/android-ci.yml`. Triggers on push/PR to `main` and `develop`.

| Job | What it does |
|---|---|
| `test` | Runs `testDebugUnitTest` + `connectedAndroidTest`; uploads test results |
| `build` | Runs `assembleRelease` (depends on `test`); uploads APK artifact |
| `lint` | Runs `lint`; uploads lint report |

All jobs use **Java 11 (Temurin)** and **ubuntu-latest**.

---

## Android Manifest Permissions

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
```

Location permissions must be requested at runtime for Android 6+. The app handles permission results in `MainActivity.java`.

---

## SDK Targets

| Setting | Value |
|---|---|
| `compileSdk` | 34 |
| `minSdk` | 27 |
| `targetSdk` | 34 |
| Java compatibility | 1.8 |

---

## Planned Work (roadmaps)

Two detailed planning documents exist in the repo root:

- **`FGlutenApp_Architecture_Improvements.md`** — four-phase roadmap covering navigation enhancements, offline-first caching, content moderation, push notifications, and accessibility.
- **`FGlutenApp_AI_Enhancement_Plan.md`** — four-phase AI roadmap covering menu analysis, recommendation engine, sentiment analysis, photo recognition, and AI-powered search.

When implementing features from these documents, follow the phased approach described within them and ensure new code includes matching unit tests.
