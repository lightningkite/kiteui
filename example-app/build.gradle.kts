import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.*
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.androidApplication)
    id("dev.opensavvy.vite.kotlin") version "0.6.0"
}
apply<KiteUiPlugin>()
configure<KiteUiPluginExtension> {
    this.packageName = "com.lightningkite.mppexampleapp"
    this.iosProjectRoot = project.file("../example-app-ios/KiteUI Example App")
}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

group = "com.lightningkite"
version = "1.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()

    jvm()
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        binaries.executable()
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library"))
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(commonHtmlMain)
        }
        val jsMain by getting {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(devNpm("webpack-bundle-analyzer", "4.10.2"))
            }
        }
    }

    cocoapods {
        // Required properties
        // Specify the required Pod version here. Otherwise, the Gradle project version is used.
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "14.0"

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "shared"

        framework {
            baseName = "shared"
            export(project(":library"))
//            embedBitcode(BitcodeEmbeddingMode.DISABLE)
//            podfile = project.file("../example-app-ios/Podfile")
        }
//        pod("Library") {
//            version = "1.0"
//            source = path(project.file("../library"))
//        }

        // Maps custom Xcode configuration to NativeBuildType
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }
}

android {
    namespace = "$group.mppexampleapp"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lightningkite.kiteuiexample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = project.version.toString()

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        coreLibraryDesugaring(libs.desugar.jdk.libs)
    }
}

fun env(name: String, profile: String) {
    tasks.create("deployWeb${name}Init", Exec::class.java) {
        group = "deploy"
        this.dependsOn("viteBuild")
        this.environment("AWS_PROFILE", "$profile")
        val props = Properties()
        props.entries.forEach {
            environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' '))
        }
        this.executable = "terraform"
        this.args("init")
        this.workingDir = file("terraform/$name")
    }
    tasks.create("deployWeb${name}", Exec::class.java) {
        group = "deploy"
        this.dependsOn("deployWeb${name}Init")
        this.environment("AWS_PROFILE", "$profile")
        val props = Properties()
        props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }
        this.executable = "terraform"
        this.args("apply", "-auto-approve")
        this.workingDir = file("terraform/$name")
    }
}
env("lk", "lk")

vite {
    server {
        this.host.set("0.0.0.0")
        this.port.set(8907)
    }
    publicDir.set("public")
}