package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.ssr.SsrRouter
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import java.io.File
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlinx.coroutines.runBlocking

/**
 * Static site generator that prerenders all example app pages to HTML files.
 *
 * Usage:
 * ```
 * ./gradlew :example-app:ssrPrerender
 * ```
 *
 * Or call directly:
 * ```kotlin
 * SsrPrerender.prerenderAll(File("./output"))
 * ```
 */
object SsrPrerender {
    private val router = SsrRouter(
        routes = AutoRoutes,
        theme = defaultTheme,
        basePath = "/",
        appWrapper = { navigator, dialog -> app(navigator, dialog) }
    )

    /**
     * Dynamically discover all routes from AutoRoutes.
     * For object pages, we can get the route automatically.
     * For class pages with parameters, we provide sample instances.
     */
    private fun discoverRoutes(): List<String> {
        val routes = mutableListOf<String>()

        // Iterate through all registered page classes in AutoRoutes
        for ((pageClass, renderer) in AutoRoutes.renderers) {
            try {
                // Try to get an instance of the page
                val page: Page? = when {
                    // For Kotlin objects (singletons), use objectInstance
                    pageClass.objectInstance != null -> pageClass.objectInstance as Page

                    // For classes, try to create a default instance (works for no-arg constructors)
                    else -> try {
                        pageClass.createInstance() as Page
                    } catch (e: Exception) {
                        // Skip pages that require constructor arguments - they'll be in manualRoutes
                        null
                    }
                }

                if (page != null) {
                    val rendered = renderer(page)
                    if (rendered != null) {
                        val route = "/" + rendered.urlLikePath.segments.joinToString("/")
                        routes.add(route)
                    }
                }
            } catch (e: Exception) {
                // Skip pages that can't be instantiated
                println("Skipping ${pageClass.simpleName}: ${e.message}")
            }
        }

        return routes
    }

    /**
     * Manual routes for pages that require constructor parameters.
     * These need sample values to be prerendered.
     */
    private val manualRoutes = listOf(
        // Pages with parameters - provide sample values
        "/arguments-example/example-value",
        "/test/ssr-resource/demo-user-123",
    )

    /**
     * Get all routes to prerender (automatic + manual).
     */
    fun getAllRoutes(): List<String> {
        val discovered = discoverRoutes()
        val all = (discovered + manualRoutes).distinct().sorted()
        return all
    }

    /**
     * Prerender all known routes to HTML files in the output directory.
     *
     * @param outputDir The directory to write HTML files to
     * @param verbose If true, print progress for each page
     * @return Map of route to render result (success message or error)
     */
    fun prerenderAll(outputDir: File, verbose: Boolean = true): Map<String, Result<String>> {
        outputDir.mkdirs()

        val routes = getAllRoutes()

        if (verbose) {
            println("Prerendering ${routes.size} pages to ${outputDir.absolutePath}")
            println("=".repeat(60))
        }

        val results = mutableMapOf<String, Result<String>>()

        runBlocking {
            for (route in routes) {
                try {
                    val html = router.renderOrFallbackWithPreload(route)

                    // Convert route to file path
                    val filePath = when {
                        route == "/" -> "index.html"
                        else -> "${route.trimStart('/')}/index.html"
                    }

                    val file = File(outputDir, filePath)
                    file.parentFile.mkdirs()
                    file.writeText(html)

                    results[route] = Result.success("${file.length()} bytes")

                    if (verbose) {
                        println("✓ $route -> $filePath (${file.length()} bytes)")
                    }
                } catch (e: Exception) {
                    results[route] = Result.failure(e)

                    if (verbose) {
                        println("✗ $route -> ERROR: ${e.message}")
                    }
                }
            }
        }

        if (verbose) {
            println("=".repeat(60))
            val successes = results.count { it.value.isSuccess }
            val failures = results.count { it.value.isFailure }
            println("Completed: $successes succeeded, $failures failed")
        }

        return results
    }

    /**
     * Prerender a single route and return the HTML.
     */
    suspend fun prerenderOne(route: String): String {
        return router.renderOrFallbackWithPreload(route)
    }
}

/**
 * Command-line entry point for prerendering and SSR server.
 *
 * Usage:
 *   prerender <output-dir>   - Prerender all pages to the specified directory
 *   server [--hydrate] [-p PORT]  - Start the SSR server (default port 8001)
 *   --hydrate                - Enable hydration mode (serve JS bundle for client-side hydration)
 *
 * Examples:
 *   ./gradlew ssrServerRun                          # SSR only
 *   ./gradlew ssrServerRun --args="--hydrate"       # SSR + hydration
 *   ./gradlew ssrServerRun --args="--hydrate -p 3000"  # Custom port with hydration
 *
 * by Claude - updated to support hydration flag
 */
fun main(args: Array<String>) {
    val hydrate = args.contains("--hydrate") || args.contains("-h")
    val portIndex = args.indexOf("-p").takeIf { it >= 0 } ?: args.indexOf("--port")
    val port = if (portIndex >= 0 && portIndex + 1 < args.size) {
        args[portIndex + 1].toIntOrNull() ?: 8001
    } else {
        8001
    }

    // Allow custom JS path via environment variable
    System.getenv("KITEUI_JS_PATH")?.let { SsrServer.jsDistPath = it }

    when {
        args.getOrNull(0) == "prerender" -> {
            val outputDir = args.getOrNull(1) ?: "./local/prerendered"
            println("Starting prerender to: $outputDir")
            val results = SsrPrerender.prerenderAll(File(outputDir))

            val failures = results.count { it.value.isFailure }
            System.exit(if (failures > 0) 1 else 0)
        }
        args.getOrNull(0) == "server" -> {
            SsrServer.start(port = port, hydrate = hydrate, wait = true)
        }
        else -> {
            // Default: start server (with optional hydration)
            SsrServer.start(port = port, hydrate = hydrate, wait = true)
        }
    }
}
