package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.RContext
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import kotlin.math.min

public enum class PageNavigatorBehavior {
    /**
     * The browser back button returns the user to the previous screen state.
     * Reset does not carry over here.
     */
    Separate,

    /**
     * Reset will clear out the browser stack as well where possible.
     */
    Link,

    /**
     * Whatever the weird old behavior was.
     */
    Deprecated;

    public companion object {
        public var current: PageNavigatorBehavior = PageNavigatorBehavior.Separate
    }
}

@Deprecated("")
public var PageNavigatorUseExperimentalBehavior: Boolean
    get() = PageNavigatorBehavior.current == PageNavigatorBehavior.Link
    set(value) {
        PageNavigatorBehavior.current = if (value) PageNavigatorBehavior.Link else PageNavigatorBehavior.Deprecated
    }

public actual fun PageNavigator.bindToPlatform(context: RContext) {
    when (PageNavigatorBehavior.current) {
        PageNavigatorBehavior.Separate -> {
            val log: Console? = ConsoleRoot.tag("ScreenStack.bindToPlatform")
            val lastStackForPath = HashMap<UrlLikePath, List<Page>>()

            val initBar = window.location.urlLike()

            // load stack
            fun guessAndImplementFromUrlBar(urlLikePath: UrlLikePath) {
                stack.value = lastStackForPath.getOrPut(urlLikePath) {
                    routes.parseOrFallback(urlLikePath)?.let {
                        listOf(it)
                    } ?: listOf()
                }
            }
            guessAndImplementFromUrlBar(initBar)

            var suppressNav = false
            window.addEventListener("popstate", { event ->
                if (suppressNav) return@addEventListener
                event as PopStateEvent
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
                    if (s.lastOrNull() != lastStack.lastOrNull()) {
                        val new = s.lastOrNull() ?: return@reactiveScope
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
            AppScope.reactiveScope {
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

        PageNavigatorBehavior.Link -> {
            val log: Console? = ConsoleRoot.tag("ScreenStack.bindToPlatform")

            val initBar = window.location.urlLike()
            val nextStackId = PersistentProperty("last-stack-id", 'a')
            val currentStackId =
                initBar.parameters["s"] ?: nextStackId.value.also { nextStackId.value = it + 1 }.toString()
            val currentStack = PersistentProperty<List<String>>("main-stack-$currentStackId", listOf())
                .lens(
                    get = { it.map { UrlLikePath.fromUrlString(it) } },
                    set = { it.map { it.render() } }
                )

            // load stack
            fun guessAndImplementFromUrlBar(urlLikePath: UrlLikePath) {
                val fixed = urlLikePath.copy(parameters = urlLikePath.parameters - "s")
                val index = currentStack.value.indexOfLast { it == fixed }
                if (index == -1) {
                    // Append if not found
                    log?.log("guessAndImplementFromUrlBar: appending new value '$fixed'")
                    currentStack.value += fixed
                } else {
                    // Keep history that matches
                    log?.log("guessAndImplementFromUrlBar: loading stack position ${index}")
                    currentStack.value = currentStack.value.subList(0, index + 1)
                }
                this.stack.value = currentStack.value.mapNotNull { routes.parseOrFallback(it) }
            }
            guessAndImplementFromUrlBar(initBar)

            var suppressNav = false
            window.addEventListener("popstate", { event ->
                if (suppressNav) return@addEventListener
                event as PopStateEvent
                try {
                    suppressNav = true
                    (event.state as Int?)
                        ?.takeIf { it < stack.value.size }
                        ?.takeIf { window.location.urlLike() == routes.render(stack.value[it])?.urlLikePath }
                        ?.let {
                            log?.log("popstate pops back to index $it")
                            stack.value = stack.value.subList(0, it + 1)
                        } ?: run {
                        log?.log("popstate is going to match the best it can")
                        guessAndImplementFromUrlBar(window.location.urlLike())
                    }
                } finally {
                    suppressNav = false
                }
            })
            AppScope.reactiveScope {
                // After boot, we want to make storage ALWAYS match the public actual stack.
                currentStack.value = stack().mapNotNull { routes.render(it)?.urlLikePath }
            }
            var lastStack = stack.value
            AppScope.reactiveScope {
                suppressNav = true
                try {
                    // Whenever the stack changes, we need to update the history as best we can.
                    val s = stack()
                    if (!suppressNav) {
                        var indexWhereChangesStart = s.zip(lastStack).indexOfFirst { it.first != it.second }
                        if (indexWhereChangesStart == -1) indexWhereChangesStart = min(s.size, lastStack.size)
                        val removed = lastStack.subList(indexWhereChangesStart, lastStack.size)
                        val added = s.subList(indexWhereChangesStart, s.size)
                        if (removed.isEmpty() && added.isEmpty()) return@reactiveScope
                        log?.log("Nav changed!  Removed $removed, added $added")
                        val canDoSwap = removed.isNotEmpty() && added.isNotEmpty()
                        if (removed.isNotEmpty()) {
                            val toRemove = if (canDoSwap) removed.dropLast(1) else removed
                            // Pop the states
                            for (item in toRemove) {
                                val rendered = routes.render(item)
                                if (rendered == null) continue
                                log?.log("Popping off (we hope) '${rendered?.urlLikePath?.render()}'...")
                                window.history.back()
                            }
                        }
                        if (canDoSwap) {
                            val it = routes.render(added.first())?.urlLikePath ?: return@reactiveScope
                            log?.log("Swapping '${it.render()}'...")
                            window.history.replaceState(
                                stack.value.lastIndex,
                                "",
                                basePath + it.copy(parameters = it.parameters + ("s" to currentStackId)).render()
                            )
                        }
                        if (added.isNotEmpty()) {
                            for ((index, new) in added.withIndex()) {
                                if (canDoSwap && index == 0) continue
                                routes.render(new)?.urlLikePath?.let {
                                    log?.log("Pushing (we hope) '${it.render()}' as index ${index + indexWhereChangesStart}...")
                                    window.history.pushState(
                                        index + indexWhereChangesStart,
                                        "",
                                        basePath + it.copy(parameters = it.parameters + ("s" to currentStackId))
                                            .render()
                                    )
                                }
                            }
                        }
                    }
                    lastStack = s
                } finally {
                    suppressNav = false
                }
            }
            AppScope.reactiveScope {
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
                            basePath + it.copy(parameters = it.parameters + ("s" to currentStackId)).render()
                        )
                    }
                }
            }

            AppScope.reactive {
                document.title = stack().lastOrNull()?.title?.invoke() ?: "App"
            }
        }

        PageNavigatorBehavior.Deprecated -> {
            val log: Console? = ConsoleRoot.tag("ScreenStack.bindToPlatform")
            val storedStack = PersistentProperty<List<String>>("main-stack", listOf())

            fun guessAndImplementFromUrlBar() {
                val urlBar = window.location.urlLike()
                val goToIndex = storedStack.value.indexOfLast { UrlLikePath.fromUrlString(it) == urlBar }
                log?.log("Finding $urlBar in ${storedStack.value}, index $goToIndex")
                if (goToIndex == -1) {
                    log?.log("Could not find, pushing")
                    val newPage = (routes.parseOrFallback(urlBar) ?: routes.fallback)
                    this.stack.value =
                        storedStack.value.mapNotNull { routes.parseOrFallback(UrlLikePath.fromUrlString(it)) } + newPage
                    routes.render(newPage)?.let { storedStack.value += it.urlLikePath.render() }
                } else {
                    log?.log("Found, popping backwards")
                    storedStack.value = storedStack.value.subList(0, goToIndex + 1)
                    this.stack.value =
                        storedStack.value.mapNotNull { routes.parseOrFallback(UrlLikePath.fromUrlString(it)) }
                }
            }
            guessAndImplementFromUrlBar()

            var suppressNav = false
            window.addEventListener("popstate", { event ->
                if (suppressNav) return@addEventListener
                event as PopStateEvent
                try {
                    suppressNav = true
                    (event.state as Int?)
                        ?.takeIf { it < stack.value.size }
                        ?.takeIf { window.location.urlLike() == routes.render(stack.value[it])?.urlLikePath }
                        ?.let {
                            log?.log("popstate pops back to index $it")
                            stack.value = stack.value.subList(0, it + 1)
                        } ?: run {
                        log?.log("popstate is going to match the best it can")
                        guessAndImplementFromUrlBar()
                    }
                } finally {
                    suppressNav = false
                }
            })
            AppScope.reactiveScope {
                // After boot, we want to make storage ALWAYS match the public actual stack.
                storedStack.value = stack().mapNotNull { routes.render(it)?.urlLikePath?.render() }
            }
            var lastStack = stack.value
            AppScope.reactiveScope {
                // Whenever the stack changes, we need to update the history as best we can.
                val s = stack()
                if (!suppressNav) {
                    var indexWhereChangesStart = s.zip(lastStack).indexOfFirst { it.first != it.second }
                    if (indexWhereChangesStart == -1) indexWhereChangesStart = min(s.size, lastStack.size)
                    val removed = lastStack.subList(indexWhereChangesStart, lastStack.size)
                    val added = s.subList(indexWhereChangesStart, s.size)
                    log?.log("Nav changed!  Removed $removed, added $added")
                    if (added.isNotEmpty()) {
                        for ((index, new) in added.withIndex()) {
                            routes.render(new)?.urlLikePath?.render()?.let {
                                log?.log("Pushing $it as index ${index + indexWhereChangesStart}...")
                                window.history.pushState(index + indexWhereChangesStart, "", basePath + it)
                            }
                        }
                    }
                }
                lastStack = s
            }
            AppScope.reactiveScope {
                // Whenever the stack's top changes, we want to update the URL bar.
                val s = stack()
                s.lastOrNull()?.let { routes.render(it) }?.let {
                    it.listenables.forEach { rerunOn(it) }
                    log?.log("Replacing  state as index ${s.lastIndex}")
                    window.history.replaceState(
                        s.lastIndex, "", basePath + it.urlLikePath.render()
                    )
                }
            }
        }
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
    .also { println("Base path is $it") }

private fun Location.urlLike() = UrlLikePath(
    segments = pathname.removePrefix("/" + basePath.substringAfter("://").substringAfter('/')).split('/')
        .filter { it.isNotBlank() },
    parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
        .associate { it.substringBefore('=') to decodeURIComponent(it.substringAfter('=')) }
)