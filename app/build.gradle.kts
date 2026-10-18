plugins {
    id("com.android.application")
    kotlin("android")
}

// Upload signing is injected from the environment only (CI secrets).
// Builds without these variables stay unsigned; no keystore or password
// is ever stored in this repo.
val uploadStoreFile = System.getenv("PIPER_UPLOAD_STORE_FILE")
val hasUploadSigning = !uploadStoreFile.isNullOrBlank()

android {
    namespace = "dev.ihorshevchuk.piper"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.ihorshevchuk.piper"
        minSdk = 26
        // Play requires new apps to target API 36 (Android 16).
        targetSdk = 36
        versionCode = System.getenv("PIPER_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        if (hasUploadSigning) {
            create("upload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = System.getenv("PIPER_UPLOAD_STORE_PASSWORD")
                keyAlias = System.getenv("PIPER_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("PIPER_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("upload")
        }
    }
}

dependencies {
    // System TTS service module (this repo).
    implementation(project(":tts-service"))
    // Voice catalog + download manager + voice list UI (this repo).
    implementation(project(":voice-download"))
    // Resolved to the sibling piper-kotlin repo via the composite build
    // in settings.gradle.kts (no publishing step needed).
    implementation("dev.ihorshevchuk.piper:piper-engine")
    implementation("dev.ihorshevchuk.piper:piper-utils")
    implementation("dev.ihorshevchuk.piper:piper-player")
    // Activity Result API (voice file import).
    implementation("androidx.activity:activity:1.9.3")
    // FileProvider for sharing exported WAV samples.
    implementation("androidx.core:core:1.13.1")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for JVM unit tests: android.jar only ships stubs that
    // throw, so JSON parsing paths would silently hit their fallbacks.
    testImplementation("org.json:json:20240303")
}
