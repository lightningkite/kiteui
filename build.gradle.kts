
group = "com.lightningkite.kiteui"
version = "1.0-SNAPSHOT"

buildscript {
    val kotlinVersion:String by extra
    repositories {
//        mavenLocal()
//        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.9.10")
        classpath("com.lightningkite:deploy-helpers:0.0.7")
        classpath("com.android.tools.build:gradle:7.4.2")
    }
}
allprojects {
    group = "com.lightningkite.kiteui"
    repositories {
        mavenLocal()
        maven("https://jitpack.io")
//        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
        google()
        mavenCentral()
    }
}
repositories {
    mavenLocal()
    maven("https://jitpack.io")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/releases/")
    google()
    mavenCentral()
}