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
    alias(libs.plugins.kjsplain)
    alias(libs.plugins.composeKotlin)
}

// Without iOS targets CocoaPods has no framework to build, and its generateDummyFramework task fails IDE sync.
if (iosEnabled) apply(plugin = libs.plugins.kotlin.cocoapods.get().pluginId)

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.lightningkite.kiteui.map"
        compileSdk = 37
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
        // No `cocoapods {}` accessor, since the plugin isn't applied in the plugins block.
        (this as ExtensionAware).extensions.configure<CocoapodsExtension> {
            summary = "KiteUI Map Support"
            homepage = "https://github.com/lightningkite/kiteui"
            version = "1.0"
            ios.deploymentTarget = "14.0"
        }
    }
    js { browser() }
    jvm("jvmSsr")

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
                implementation("androidx.compose.ui:ui:1.8.2")

                implementation(libs.maplibre.compose.android)
                runtimeOnly("org.maplibre.compose:maplibre-compose-runtime-vulkan-android:0.19.0")
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

        val jsMain = getByName("jsMain") {
            dependencies {
                implementation(npm("maplibre-gl", "6.12.0"))
            }
        }

        val jvmSsrMain = getByName("jvmSsrMain")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

lkLibrary("lightningkite", "kiteui-map") {
    description.set("KiteUI Map Support")
}
