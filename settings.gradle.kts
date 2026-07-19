rootProject.name = "meshcore"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    "meshcore-core",
    "meshcore-crypto",
    "meshcore-storage",
    "meshcore-network",
    "meshcore-discovery",
    "meshcore-routing",
    "meshcore-sync",
    "meshcore-api",
    "meshcore-cli",
    "meshcore-web",
    "meshcore-plugin",
    "meshcore-messenger",
    "meshcore-files",
    "meshcore-voice",
    "meshcore-video"
)
