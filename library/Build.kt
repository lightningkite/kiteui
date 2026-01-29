// KBuild dependencies
@file:Repository("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
@file:DependsOn("com.ivieleague:kbuild:1.0-SNAPSHOT")
@file:DependsOn("com.lightningkite:reactive-jvm:6.0.0-prerelease-26")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

/**
 * KBuild configuration for the KiteUI library.
 *
 * by Claude
 *
 * Usage:
 *   kbuild -b KiteUiBuild --list
 *   kbuild -b KiteUiBuild KiteUiBuild.compileJvm
 *   kbuild -b KiteUiBuild KiteUiBuild.compileJs
 *   kbuild -b KiteUiBuild KiteUiBuild.compileIos
 */

import com.ivieleague.kbuild.cli.DependsOn
import com.ivieleague.kbuild.cli.Repository
import com.ivieleague.kbuild.common.Version
import com.ivieleague.kbuild.git.gitVersion
import com.ivieleague.kbuild.kmp.*
import com.ivieleague.kbuild.kotlin.*
import com.ivieleague.kbuild.maven.MavenAether
import com.ivieleague.kbuild.native.KotlinNativeCompile
import com.ivieleague.kbuild.native.NativeOutputKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * KiteUI Library build configuration.
 *
 * This is a complex KMP library with:
 * - Custom source set hierarchy (commonJvmMain, commonHtmlMain)
 * - Multiple targets: JVM SSR, JS, iOS, Android
 * - Serialization plugin
 * - cInterop for iOS
 */
object KiteUiBuild {
    val name = "kiteui"
    val projectRoot = File(".").absoluteFile.canonicalFile
    val group = "com.lightningkite"
    val version = gitVersion(projectRoot.parentFile) // Git version from parent (monorepo root)

    val buildDir = projectRoot.resolve("build")

    // Kotlin version must match what KBuild uses
    val kotlinVersion = "2.2.0"

    // ============== Source Directories ==============

    val srcDir = projectRoot.resolve("src")

    // Common sources
    val commonMainSources = srcDir.resolve("commonMain/kotlin")
    val commonTestSources = srcDir.resolve("commonTest/kotlin")

    // Intermediate source sets (KiteUI-specific)
    val commonJvmMainSources = srcDir.resolve("commonJvmMain/kotlin")
    val commonHtmlMainSources = srcDir.resolve("commonHtmlMain/kotlin")
    val nativeMainSources = srcDir.resolve("nativeMain/kotlin")

    // Platform-specific sources
    val jvmSsrMainSources = srcDir.resolve("jvmSsrMain/kotlin")
    val jsMainSources = srcDir.resolve("jsMain/kotlin")
    val iosMainSources = srcDir.resolve("iosMain/kotlin")
    val androidMainSources = srcDir.resolve("androidMain/kotlin")

    // ============== Dependencies ==============

    // JVM dependencies (for jvmSsr target)
    // Note: For JVM targets, we need the -jvm artifact variants
    val jvmCommonDependencies = listOf(
        "com.lightningkite:reactive-jvm:6.0.0-prerelease-26",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-properties-jvm:1.9.0",
        "org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.1",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2"
    )

    // JS dependencies
    val jsCommonDependencies = listOf(
        "com.lightningkite:reactive-js:6.0.0-prerelease-26",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-js:1.9.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-properties-js:1.9.0",
        "org.jetbrains.kotlinx:kotlinx-datetime-js:0.6.1",
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.10.2"
    )

    // For backwards compatibility
    val commonDependencies = jvmCommonDependencies

    // JVM-specific dependencies (shared by jvmSsr and Android)
    val jvmDependencies = listOf(
        "org.apache.commons:commons-lang3:3.20.0",
        "io.ktor:ktor-client-core-jvm:3.3.3",
        "io.ktor:ktor-client-okhttp-jvm:3.3.3",
        "io.ktor:ktor-client-websockets-jvm:3.3.3"
    )

    // iOS dependencies
    val iosDependencies = listOf(
        "io.ktor:ktor-client-darwin:3.3.3",
        "io.ktor:ktor-client-websockets:3.3.3"
    )

    // Compiler options
    val compilerOptions = listOf(
        "-Xexpect-actual-classes",
        "-opt-in=kotlinx.cinterop.BetaInteropApi",
        "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
        "-opt-in=kotlin.time.ExperimentalTime",
        "-opt-in=kotlin.uuid.ExperimentalUuidApi"
    )

    // ============== Dependency Resolution ==============

    private suspend fun resolveJvmClasspath(extraDeps: List<String> = emptyList()): Set<File> {
        val allDeps = jvmCommonDependencies + jvmDependencies + extraDeps
        return allDeps.flatMap { dep ->
            MavenAether.libraries(dep).mapNotNull { it.default }
        }.toSet()
    }

    private suspend fun resolveJsClasspath(): Set<File> {
        return jsCommonDependencies.flatMap { dep ->
            MavenAether.libraries(dep).mapNotNull { it.default }
        }.toSet()
    }

    // ============== Compilation ==============

    /**
     * Compile JVM SSR target.
     *
     * Source hierarchy:
     *   commonMain -> commonJvmMain -> jvmSsrMain
     *   commonMain -> commonHtmlMain -> jvmSsrMain
     */
    suspend fun compileJvmSsr(): File {
        println("Compiling JVM SSR target...")

        val classpath = resolveJvmClasspath()
        val serializationPlugin = SerializationPlugin.pluginJar()

        // Collect all source roots for jvmSsr
        // jvmSsr depends on: commonMain, commonJvmMain, commonHtmlMain, jvmSsrMain
        val sourceRoots = setOf(
            commonMainSources,
            commonJvmMainSources,
            commonHtmlMainSources,
            jvmSsrMainSources
        ).filter { it.exists() }.toSet()

        // For multiplatform, only commonMain should be marked as common sources
        // Intermediate source sets are platform-specific and should not be in commonSources
        val commonSourceFiles = commonMainSources.walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.absolutePath }
            .toList()
            .toTypedArray()

        return withContext(Dispatchers.IO) {
            kotlinJvmCompileBlocking(
                name = "$name-jvmSsr",
                sourceRoots = sourceRoots,
                classpathJars = classpath,
                arguments = {
                    multiPlatform = true
                    expectActualClasses = true
                    commonSources = commonSourceFiles
                    // Use optIn for experimental API opt-ins
                    optIn = arrayOf(
                        "kotlinx.cinterop.BetaInteropApi",
                        "kotlinx.cinterop.ExperimentalForeignApi",
                        "kotlin.time.ExperimentalTime",
                        "kotlin.uuid.ExperimentalUuidApi"
                    )
                    pluginClasspaths = arrayOf(serializationPlugin.absolutePath)
                },
                cache = buildDir.resolve("cache/jvmSsr"),
                outputFolder = buildDir.resolve("classes/kotlin/jvmSsr/main")
            )
        }
    }

    /**
     * Compile JS target.
     *
     * Source hierarchy:
     *   commonMain -> commonHtmlMain -> jsMain
     */
    suspend fun compileJs(): File {
        println("Compiling JS target...")

        val classpath = resolveJsClasspath()
        val serializationPlugin = SerializationPlugin.pluginJar()

        // Collect all source roots for JS
        // JS depends on: commonMain, commonHtmlMain, jsMain
        val sourceRoots = setOf(
            commonMainSources,
            commonHtmlMainSources,
            jsMainSources
        ).filter { it.exists() }.toSet()

        val commonSourceFiles = commonMainSources.walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.absolutePath }
            .toList()
            .toTypedArray()

        return withContext(Dispatchers.IO) {
            kotlinJsCompileBlocking(
                name = "$name-js",
                sourceRoots = sourceRoots,
                libraries = classpath,
                outputDir = buildDir.resolve("classes/kotlin/js/main"),
                outputMode = JsOutputMode.KLIB,
                arguments = {
                    multiPlatform = true
                    commonSources = commonSourceFiles
                    optIn = arrayOf(
                        "kotlinx.cinterop.BetaInteropApi",
                        "kotlinx.cinterop.ExperimentalForeignApi",
                        "kotlin.time.ExperimentalTime",
                        "kotlin.uuid.ExperimentalUuidApi"
                    )
                    pluginClasspaths = arrayOf(serializationPlugin.absolutePath)
                },
                cache = buildDir.resolve("cache/js")
            )
        }
    }

    /**
     * Compile iOS targets (arm64, simulatorArm64, x64).
     *
     * Source hierarchy:
     *   commonMain -> nativeMain -> iosMain -> [iosArm64Main, etc.]
     *
     * Note: This requires cInterop support which may not be fully implemented yet.
     */
    suspend fun compileIos(target: KmpTarget.Native = KmpTarget.Native.IosArm64): File {
        println("Compiling iOS target: ${target.name}...")

        // Collect all source roots for iOS
        val sourceRoots = setOf(
            commonMainSources,
            nativeMainSources,
            iosMainSources
        ).filter { it.exists() }.toSet()

        val commonSources = commonMainSources.walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.absolutePath }
            .toList()
            .toTypedArray()

        // Resolve iOS dependencies (need native artifacts)
        val klibDeps = iosDependencies.flatMap { dep ->
            val nativeArtifact = "$dep-${target.name.lowercase()}"
            try {
                MavenAether.libraries(nativeArtifact).mapNotNull { it.default }
            } catch (e: Exception) {
                emptyList()
            }
        }.toSet()

        return withContext(Dispatchers.IO) {
            val compiler = KotlinNativeCompile(
                name = "$name-${target.name.lowercase()}",
                target = target.konanTarget,
                sourceRoots = { sourceRoots },
                libraries = { klibDeps },
                outputKind = NativeOutputKind.LIBRARY,
                outputDir = buildDir.resolve("classes/kotlin/${target.name.lowercase()}/main"),
                additionalArgs = listOf("-Xmulti-platform") +
                    commonSources.map { "-Xcommon-sources=$it" } +
                    compilerOptions
            )
            compiler()
        }
    }

    /**
     * Build all targets.
     */
    suspend fun buildAll(): Map<String, File> {
        val results = mutableMapOf<String, File>()

        results["jvmSsr"] = compileJvmSsr()
        results["js"] = compileJs()

        // iOS targets
        for (iosTarget in listOf(
            KmpTarget.Native.IosArm64,
            KmpTarget.Native.IosSimulatorArm64,
            KmpTarget.Native.IosX64
        )) {
            try {
                results[iosTarget.name] = compileIos(iosTarget)
            } catch (e: Exception) {
                println("Warning: Failed to compile ${iosTarget.name}: ${e.message}")
            }
        }

        return results
    }

    /**
     * Clean build artifacts.
     */
    fun clean() {
        buildDir.deleteRecursively()
        println("Cleaned build directory")
    }

    /**
     * Print build configuration summary.
     */
    fun printSummary() {
        println("KiteUI Library Build Configuration")
        println("===================================")
        println("Name: $name")
        println("Group: $group")
        println("Version: $version")
        println("Project Root: $projectRoot")
        println()
        println("Source Sets:")
        println("  commonMain: $commonMainSources (exists: ${commonMainSources.exists()})")
        println("  commonJvmMain: $commonJvmMainSources (exists: ${commonJvmMainSources.exists()})")
        println("  commonHtmlMain: $commonHtmlMainSources (exists: ${commonHtmlMainSources.exists()})")
        println("  jvmSsrMain: $jvmSsrMainSources (exists: ${jvmSsrMainSources.exists()})")
        println("  jsMain: $jsMainSources (exists: ${jsMainSources.exists()})")
        println("  iosMain: $iosMainSources (exists: ${iosMainSources.exists()})")
        println("  nativeMain: $nativeMainSources (exists: ${nativeMainSources.exists()})")
        println()
        println("Common Dependencies:")
        commonDependencies.forEach { println("  - $it") }
        println()
        println("JVM Dependencies:")
        jvmDependencies.forEach { println("  - $it") }
    }
}
