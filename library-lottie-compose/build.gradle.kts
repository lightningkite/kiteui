import com.lightningkite.deployhelpers.lkLibrary
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

// :library-lottie for the Compose backend; see library-compose/build.gradle.kts for why it's a separate module.
val lottie = project(":library-lottie").projectDir

kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir("$lottie/src/commonMain/kotlin")
            dependencies {
                api(project(":library-compose"))
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Tests render offscreen; without this, AWT tries to reach the window server.
tasks.named<Test>("jvmTest") {
    systemProperty("java.awt.headless", "true")
}

lkLibrary("lightningkite", "kiteui") {
    description.set("KiteUI Lottie Animation Support on Compose Multiplatform")
}
