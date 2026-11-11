enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
    }
    versionCatalogs {
        create("sa") { from("com.lightningkite.stack:service-abstractions-catalog:2026.11.11.0952") }
        create("ls") { from("com.lightningkite.stack:lightning-server-catalog:2026.11.11.0954") }
        create("rx") { from("com.lightningkite.stack:reactive-catalog:2026.11.11.0956") }
    }
}

rootProject.name = "kiteui"

include(":library")
include(":library-compose")
include(":library-lottie")
include(":library-camera")
include(":library-lottie-compose")
include(":library-camera-compose")
include(":library-map")
include(":example-app")
include(":example-app-desktop")
include(":example-app-android")
include(":gradle-plugin")
include(":test-utilities")
include(":test-server")
include(":ai-driver-server")
