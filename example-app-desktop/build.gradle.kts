import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
}

// example-app on the Compose desktop backend.  Kotlin allows one JVM target per module and example-app's is
// jvmSsr, so this module compiles example-app's common code itself.
val exampleApp = project(":example-app").projectDir

apply<KiteUiPlugin>()
configure<KiteUiPluginExtension> {
    packageName = "com.lightningkite.mppexampleapp"
    iosProjectRoot = file("build/no-ios") // required by the plugin; this module has no iOS target
    commonMainDirectory = exampleApp.resolve("src/commonMain")
}

// Same as in library-compose: Compose's desktop runtime ships Skia natives per OS/arch.
val hostComposeRuntime = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "arm64" else "x64"
    val osName = when {
        os.startsWith("mac") -> "macos"
        os.startsWith("windows") -> "windows"
        else -> "linux"
    }
    "org.jetbrains.compose.desktop:desktop-jvm-$osName-$arch:${libs.versions.composeMultiplatform.get()}"
}

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir("$exampleApp/src/commonMain/kotlin")
            dependencies {
                api(project(":library-compose"))
                api(project(":library-lottie-compose"))
                api(project(":library-camera-compose"))
            }
        }
        getByName("jvmMain") {
            dependencies {
                implementation(hostComposeRuntime)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
            }
        }
    }
}

// The plugin's generated JVM resources refer to files as `common/<path>` on the classpath.
tasks.named<ProcessResources>("jvmProcessResources") {
    from("$exampleApp/src/commonMain/resources") { into("common") }
}

tasks.named<Test>("jvmTest") {
    systemProperty("java.awt.headless", "true")
}

tasks.register<JavaExec>("run") {
    group = "application"
    mainClass.set("com.lightningkite.mppexampleapp.MainKt")
    val jvm = kotlin.jvm().compilations.getByName("main")
    classpath = files(jvm.output.allOutputs, jvm.runtimeDependencyFiles)
}
