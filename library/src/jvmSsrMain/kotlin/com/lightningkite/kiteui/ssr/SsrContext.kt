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
 * @param windowWidth The assumed window width for responsive calculations
 * @param windowHeight The assumed window height for responsive calculations
 * @param userAgent The client's User-Agent string from the HTTP request.
 *                  This is used to determine Platform.probablyAppleUser and other
 *                  platform-specific rendering decisions to match client-side hydration.
 *                  - by Claude
 */
class SsrContext(
    val basePath: String = "/",
    val windowWidth: Int = 1920,
    val windowHeight: Int = 1080,
    val userAgent: String? = null,
) : SsrResourceRegistry {
    /** The coroutine scope for SSR operations */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The underlying RContext for KiteUI rendering */
    val elementContext = ElementContext(basePath)

    /** The default theme to use for rendering */
    var theme: Theme? = null

    /** Page metadata - can be set during rendering */
    var title: String? = null
    var description: String? = null
    var canonicalUrl: String? = null
    val metaTags: MutableMap<String, String> = mutableMapOf()

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
        resources[resource.key] = resource
        resource.startLoading(scope)
    }

    /**
     * Await all registered resources until they are loaded.
     * Throws if any resource fails to load.
     */
    suspend fun awaitAllResources() {
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
    fun exportResourceData(): Map<String, String> {
        return resources.mapValues { (_, resource) -> resource.serialize() }
    }

    /**
     * Preload data that can be retrieved during rendering.
     */
    fun <T> preload(key: String, value: T) {
        preloadedData[key] = value
    }

    /**
     * Get preloaded data by key.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getPreloaded(key: String): T? = preloadedData[key] as? T

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
    fun render(content: ViewWriter.() -> Unit) {
        // Set user agent context for platform detection during rendering - by Claude
        SsrUserAgentContext.withUserAgent(userAgent) {
            // Flush any pending CSS from previous operations
            elementContext.dynamicCss.flush()

            val frame = Frame(elementContext)
            // Use Unconfined so reactive bindings update synchronously when resources load
            val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

            @OptIn(OverrideOnly::class)
            val viewWriter = object : ViewWriter, CoroutineScope by appScope {
                override val context: ElementContext = elementContext

                override fun willAddChild(element: Element) {
                    theme?.let { t ->
                        // Use direct assignment (not reactive binding) to match JS behavior
                        // This ensures the theme is applied immediately before the view is added
                        // SetAsBase creates a theme with background but NO padding (for root elements)
                        element.themeChoice = ThemeDerivation.SetAsBase(t)
                    }
                }

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
    fun serialize(): SsrResult {
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
    fun renderAndSerialize(content: ViewWriter.() -> Unit): SsrResult {
        render(content)
        return serialize()
    }

    /**
     * Render a Page (deferred serialization).
     */
    fun renderPage(page: Page) {
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
    fun renderPageAndSerialize(page: Page): SsrResult {
        renderPage(page)
        return serialize()
    }
}
