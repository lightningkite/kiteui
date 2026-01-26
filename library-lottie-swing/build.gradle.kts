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
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(project(":library-swing"))
                // Share LottieSource models from library-lottie
                api(project(":library-lottie"))

                // JavaFX for embedding WebView in Swing
                val os = System.getProperty("os.name").lowercase()
                val platform = when {
                    os.contains("mac") -> "mac"
                    os.contains("win") -> "win"
                    else -> "linux"
                }
                api("org.openjfx:javafx-swing:21:$platform")
                api("org.openjfx:javafx-web:21:$platform")
                api("org.openjfx:javafx-base:21:$platform")
                api("org.openjfx:javafx-graphics:21:$platform")
            }
        }
    }
}

lkLibrary("lightningkite", "kiteui-lottie-swing") {
    description.set("KiteUI Lottie Animation Support for Swing (Desktop)")
}
