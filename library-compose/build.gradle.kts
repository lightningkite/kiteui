import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// Kotlin allows one JVM target per module and :library's is jvmSsr, so this backend compiles
// :library's common (and plain-JVM) sources itself instead of depending on :library.
val library = project(":library").projectDir


kotlin {
    explicitApi()
    jvm()

    // Claude: a group (rather than dependsOn) keeps the default hierarchy template applied.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        // This is 
        common {
            group("compose") { withJvm() }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xreturn-value-checker=check")
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
        getByName("composeMain") {
            dependencies {
                api(libs.compose.foundation)
            }
        }
        getByName("jvmMain") {
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
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

dependencies {
    // ComposeMediaPlayer's published metadata leaks kotlinx-coroutines-test into runtime (its issue #250).
    addProvider<MinimalExternalModuleDependency, ExternalModuleDependency>("composeMainImplementation", libs.composemediaplayer) { exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-test") }
}

// Tests render offscreen; without this, AWT tries to reach the window server.
tasks.named<Test>("jvmTest") {
    systemProperty("java.awt.headless", "true")
    // PlatformStorage's preferences live in memory, not in the developer's real ones.
    systemProperty("java.util.prefs.PreferencesFactory", "com.lightningkite.kiteui.InMemoryPreferencesFactory")
}

