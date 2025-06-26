import com.lightningkite.deployhelpers.publishing
import com.lightningkite.deployhelpers.useGitBasedVersion
import com.lightningkite.deployhelpers.useLocalDependencies
group = "com.lightningkite.kiteui"
version = "1.0-SNAPSHOT"

buildscript {
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath(libs.lkGradleHelpers)
//        classpath(libs.androidGradle)
    }
}
allprojects {
    group = "com.lightningkite.kiteui"
    useLocalDependencies()
    useGitBasedVersion()
    publishing()
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        maven("https://jitpack.io")
        google()
        mavenCentral()
    }
}
plugins {

    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.comLightningkiteTestingManual) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}