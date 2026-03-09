// by Claude - JS/Web platform integration test via ai-driver-server
package com.lightningkite.mppexampleapp.integration

import com.lightningkite.kiteui.testing.ensureDaemon
import com.lightningkite.kiteui.testing.remoteUiTest
import com.lightningkite.kiteui.testing.remoteWebUiTest
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test that exercises the example app's JS/Web build through the ai-driver-server.
 *
 * The test will:
 * 1. Connect to an already-running web app, OR
 * 2. Open a Vite dev server URL in the browser (if running on :3000), OR
 * 3. Serve pre-compiled JS from build output with an embedded HTTP server
 *
 * Prerequisites (any ONE of):
 * - Web app already running and connected to daemon
 * - Vite dev server running: `./gradlew :example-app:viteRun`
 * - JS compiled: `./gradlew :example-app:jsBrowserDevelopmentWebpack`
 *
 * Daemon must be running: `./gradlew aiDriverStart`
 *
 * Full automated run: `./scripts/run-integration-tests.sh web`
 */
// by Claude
class WebIntegrationTest {

    @Test
    fun fullWalkthrough() {
        // by Claude - skip gracefully in CI where daemon is unavailable
        try { ensureDaemon() } catch (e: IllegalStateException) {
            skipUnless(false, "Daemon not available: ${e.message}")
        }

        // Strategy 1: Reuse an already-connected example web app
        val allApps = tryListApps()
        val existingWeb = allApps?.firstOrNull {
            it.platform == "web" && it.appName == EXAMPLE_APP_NAME
        }
        if (existingWeb != null) {
            remoteUiTest(appId = existingWeb.appId) {
                navigate("/")
                exampleAppWalkthrough()
            }
            return
        }

        // Strategy 2: Open browser against running Vite dev server
        // Only works when KITEUI_OPEN_BROWSER=true is set (e.g. from run-integration-tests.sh)
        // because Desktop.browse() can block in some environments.
        val port3000Open = isPortOpen(3000)
        val canOpenBrowser = System.getenv("KITEUI_OPEN_BROWSER") == "true"
        if (port3000Open && canOpenBrowser) {
            remoteWebUiTest(
                url = "http://localhost:3000",
                appIdPrefix = "web",
                appName = EXAMPLE_APP_NAME,
                connectTimeout = 30.seconds,
            ) {
                exampleAppWalkthrough()
            }
            return
        }

        // Strategy 3: Serve pre-compiled JS from build output (also requires KITEUI_OPEN_BROWSER)
        val distDir = if (canOpenBrowser) findWebDistDir() else null
        if (distDir != null) {
            val server = startStaticFileServer(distDir, port = 8088)
            try {
                remoteWebUiTest(
                    url = "http://localhost:8088",
                    appIdPrefix = "web",
                    appName = EXAMPLE_APP_NAME,
                    connectTimeout = 30.seconds,
                ) {
                    exampleAppWalkthrough()
                }
            } finally {
                server.stop(0)
            }
            return
        }

        // Nothing available - skip with instructions
        skipUnless(false, buildString {
            appendLine("No web app available. Use one of:")
            appendLine("  1. Open localhost:3000 in a browser with the example app connected to the daemon")
            appendLine("  2. KITEUI_OPEN_BROWSER=true ./gradlew :example-app:jvmSsrTest  (auto-opens browser)")
            appendLine("  3. ./scripts/run-integration-tests.sh web  (full automated run)")
        })
    }

    /** Look for the compiled JS output directory with an index.html. */
    private fun findWebDistDir(): File? {
        val candidates = listOf(
            "example-app/build/dist/js/developmentExecutable",
            "example-app/build/dist/js/productionExecutable",
            "example-app/build/dist/vite",
        )
        for (candidate in candidates) {
            val dir = File(candidate)
            if (dir.exists() && dir.resolve("index.html").exists()) return dir
        }
        val projectRoot = findProjectRoot()
        if (projectRoot != null) {
            for (candidate in candidates) {
                val dir = File(projectRoot, candidate)
                if (dir.exists() && dir.resolve("index.html").exists()) return dir
            }
        }
        return null
    }

    private fun findProjectRoot(): File? {
        var dir = File(System.getProperty("user.dir"))
        while (dir.parentFile != null) {
            if (File(dir, "gradlew").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }

    /**
     * Start a minimal HTTP file server for serving the compiled SPA.
     * Serves files from [root] with SPA fallback (unknown paths serve index.html).
     */
    private fun startStaticFileServer(root: File, port: Int): HttpServer {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            var path = exchange.requestURI.path
            if (path == "/") path = "/index.html"

            val file = File(root, path.removePrefix("/"))
            val toServe = if (file.exists() && file.isFile) file else File(root, "index.html")

            if (toServe.exists()) {
                val contentType = when (toServe.extension.lowercase()) {
                    "html" -> "text/html"
                    "js" -> "application/javascript"
                    "mjs" -> "application/javascript"
                    "css" -> "text/css"
                    "json" -> "application/json"
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "svg" -> "image/svg+xml"
                    "woff2" -> "font/woff2"
                    "woff" -> "font/woff"
                    "ico" -> "image/x-icon"
                    else -> "application/octet-stream"
                }
                val bytes = toServe.readBytes()
                exchange.responseHeaders.add("Content-Type", contentType)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } else {
                exchange.sendResponseHeaders(404, 0)
                exchange.responseBody.close()
            }
        }
        server.start()
        return server
    }
}
