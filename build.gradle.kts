
group = "com.lightningkite.kiteui"
version = "1.0-SNAPSHOT"

buildscript {
    val kotlinVersion:String by extra
    repositories {
        mavenLocal()
        maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.lightningkite:lk-gradle-helpers:1.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
        classpath("com.android.tools.build:gradle:8.5.2")
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