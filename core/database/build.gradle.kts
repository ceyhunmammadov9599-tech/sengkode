plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.hjinlabs.sengkode.core.database"
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
}

dependencies {
    api(project(":core:model"))
    // Room preparation (validated pin-set). The @Database surface is a
    // Phase 3 deliverable; runtime + compiler are staged now so the
    // wiring is proven from the first schema commit.
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
}
