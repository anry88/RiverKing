plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val ktorVersion = "2.3.11"
val apiBaseUrl = (findProperty("RIVERKING_API_BASE_URL") as String?) ?: "http://10.0.2.2:8080"
val googleClientId = (findProperty("RIVERKING_GOOGLE_AUTH_CLIENT_ID") as String?) ?: ""

android {
    namespace = "com.riverking.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.riverking.mobile"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GOOGLE_AUTH_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("boolean", "GOOGLE_AUTH_ENABLED", googleClientId.isNotBlank().toString())
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            applicationIdSuffix = ".play"
            versionNameSuffix = "-play"
        }
        create("direct") {
            dimension = "distribution"
            applicationIdSuffix = ".direct"
            versionNameSuffix = "-direct"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.credentials:credentials:1.6.0-rc02")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-rc02")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
