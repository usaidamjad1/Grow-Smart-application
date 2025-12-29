plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.usaid.growsmartapplication"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.usaid.growsmartapplication"
        minSdk = 24
        targetSdk = 36
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.drawerlayout)
    implementation(libs.filament.android)
    implementation(libs.firebase.auth)
    implementation(libs.ui.text)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // For Guava ListenableFuture support
    implementation("com.google.guava:guava:31.0.1-android")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Required for Guava/Futures support in Java
    implementation("com.google.guava:guava:31.1-android")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.firebase:firebase-database:20.3.0")
// For Authentication
    implementation("com.google.firebase:firebase-auth:22.3.1")

}
