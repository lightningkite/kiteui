import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import java.time.Duration
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// example-app on the Compose desktop backend.  Kotlin allows one JVM target per module and example-app's is
// jvmSsr, so this module compiles example-app's common code itself.
val exampleApp = project(":example-app").projectDir

apply<KiteUiPlugin>()
configure<KiteUiPluginExtension> {
    packageName = "com.lightningkite.mppexampleapp"
    commonMainDirectory = exampleApp.resolve("src/commonMain")
}


kotlin {
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        getByName("commonMain") {
            kotlin.srcDir("$exampleApp/src/commonMain/kotlin")
            dependencies {
                api(project(":library-compose"))
                api(project(":library-lottie-compose"))
                api(project(":library-camera-compose"))
            }
        }
        getByName("jvmMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    systemProperty("java.awt.headless", "true")
    timeout.set(Duration.ofMinutes(5))
    // The example app turns on KiteUI's leak detector, which calls System.gc(); by default that's a full stop-the-world
    // collection, long enough to drop frames.
    jvmArgs("-XX:+ExplicitGCInvokesConcurrent")
    // -Pjfr=<file> records a Flight Recorder profile, e.g. of ScrollBenchmark.
    providers.gradleProperty("jfr").orNull?.let { jvmArgs("-XX:StartFlightRecording=filename=$it,settings=profile") }
}

compose.desktop {
    application {
        mainClass = "com.lightningkite.mppexampleapp.MainKt"
        jvmArgs += "-XX:+ExplicitGCInvokesConcurrent" // See jvmTest above.
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KiteUI Example"
            packageVersion = "1.0.0"
        }
    }
}
