package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.dismissTopDialog
import com.lightningkite.kotlinx.serialization.uri.decodeURIComponent
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*

/**
 * Determines how the page navigator interacts with the browser history API.
 * Only [Separate] is active; [Link] and [Deprecated] are retained for source
 * compatibility but no longer change behavior.
 */
public enum class PageNavigatorBehavior {
    /**
     * Each navigate() call creates one browser history entry.  Browser back /
     * forward move through the app stack accordingly.  This is the only active
     * strategy.
     */
    Separate,

    /**
     * Formerly reset-aware history linking.  Now a no-op alias for [Separate].
     */
    @Deprecated("No longer has any effect; PageNavigatorBehavior.Separate is always used.", level = DeprecationLevel.WARNING)
    Link,

    /**
     * Formerly the legacy behavior.  Now a no-op alias for [Separate].
     */
    @Deprecated("No longer has any effect; PageNavigatorBehavior.Separate is always used.", level = DeprecationLevel.WARNING)
    Deprecated;

    public companion object {
        /** Setting this is a no-op; [Separate] is always used. */
        @Deprecated("PageNavigatorBehavior is no longer configurable; Separate is always used.", level = DeprecationLevel.WARNING)
        public var current: PageNavigatorBehavior = PageNavigatorBehavior.Separate
    }
}

/** Setting this is a no-op; [PageNavigatorBehavior.Separate] is always used. */
@Deprecated("PageNavigatorBehavior is no longer configurable; Separate is always used.", level = DeprecationLevel.WARNING)
public var PageNavigatorUseExperimentalBehavior: Boolean
    get() = false
    set(@Suppress("UNUSED_PARAMETER") value) { /* no-op */ }

/**
 * A map with a maximum size that evicts least-recently-used entries.
 * Both get() and put() mark an entry as recently used.
 */
private class MaxSizeMap<K : Any, V>(val maxSize: Int) : LinkedHashMap<K, V>() {
    override fun get(key: K): V? {
        val value = super.remove(key) ?: return null
        super.put(key, value)  // Move to end (most recently used)
        return value
    }

    override fun put(key: K, value: V): V? {
        val old = super.remove(key)  // Remove to reorder
        val result = super.put(key, value)  // Add at end
        while (size > maxSize) {
            val oldest = keys.iterator().next()
            super.remove(oldest)
        }
        return old ?: result
    }
}

public actual fun PageNavigator.bindToPlatform(context: ElementContext) {
    val beforeUnload = { event: Event ->
        val canLeave = this.currentPage.state.raw?.let { it as? CanBlockBack }?.onNavigateAwayAttempt() ?: true
        if (!canLeave) {
            event.preventDefault();
            event.asDynamic().returnValue = ""; // Required for Chrome
        }
    }
    window.addEventListener("beforeunload", beforeUnload)

    val log: Log? = null
    val lastStackForPath = MaxSizeMap<UrlLikePath, List<Page>>(maxSize = 50)

    val initBar = window.location.urlLike()

    // load stack
    fun guessAndImplementFromUrlBar(urlLikePath: UrlLikePath) {
        stack.value = lastStackForPath.getOrPut(urlLikePath) {
            listOf(routes.parseOrFallback(urlLikePath))
        }
    }
    guessAndImplementFromUrlBar(initBar)

    var suppressNav = false
    window.addEventListener("popstate", { event ->
        if (suppressNav) return@addEventListener
        event as PopStateEvent

        // Dialog-dismiss intercept: if a dismissable dialog is open, close it
        // and re-push the current URL so the browser back button still works.
        if (context.dismissTopDialog()) {
            val currentUrl = routes.render(stack.value.lastOrNull() ?: return@addEventListener)
                ?.urlLikePath ?: return@addEventListener
            window.history.pushState(null, "", basePath + currentUrl.render())
            return@addEventListener
        }

        try {
            suppressNav = true
            guessAndImplementFromUrlBar(window.location.urlLike())
        } finally {
            suppressNav = false
        }
    })

    var lastStack = stack.value
    AppScope.reactive {
        val s = stack()
        if (suppressNav) return@reactive
        try {
            suppressNav = true
            if (s.lastOrNull() != lastStack.lastOrNull()) {
                val new = s.lastOrNull() ?: return@reactive
                routes.render(new)?.urlLikePath?.let {
                    log?.log("pushState '${it.render()}'...")
                    window.history.pushState(
                        null,
                        "",
                        basePath + it.render()
                    )
                }
            }
        } finally {
            suppressNav = false
        }
    }
    AppScope.reactive {
        // Whenever the stack's top changes, we want to update the URL bar.
        val s = stack()
        s.lastOrNull()?.let { routes.render(it) }?.let {
            it.listenables.forEach { rerunOn(it) }
            if (suppressNav) return@let
            log?.log("Replacing state as index ${s.lastIndex} with '${it.urlLikePath.render()}'")
            it.urlLikePath.let {
                window.history.replaceState(
                    s.lastIndex,
                    "",
                    basePath + it.render()
                )
            }
            lastStackForPath[it.urlLikePath] = s
        }
    }

    AppScope.reactive {
        document.title = stack().lastOrNull()?.title?.invoke() ?: "App"
    }
}

// From URL Bar
// From Stack / Last Update

public external interface BaseUrlScript {
    public val baseUrl: String
}

public var basePath: String = ((document.getElementById("baseUrlLocation") as? HTMLScriptElement)
    ?.innerText
    ?.let { JSON.parse<BaseUrlScript>(it).baseUrl }
    ?: document.baseURI.takeIf { document.getElementsByTagName("base").length != 0 }
    ?: "/")
    .also { Log.info("Base path is $it") }

internal fun Location.urlLike() = UrlLikePath(
    segments = pathname.removePrefix("/" + basePath.substringAfter("://").substringAfter('/')).split('/')
        .filter { it.isNotBlank() }.map { decodeURIComponent(it) },
    parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
        .associate { decodeURIComponent(it.substringBefore('=')) to decodeURIComponent(it.substringAfter('=')) }
)

public actual fun PageNavigator.askForConfirmNavigateAway(): Boolean {
    return window.confirm(
        "Are you sure you want to leave this page?\nChanges will not be saved."
    )
}
