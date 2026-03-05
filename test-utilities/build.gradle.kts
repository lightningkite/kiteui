import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
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
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library"))
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest) // by Claude - for runTest in uiTest()
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

        val iosMain by getting {
            dependsOn(commonInteractiveMain)
            dependencies {
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
        }
    }
}

android {
    namespace = "com.lightningkite.kiteui.testing"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// Note: Publishing configuration disabled as it requires vanniktech publishing plugin
// Uncomment when publishing is needed:
lkLibrary("lightningkite", "kiteui") {
    description.set("KiteUI's testing companion.")
}