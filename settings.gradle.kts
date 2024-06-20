enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencyResolutionManagement {
        repositories {
            mavenLocal()
            google()
            mavenCentral()
            maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
            maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
            maven("https://jitpack.io")
        }
    }
}

rootProject.name = "kiteui"

include(":library")
include(":example-app")
include(":processor")
include(":gradle-plugin")
include(":example-app-android")
