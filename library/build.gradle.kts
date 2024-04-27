import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        dependencies {
            api("androidx.transition:transition:1.4.1")
            api("androidx.cardview:cardview:1.0.0")
            api("com.jakewharton.timber:timber:5.0.1")
            api("com.squareup.picasso:picasso:2.8")
            api("io.ktor:ktor-client-core:$ktorVersion")
            api("io.ktor:ktor-client-cio:$ktorVersion")
            api("io.ktor:ktor-client-websockets:$ktorVersion")
            api("androidx.media3:media3-exoplayer:1.2.1")
            api("androidx.media3:media3-ui:1.2.1")
            api("androidx.media3:media3-common:1.2.1")
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
        browser()
    }
//    wasmJs {
//        browser()
//    }

    sourceSets {
        applyDefaultHierarchyTemplate()

        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                api("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.6.3")
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.recyclerview:recyclerview:1.3.2")
                api("com.google.android.material:material:1.11.0")
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
                api("org.apache.commons:commons-lang3:3.12.0")
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
        minSdk = 26
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependencies {
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    }
}

standardPublishing {
    name.set("KiteUI")
    description.set("A lightweight, highly opinionated UI framework for Kotlin Multiplatform")
    github("lightningkite", "kiteui")

    licenses {
        mit()
    }

    developers {
        developer(
            id = "LightningKiteJoseph",
            name = "Joseph Ivie",
            email = "joseph@lightningkite.com",
        )
        developer(
            id = "bjsvedin",
            name = "Brady Svedin",
            email = "brady@lightningkite.com",
        )
        developer(
            id = "shanelk",
            name = "Shane Thompson",
            email = "shane@lightningkite.com",
        )
    }
}
