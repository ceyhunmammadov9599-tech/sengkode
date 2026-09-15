plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    // ZXing core: pure-Java artifact, no Android/network surface.
    // Its QRCodeReader is used by the round-trip scannability tests.
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
