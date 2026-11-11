import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.androidKmpLibrary)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "com.lightningkite.kiteui.lottie"
        compileSdk = 37
        minSdk = 21
        enableCoreLibraryDesugaring = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    if (iosEnabled) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
        // The iOS implementation is a placeholder: importing lottie-ios ran into cinterop commonizer
        // issues with Metal framework types.  For production iOS apps, use Swift Lottie directly.
    }

    js {
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
            }
        }

        val androidMain = getByName("androidMain") {
            dependencies {
                api(libs.lottie)
            }
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

        val commonHtmlMain = create("commonHtmlMain") {
            dependsOn(commonMain)
        }
        val jsMain = getByName("jsMain") {
            dependsOn(commonHtmlMain)
            dependencies {
                implementation(npm("lottie-web", "5.12.2"))
            }
        }
    }

    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain = getByName("jvmSsrMain") {
            dependsOn(get("commonHtmlMain"))
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    pom {
        description.set("KiteUI Lottie Animation Support")
        scm {
            connection.set("scm:git:https://github.com/lightningkite/kiteui-lottie.git")
            developerConnection.set("scm:git:https://github.com/lightningkite/kiteui-lottie.git")
            url.set("https://github.com/lightningkite/kiteui-lottie")
        }
    }
}
dokka {
    dokkaSourceSets.configureEach {
        sourceLinks.configureEach { remoteUrl.set(uri("https://github.com/lightningkite/kiteui-lottie/tree/master/")) }
    }
}
