plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    // Pure Kotlin by design: the styled-render geometry must be
    // verifiable on the JVM with the same round-trip decode pipeline
    // as the Phase 1 engine - styling is exactly where scannability
    // dies, so it is exactly where tests must run without an emulator.
    testImplementation(project(":core:qr"))
    testImplementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
