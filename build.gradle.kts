import com.lightningkite.deployhelpers.publishing
import com.lightningkite.deployhelpers.useGitBasedVersion
import com.lightningkite.deployhelpers.useLocalDependencies
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
        classpath(libs.lkGradleHelpers)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
        classpath(libs.gradle)
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