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
