pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "sengkode"
include(":app")
include(":core:model")
include(":core:qr")
include(":core:database")
include(":core:export")
include(":core:designsystem")
include(":feature:generator")
include(":feature:history")
include(":feature:templates")
