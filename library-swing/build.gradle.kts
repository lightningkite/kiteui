import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.dokka)
    signing
    alias(libs.plugins.vannitechPublishing)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain by getting {
            // Share source code from library's common source sets
            kotlin.srcDir("${project(":library").projectDir}/src/commonMain/kotlin")
            resources.srcDir("${project(":library").projectDir}/src/commonMain/resources")

            dependencies {
                api(libs.reactive)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.serialization.properties)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.uri)
            }
        }

        val jvmMain by getting {
            // Swing-specific implementations (includes JVM actuals)
            kotlin.srcDir("src/jvmMain/kotlin")

            dependencies {
                // commonJvmMain dependencies
                api(libs.commonsLang3)
                api(libs.ktor.client.core)
                api(libs.ktor.client.okhttp)
                api(libs.ktor.client.websockets)

                // Swing-specific dependencies
                api(libs.miglayout.swing)
                api(libs.swingx.all)
                api(libs.kotlinx.coroutines.swing)

                // JInput for gamepad support (2.0.10 is an uber jar with natives included)
                api(libs.jinput)
            }
        }
    }
}

lkLibrary("lightningkite", "kiteui-swing") {
    description.set("KiteUI Swing (Desktop) implementation")
}
