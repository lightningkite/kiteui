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
            api("androidx.appcompat:appcompat:1.7.0")
            api("androidx.recyclerview:recyclerview:1.3.2")
            api("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
            api("com.google.android.material:material:1.12.0")
            api("androidx.transition:transition:1.5.1")
            api("androidx.cardview:cardview:1.0.0")
            api("com.jakewharton.timber:timber:5.0.1")
            api("com.github.bumptech.glide:glide:5.0.0-rc01")
            api("com.github.chrisbanes:PhotoView:2.3.0")
            api("io.ktor:ktor-client-core:$ktorVersion")
            api("io.ktor:ktor-client-cio:$ktorVersion")
            api("io.ktor:ktor-client-okhttp:$ktorVersion")
            api("io.ktor:ktor-client-websockets:$ktorVersion")
            api("androidx.media3:media3-exoplayer:1.4.1")
            api("androidx.media3:media3-ui:1.4.1")
            api("androidx.media3:media3-common:1.4.1")
            api("androidx.autofill:autofill:1.3.0-rc01")
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
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
                api("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.7.1")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC.2")
                implementation(lk.kotlinTestManualRuntime())
            }
        }
        val androidMain by getting {
            dependencies {
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.13")
            }
        }

        val commonHtmlMain by creating {
            dependsOn(commonMain)
        }

        val iosMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.7")
                implementation("io.ktor:ktor-client-websockets:2.3.7")
            }
        }

        val jvmMain by getting {
            dependsOn(commonHtmlMain)
            dependencies {
                api("org.apache.commons:commons-lang3:3.14.0")
                api("io.ktor:ktor-client-core:$ktorVersion")
                api("io.ktor:ktor-client-okhttp:$ktorVersion")
                api("io.ktor:ktor-client-websockets:$ktorVersion")
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
    compileSdk = 34

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