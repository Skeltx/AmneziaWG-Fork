plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.amneziawg.fork"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.amneziawg.fork"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,LICENSE.txt,NOTICE,NOTICE.txt}"
        }
    }
}

dependencies {
    // Core Android
    val coreKtx = "1.12.0"
    val activityCompose = "1.9.0"
    val lifecycle = "2.7.0"
    val room = "2.6.1"
    val material3 = "1.2.1"
    val navigation = "2.7.7"
    val hilt = "2.48"
    val kotlinxCoroutines = "1.7.3"
    val kotlinxSerialization = "1.6.3"
    val okhttp = "4.12.0"
    val retrofit = "2.11.0"
    val coil = "2.6.0"
    val datastore = "1.0.0"
    val biometric = "1.2.0-alpha03"
    val workManager = "2.9.0"

    // AndroidX Core
    implementation("androidx.core:core-ktx:$coreKtx")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:$activityCompose")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")

    // Material3
    implementation("androidx.compose.material3:material3:$material3")
    implementation("androidx.compose.material3:material3-window-size-class:$material3")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navigation")
    implementation("androidx.navigation:navigation-fragment-ktx:$navigation")
    implementation("androidx.navigation:navigation-ui-ktx:$navigation")

    // Room Database
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:$hilt")
    ksp("com.google.dagger:hilt-compiler:$hilt")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$kotlinxCoroutines")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerialization")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerialization")

    // Network
    implementation("com.squareup.okhttp3:okhttp:$okhttp")
    implementation("com.squareup.retrofit2:retrofit:$retrofit")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:$retrofit")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp")

    // Image loading
    implementation("io.coil-kt:coil-compose:$coil")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:$datastore")
    implementation("androidx.datastore:datastore-core:$datastore")

    // Biometric
    implementation("androidx.biometric:biometric-ktx:$biometric")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:$workManager")

    // WireGuard
    implementation("com.wireguard.android:wireguard-android:1.0.20240115")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutines")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.6")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.6.6")
}