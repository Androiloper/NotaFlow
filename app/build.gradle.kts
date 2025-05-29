plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // Use KSP instead of kapt
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.notaflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.notaflow"
        minSdk = 25 // Set to 25 due to potential library requirements (e.g., from previous errors or new rich text lib)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Room schema export using KSP
    applicationVariants.all {
        kotlin {
            sourceSets {
                getByName(name) {
                    kotlin.srcDir("build/generated/ksp/$name/kotlin")
                }
            }
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = false
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // Ensure this is compatible with your Compose BOM and Kotlin
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose dependencies - Using the BOM (Bill of Materials) approach
    implementation(platform("androidx.compose:compose-bom:2023.10.01")) // User specified version
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // For extended material icons

    // Lifecycle and ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.7.6") // User specified version

    // Room components
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // implementation(libs.androidx.compose.material) // This implies a version catalog 'libs'
    // If you have it defined, it's fine.
    // Otherwise, specify the version directly or use the BOM.
    // Assuming this was meant for androidx.compose.material:material
    // which is usually covered by material3 or bom.
    // If it's a specific artifact, ensure 'libs' is set up.
    // For now, I'll comment it out if 'libs' isn't defined in this context.
    // If you meant Material 1 components: implementation("androidx.compose.material:material")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt for dependency injection
    implementation("com.google.dagger:hilt-android:2.48") // User specified version
    ksp("com.google.dagger:hilt-android-compiler:2.48") // User specified version
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0") // User specified version

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0") // User specified version

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Markdown and Rich Text Editor - Compose-based (User specified)
    implementation("com.halilibo.compose-richtext:richtext-ui:0.20.0")
    implementation("com.halilibo.compose-richtext:richtext-ui-material3:0.20.0")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:0.20.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8") // User specified version
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Compose testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01")) // User specified version
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Mockito
    testImplementation("org.mockito:mockito-core:5.7.0") // User specified version

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1") // User specified version
}

// KSP arguments for Room schema location
// This block should be at the root of your build.gradle.kts file, not inside android {} or dependencies {}
tasks.withType<com.google.devtools.ksp.gradle.KspTask>().configureEach {
    // Check if the task is related to Room, though often applying to all KSP tasks is fine
    // if (!name.contains("Test", ignoreCase = true) ) { // Example to avoid test KSP tasks if needed
    kspArgs["room.schemaLocation"] = "$projectDir/schemas"
    // }
}
