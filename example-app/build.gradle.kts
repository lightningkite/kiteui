import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import java.util.*
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    id("com.android.application")
    id("dev.opensavvy.vite.kotlin") version "0.4.0"
}
apply<KiteUiPlugin>()

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

group = "com.lightningkite"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

vite {
    plugin(
        packageName = "vite-bundle-analyzer",
        "analyzer",
        "0.17.1",
        configuration = """
        {
        "analyzerMode": "static"
        }
        """.trimIndent(),
        isNamedExport = true
    )
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()

    jvm()
    androidTarget {
        this.compilerOptions {
            this.jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
//    ios()
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "library"
//        }
//    }
    js {
        binaries.executable()
        browser()
//        useEsModules()
    }
//    wasmJs {
//        binaries.executable()
//        browser {
//            commonWebpackConfig {
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    // Uncomment and configure this if you want to open a browser different from the system default
//                    // open = mapOf(
//                    //     "app" to mapOf(
//                    //         "name" to "google chrome"
//                    //     )
//                    // )
//
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        add(project.rootDir.path)
//                    }
//                }
//            }
//
//            // Uncomment the next line to apply Binaryen and get optimized wasm binaries
//             applyBinaryen()
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library"))
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(commonHtmlMain)
        }
        val jsMain by getting {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(devNpm("webpack-bundle-analyzer", "4.10.2"))
            }
        }
    }

    cocoapods {
        // Required properties
        // Specify the required Pod version here. Otherwise, the Gradle project version is used.
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "14.0"

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "shared"

        framework {
            baseName = "shared"
            export(project(":library"))
            embedBitcode(BitcodeEmbeddingMode.BITCODE)
//            embedBitcode(BitcodeEmbeddingMode.DISABLE)
//            podfile = project.file("../example-app-ios/Podfile")
        }
//        pod("Library") {
//            version = "1.0"
//            source = path(project.file("../library"))
//        }

        // Maps custom Xcode configuration to NativeBuildType
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }
}

configure<KiteUiPluginExtension> {
    this.packageName = "com.lightningkite.mppexampleapp"
    this.iosProjectRoot = project.file("../example-app-ios/KiteUI Example App")
}

android {
    namespace = "$group.mppexampleapp"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lightningkite.kiteuiexample"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = project.version.toString()

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    }
}

kotlin {
    targets
        .matching { it is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget }
        .configureEach {
            this as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

            compilations.getByName("main") {
                this.kotlinOptions {
//                    this.freeCompilerArgs += "-Xruntime-logs=gc=info"
//                    this.freeCompilerArgs += "-Xallocator=mimalloc"
                }
            }
        }
}

fun env(name: String, profile: String) {
    tasks.create("deployWeb${name}Init", Exec::class.java) {
        group = "deploy"
        this.dependsOn("viteBuild")
        this.environment("AWS_PROFILE", "$profile")
        val props = Properties()
        props.entries.forEach {
            environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' '))
        }
        this.executable = "terraform"
        this.args("init")
        this.workingDir = file("terraform/$name")
    }
    tasks.create("deployWeb${name}", Exec::class.java) {
        group = "deploy"
        this.dependsOn("deployWeb${name}Init")
        this.environment("AWS_PROFILE", "$profile")
        val props = Properties()
        props.entries.forEach { environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' ')) }
        this.executable = "terraform"
        this.args("apply", "-auto-approve")
        this.workingDir = file("terraform/$name")
    }
}
env("lk", "lk")
//tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
//    this.args
//}
