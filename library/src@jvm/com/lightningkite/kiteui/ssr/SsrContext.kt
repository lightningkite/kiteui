package com.lightningkite.kiteui.ssr

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.SsrUserAgentContext
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay

/**
 * Context for a single SSR request. Each request should create its own SsrContext
 * to ensure isolation between concurrent requests.
 *
 * Usage:
 * ```kotlin
 * val context = SsrContext("/", userAgent = request.headers["User-Agent"])
 * val result = context.render { col { text("Hello World") } }
 * // Or render a page:
 * val result = context.renderPage(MyPage())
 * ```
 *
 * @param basePath The base path for URL resolution
 * @param userAgent The client's User-Agent string from the HTTP request.
 *                  This is used to determine Platform.probablyAppleUser and other
 *                  platform-specific rendering decisions to match client-side hydration.
 *                  - by Claude
 */
public class SsrContext(
    internal val basePath: String = "/",
    internal val userAgent: String? = null,
) : SsrResourceRegistry {
    /**
     * The viewport (window width/height) is unavailable during SSR: the server never learns the
     * client's actual screen size, so any value here would be a guess that can silently mismatch
     * real devices - e.g. a phone hydrating a tree that was structured for a hard-coded desktop
     * width. These accessors throw instead of returning a guessed value, so that any code that
     * needs viewport-dependent DOM *structure* fails loudly during SSR rather than shipping a
     * layout that's subtly wrong on hydration. Structural (DOM-shape) responsiveness must be
     * CSS-only (media queries) or use the coarse [com.lightningkite.kiteui.Platform] hint instead.
     * - by Claude
     */
    internal val windowWidth: Int get() = throw UnsupportedOperationException(
        "Viewport width is unavailable during SSR; structural (DOM-shape) responsiveness must be " +
            "CSS-only - use CSS media queries or the coarse Platform hint instead."
    )
    internal val windowHeight: Int get() = throw UnsupportedOperationException(
        "Viewport height is unavailable during SSR; structural (DOM-shape) responsiveness must be " +
            "CSS-only - use CSS media queries or the coarse Platform hint instead."
    )
    /** The coroutine scope for loading SsrResource data */
    private val loadingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The coroutine scope for view rendering (unconfined so reactive updates run synchronously) */
    private val renderScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    /** Cancel all coroutines started during rendering. Call this after serialize() is done. */
    internal fun cancel() {
        // Shut down the element tree first so onRemove callbacks fire before scopes are cancelled
        renderedFrame?.onShutdown()
        loadingScope.cancel()
        renderScope.cancel()
    }

    /** The underlying RContext for KiteUI rendering */
    internal val elementContext: ElementContext = ElementContext(basePath)

    /** The default theme to use for rendering */
    internal var theme: Theme? = null

    /** Page metadata - can be set during rendering */
    internal var title: String? = null
    internal var description: String? = null
    internal var canonicalUrl: String? = null
    internal val metaTags: MutableMap<String, String> = mutableMapOf()

    /** Storage for preloaded data (legacy API) */
    private val preloadedData = mutableMapOf<String, Any?>()

    /** SSR resources registered during rendering (LinkedHashMap preserves insertion order) */
    private val resources = linkedMapOf<String, SsrResource<*>>()

    init {
        // Register this context as the SSR resource registry
        elementContext.ssrResourceRegistry = this
        // Use Unconfined dispatcher for synchronous reactive scope execution in SSR
        elementContext.ssrDispatcher = Dispatchers.Unconfined
    }

    /**
     * Register an SSR resource for tracking and data loading.
     */
    override fun registerResource(resource: SsrResource<*>) {
        // Keys must be unique per page; a silent overwrite here would drop the first
        // resource's data from the exported map while leaving it wired into that
        // component's reactive bindings, producing incomplete SSR HTML with no error.
        check(resource.key !in resources) { "Duplicate SsrResource key: '${resource.key}' - keys must be unique per page" }
        resources[resource.key] = resource
        resource.startLoading(loadingScope)
    }

    /**
     * Await all registered resources until they are loaded.
     * Throws if any resource fails to load.
     */
    internal suspend fun awaitAllResources() {
        resources.values.forEach { resource ->
            resource.awaitLoaded()
        }
        // Brief delay to ensure all reactive bindings have propagated
        // This gives the Unconfined dispatcher time to process queued updates
        delay(1)
    }

    /**
     * Export all resource data as a map of key to serialized JSON.
     * Call this after awaitAllResources() completes.
     */
    internal fun exportResourceData(): Map<String, String> {
        return resources.mapValues { (_, resource) -> resource.serialize() }
    }

    /**
     * Preload data that can be retrieved during rendering.
     */
    internal fun <T> preload(key: String, value: T) {
        preloadedData[key] = value
    }

    /**
     * Get preloaded data by key.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T> getPreloaded(key: String): T? = preloadedData[key] as? T

    /** The rendered frame - stored for deferred serialization */
    private var renderedFrame: Frame? = null

    /**
     * Render content using the ViewWriter DSL.
     * Builds the component tree but defers HTML serialization.
     * Call [serialize] after awaiting resources to get the final HTML.
     *
     * Uses Dispatchers.Unconfined so reactive updates happen synchronously
     * when resources complete loading.
     *
     * The userAgent from this SsrContext is set during rendering so that
     * Platform.probablyAppleUser returns correct values for the client.
     * - by Claude
     */
    internal fun render(content: ViewWriter.() -> Unit) {
        // Set user agent context for platform detection during rendering - by Claude
        SsrUserAgentContext.withUserAgent(userAgent) {
            // Flush any pending CSS from previous operations
            elementContext.dynamicCss.flush()

            val frame = Frame(elementContext)
            val viewWriter = object : ViewWriter, CoroutineScope by renderScope {
                override val context: ElementContext = elementContext

                @OverrideOnly
                override fun willAddChild(element: Element) {
                    theme?.let { t ->
                        // Use direct assignment (not reactive binding) to match JS behavior
                        // This ensures the theme is applied immediately before the view is added
                        // SetAsBase creates a theme with background but NO padding (for root elements)
                        element.themeChoice = ThemeDerivation.SetAsBase(t)
                    }
                }

                @OverrideOnly
                override fun addChild(element: Element) {
                    frame.addChild(element)
                }
            }

            with(viewWriter) {
                content()
            }

            // Store for later serialization
            renderedFrame = frame

            // Flush CSS rules after rendering
            elementContext.dynamicCss.flush()
        }
    }

    /**
     * Serialize the rendered frame to an SsrResult.
     * Call this AFTER awaiting all resources so reactive bindings have updated.
     */
    internal fun serialize(): SsrResult {
        val frame = renderedFrame ?: throw IllegalStateException("render() must be called before serialize()")

        // Build HTML from rendered content (now with updated reactive values)
        val html = buildString {
            frame.children.forEach { child ->
                child.native.render(this)
            }
        }

        return SsrResult(
            html = html,
            css = elementContext.dynamicCss.emit(),
            headElements = elementContext.dynamicCss.headElements.toList(),
            title = title,
            description = description,
            canonicalUrl = canonicalUrl,
            metaTags = metaTags.toMap()
        )
    }

    /**
     * Legacy: Render content and serialize immediately (no resource waiting).
     * For pages that don't use SsrResource, this is equivalent to the old behavior.
     */
    internal fun renderAndSerialize(content: ViewWriter.() -> Unit): SsrResult {
        render(content)
        return serialize()
    }

    /**
     * Render a Page (deferred serialization).
     */
    internal fun renderPage(page: Page) {
        // Use the page's title if we don't have one set
        if (title == null) {
            // The title is reactive, so we read its current value
            title = page.title.state.getOrNull()
        }

        render {
            with(page) {
                render()
            }
        }
    }

    /**
     * Legacy: Render a Page and serialize immediately.
     */
    internal fun renderPageAndSerialize(page: Page): SsrResult {
        renderPage(page)
        return serialize()
    }
}
