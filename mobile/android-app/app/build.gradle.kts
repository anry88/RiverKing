plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val ktorVersion = "2.3.11"
val canonicalApplicationId = "com.riverking.mobile"
// Android Studio local runs keep flavor/build-type suffixes so debug and ad-hoc release
// installs do not collide with the signed store package on the same device.
val useCanonicalApplicationId = (findProperty("RIVERKING_CANONICAL_APPLICATION_ID") as String?)
    ?.toBooleanStrictOrNull()
    ?: false
val apiBaseUrl = (findProperty("RIVERKING_API_BASE_URL") as String?)
    ?: "https://v759468.hosted-by-vdsina.com"
val publicWebUrl = ((findProperty("RIVERKING_PUBLIC_WEB_URL") as String?) ?: apiBaseUrl).trimEnd('/')
val googleClientId = (findProperty("RIVERKING_GOOGLE_AUTH_CLIENT_ID") as String?) ?: ""
val versionCodeValue = (findProperty("RIVERKING_VERSION_CODE") as String?)?.toIntOrNull() ?: 1
val versionNameValue = (findProperty("RIVERKING_VERSION_NAME") as String?) ?: "0.1.0"
val itchProjectUrl = (findProperty("RIVERKING_ITCH_PROJECT_URL") as String?)
    ?.takeIf { it.isNotBlank() }
    ?: "$publicWebUrl/support"
val playStoreUrl = (findProperty("RIVERKING_PLAY_STORE_URL") as String?)
    ?.takeIf { it.isNotBlank() }
    ?: "https://play.google.com/store/apps/details?id=$canonicalApplicationId"
val supportUrl = (findProperty("RIVERKING_SUPPORT_URL") as String?)
    ?.takeIf { it.isNotBlank() }
    ?: "$publicWebUrl/support"
val privacyPolicyUrl = (findProperty("RIVERKING_PRIVACY_POLICY_URL") as String?)
    ?.takeIf { it.isNotBlank() }
    ?: "$publicWebUrl/privacy"
val accountDeletionUrl = (findProperty("RIVERKING_ACCOUNT_DELETION_URL") as String?)
    ?.takeIf { it.isNotBlank() }
    ?: "$publicWebUrl/account/delete"
val signingStoreFile = findProperty("RIVERKING_SIGNING_STORE_FILE") as String?
val signingStorePassword = findProperty("RIVERKING_SIGNING_STORE_PASSWORD") as String?
val signingKeyAlias = findProperty("RIVERKING_SIGNING_KEY_ALIAS") as String?
val signingKeyPassword = findProperty("RIVERKING_SIGNING_KEY_PASSWORD") as String?
val hasReleaseSigning = listOf(
    signingStoreFile,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = canonicalApplicationId
    compileSdk = 35

    defaultConfig {
        applicationId = canonicalApplicationId
        minSdk = 28
        targetSdk = 35
        versionCode = versionCodeValue
        versionName = versionNameValue

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GOOGLE_AUTH_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("boolean", "GOOGLE_AUTH_ENABLED", googleClientId.isNotBlank().toString())
        buildConfigField("String", "ITCH_PROJECT_URL", "\"$itchProjectUrl\"")
        buildConfigField("String", "PLAY_STORE_URL", "\"$playStoreUrl\"")
        buildConfigField("String", "SUPPORT_URL", "\"$supportUrl\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyPolicyUrl\"")
        buildConfigField("String", "ACCOUNT_DELETION_URL", "\"$accountDeletionUrl\"")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            if (!useCanonicalApplicationId) {
                applicationIdSuffix = ".play"
            }
            versionNameSuffix = "-play"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"play\"")
        }
        create("direct") {
            dimension = "distribution"
            if (!useCanonicalApplicationId) {
                applicationIdSuffix = ".direct"
            }
            versionNameSuffix = "-direct"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"direct\"")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(signingStoreFile))
                storePassword = requireNotNull(signingStorePassword)
                keyAlias = requireNotNull(signingKeyAlias)
                keyPassword = requireNotNull(signingKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            if (!useCanonicalApplicationId) {
                applicationIdSuffix = ".local"
            }
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            installation {
                enableBaselineProfile = false
            }
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.credentials:credentials:1.6.0-rc02")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-rc02")
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.android.billingclient:billing-ktx:7.0.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
