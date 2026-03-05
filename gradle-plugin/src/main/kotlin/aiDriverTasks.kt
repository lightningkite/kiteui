// by Claude - Gradle tasks for installing, starting, and stopping the KiteUI AI driver daemon
package com.lightningkite.kiteui

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Registers `aiDriverInstall`, `aiDriverStart`, and `aiDriverStop` tasks.
 * Call from [KiteUiPlugin.apply].
 */
fun registerAiDriverTasks(project: Project) {
    project.tasks.register("aiDriverInstall") {
        group = "kiteui"
        description = "Install the AI driver server globally at ~/.kiteui/"
        doLast {
            val version = resolveKiteUiVersion(project)
            val installDir = File(System.getProperty("user.home"), ".kiteui/ai-driver")
            val binDir = File(System.getProperty("user.home"), ".kiteui/bin")
            installDir.mkdirs()
            binDir.mkdirs()

            // Resolve fat JAR from Maven
            val dep = project.dependencies.create(
                "com.lightningkite.kiteui:ai-driver-server:$version"
            )
            val config = project.configurations.detachedConfiguration(dep)
            config.isTransitive = false
            val jar = config.singleFile

            // Clean old versions and copy new JAR
            installDir.listFiles { f -> f.name.endsWith(".jar") }?.forEach { it.delete() }
            val targetJar = File(installDir, jar.name)
            jar.copyTo(targetJar, overwrite = true)
            File(installDir, "version.txt").writeText(version)

            // Write shell wrapper with absolute path to the installed JAR
            val wrapper = File(binDir, "kiteui-drive")
            wrapper.writeText(
                "#!/bin/sh\n" +
                "# by Claude - global CLI for KiteUI AI driver\n" +
                "exec java -jar \"${targetJar.absolutePath}\" \"\$@\"\n"
            )
            wrapper.setExecutable(true)

            println("Installed kiteui-drive $version to ~/.kiteui/")
            if (System.getenv("PATH")?.contains(".kiteui/bin") != true) {
                println("Add to your shell profile: export PATH=\"\$HOME/.kiteui/bin:\$PATH\"")
            }
        }
    }

    project.tasks.register("aiDriverStart") {
        group = "kiteui"
        description = "Start the AI driver daemon"
        doLast {
            if (isDaemonRunning()) {
                println("AI driver daemon already running")
                return@doLast
            }
            val wrapper = File(System.getProperty("user.home"), ".kiteui/bin/kiteui-drive")
            if (!wrapper.exists()) {
                throw GradleException("kiteui-drive not installed. Run: ./gradlew aiDriverInstall")
            }
            ProcessBuilder(wrapper.absolutePath, "start")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            // Poll until ready
            repeat(30) {
                Thread.sleep(1000)
                if (isDaemonRunning()) {
                    println("AI driver daemon started")
                    return@doLast
                }
            }
            println("WARNING: Daemon may not be ready yet")
        }
    }

    project.tasks.register("aiDriverStop") {
        group = "kiteui"
        description = "Stop the AI driver daemon"
        doLast {
            val wrapper = File(System.getProperty("user.home"), ".kiteui/bin/kiteui-drive")
            if (!wrapper.exists()) {
                println("kiteui-drive not installed")
                return@doLast
            }
            ProcessBuilder(wrapper.absolutePath, "stop")
                .inheritIO()
                .start()
                .waitFor()
        }
    }
}

/**
 * Resolves the KiteUI version to use for the ai-driver-server artifact.
 * Checks (in order):
 * 1. The ai-driver-server module's own version (when running inside the kiteui repo)
 * 2. A resolved kiteui dependency from the project's configurations
 * 3. Falls back to the project's own version
 */
private fun resolveKiteUiVersion(project: Project): String {
    // Inside kiteui repo: use ai-driver-server module's version directly
    project.rootProject.findProject(":ai-driver-server")?.let { serverProject ->
        val v = serverProject.version.toString()
        if (v != "unspecified" && v != "1.0-SNAPSHOT") return v
    }

    // External project: find kiteui version from resolved dependencies
    for (config in project.configurations) {
        try {
            if (!config.isCanBeResolved) continue
            for (dep in config.resolvedConfiguration.firstLevelModuleDependencies) {
                if (dep.moduleGroup == "com.lightningkite.kiteui" && dep.moduleName.startsWith("kiteui")) {
                    return dep.moduleVersion
                }
            }
        } catch (_: Exception) {
            // Configuration may not be resolvable yet
        }
    }

    // Last resort
    val v = project.version.toString()
    if (v != "unspecified") return v
    throw GradleException("Could not determine KiteUI version. Specify it via: ./gradlew aiDriverInstall -PkiteuiVersion=X.Y.Z")
}

private fun isDaemonRunning(): Boolean = try {
    val conn = URL("http://localhost:7475/cli").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 2000
    conn.readTimeout = 2000
    conn.doOutput = true
    conn.outputStream.use { it.write("""{"type":"status"}""".toByteArray()) }
    conn.responseCode == 200
} catch (_: Exception) {
    false
}
