import com.lightningkite.kiteui.KiteUiPlugin
import com.lightningkite.kiteui.KiteUiPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinPluginSerialization)
}
apply<KiteUiPlugin>()
configure<KiteUiPluginExtension> {
    this.packageName = "com.lightningkite.mppexampleapp"
    // Use example-app's iOS project since we don't have one for Swing
    this.iosProjectRoot = project.file("../example-app-ios/KiteUI Example App")
}

group = "com.lightningkite"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
    }

    sourceSets {
        val commonMain by getting {
            // Source code shared via symlink: src/commonMain -> ../../example-app/src/commonMain
            dependencies {
                api(project(":library-swing"))
            }
        }

        val jvmMain by getting {
            kotlin.srcDir("src/jvmMain/kotlin")

            dependencies {
                // Swing coroutines
                implementation(libs.kotlinxCoroutinesSwing)
            }
        }
    }
}

// Create a run task for Swing
tasks.register<JavaExec>("run") {
    dependsOn("jvmJar")
    group = "application"
    description = "Run the Swing example app"
    mainClass.set("com.lightningkite.mppexampleapp.MainKt")
    val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")
    classpath = files(
        jvmMainCompilation.output.allOutputs,
        jvmMainCompilation.runtimeDependencyFiles
    )
}

// Override the JVM resources task to generate Swing-compatible font code
tasks.named("kiteuiResourcesJvm") {
    doLast {
        // The plugin generates Resources.jvm.kt with SSR font code
        // Swing uses java.awt.Font, so we need to rewrite the font entries
        val out = project.file("build/generated/kiteui-jvm/Resources.jvm.kt")
        if (out.exists()) {
            val content = out.readText()
            // Replace entire font lines with Swing-compatible code
            val fixedContent = content.lines().joinToString("\n") { line ->
                if (line.trimStart().startsWith("actual val fonts") && line.contains(": Font =")) {
                    val fieldName = line.substringBefore(":").trim()
                    """$fieldName: Font = java.awt.Font("SansSerif", java.awt.Font.PLAIN, 14)"""
                } else {
                    line
                }
            }
            out.writeText(fixedContent)
        }
    }
}
