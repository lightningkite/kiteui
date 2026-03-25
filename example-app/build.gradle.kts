import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.util.*

// KMP currently doesn't disable iOS target and dependency resolution correctly when not on a mac.
// So we work around it on non mac machines with this check
val onMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        alias(libs.plugins.kotlinCocoapods)
    }
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.roborazzi)
    id("dev.opensavvy.vite.kotlin") version "0.6.0"
}
apply<KiteUiPlugin>()
configure<KiteUiPluginExtension> {
    this.packageName = "com.lightningkite.mppexampleapp"
    this.iosProjectRoot = project.file("../example-app-ios/KiteUI Example App")
}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<YarnRootExtension>().yarnLockMismatchReport =
        YarnLockMismatchReport.WARNING // NONE | FAIL
    rootProject.the<YarnRootExtension>().reportNewYarnLock = true
    rootProject.the<YarnRootExtension>().yarnLockAutoReplace = true
}

group = "com.lightningkite"
version = "1.0-SNAPSHOT"

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    if (onMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
    js(IR) {
        binaries.executable()
        browser()
    }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":library"))
                api(project(":library-lottie"))
                api(project(":library-camera"))
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }

        val jsMain by getting {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(devNpm("webpack-bundle-analyzer", "4.10.2"))
            }
        }

        val androidMain by getting {
        }

        if (onMac) {
            val iosMain by getting {
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":test-utilities"))
            }
        }

        val commonInteractiveTest by creating() {
            dependsOn(commonTest)
        }
        val jsTest by getting {
            dependsOn(commonInteractiveTest)
        }
        val androidUnitTest by getting {
            dependsOn(commonInteractiveTest)
        }
        if (onMac) {
            val iosTest by getting {
                dependsOn(commonInteractiveTest)
            }
        }
    }

    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain by getting {
            dependsOn(get("commonHtmlMain"))
            dependencies {
                implementation(libs.ktorServerCore)
                implementation(libs.ktorServerNetty)
                implementation(libs.kotlinxCoroutinesSwing) // Provides Dispatchers.Main for JVM
            }
        }
    }
//    jvm("jvmSwing")
//    sourceSets {
//        val jvmSwingMain by getting {
//        }
//    }

    if (onMac) {
        // We have to manually call this because the shortcut isn't available when plugin is not applied and gradle will
        // fail even behind the if check
        (this as ExtensionAware).extensions.configure<CocoapodsExtension>("cocoapods", {
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
        })
    }
}

android {
    namespace = "$group.mppexampleapp"
    testNamespace = "$group.mppexampleapp.test"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lightningkite.kiteuiexample"
        minSdk = 24  // library-skia (Skiko) requires API 24+
        targetSdk = 36
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    dependencies {
        coreLibraryDesugaring(libs.desugar.jdk.libs)
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

// SSR Server run task (runs server mode by default)
tasks.register<JavaExec>("ssrServerRun") {
    group = "application"
    description = "Run the SSR server (default) or prerender with --args=\"prerender <outputDir>\""
    mainClass.set("com.lightningkite.mppexampleapp.SsrPrerenderKt")
    val jvmSsrCompilation = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("jvmSsr")
        .compilations.getByName("main")
    classpath = files(
        jvmSsrCompilation.output.allOutputs,
        jvmSsrCompilation.runtimeDependencyFiles
    )
    dependsOn("jvmSsrJar")
}

// Convenience task for prerendering
tasks.register<JavaExec>("ssrPrerender") {
    group = "application"
    description = "Prerender all SSR pages to ./local/prerendered"
    mainClass.set("com.lightningkite.mppexampleapp.SsrPrerenderKt")
    args = listOf("prerender", "${project.rootDir}/local/prerendered")
    val jvmSsrCompilation = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("jvmSsr")
        .compilations.getByName("main")
    classpath = files(
        jvmSsrCompilation.output.allOutputs,
        jvmSsrCompilation.runtimeDependencyFiles
    )
    dependsOn("jvmSsrJar")
}
vite {
    publicDir.set("public")
    base.set("/")
    server {
        port.set(3000)
    }
}


