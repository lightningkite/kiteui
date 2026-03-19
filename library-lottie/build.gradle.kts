import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

// KMP currently doesn't disable iOS target and dependency resolution correctly when not on a mac.
// So we work around it on non mac machines with this check
val onMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        alias(libs.plugins.kotlinCocoapods)
    }
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
    if (onMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
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
    if (onMac) {
        // We have to manually call this because the shortcut isn't available when plugin is not applied and gradle will
        // fail even behind the if check
        (this as ExtensionAware).extensions.configure<CocoapodsExtension>("cocoapods", {
            summary = "KiteUI Lottie Animation Support"
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
                api(libs.lottie)
            }
        }

        if (onMac) {
            val iosMain by getting
        }

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
