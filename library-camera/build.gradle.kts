// by Claude
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.androidLibrary)
    signing
//    alias(libs.plugins.vannitechPublishing)
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
    jvm("jvmSsr")

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    cocoapods {
        summary = "KiteUI Camera and Barcode Scanning Support"
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
                // CameraX
                api("androidx.camera:camera-core:1.4.2")
                api("androidx.camera:camera-camera2:1.4.2")
                api("androidx.camera:camera-lifecycle:1.4.2")
                api("androidx.camera:camera-view:1.4.2")
                // ML Kit Barcode Scanning
                api("com.google.mlkit:barcode-scanning:17.3.0")
            }
        }

        val iosMain by getting

        val jsMain by getting {
            dependencies {
                // Spec-compliant BarcodeDetector polyfill using ZXing WASM
                implementation(npm("barcode-detector", "3.0.8"))
            }
        }

        val jvmSsrMain by getting
    }
}

android {
    namespace = "com.lightningkite.kiteui.camera"
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

//lkLibrary("lightningkite", "kiteui-camera") {
//    description.set("KiteUI Camera and Barcode Scanning Support")
//}
