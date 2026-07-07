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
//include(":library-swing")
include(":library-lottie")
//include(":library-lottie-swing")
include(":library-camera")
//include(":library-camera-swing")
include(":example-app")
//include(":example-app-swing")
// The physical directory is build-companion; the project name (and Maven artifactId) is kiteui-build.
include(":kiteui-build")
project(":kiteui-build").projectDir = file("build-companion")
include(":gradle-plugin")
include(":test-utilities")
include(":ai-driver-server")
