
group = "com.lightningkite.kiteui"
version = "1.0-SNAPSHOT"

plugins {
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kiteUI) apply false
    alias(libs.plugins.kotlinCocoapods) apply false
//    alias(libs.plugins.lkDeployHelpers)
}

buildscript {
    dependencies { classpath(libs.lkDeployHelpers) }
}

//kotlin("plugin.serialization") version kotlinVersion
//id("com.google.devtools.ksp") version kspVersion
//id("com.lightningkite.kiteui") version "main-SNAPSHOT"

//buildscript {
//    val kotlinVersion:String by extra
//    repositories {
//        mavenLocal()
//        google()
//        mavenCentral()
//        maven("https://jitpack.io")
//    }
//    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
//        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
//        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
//        classpath("com.lightningkite:deploy-helpers:0.0.7")
//        classpath("com.android.tools.build:gradle:7.4.2")
//    }
//}
//allprojects {
//    group = "com.lightningkite.kiteui"
//    repositories {
//        mavenLocal()
////        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
//        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
//        google()
//        mavenCentral()
//    }
//}
//repositories {
//    mavenLocal()
//    maven("https://jitpack.io")
//    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
//    maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
//    google()
//    mavenCentral()
//}