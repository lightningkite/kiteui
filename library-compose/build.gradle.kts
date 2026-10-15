plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
}

// Kotlin allows one JVM target per module and :library's is jvmSsr, so this backend compiles
// :library's common (and plain-JVM) sources itself instead of depending on :library.
val library = project(":library").projectDir

// Compose's desktop runtime ships Skia natives per OS/arch; tests need the host's.
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
    explicitApi()
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            kotlin.srcDir("$library/src/commonMain/kotlin")
            resources.srcDir("$library/src/commonMain/resources")
            dependencies {
                api(libs.reactive)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.serialization.properties)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.uri)
            }
        }
        // Compose view implementations, kept free of JVM-only APIs so Android/iOS can reuse them later.
        val composeMain = create("composeMain") {
            dependsOn(commonMain)
            dependencies {
                api(libs.compose.foundation)
            }
        }
        getByName("jvmMain") {
            dependsOn(composeMain)
            kotlin.srcDir("$library/src/commonJvmMain/kotlin")
            dependencies {
                api(libs.compose.desktop)
                api(libs.kotlinx.coroutines.swing)
                api(libs.commonsLang3)
                api(libs.ktor.client.core)
                api(libs.ktor.client.okhttp)
                api(libs.ktor.client.websockets)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.compose.ui.test)
                implementation(hostComposeRuntime)
            }
        }
    }
}

dependencies {
    // ComposeMediaPlayer's published metadata leaks kotlinx-coroutines-test into runtime (its issue #250).
    addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>("composeMainImplementation", libs.composemediaplayer.asProvider()) { exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-test") }
    addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>("composeMainImplementation", libs.composemediaplayer.audio) { exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-test") }
}

// Tests render offscreen; without this, AWT tries to reach the window server.
tasks.named<Test>("jvmTest") {
    systemProperty("java.awt.headless", "true")
}

