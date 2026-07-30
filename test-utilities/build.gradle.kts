import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP currently doesn't disable iOS target and dependency resolution correctly when not on a mac.
// So we work around it on non mac machines with this check
val onMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)
val iosTargetOverride = false

val iosTarget = iosTargetOverride || onMac

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.dokka)
    signing
    alias(libs.plugins.vannitechPublishing)
}

dokka {
    // Dokka generates a new process managed by Gradle
    dokkaGeneratorIsolation = ProcessIsolation {
        // Configures heap size
        maxHeapSize = "4g"
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    if (iosTarget) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
    }
    js {
        browser()
    }
    // jvm("jvmDesktop") - Commented out due to Kotlin Multiplatform limitation:
    // Cannot declare multiple JVM targets (jvmDesktop + jvmSsr) in the same module.
    // JVM desktop testing support is implemented in src/jvmDesktopMain but cannot
    // be used until either:
    // 1. Kotlin supports multiple JVM targets, or
    // 2. We create a separate test-utilities-desktop module
    // For now, use androidUnitTest for comprehensive async testing validation.

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library"))
                implementation(kotlin("test"))
            }
        }

        // Interactive testing utilities for platforms with UI (Android, iOS, JS, JVM)
        // These utilities require actual UI rendering and don't work in SSR
        val commonInteractiveMain by creating {
            dependsOn(commonMain)
        }

        val androidMain by getting {
            dependsOn(commonInteractiveMain)
            dependencies {
                api(libs.junit)
                api(libs.robolectric)
                api(libs.roborazzi)
            }
        }

        if (iosTarget) {
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
            val iosMain by getting {
                dependsOn(commonInteractiveMain)
                dependencies {
                }
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(commonHtmlMain)
            dependsOn(commonInteractiveMain)
            dependencies {
            }
        }

        // jvmDesktopMain - Commented out because jvmDesktop target is disabled
        // See comment above for jvm("jvmDesktop")
        // val jvmDesktopMain by getting {
        //     dependsOn(commonInteractiveMain)
        //     dependencies {
        //     }
        // }
    }

    // JvmSsr target for server-side rendering
    // SSR doesn't have interactive UI, so it only gets commonMain and commonHtmlMain
    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain by getting {
            // Note: dependsOn(commonMain) is automatic from hierarchy template
            dependsOn(get("commonHtmlMain"))
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.lightningkite.kiteui.testing"
    compileSdk = 36
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

lkLibrary("lightningkite", "kiteui") {
    description.set("KiteUI's testing companion.")
}