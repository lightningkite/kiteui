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
            // Share source code from library-camera's commonMain
            kotlin.srcDir("${project(":library-camera").projectDir}/src/commonMain/kotlin")

            dependencies {
                api(project(":library-swing"))
            }
        }

        val jvmMain by getting {
            kotlin.srcDir("src/jvmMain/kotlin")

            dependencies {
            }
        }
    }
}

lkLibrary("lightningkite", "kiteui-camera-swing") {
    description.set("KiteUI Camera Support for JVM (Swing/Desktop)")
}
