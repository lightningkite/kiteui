import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
//    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.comLightningkiteTestingManual)
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
//    explicitApi()

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
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("kotlinx.cinterop.BetaInteropApi")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }
    sourceSets {

        val commonMain by getting {
            dependencies {
                api(libs.comLightningkiteReactive)
                api(libs.kotlinxSerializationJson)
                api(libs.kotlinxSerializationProperties)
                api(libs.kotlinxDatetime)
                api(libs.kotlinxCoroutinesCore)
                api(libs.kotlinxSerializationUri)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinxCoroutinesTest)
                implementation(libs.comLightningkiteTestingKotlinTestManualRuntime)
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
                implementation(libs.roborazzi)
                implementation(project(":test-utilities"))
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(libs.ktorClientDarwin)
                implementation(libs.ktorClientWebsockets)
            }
        }

        val commonJvmMain by creating {
            dependsOn(commonMain)
            dependencies {
                api(libs.commonsLang3)
                api(libs.ktorClientCore)
                api(libs.ktorClientOkhttp)
                api(libs.ktorClientWebsockets)
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
    compileSdk = 36

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