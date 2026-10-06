import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
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
            jvmTarget.set(JvmTarget.JVM_11)
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
            pod("MapLibre") {
                version = "~> 6.0"
            }
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
                api("com.lightningkite.services:data-shared:1.3.0-prerelease-86-c4aefa99")
            }
        }

        val androidMain = getByName("androidMain") {
            dependencies {
                implementation("androidx.compose.ui:ui:1.12.1")
                implementation("androidx.compose.foundation:foundation:1.12.1")

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

composeCompiler {
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
}

if (iosEnabled) {
    afterEvaluate {
        val xcframeworkDir = file("build/cocoapods/synthetic/ios/Pods/MapLibre/MapLibre.xcframework")
        val defFile = file("build/cocoapods/defs/MapLibre.def")

        val patchMapLibreDef by tasks.registering {
            dependsOn("generateDefMapLibre", "podInstallSyntheticIos")
            doLast {
                if (!defFile.exists()) return@doLast
                val content = defFile.readText()
                if ("-F" in content) return@doLast // already patched
                val arm64 = xcframeworkDir.resolve("ios-arm64").absolutePath
                val sim = xcframeworkDir.resolve("ios-arm64_x86_64-simulator").absolutePath
                defFile.writeText(
                    content.replace(
                        "linkerOpts = -framework MapLibre",
                        "linkerOpts.ios_arm64 = -framework MapLibre -F$arm64\n" +
                        "linkerOpts.ios_simulator_arm64 = -framework MapLibre -F$sim\n" +
                        "linkerOpts.ios_x64 = -framework MapLibre -F$sim"
                    )
                )
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.CInteropProcess>()
            .matching { it.name.contains("MapLibre") }
            .configureEach { dependsOn(patchMapLibreDef) }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

lkLibrary("lightningkite", "kiteui-map") {
    description.set("KiteUI Map Support")
}
