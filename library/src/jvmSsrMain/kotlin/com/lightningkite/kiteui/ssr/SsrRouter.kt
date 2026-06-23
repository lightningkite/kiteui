package com.lightningkite.kiteui.ssr

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.col

/**
 * Function type for custom app wrappers.
 * Receives the ViewWriter, main navigator, dialog navigator, and current page.
 */
typealias AppWrapper = ViewWriter.(navigator: PageNavigator, dialog: PageNavigator) -> Unit

/**
 * Router for server-side rendering that integrates with KiteUI's Routes system.
 *
 * Takes any URL and automatically parses it to the correct Page using
 * the app's generated AutoRoutes, then renders it to HTML.
 *
 * Example usage:
 * ```kotlin
 * // Simple usage without app navigation:
 * val router = SsrRouter(AutoRoutes, defaultTheme)
 *
 * // With app wrapper for navigation:
 * val router = SsrRouter(
 *     routes = AutoRoutes,
 *     theme = defaultTheme,
 *     appWrapper = { navigator, dialog -> app(navigator, dialog) }
 * )
 *
 * // In your HTTP handler:
 * get("/{path...}") {
 *     val url = call.request.uri
 *     val result = router.render(url)
 *     if (result != null) {
 *         call.respondText(result, ContentType.Text.Html)
 *     } else {
 *         call.respond(HttpStatusCode.NotFound)
 *     }
 * }
 * ```
 */
