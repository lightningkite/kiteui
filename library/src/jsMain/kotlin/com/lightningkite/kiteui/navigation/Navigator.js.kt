package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.dismissTopDialog
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

public actual fun PageNavigator.bindToPlatform(context: ElementContext) {
    val beforeUnload = { event: Event ->
        val canLeave = this.currentPage.state.raw?.let { it as? CanBlockBack }?.onNavigateAwayAttempt() ?: true
        if (!canLeave) {
            event.preventDefault();
            event.asDynamic().returnValue = ""; // Required for Chrome
        }
    }
    window.addEventListener("beforeunload", beforeUnload)

    // One-time cleanup: remove localStorage keys left behind by the removed Link / Deprecated modes.
    run {
        val keysToRemove = mutableListOf<String>()
        for (i in 0 until window.localStorage.length) {
            val key = window.localStorage.key(i) ?: continue
            if (key == "main-stack" || key.startsWith("main-stack-") || key == "last-stack-id") {
                keysToRemove.add(key)
            }
        }
        keysToRemove.forEach { window.localStorage.removeItem(it) }
    }

    val log: Log? = LogRoot.tag("ScreenStack.bindToPlatform")

    // Remembers the page stack for previously-visited URLs so back/forward can restore it.
    // Bounded (with oldest-first eviction) so a long session doesn't retain every stack -
    // and every Page it references - forever. Beyond the cap, an old URL simply reparses
    // into a fresh single-page stack, which is acceptable graceful degradation.
    val maxRememberedStacks = 50
    val lastStackForPath = LinkedHashMap<UrlLikePath, List<Page>>()
    fun rememberStack(path: UrlLikePath, value: List<Page>) {
        lastStackForPath.remove(path) // re-insert to refresh recency (insertion-ordered map)
        lastStackForPath[path] = value
        while (lastStackForPath.size > maxRememberedStacks) {
            lastStackForPath.remove(lastStackForPath.keys.first())
        }
    }

    val initBar = window.location.urlLike()

    // Restore the stack that matches the current URL, or parse a fresh one-page stack.
    fun guessAndImplementFromUrlBar(urlLikePath: UrlLikePath) {
        stack.value = lastStackForPath[urlLikePath]
            ?: listOf(routes.parseOrFallback(urlLikePath)).also { rememberStack(urlLikePath, it) }
    }
    guessAndImplementFromUrlBar(initBar)

    // suppressNav prevents re-entrant history mutations while we are already
    // reacting to a popstate event or programmatically driving history.
    var suppressNav = false

    window.addEventListener("popstate", { event ->
        if (suppressNav) return@addEventListener
        event as PopStateEvent

        // Dialog-dismiss intercept: if a dismissable dialog is open, close it
        // and re-push the current URL so the browser back button still works
        // for subsequent presses.
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
    AppScope.reactiveScope {
        val s = stack()
        if (suppressNav) return@reactiveScope
        try {
            suppressNav = true

            val isReset = s.size == 1 && lastStack.size > 1
            if (isReset) {
                // Clear browser forward/back history so the user cannot navigate back
                // into the old app stack after a reset.  Strategy:
                //   1. go(-(length-1)) to wind back all entries except the oldest
                //   2. replaceState the current page URL over that oldest entry
                // We guard with suppressNav during the async gap so the popstate
                // fired by go() does not trigger a stack change.
                val stepsBack = window.history.length - 1
                if (stepsBack > 0) {
                    // suppressNav remains true through the async popstate fired by go().
                    // We set it false again only after replaceState completes.
                    window.history.go(-stepsBack)
                    // go() is async; replaceState is safe to call synchronously right
                    // after because the popstate fires on the next event-loop tick.
                }
                val new = s.lastOrNull() ?: return@reactiveScope
                routes.render(new)?.urlLikePath?.let { url ->
                    log?.log("reset: replaceState '${url.render()}'")
                    window.history.replaceState(null, "", basePath + url.render())
                    rememberStack(url, s)
                }
            } else if (s.lastOrNull() != lastStack.lastOrNull()) {
                val new = s.lastOrNull() ?: return@reactiveScope
                routes.render(new)?.urlLikePath?.let { url ->
                    log?.log("pushState '${url.render()}'...")
                    window.history.pushState(null, "", basePath + url.render())
                }
            }
        } finally {
            // Delay releasing suppressNav by one microtask so the popstate event
            // triggered by history.go() (in the reset path) is still suppressed.
            val release = { suppressNav = false }
            window.setTimeout(release, 0)
        }
        lastStack = s
    }

    AppScope.reactiveScope {
        // Whenever the stack's top page changes its own URL (e.g. a query-param
        // update), keep the address bar in sync without creating a new history entry.
        val s = stack()
        s.lastOrNull()?.let { routes.render(it) }?.let {
            it.listenables.forEach { rerunOn(it) }
            if (suppressNav) return@let
            log?.log("Replacing state as index ${s.lastIndex} with '${it.urlLikePath.render()}'")
            window.history.replaceState(
                s.lastIndex,
                "",
                basePath + it.urlLikePath.render()
            )
            rememberStack(it.urlLikePath, s)
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

private fun Location.urlLike() = UrlLikePath(
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
