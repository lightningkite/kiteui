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

    // Each browser history entry's state is the depth of the stack it shows, so the browser's entries mirror the
    // stack: navigate() pushes, replace() overwrites, and popping (goBack, a shorter reset) steps the browser back
    // with history.go() instead of overwriting, so browser Back and Forward match the app.
    window.history.replaceState(stack.value.lastIndex, "", window.location.href)
    var suppressNav = false
    var rewinding = false
    fun writeToHistory(s: List<Page>) {
        val top = s.lastOrNull()?.let { routes.render(it) } ?: return
        val url = basePath + top.urlLikePath.render()
        val depth = window.history.state as Int
        if (s.lastIndex < depth) {
            // history.go() is asynchronous; its popstate finishes the write with whatever the stack is by then.
            rewinding = true
            window.history.go(s.lastIndex - depth)
            return
        }
        for (i in depth + 1..s.lastIndex) {
            val pageUrl = routes.render(s[i])?.let { basePath + it.urlLikePath.render() } ?: url
            window.history.pushState(i, "", pageUrl)
        }
        log?.log("Replacing state as index ${s.lastIndex} with '$url'")
        window.history.replaceState(s.lastIndex, "", url)
        lastStackForPath[top.urlLikePath] = s
    }

    window.addEventListener("popstate", { event ->
        if (rewinding) {
            rewinding = false
            writeToHistory(stack.value)
            return@addEventListener
        }
        if (suppressNav) return@addEventListener
        event as PopStateEvent

        // Dialog-dismiss intercept: if a dismissable dialog is open, close it
        // and re-push the current URL so the browser back button still works.
        if (context.dismissTopDialog()) {
            val currentUrl = routes.render(stack.value.lastOrNull() ?: return@addEventListener)
                ?.urlLikePath ?: return@addEventListener
            window.history.pushState(stack.value.lastIndex, "", basePath + currentUrl.render())
            return@addEventListener
        }

        try {
            suppressNav = true
            guessAndImplementFromUrlBar(window.location.urlLike())
        } finally {
            suppressNav = false
        }
        // The stack remembered for this URL may be a different depth than the entry recorded (e.g. after a reload).
        window.history.replaceState(stack.value.lastIndex, "", window.location.href)
    })

    AppScope.reactive {
        val s = stack()
        s.lastOrNull()?.let { routes.render(it) }?.listenables?.forEach { rerunOn(it) }
        if (suppressNav || rewinding) return@reactive
        writeToHistory(s)
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
