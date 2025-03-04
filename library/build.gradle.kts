import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    id("com.android.library")
    alias(libs.plugins.comLightningkiteTestingManual)
//    id("org.jetbrains.dokka")
    signing
    id("com.vanniktech.maven.publish") version "0.30.0"
}

val ktorVersion = "3.0.0"

val lk = project.lk {
    kotlinTestManualPlugin()
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
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
        }
        this.compilerOptions {
            this.jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "library"
//        }
//    }
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
//    wasmJs {
//        browser()
//    }

//    explicitApi = ExplicitApiMode.Warning
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.BetaInteropApi")
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
    }
    sourceSets {
        applyDefaultHierarchyTemplate()

        val commonMain by getting {
            dependencies {
                api(lk.readable(1))
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
                implementation(lk.kotlinTestManualRuntime())
            }
        }
        val androidMain by getting {
            dependencies {
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

//        val wasmJsMain by getting {
//            dependsOn(commonHtmlMain)
//        }
    }

//    cocoapods {
//        summary = "KiteUI"
//        homepage = "https://github.com/lightningkite/kiteui"
//        ios.deploymentTarget = "12.0"
//
//        pod("FlexLayout") { version = "2.0.03" }
//        pod("PinLayout") {
//            version = "1.10.5"
//            extraOpts += listOf("-compiler-option", "-fmodules")
//        }
//    }
}

//tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.PodGenTask>().configureEach {
//    doLast {
//        podfile.get().appendText("\nENV['SWIFT_VERSION'] = '5'")
//    }
//}

kotlin {
    targets
        .matching { it is org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget }
        .configureEach {
            this as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

            compilations.getByName("main") {
                val objcAddition by cinterops.creating {
                    defFile(project.file("src/iosMain/def/objcAddition.def"))
                }
                this.kotlinOptions {
//                    this.freeCompilerArgs += "-Xruntime-logs=gc=info"
//                    this.freeCompilerArgs += "-Xallocator=mimalloc"
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
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
dependencies {
    implementation("io.ktor:ktor-client-okhttp-jvm:3.0.0")
}

mavenPublishing {
    // publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(group.toString(), name, version.toString())
    pom {
        name.set("KiteUI")
        description.set("A lightweight, highly opinionated UI framework for Kotlin Multiplatform")
        github("lightningkite", "kiteui")

        licenses {
            mit()
        }

        developers {
            joseph()
            brady()
            developer{
                id.set("shanelk")
                name.set("Shane Thompson")
                email.set("shane@lightningkite.com")
            }
        }
    }

}