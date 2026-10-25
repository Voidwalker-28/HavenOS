plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "app.havenos.launcher"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "app.havenos.launcher"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.0"
    }

    // Signing is developer-supplied and intentionally not committed here.
    // Debug builds use Android Gradle Plugin's default debug keystore.
    // For a signed release build, add your own signingConfig (see BUILDING.md)
    // and point it at a keystore that lives OUTSIDE this repository.
    // Never commit a keystore or a password to version control.

    buildTypes {
        debug {
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // No signingConfig here by default: add your own for a signed
            // release build (see BUILDING.md). An unsigned release APK is
            // still produced and is fine for local testing.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.animation)
    implementation(libs.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.room.runtime)
    implementation(libs.datastore.preferences)
    implementation(libs.savedstate)
    ksp(libs.room.compiler)
    // Unit tests (JUnit4). Resolved lazily — assemble tasks never touch this
    // configuration, so offline builds keep working; run with network for tests.
    testImplementation(libs.junit)
}

// Offline pinning: newer artifacts' metadata drags the androidx.lifecycle atomic
// group (and core-ktx) to versions absent from the offline cache. Pin to the
// artifacts that exist in the offline cache.
configurations.all {
    resolutionStrategy {
        // Compose stack: aligned to 1.11.0-beta02 — the version material3
        // 1.5.0-alpha17 declares in its POM. Mixing in older compose-ui
        // causes runtime NoSuchMethodError (binary incompatibility).
        force("androidx.compose.ui:ui:1.11.0-beta02")
        force("androidx.compose.ui:ui-graphics:1.11.0-beta02")
        force("androidx.compose.ui:ui-text:1.11.0-beta02")
        force("androidx.compose.foundation:foundation:1.11.0-beta02")
        force("androidx.compose.foundation:foundation-layout:1.11.0-beta02")
        force("androidx.compose.runtime:runtime:1.11.0-beta02")
        force("androidx.compose.runtime:runtime-saveable:1.11.0-beta02")
        force("androidx.compose.animation:animation:1.11.0-beta02")
        force("androidx.compose.animation:animation-core:1.11.0-beta02")
        // compose-ui 1.11.0-beta02 wants window-core 1.5.0; only 1.5.1 is cached.
        force("androidx.window:window-core:1.5.1")
        force("androidx.lifecycle:lifecycle-common:2.8.3")
        force("androidx.lifecycle:lifecycle-common-jvm:2.8.3")
        force("androidx.lifecycle:lifecycle-common-java8:2.6.1")
        force("androidx.lifecycle:lifecycle-viewmodel:2.8.3")
        force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
        force("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.3")
        force("androidx.lifecycle:lifecycle-process:2.8.3")
        force("androidx.lifecycle:lifecycle-livedata:2.8.3")
        force("androidx.lifecycle:lifecycle-livedata-core:2.8.3")
        force("androidx.lifecycle:lifecycle-livedata-core-ktx:2.8.3")
        force("androidx.lifecycle:lifecycle-runtime:2.8.3")
        force("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
        force("androidx.savedstate:savedstate-ktx:1.3.1")
        force("androidx.savedstate:savedstate:1.4.0")
        force("androidx.core:core-ktx:1.13.1")
    }
}

