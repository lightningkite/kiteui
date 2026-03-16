import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.androidLibrary)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    // Note: CocoaPods integration for lottie-ios is disabled due to cinterop commonizer
    // issues with Metal framework types. The iOS implementation uses a placeholder.
    // For production iOS apps, use native Swift Lottie integration directly.
    cocoapods {
        summary = "KiteUI Lottie Animation Support"
        homepage = "https://github.com/lightningkite/kiteui"
        version = "1.0"
        ios.deploymentTarget = "14.0"
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library"))
            }
        }

        val androidMain by getting {
            dependencies {
                api(libs.lottie)
            }
        }

        val iosMain by getting

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }
        val jsMain by getting {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(npm("lottie-web", "5.12.2"))
            }
        }
    }

    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain by getting {
            dependsOn(get("commonHtmlMain"))
        }
    }
}

android {
    namespace = "com.lightningkite.kiteui.lottie"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        coreLibraryDesugaring(libs.desugar.jdk.libs)
    }
}

lkLibrary("lightningkite", "kiteui-lottie") {
    description.set("KiteUI Lottie Animation Support")
}