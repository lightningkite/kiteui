import com.lightningkite.deployhelpers.lkLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// KMP currently doesn't disable iOS target and dependency resolution correctly when not on a mac.
// So we work around it on non mac machines with this check
val onMac = System.getProperty("os.name").contains("Mac", ignoreCase = true)
val iosTargetOverride = false

val iosTarget = iosTargetOverride || onMac

plugins {
    alias(libs.plugins.kotlin.multiplatform)
//    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.testing.manual)
    alias(libs.plugins.roborazzi)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

dokka {
    // Dokka generates a new process managed by Gradle
    dokkaGeneratorIsolation = ProcessIsolation {
        // Configures heap size
        maxHeapSize = "4g"
    }
}

kotlin {
    applyDefaultHierarchyTemplate()
    explicitApi()  // strict: missing visibility/return-type on public API is a compile error

    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    if (iosTarget) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
    }
    js(IR) {
        browser {
            testTask {
                useKarma {
//                    useChromeHeadless()
                    useFirefox()
                }
            }
        }
    }

//    explicitApi = ExplicitApiMode.Warning
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
    sourceSets {

        val commonMain by getting {
            dependencies {
                api(libs.reactive)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.serialization.properties)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.uri)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.test.manual.runtime)
                implementation(project(":test-utilities"))
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.appcompat)
                api(libs.ktx)
                api(libs.swiperefreshlayout)
                api(libs.material)
                api(libs.transition)
                api(libs.cardview)
                api(libs.timber)
                api(libs.glide)
                api(libs.photoview)
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                api(libs.ktor.client.okhttp)
                api(libs.ktor.client.websockets)
                api(libs.media3.exoplayer)
                api(libs.media3.ui)
                api(libs.media3.common)
                api(libs.androidxAutofill)
                api(libs.exifinterface)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.roborazzi)
                implementation(project(":test-utilities"))
            }
        }

        if (iosTarget) {
            val iosMain by getting {
                dependencies {
                    implementation(libs.ktor.client.darwin)
                    implementation(libs.ktor.client.websockets)
                }
            }
        }

        val commonJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(libs.commonsLang3)
                api(libs.ktor.client.core)
                api(libs.ktor.client.okhttp)
                api(libs.ktor.client.websockets)
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }
        val jsMain by getting {
            dependsOn(commonHtmlMain)
        }
    }

    jvm("jvmSsr")
    sourceSets {
        val jvmSsrMain by getting {
            dependsOn(get("commonJvmMain"))
            dependsOn(get("commonHtmlMain"))
        }
    }
//    jvm("jvmSwing")
//    sourceSets {
//        val jvmSwingMain by getting {
//        }
//    }

    if (iosTarget) {
        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().all {
            compilations.getByName("main") {
                val objcAddition by cinterops.creating {
                    defFile(project.file("src/iosMain/def/objcAddition.def"))
                }
            }
        }
    }
}

android {
    namespace = "com.lightningkite.kiteui"
    testNamespace = "com.lightningkite.kiteui.test"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        coreLibraryDesugaring(libs.desugar.jdk.libs)
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
dependencies {
    implementation(libs.ktor.client.okhttp.jvm)
}

lkLibrary(
    "lightningkite",
    "kiteui",
    mavenAutomaticRelease = project.findProperty("mavenAutomaticRelease") as? Boolean ?: false
) {
    description.set("A lightweight, highly opinionated UI framework for Kotlin Multiplatform")
}