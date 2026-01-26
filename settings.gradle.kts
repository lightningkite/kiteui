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
include(":library-swing")
include(":library-camera")
include(":example-app")
include(":example-app-swing")
include(":gradle-plugin")
include(":test-utilities")
