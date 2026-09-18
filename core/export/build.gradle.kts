plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hjinlabs.sengkode.core.export"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(project(":core:model"))
    api(project(":core:style"))
    api(project(":core:qr"))
    implementation(libs.core.ktx)
    implementation(libs.zxing.core)
    // Phase 1 pipeline (Bitmap/PNG/MediaStore/FileProvider) is plain
    // android.graphics - no Compose dependency by design, so the export
    // pipeline is reusable from any surface (UI, batch, future headless
    // mode). The Compose render seam, if ever needed, is a Phase 4
    // decision documented in the Technical Validation Report.
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
}
