package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.ssr.SsrDocument
import com.lightningkite.kiteui.ssr.SsrRouter
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

/**
 * SSR Server for testing KiteUI server-side rendering and hydration.
 *
 * Uses SsrRouter with AutoRoutes to automatically handle all routable pages.
 *
 * Run modes:
 * - Default: SSR only (no JS, static HTML)
 * - With --hydrate: SSR + JS hydration (serves JS bundle, page becomes interactive)
 *
 * by Claude
 */
object SsrServer {
    /**
     * Whether to enable hydration mode (serve JS bundle for client-side hydration).
     */
    var enableHydration: Boolean = false

    /**
     * Path to the compiled JS files directory.
     * Lazily detected on first access to ensure correct working directory.
     * by Claude - updated to lazy detection
     */
    var jsDistPath: String? = null
        get() {
            if (field == null) {
                field = findJsDistPath()
            }
            return field
        }

    private fun findJsDistPath(): String {
        val userDir = System.getProperty("user.dir") ?: "."
        println("  Working directory: $userDir")

        // Try various possible paths based on where the server might be run from
        val candidates = listOf(
            "$userDir/example-app/build/compileSync/js/main/developmentExecutable/kotlin",
            "$userDir/build/compileSync/js/main/developmentExecutable/kotlin",
            "example-app/build/compileSync/js/main/developmentExecutable/kotlin",
            "build/compileSync/js/main/developmentExecutable/kotlin",
        )

        for (candidate in candidates) {
            val dir = File(candidate)
            if (dir.exists() && dir.isDirectory) {
                val jsFiles = dir.listFiles { f -> f.extension == "js" }
                if (jsFiles != null && jsFiles.isNotEmpty()) {
                    println("  Found JS at: $candidate (${jsFiles.size} files)")
                    return candidate
                }
            }
        }

        // Default if nothing found
        println("  WARNING: JS directory not found in any expected location!")
        return "example-app/build/compileSync/js/main/developmentExecutable/kotlin"
    }

    /**
     * JS files in correct dependency loading order.
     * by Claude
     */
    private val jsLoadOrder = listOf(
        "kotlin-kotlin-stdlib.js",
        "kotlinx-atomicfu.js",
        "kotlinx-coroutines-core.js",
        "kotlinx-serialization-kotlinx-serialization-core.js",
        "kotlinx-serialization-kotlinx-serialization-json.js",
        "kotlinx-serialization-kotlinx-serialization-properties.js",
        "Kotlin-DateTime-library-kotlinx-datetime.js",
        "reactive.js",
        "kotlin_org_jetbrains_kotlin_kotlin_dom_api_compat.js",
        "kiteui-library.js",
        "kiteui-library-lottie.js",
        "kiteui-example-app.js",
    )

    /**
     * Generate script tags for hydration.
     * Uses dynamic loading to ensure CDN dependencies are ready before Kotlin modules load.
     * by Claude - updated to handle CDN timing issues
     */
    private fun generateScriptTags(): String = """
<script>
// Dynamic loader to ensure CDN deps are ready before loading Kotlin modules - by Claude
(function() {
    var cdnScripts = [
        { src: 'https://cdn.jsdelivr.net/npm/@js-joda/core@5.6.1/dist/js-joda.min.js', global: '@js-joda/core', assign: function() { globalThis['@js-joda/core'] = JSJoda; } },
        { src: 'https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js', global: 'lottie-web', assign: function() { globalThis['lottie-web'] = lottie; lottie_0 = lottie; } }
    ];
    var localScripts = [
        '/js/kotlin-kotlin-stdlib.js',
        '/js/kotlinx-atomicfu.js',
        '/js/kotlinx-coroutines-core.js',
        '/js/kotlinx-serialization-kotlinx-serialization-core.js',
        '/js/kotlinx-serialization-kotlinx-serialization-json.js',
        '/js/kotlinx-serialization-kotlinx-serialization-properties.js',
        '/js/Kotlin-DateTime-library-kotlinx-datetime.js',
        '/js/reactive.js',
        '/js/kotlin_org_jetbrains_kotlin_kotlin_dom_api_compat.js',
        '/js/kiteui-library.js',
        '/js/kiteui-library-lottie.js',
        '/js/kiteui-example-app.js'
    ];

    var cdnLoaded = 0;
    function onCdnLoaded() {
        cdnLoaded++;
        if (cdnLoaded === cdnScripts.length) {
            // Assign globals
            cdnScripts.forEach(function(cdn) { cdn.assign(); });
            // Load local scripts in order
            loadLocalScripts(0);
        }
    }

    function loadLocalScripts(index) {
        if (index >= localScripts.length) return;
        var script = document.createElement('script');
        script.src = localScripts[index];
        script.onload = function() { loadLocalScripts(index + 1); };
        script.onerror = function() { console.error('Failed to load: ' + localScripts[index]); loadLocalScripts(index + 1); };
        document.body.appendChild(script);
    }

    // Load CDN scripts
    cdnScripts.forEach(function(cdn) {
        var script = document.createElement('script');
        script.src = cdn.src;
        script.onload = onCdnLoaded;
        script.onerror = function() { console.error('Failed to load CDN: ' + cdn.src); onCdnLoaded(); };
        document.head.appendChild(script);
    });
})();
</script>
"""

    /**
     * Create the SsrDocument configured for optional hydration.
     * by Claude
     */
    private fun createDocument(): SsrDocument = SsrDocument(
        baseHref = "/",
        additionalBodyContent = if (enableHydration) generateScriptTags() else ""
    )

