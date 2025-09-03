import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

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
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.comLightningkiteTestingManual) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.vannitechPublishing) apply false
    alias(libs.plugins.dokka) apply false
}
plugins.withType(YarnPlugin::class.java) {
    the<YarnRootExtension>().yarnLockMismatchReport = YarnLockMismatchReport.NONE
    the<YarnRootExtension>().reportNewYarnLock = false
    the<YarnRootExtension>().yarnLockAutoReplace = true
}