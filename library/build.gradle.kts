import com.lightningkite.deployhelpers.lkLibrary
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
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
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
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
            // Opt in across the whole iOS hierarchy rather than on the native compilations. The
            // shared iosMain metadata compilation is not a KotlinNativeTarget compilation, so a
            // target-level opt-in leaves compileIosMainKotlinMetadata without it. Kotlin also
            // requires a source set's opt-ins to be a superset of those of the source sets it
            // depends on, so the leaf target source sets must be covered too, not just iosMain.
            matching { it.name.startsWith("ios") }.configureEach {
                languageSettings {
                    optIn("kotlinx.cinterop.BetaInteropApi")
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                }
            }
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
            // Without this, any commonTest that reaches a logging path fails on Android with
            // "Method w in android.util.Log not mocked" - the stubbed android.jar throws on every
            // call. Common code legitimately logs (Routes.parse warns on an unparseable URL), so
            // the alternative is that no shared test may exercise such a path at all. Returning
            // defaults is scoped to unit tests; instrumented and Robolectric tests are unaffected.
            isReturnDefaultValues = true
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
// ---------------------------------------------------------------------------------------------
// Local echo server for the cross-platform network tests (see docs/TESTING_GUIDE.md).
// ---------------------------------------------------------------------------------------------

/**
 * Keeps one `:test-server` process alive for the whole build.
 *
 * A build service rather than a start/stop task pair because a run like `allTests` has several test
 * tasks and they must share one server: a task-scoped server would be stopped by the first task's
 * finalizer and then reported up-to-date rather than restarted for the second. Gradle closes a build
 * service once, after the last task that used it.
 *
 * The server runs in its own process rather than in the daemon so that the test classpath and the
 * build classpath stay independent, and so a stuck server can be killed without taking the daemon
 * with it.
 */
abstract class EchoServerService : BuildService<EchoServerService.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val classpath: ConfigurableFileCollection
        val port: Property<Int>
    }

    private var process: Process? = null

    /** Starts the server if it is not already up, and returns only once it is answering. */
    @Synchronized
    fun ensureRunning() {
        if (process != null) return
        val started = ProcessBuilder(
            File(File(System.getProperty("java.home"), "bin"), "java").absolutePath,
            "-cp", parameters.classpath.files.joinToString(File.pathSeparator),
            "com.lightningkite.kiteui.testserver.MainKt",
            parameters.port.get().toString(),
        ).redirectErrorStream(true).start()
        process = started

        // The server announces itself once it is bound; waiting for that beats polling the port,
        // which cannot tell "not up yet" from "someone else's server is on this port".
        val listening = CompletableFuture<String>()
        Thread {
            started.inputStream.bufferedReader().forEachLine { line ->
                if (line.startsWith("LISTENING ")) listening.complete(line)
            }
            // Reached on the server's exit, and ignored if it had already announced itself.
            listening.completeExceptionally(IllegalStateException("The test echo server exited before it began listening"))
        }.apply { isDaemon = true }.start()

        try {
            listening.get(60, TimeUnit.SECONDS)
        } catch (e: Exception) {
            started.destroyForcibly()
            process = null
            throw IllegalStateException(
                "The test echo server did not start on port ${parameters.port.get()}. " +
                        "Another process may be holding the port; override it with " +
                        "-Pkiteui.testServerPort=<port>.",
                e
            )
        }
    }

    /** Closing stdin is the server's shutdown signal; see its `main`. */
    override fun close() {
        val running = process ?: return
        running.outputStream.close()
        if (!running.waitFor(10, TimeUnit.SECONDS)) running.destroyForcibly()
        process = null
    }
}

val echoServerDependencies = configurations.dependencyScope("echoServerDependencies")
val echoServerClasspath = configurations.resolvable("echoServerClasspath") {
    extendsFrom(echoServerDependencies.get())
}
dependencies { echoServerDependencies(project(":test-server")) }

val testServerPort = (providers.gradleProperty("kiteui.testServerPort").orNull ?: "18787").toInt()

/**
 * Publishes the port to the tests. Generated rather than written by hand so that the build and the
 * tests cannot disagree about it, and so `-Pkiteui.testServerPort` reaches both.
 */
val generateTestServerPort = tasks.register("generateTestServerPort") {
    val outputDirectory = layout.buildDirectory.dir("generated/testServerPort/kotlin")
    val port = testServerPort
    inputs.property("port", port)
    outputs.dir(outputDirectory)
    doLast {
        outputDirectory.get().asFile.resolve("com/lightningkite/kiteui/TestServerPort.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
                package com.lightningkite.kiteui

                // Generated by :library:generateTestServerPort. Do not edit.
                internal const val TEST_SERVER_PORT: Int = $port

                """.trimIndent()
            )
        }
    }
}
kotlin.sourceSets.named("commonTest") { kotlin.srcDir(generateTestServerPort) }

val echoServer = gradle.sharedServices.registerIfAbsent("kiteuiEchoServer", EchoServerService::class) {
    parameters.classpath.from(echoServerClasspath)
    parameters.port.set(testServerPort)
}

tasks.withType<AbstractTestTask>().configureEach {
    dependsOn(echoServerClasspath)
    usesService(echoServer)
    doFirst { echoServer.get().ensureRunning() }
}
