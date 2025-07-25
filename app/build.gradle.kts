plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id ("kotlin-parcelize")
}

kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.gdd.rankingfilter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gdd.rankingfilter"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    implementation("com.intuit.ssp:ssp-android:1.1.1")
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    //Navigation
    val navigation_version = "2.9.0"
    implementation("androidx.navigation:navigation-fragment:$navigation_version")
    implementation("androidx.navigation:navigation-ui:$navigation_version")
    // DI
    implementation("com.google.dagger:hilt-android:2.56.2")
    kapt("com.google.dagger:hilt-android-compiler:2.56.2")
    // Epoxy
    implementation("com.airbnb.android:epoxy:5.1.4")
    kapt("com.airbnb.android:epoxy-processor:5.1.4")
    implementation("com.airbnb.android:epoxy-databinding:5.1.4")
    // Retrofit 3.0.0 + Gson converter
    implementation("com.squareup.retrofit2:retrofit:3.0.0")         
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    // OkHttp 4.12.0 + logger
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Coroutine
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Gson
    implementation ("com.google.code.gson:gson:2.13.1")
    // Media3
    implementation("androidx.media3:media3-exoplayer:1.6.0")
    implementation("androidx.media3:media3-ui:1.6.0")
    implementation("androidx.media3:media3-database:1.6.0")
    implementation("androidx.media3:media3-datasource:1.6.0")
    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // ViewModel + LiveData
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")

    val camerax_version = "1.4.2"
    implementation ("androidx.camera:camera-core:$camerax_version")
    implementation ("androidx.camera:camera-camera2:$camerax_version")
    implementation ("androidx.camera:camera-lifecycle:$camerax_version")
    implementation ("androidx.camera:camera-video:$camerax_version")
    implementation ("androidx.camera:camera-view:$camerax_version")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}