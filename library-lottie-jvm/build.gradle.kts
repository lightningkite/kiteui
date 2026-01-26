import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.dokka)
    signing
    alias(libs.plugins.vannitechPublishing)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain by getting {
            // Share source code from library-lottie's commonMain
            kotlin.srcDir("${project(":library-lottie").projectDir}/src/commonMain/kotlin")

            dependencies {
                api(project(":library-swing"))
            }
        }

        val jvmMain by getting {
            kotlin.srcDir("src/jvmMain/kotlin")

            dependencies {
                // JavaFX for embedding WebView in Swing
                val os = System.getProperty("os.name").lowercase()
                val arch = System.getProperty("os.arch").lowercase()
                val platform = when {
                    os.contains("mac") -> if (arch.contains("aarch64") || arch.contains("arm64")) "mac-aarch64" else "mac"
                    os.contains("win") -> "win"
                    else -> if (arch.contains("aarch64") || arch.contains("arm64")) "linux-aarch64" else "linux"
                }
                api("org.openjfx:javafx-swing:21:$platform")
                api("org.openjfx:javafx-web:21:$platform")
                api("org.openjfx:javafx-base:21:$platform")
                api("org.openjfx:javafx-graphics:21:$platform")
                api("org.openjfx:javafx-controls:21:$platform")
                api("org.openjfx:javafx-media:21:$platform")
            }
        }
    }
}

lkLibrary("lightningkite", "kiteui-lottie-jvm") {
    description.set("KiteUI Lottie Animation Support for JVM (Swing/Desktop)")
}
