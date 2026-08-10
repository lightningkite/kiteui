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
include(":library-lottie")
include(":library-camera")
include(":example-app")
include(":gradle-plugin")
include(":test-utilities")
include(":ai-driver-server")
