import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.kjsplain)
    alias(libs.plugins.kfc)
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

    // The installable Android app lives in :example-app-android - AGP 9 doesn't allow com.android.application in a
    // KMP module.
    android {
        // Must match the KiteUI packageName above: the generated Resources.android.kt uses an unqualified R.
        namespace = "$group.mppexampleapp"
        testNamespace = "$group.mppexampleapp.test"
        compileSdk = 36
        minSdk = 24  // library-skia (Skiko) requires API 24+
        enableCoreLibraryDesugaring = true
        androidResources { enable = true }
        withHostTest {
            isIncludeAndroidResources = true
            // Matches :library - see the same setting there for why. Kept in step deliberately:
            // both modules compile commonTest sources against the same stubbed android.jar, so a
            // test that logs must not be writable in one module and impossible in the other.
            isReturnDefaultValues = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    if (iosEnabled) {
        listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
            target.binaries.framework {
                baseName = "shared"
                export(project(":library"))
            }
        }
    }

    js {
        binaries.executable()
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
                api(project(":library-lottie"))
                api(project(":library-camera"))
                api(project(":library-map"))
                api("com.lightningkite.services:data-shared:1.3.0")
                api("com.lightningkite.services:database-shared:1.3.0")
            }
        }

        val commonHtmlMain = create("commonHtmlMain") {
            dependsOn(commonMain)
        }

        val jsMain = getByName("jsMain") {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(devNpm("webpack-bundle-analyzer", "4.10.2"))
            }
        }

        val androidMain = getByName("androidMain") {
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

        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":test-utilities"))
            }
        }

        val commonInteractiveTest = create("commonInteractiveTest") {
            dependsOn(commonTest)
        }
        val jsTest = getByName("jsTest") {
            dependsOn(commonInteractiveTest)
        }
        val androidHostTest = getByName("androidHostTest") {
            dependsOn(commonInteractiveTest)
        }
        matching { it.name == "iosTest" }.configureEach {
            dependsOn(commonInteractiveTest)
        }
    }

    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain = getByName("jvmSsrMain") {
            dependsOn(get("commonHtmlMain"))
            dependencies {
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.kotlinx.coroutines.swing) // Provides Dispatchers.Main for JVM
            }
        }
    }
//    jvm("jvmSwing")
//    sourceSets {
//        val jvmSwingMain = getByName("jvmSwingMain") {
//        }
//    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

// Serve the brand mark at /logo.svg (the web favicon) straight from the designer's file at the
// repository root rather than a copy of it, so there is one logo to update.
tasks.named<ProcessResources>("jsProcessResources") {
    from(rootProject.file("logo.svg"))
}

fun env(name: String, profile: String) {
    tasks.register("deployWeb${name}Init", Exec::class.java) {
        group = "deploy"
        this.dependsOn("jsBundleProduction")
        this.environment("AWS_PROFILE", "$profile")
        val props = Properties()
        props.entries.forEach {
            environment(it.key.toString().trim('"', ' '), it.value.toString().trim('"', ' '))
        }
        this.executable = "terraform"
        this.args("init")
        this.workingDir = file("terraform/$name")
    }
    tasks.register("deployWeb${name}", Exec::class.java) {
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

