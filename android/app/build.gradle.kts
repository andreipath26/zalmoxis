plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}
val updateKeystorePath = System.getenv("ZALMOXIS_KEYSTORE_PATH")
val updateStorePassword = System.getenv("ZALMOXIS_KEYSTORE_PASSWORD")
val updateKeyAlias = System.getenv("ZALMOXIS_KEY_ALIAS")
val updateKeyPassword = System.getenv("ZALMOXIS_KEY_PASSWORD")
val canSignUpdates = listOf(
    updateKeystorePath, updateStorePassword, updateKeyAlias, updateKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.zalmoxis.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    defaultConfig {
        applicationId = "com.zalmoxis.app"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()?.let { 100_000 + it } ?: 1
        versionName = "0.1.0"
    }
    if (canSignUpdates) {
        signingConfigs {
            create("updates") {
                storeFile = file(requireNotNull(updateKeystorePath))
                storePassword = updateStorePassword
                keyAlias = updateKeyAlias
                keyPassword = updateKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (canSignUpdates) signingConfig = signingConfigs.getByName("updates")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.18.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
