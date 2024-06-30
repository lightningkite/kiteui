import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import com.lightningkite.deployhelpers.*

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinCocoapods)
    id("maven-publish")
    id("signing")
}

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
            api(libs.transition)
            api(libs.cardView)
            api(libs.timber)
            api(libs.glide)
            api(libs.touchImageView)
            api(libs.ktorClientCore)
            api(libs.ktorClientCio)
            api(libs.ktorClientWebsockets)
            api(libs.androidxMedia3Common)
            api(libs.androidxMedia3ExoPLayer)
            api(libs.androidxMedia3UI)
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
                api(libs.kotlinxJson)
                api(libs.kotlinxProperties)
                api(libs.kotlinxDateTime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinTest)
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.appCompat)
                api(libs.recyclerView)
                api(libs.material)
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
                api(libs.apacheCommonsLang)
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
        coreLibraryDesugaring(libs.jdkDesugar)
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
