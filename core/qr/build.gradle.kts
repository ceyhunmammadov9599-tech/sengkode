plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    // Pinned in Phase 0 (validated pin-set); wired in Phase 1 when the
    // encoder surface exists - no speculative dependency usage.
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
