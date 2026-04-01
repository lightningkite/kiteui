@file:OptIn(OverrideOnly::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.dom.DOMElement
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.ssr.HydrationContext
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers

private class Root(val beforeDocumentAppend: Element.() -> Unit) : ViewWriter, CoroutineScope by AppScope {
    override val context: ElementContext = ElementContext(basePath).also {
        @Suppress("DEPRECATION")
        ExternalServices.baseContext = it
    }

    override fun willAddChild(element: Element) {}

    override fun addChild(element: Element) {
        println("Root adding element: $element")
        beforeDocumentAppend(element)
        document.body?.append(element.native.create())
    }
}

fun root(theme: Theme, app: ViewWriter.() -> Unit) {
    Root {
        themeChoice = ThemeDerivation.SetAsBase(theme)
    }.app()
}

fun root(theme: Reactive<Theme>, app: ViewWriter.() -> Unit) {
    Root {
        ::themeChoice {
            ThemeDerivation.SetAsBase(theme()).also { println("Setting theme base: ${it.theme.id}") }
        }
    }.run {
        if (debugMode) setupDebugSafeInsets()

        app()
    }
}

/**
 * Hydrate the application by reusing existing SSR-rendered DOM elements.
 * Call this instead of root() when the page was server-side rendered.
 *
 * Uses deferred hydration to allow reactive bindings to execute before matching
 * the view tree against the DOM. This prevents hydration mismatches caused by
 * reactive scopes that populate children asynchronously.
 *
 * ## Hydration Contract
 * For successful hydration, components must produce the same DOM structure on
 * both SSR and client. When mismatches occur (e.g., different tags), the SSR
 * element is replaced with a freshly created client element.
 *
 * by Claude
 *
 * @param theme The application theme
 * @param app The application content builder
 */
fun hydrateRoot(theme: Theme, app: ViewWriter.() -> Unit) {
    hydrateRootInternal(
        themeApplicator = { view -> view.themeChoice = ThemeDerivation.SetAsBase(theme) },
        fallback = { root(theme, app) },
        app = app
    )
}

/**
 * Hydrate the application by reusing existing SSR-rendered DOM elements (reactive theme version).
 * Call this instead of root() when the page was server-side rendered.
 *
 * Uses deferred hydration to allow reactive bindings to execute before matching
 * the view tree against the DOM. This prevents hydration mismatches caused by
 * reactive scopes that populate children asynchronously.
 *
 * ## Hydration Contract
 * For successful hydration, components must produce the same DOM structure on
 * both SSR and client. When mismatches occur (e.g., different tags), the SSR
 * element is replaced with a freshly created client element.
 *
 * by Claude
 *
 * @param theme The reactive application theme
 * @param app The application content builder
 */
fun hydrateRoot(theme: Reactive<Theme>, app: ViewWriter.() -> Unit) {
    hydrateRootInternal(
        themeApplicator = { view ->
            with(view) {
                ::themeChoice { ThemeDerivation.SetAsBase(theme()) }
            }
        },
        fallback = { root(theme, app) },
        setupDebugMode = true,
        app = app
    )
}

/**
 * Internal implementation of hydration logic shared between theme variants.
 * Eliminates code duplication between hydrateRoot overloads.
 * by Claude
 */
@OptIn(DelicateCoroutinesApi::class)
private fun hydrateRootInternal(
    themeApplicator: (Element) -> Unit,
    fallback: () -> Unit,
    setupDebugMode: Boolean = false,
    app: ViewWriter.() -> Unit
) {
    HydrationContext.initFromDom()

    val body = document.body ?: return fallback()

    // Check if there are any SSR-rendered children to hydrate
    if (!HydrationContext.isHydrating || body.childElementCount == 0) {
        HydrationContext.clear()
        return fallback()
    }

    // Create RContext with Unconfined dispatcher to encourage synchronous reactive execution
    // by Claude
    val elementContext = ElementContext(basePath).apply {
        ssrDispatcher = Dispatchers.Unconfined
        ExternalServices.baseContext = this  // Set consistently for both theme variants
    }

    // Track current child index for matching against SSR DOM
    var childIndex = 0
    val pendingHydrations = mutableListOf<Pair<Element, DOMElement>>()

    val root = object : ViewWriter, CoroutineScope by AppScope {
        override val context: ElementContext = elementContext

        override fun willAddChild(element: Element) {}

        override fun addChild(element: Element) {
            themeApplicator(element)

            val existingChild = body.children.item(childIndex)
            if (existingChild != null && HydrationContext.isHydrating) {
                // Defer hydration - collect view and target element
                pendingHydrations.add(element to existingChild)
                childIndex++
            } else {
                body.append(element.native.create())
            }
        }
    }

    if (setupDebugMode && debugMode) {
        root.setupDebugSafeInsets()
    }

    root.app()

    // Perform deferred hydration using queueMicrotask for faster execution
    // Falls back to setTimeout(0) if queueMicrotask is unavailable
    // Mismatches are handled gracefully by creating fresh elements when tags don't match
    // by Claude
    if (pendingHydrations.isNotEmpty()) {
        val hydrationStartTime = kotlin.js.Date.now()

        queueMicrotaskOrTimeout {
            try {
                pendingHydrations.forEach { (view, existingChild) ->
                    view.native.hydrateRecursive(existingChild)
                }

                // Clear ssrDispatcher after hydration
                elementContext.ssrDispatcher = null

                val elapsed = kotlin.js.Date.now() - hydrationStartTime
                HydrationContext.recordHydrationTime(elapsed)
                HydrationContext.clear()
            } catch (e: Exception) {
                console.error("[KiteUI Hydration] Hydration failed, page may be in inconsistent state:", e)
                elementContext.ssrDispatcher = null
                HydrationContext.clear()
                // Note: We don't fall back to full CSR here as partial hydration may have occurred
                // and re-rendering could cause flicker or data loss
            }
        }
    } else {
        elementContext.ssrDispatcher = null
        HydrationContext.clear()
    }
}

/**
 * Smart root that auto-detects SSR and hydrates if appropriate.
 * Checks for __SSR_DATA__ element to determine if the page was server-side rendered.
 *
 * @param theme The application theme
 * @param app The application content builder
 */
fun smartRoot(theme: Theme, app: ViewWriter.() -> Unit) {
    val ssrDataElement = document.getElementById("__SSR_DATA__")
    if (ssrDataElement != null) {
        console.log("[KiteUI Hydration] SSR data detected, starting hydration...")
        hydrateRoot(theme, app)
        // Note: Actual hydration completion is logged by HydrationContext.clear()
    } else {
        console.log("[KiteUI] Client-side rendering (no SSR data found)")
        root(theme, app)
    }
}

/**
 * Smart root that auto-detects SSR and hydrates if appropriate (reactive theme version).
 * Checks for __SSR_DATA__ element to determine if the page was server-side rendered.
 *
 * @param theme The reactive application theme
 * @param app The application content builder
 */
fun smartRoot(theme: Reactive<Theme>, app: ViewWriter.() -> Unit) {
    val ssrDataElement = document.getElementById("__SSR_DATA__")
    if (ssrDataElement != null) {
        console.log("[KiteUI Hydration] SSR data detected, starting hydration...")
        hydrateRoot(theme, app)
        // Note: Actual hydration completion is logged by HydrationContext.clear()
    } else {
        console.log("[KiteUI] Client-side rendering (no SSR data found)")
        root(theme, app)
    }
}

/**
 * Setup debug safe insets keyboard shortcut (Alt+E to toggle).
 * by Claude
 */
private fun ElementWriter.setupDebugSafeInsets() {
    val safe = Signal(Edges.ZERO)
    context.safeInsets = safe
    var times = 0
    AppState.onUniversalKeyboard {
        if (it.alt && it.code == KeyCodes.letter('e')) {
            println("Setting edges")
            safe.value = if (times++ % 2 == 0) Edges(100.dp) else Edges.ZERO
            true
        } else false
    }
}

/**
 * Schedule a task using queueMicrotask if available, otherwise fall back to setTimeout(0).
 * queueMicrotask executes earlier in the event loop (after current task, before rendering).
 * by Claude
 */
private fun queueMicrotaskOrTimeout(block: () -> Unit) {
    // Check if queueMicrotask is available (modern browsers)
    val hasQueueMicrotask = js("typeof queueMicrotask === 'function'") as Boolean
    if (hasQueueMicrotask) {
        js("queueMicrotask")(block)
    } else {
        window.setTimeout(block, 0)
    }
}