class SsrRouter(
    val routes: Routes,
    val theme: Theme,
    val basePath: String = "/",
    val document: SsrDocument = SsrDocument(baseHref = basePath),
    /**
     * Optional app wrapper function that provides the full app shell (navigation, etc).
     * If null, pages are rendered directly without navigation wrapper.
     */
    val appWrapper: AppWrapper? = null
) {
    // ==================== Suspend versions with preloading ====================

    /**
     * Parse a URL string and render with data preloading.
     *
     * If the page implements [SsrPreloadable], its preload() method is called
     * before rendering, allowing async data to be fetched.
     *
     * @param url The URL path (e.g., "/docs/getting-started" or "/users/123?tab=profile")
     * @param userAgent Optional User-Agent string for platform detection - by Claude
     * @return Complete HTML document string, or null if the URL doesn't match any route
     */
    suspend fun renderWithPreload(url: String, userAgent: String? = null): String? {
        val path = UrlLikePath.fromUrlString(url)
        val page = routes.parse(path) ?: return null
        return renderPageWithPreload(page, userAgent)
    }

    /**
     * Parse a URL and render with preloading, using the fallback page for unknown routes.
     *
     * @param url The URL path
     * @param userAgent Optional User-Agent string for platform detection - by Claude
     * @return Complete HTML document string (never null - uses fallback for 404)
     */
    suspend fun renderOrFallbackWithPreload(url: String, userAgent: String? = null): String {
        val path = UrlLikePath.fromUrlString(url)
        val page = routes.parseOrFallback(path)
        return renderPageWithPreload(page, userAgent)
    }

    /**
     * Render a specific Page instance with data preloading.
     *
     * Uses single-pass rendering with automatic resource discovery:
     * 1. Render creates FutureElement tree with reactive bindings
     * 2. ssrResource() calls register resources and start loading
     * 3. awaitAllResources() waits for all data to load
     * 4. Reactive bindings auto-update the FutureElement tree
     * 5. serialize() captures the updated HTML
     * 6. Final HTML includes embedded __SSR_DATA__
     *
     * If an appWrapper is configured, it will be used to render the full app shell
     * with navigation. Otherwise, pages are rendered directly.
     *
     * Also supports legacy [SsrPreloadable] interface for backwards compatibility.
     *
     * @param page The Page to render
     * @param userAgent Optional User-Agent string from the HTTP request.
     *                  Used for platform detection (e.g., Platform.probablyAppleUser)
     *                  to ensure SSR output matches client-side hydration expectations.
     *                  - by Claude
     * @return Complete HTML document string
     */
    suspend fun renderPageWithPreload(page: Page, userAgent: String? = null): String {
        val context = SsrContext(basePath, userAgent = userAgent)
        context.theme = theme  // Set theme on context - willAddChild applies SetAsBase (no padding on root)
        context.title = page.title.state.getOrNull()

        // Legacy support: Call preload if the page implements SsrPreloadable
        page.asPreloadable()?.preload(context)

        // Single render - creates reactive structure with bindings
        // ssrResource() calls during render will register resources and start loading
        context.render {
            // Create navigators for SSR
            val navigator = PageNavigator { routes }
            val dialog = PageNavigator { routes }

            // Set the current page in the navigator stack
            navigator.reset(page)

            // Set navigators on the ViewWriter context
            this.pageNavigator = navigator
            this.mainPageNavigator = navigator
            this.dialogPageNavigator = dialog

            if (appWrapper != null) {
                // Use the app wrapper for full navigation shell
                // Don't use theme.onNext here - SsrContext's willAddChild sets SetAsBase(theme)
                // which correctly uses withBackNoPadding (background but no padding on root)
                appWrapper(navigator, dialog)
            } else {
                // Render page directly without navigation
                col {
                    with(page) {
                        render()
                    }
                }
            }
        }

        try {
            // Await all discovered resources
            // Reactive bindings auto-update the FutureElement tree when resources load
            context.awaitAllResources()

            // Serialize the now-complete tree (with updated reactive values) + resource data
            val result = context.serialize()
            return document.render(result, context.exportResourceData())
        } finally {
            context.cancel()
        }
    }

    // ==================== Non-suspend versions (no preloading) ====================

    /**
     * Parse a URL string and render the corresponding page to HTML.
     *
     * Note: This does NOT call preload(). Use [renderWithPreload] for pages
     * that need async data loading.
     *
     * @param url The URL path (e.g., "/docs/getting-started" or "/users/123?tab=profile")
     * @return Complete HTML document string, or null if the URL doesn't match any route
     */
    fun render(url: String): String? {
        val path = UrlLikePath.fromUrlString(url)
        val page = routes.parse(path) ?: return null
        return renderPage(page)
    }

    /**
     * Parse a URL and render, using the fallback page for unknown routes.
     *
     * Note: This does NOT call preload(). Use [renderOrFallbackWithPreload] for pages
     * that need async data loading.
     *
     * @param url The URL path
     * @return Complete HTML document string (never null - uses fallback for 404)
     */
    fun renderOrFallback(url: String): String {
        val path = UrlLikePath.fromUrlString(url)
        val page = routes.parseOrFallback(path)
        return renderPage(page)
    }

    /**
     * Render a specific Page instance to HTML.
     *
     * Note: This does NOT call preload(). Use [renderPageWithPreload] for pages
     * that need async data loading.
     *
     * @param page The Page to render
     * @return Complete HTML document string
     */
    fun renderPage(page: Page): String {
        val context = SsrContext(basePath)
        context.theme = theme  // Set theme on context - willAddChild applies SetAsBase (no padding on root)
        context.title = page.title.state.getOrNull()

        val result = context.renderAndSerialize {
            col {
                with(page) {
                    render()
                }
            }
        }

        return document.render(result)
    }

    /**
     * Render a page with custom content wrapper.
     *
     * Useful when you need to wrap the page in additional layout components.
     *
     * @param page The Page to render
     * @param wrapper Function that receives the page and ViewWriter to customize rendering
     * @return Complete HTML document string
     */
    fun renderPage(page: Page, wrapper: ViewWriter.(Page) -> Unit): String {
        val context = SsrContext(basePath)
        context.title = page.title.state.getOrNull()

        val result = context.renderAndSerialize {
            wrapper(page)
        }

        return document.render(result)
    }

    /**
     * Get the Page for a URL without rendering.
     *
     * Useful for checking if a route exists or getting page metadata.
     *
     * @param url The URL path
     * @return The Page instance, or null if not found
     */
    fun getPage(url: String): Page? {
        val path = UrlLikePath.fromUrlString(url)
        return routes.parse(path)
    }

    /**
     * Get the Page for a URL, using fallback for unknown routes.
     *
     * @param url The URL path
     * @return The Page instance (never null - uses fallback for 404)
     */
    fun getPageOrFallback(url: String): Page {
        val path = UrlLikePath.fromUrlString(url)
        return routes.parseOrFallback(path)
    }
}
