import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("com.google.gms.google-services")
}

// 1. Local Properties load karne ka code
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.usaid.growsmartapplication"
    compileSdk = 36 // Stable version

    defaultConfig {
        applicationId = "com.usaid.growsmartapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 2. Key fetch karke BuildConfig mein daalna
        val mapsKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsKey\"")
    }

    buildFeatures {
        buildConfig = true // Isse BuildConfig generate hogi
        viewBinding = true
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
}

// 3. Secrets Plugin Settings
secrets {
    defaultPropertiesFileName = "local.properties"
    ignoreList.add("sdk.dir")
    ignoreList.add("datadir")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.drawerlayout)
    implementation(libs.firebase.auth)
    implementation(libs.ui.text)

    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}