    private fun createRouter(): SsrRouter = SsrRouter(
        routes = AutoRoutes,
        theme = defaultTheme,
        basePath = "/",
        document = createDocument(),
        appWrapper = { navigator -> app(navigator) }
    )

    /**
     * Start the SSR server on the given port.
     *
     * @param port Server port (default 8001)
     * @param wait Whether to block until server stops
     * @param hydrate Enable hydration mode (serve JS bundle)
     *
     * by Claude
     */
    fun start(
        port: Int = 8001,
        wait: Boolean = true,
        hydrate: Boolean = false
    ): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
        enableHydration = hydrate
        val router = createRouter()

        val mode = if (enableHydration) "SSR + Hydration" else "SSR only"
        println("Starting $mode server on port $port...")
        val resolvedJsPath = jsDistPath ?: "example-app/build/compileSync/js/main/developmentExecutable/kotlin"
        if (enableHydration) {
            println("  JS bundle path: $resolvedJsPath")
            val jsDir = File(resolvedJsPath)
            if (!jsDir.exists()) {
                println("  ⚠️  WARNING: JS directory not found!")
                println("  Run './gradlew :example-app:compileKotlinJs' first")
            } else {
                val jsFiles = jsDir.listFiles { f -> f.extension == "js" }?.size ?: 0
                println("  Found $jsFiles JS files")
            }
        }

        val server = embeddedServer(Netty, port = port) {
            routing {
                // Serve JS files for hydration - by Claude
                if (enableHydration) {
                    get("/js/{fileName}") {
                        val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.NotFound)
                        val file = File(resolvedJsPath, fileName)
                        if (file.exists() && file.extension == "js") {
                            call.respondText(file.readText(), ContentType.Application.JavaScript)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "JS file not found: $fileName (path: $resolvedJsPath)")
                        }
                    }
                    // Also serve source maps for debugging
                    get("/js/{fileName}.map") {
                        val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.NotFound)
                        val file = File(resolvedJsPath, "$fileName.map")
                        if (file.exists()) {
                            call.respondText(file.readText(), ContentType.Application.Json)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }

                // SSR data preloading example page
                get("/ssr-data-example") {
                    handleSsrDataExample(call, router)
                }
                get("/ssr-data-example/{userId}") {
                    handleSsrDataExample(call, router, call.parameters["userId"])
                }

                // Catch-all route - handles everything via AutoRoutes
                get("/{path...}") {
                    handleRequest(call, router)
                }

                // Root path
                get("/") {
                    handleRequest(call, router)
                }
            }
        }

        server.start(wait = wait)
        println("Server started on http://localhost:$port")
        if (enableHydration) {
            println("  Hydration enabled - page will become interactive after JS loads")
        }
        return server
    }

    private suspend fun handleRequest(
        call: io.ktor.server.application.ApplicationCall,
        router: SsrRouter
    ) {
        try {
            val url = call.request.uri
            // Get User-Agent for platform detection during SSR - by Claude
            val userAgent = call.request.headers["User-Agent"]
            // Use preload-enabled rendering to support SsrPreloadable pages
            val html = router.renderOrFallbackWithPreload(url, userAgent)
            call.respondText(html, ContentType.Text.Html)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText(
                "Error: ${e.message}\n${e.stackTraceToString()}",
                ContentType.Text.Plain,
                HttpStatusCode.InternalServerError
            )
        }
    }

    /**
     * Handle the SSR data preloading example page.
     * Demonstrates SsrPreloadable with data fetching before render.
     */
    private suspend fun handleSsrDataExample(
        call: io.ktor.server.application.ApplicationCall,
        router: SsrRouter,
        userId: String? = null
    ) {
        try {
            val page = SsrDataExamplePage(userId ?: "123")
            // Get User-Agent for platform detection during SSR - by Claude
            val userAgent = call.request.headers["User-Agent"]
            val html = router.renderPageWithPreload(page, userAgent)
            call.respondText(html, ContentType.Text.Html)
        } catch (e: Exception) {
            e.printStackTrace()
            call.respondText(
                "Error: ${e.message}\n${e.stackTraceToString()}",
                ContentType.Text.Plain,
                HttpStatusCode.InternalServerError
            )
        }
    }
}

/**
 * Main entry point for running the SSR server from command line.
 *
 * Usage:
 *   ./gradlew :example-app:jvmSsrRun                    # SSR only
 *   ./gradlew :example-app:jvmSsrRun --args="--hydrate" # SSR + hydration
 *
 * Or run the JAR directly:
 *   java -jar example-app.jar                           # SSR only
 *   java -jar example-app.jar --hydrate                 # SSR + hydration
 *   java -jar example-app.jar --hydrate --port 8080     # Custom port
 *
 * by Claude
 */
fun main(args: Array<String>) {
    val hydrate = args.contains("--hydrate") || args.contains("-h")
    val portIndex = args.indexOf("--port").takeIf { it >= 0 } ?: args.indexOf("-p")
    val port = if (portIndex >= 0 && portIndex + 1 < args.size) {
        args[portIndex + 1].toIntOrNull() ?: 8001
    } else {
        8001
    }

    // Allow custom JS path via environment variable
    System.getenv("KITEUI_JS_PATH")?.let { SsrServer.jsDistPath = it }

    SsrServer.start(port = port, hydrate = hydrate)
}
