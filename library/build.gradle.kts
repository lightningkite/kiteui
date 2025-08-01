import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
//    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.comLightningkiteTestingManual)
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
    explicitApi()

    jvm()
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    useFirefox()
                }
            }
        }
    }

//    explicitApi = ExplicitApiMode.Warning
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.BetaInteropApi")
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
    }
    sourceSets {

        val commonMain by getting {
            dependencies {
                api(libs.comLightningkiteReadable)
                api(libs.kotlinxSerializationJson)
                api(libs.kotlinxSerializationProperties)
                api(libs.kotlinxDatetime)
                api(libs.kotlinxCoroutinesCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest)
                implementation(libs.comLightningkiteTestingKotlinTestManualRuntime)
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
                api(libs.ktorClientCore)
                api(libs.ktorClientCio)
                api(libs.ktorClientOkhttp)
                api(libs.ktorClientWebsockets)
                api(libs.media3Exoplayer)
                api(libs.media3Ui)
                api(libs.media3Common)
                api(libs.androidxAutofill)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.ktorClientDarwin)
                implementation(libs.ktorClientWebsockets)
            }
        }

        val jvmMain by getting {
            dependsOn(commonHtmlMain)
            dependencies {
                api(libs.commonsLang3)
                api(libs.ktorClientCore)
                api(libs.ktorClientOkhttp)
                api(libs.ktorClientWebsockets)
            }
        }
        val jsMain by getting {
            dependsOn(commonHtmlMain)
        }
    }
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().all {
        compilations.getByName("main") {
            val objcAddition by cinterops.creating {
                defFile(project.file("src/iosMain/def/objcAddition.def"))
            }
        }
    }
}

android {
    namespace = "com.lightningkite.kiteui"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
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

lkLibrary("lightningkite", "kiteui") {
    description.set("A lightweight, highly opinionated UI framework for Kotlin Multiplatform")
}