import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.cocoapods) apply false
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.androidKmpLibrary)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

// Without iOS targets CocoaPods has no framework to build, and its generateDummyFramework task fails IDE sync.
if (iosEnabled) apply(plugin = libs.plugins.kotlin.cocoapods.get().pluginId)

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.lightningkite.kiteui.lottie"
        compileSdk = 36
        minSdk = 21
        enableCoreLibraryDesugaring = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    if (iosEnabled) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
        // Note: CocoaPods integration for lottie-ios is disabled due to cinterop commonizer
        // issues with Metal framework types. The iOS implementation uses a placeholder.
        // For production iOS apps, use native Swift Lottie integration directly.
        // No `cocoapods {}` accessor, since the plugin isn't applied in the plugins block.
        (this as ExtensionAware).extensions.configure<CocoapodsExtension> {
            summary = "KiteUI Lottie Animation Support"
            homepage = "https://github.com/lightningkite/kiteui"
            version = "1.0"
            ios.deploymentTarget = "14.0"
        }
    }

    js {
        browser()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                api(project(":library"))
            }
        }

        val androidMain = getByName("androidMain") {
            dependencies {
                api(libs.lottie)
            }
        }

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

        val commonHtmlMain = create("commonHtmlMain") {
            dependsOn(commonMain)
        }
        val jsMain = getByName("jsMain") {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(npm("lottie-web", "5.12.2"))
            }
        }
    }

    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain = getByName("jvmSsrMain") {
            dependsOn(get("commonHtmlMain"))
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

lkLibrary("lightningkite", "kiteui-lottie") {
    description.set("KiteUI Lottie Animation Support")
}
