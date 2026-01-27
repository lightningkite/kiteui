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
        val commonMain by getting {
            // Share source code from library's common source sets
            kotlin.srcDir("${project(":library").projectDir}/src/commonMain/kotlin")
            resources.srcDir("${project(":library").projectDir}/src/commonMain/resources")

            dependencies {
                api(libs.comLightningkiteReactive)
                api(libs.kotlinxSerializationJson)
                api(libs.kotlinxSerializationProperties)
                api(libs.kotlinxSerializationUri)  // by Claude - needed for UriFormat
                api(libs.kotlinxDatetime)
                api(libs.kotlinxCoroutinesCore)
            }
        }

        val jvmMain by getting {
            // Swing-specific implementations (includes JVM actuals)
            kotlin.srcDir("src/jvmMain/kotlin")

            dependencies {
                // commonJvmMain dependencies
                api(libs.commonsLang3)
                api(libs.ktorClientCore)
                api(libs.ktorClientOkhttp)
                api(libs.ktorClientWebsockets)

                // Swing-specific dependencies
                api(libs.miglayout.swing)
                api(libs.swingx.all)
                api(libs.kotlinxCoroutinesSwing)
            }
        }
    }
}

lkLibrary("lightningkite", "kiteui-swing") {
    description.set("KiteUI Swing (Desktop) implementation")
}
