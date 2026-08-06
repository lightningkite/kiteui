// by Claude
import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

// KMP currently doesn't disable iOS target and dependency resolution correctly when not on a mac.
// So we work around it on non mac machines with this check
val onMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        alias(libs.plugins.kotlin.cocoapods)
    }
    alias(libs.plugins.kotlin.plugin.serialization)
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
    if (onMac) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
    }
    js {
        browser()
    }
    jvm("jvmSsr")

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    if (onMac) {
        // We have to manually call this because the shortcut isn't available when plugin is not applied and gradle will
        // fail even behind the if check
        (this as ExtensionAware).extensions.configure<CocoapodsExtension>("cocoapods", {
            summary = "KiteUI Camera and Barcode Scanning Support"
            homepage = "https://github.com/lightningkite/kiteui"
            version = "1.0"
            ios.deploymentTarget = "14.0"
        })
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

        if (onMac) {
            // Opt in across the whole iOS hierarchy rather than on the native compilations. The
            // shared iosMain metadata compilation is not a KotlinNativeTarget compilation, so a
            // target-level opt-in leaves compileIosMainKotlinMetadata without it. Kotlin also
            // requires a source set's opt-ins to be a superset of those of the source sets it
            // depends on, so the leaf target source sets must be covered too, not just iosMain.
            matching { it.name.startsWith("ios") }.configureEach {
                languageSettings {
                    optIn("kotlinx.cinterop.BetaInteropApi")
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                }
            }
            val iosMain by getting
        }

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

lkLibrary(
    "lightningkite",
    "kiteui",
    mavenAutomaticRelease = project.findProperty("mavenAutomaticRelease") as? Boolean ?: false
) {
    description.set("KiteUI Camera and Barcode Scanning Support")
}
