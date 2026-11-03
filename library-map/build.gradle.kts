import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.androidKmpLibrary)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kjsplain)
}

val maplibreWebVersion = "6.12.0"
val minMapLibreIosVersion = "6.0.0"

val generateMapLibreLayerTypes = tasks.register("generateMapLibreLayerTypes") {
    description = "Regenerate the MapLibre layer types from the MapLibre style spec"

    val specFile = file("maplibre-style-spec/v8.json")
    val cocoaConventionsFile = file("maplibre-style-spec/style-spec-cocoa-conventions-v8.json")
    val packageName = "com.lightningkite.kiteui.map"
    val webVersion = maplibreWebVersion
    val androidVersion = libs.versions.maplibreAndroidCore.get()
    val iosVersion = minMapLibreIosVersion
    val outputDir = layout.buildDirectory.dir("generated/maplibreLayers/kotlin")

    inputs.files(specFile, cocoaConventionsFile).withPathSensitivity(PathSensitivity.NONE)
    inputs.property("packageName", packageName)
    inputs.property("versions", listOf(webVersion, androidVersion, iosVersion))
    outputs.dir(outputDir)
    outputs.cacheIf { true }

    doLast {
        generateMapLibreLayerTypes(
            specFile, cocoaConventionsFile, packageName,
            webVersion, androidVersion, iosVersion,
            outputDir.get().asFile,
        )
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.lightningkite.kiteui.map"
        compileSdk = 36
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
        swiftPMDependencies {
            swiftPackage(
                url = url("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                version = from(minMapLibreIosVersion),
                products = listOf(product("MapLibre")),
            )
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
            kotlin.srcDir(generateMapLibreLayerTypes)
            dependencies {
                api(project(":library"))
                api("com.lightningkite.services:data-shared:1.3.0")
                api("com.lightningkite.services:database-shared:1.3.0")
                api(libs.spatialk.geojson)
                api(libs.kotlinx.serialization.json)
            }
        }

        val androidMain = getByName("androidMain") {
            dependencies {
                implementation(libs.maplibre.android.core)
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
                implementation(npm("maplibre-gl", maplibreWebVersion))
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
