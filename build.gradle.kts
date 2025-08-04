// Top-level build file where you can add configuration options common to all sub-projects/modules.
// Top-level plugins shared by all modules in the project.
// Adding the Kotlin Android plugin here ensures that every module
// depending on Kotlin uses the same Kotlin version. This avoids
// pulling in mismatched stdlib artifacts which previously caused
// duplicate class errors at build time.
plugins {
    id("com.android.application") version "8.1.0" apply false
    // Align Kotlin dependencies to 1.8.10 for all modules
    kotlin("android") version "1.8.10" apply false
}