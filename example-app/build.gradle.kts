import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
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
    alias(libs.plugins.composeCompiler)

    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.androidApplication)
    id("dev.opensavvy.vite.kotlin") version "DEV"
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
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                api(project(":library"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinx.coroutinesSwing)
            }
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

compose.desktop {
    application {
        mainClass = "$group.mppexampleapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "$group.mppexampleapp"
            packageVersion = "1.0.0"
        }
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.8.2"
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
