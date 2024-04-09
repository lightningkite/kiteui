package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.dom.HTMLScriptElement
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.NContext
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Location
import org.w3c.dom.MANUAL
import org.w3c.dom.PopStateEvent
import org.w3c.dom.ScrollRestoration
import kotlin.math.min

actual fun ScreenStack.bindToPlatform(context: NContext) {
    val storedStack = PersistentProperty<List<String>>("main-stack", listOf())

    fun guessAndImplementFromUrlBar() {
        val urlBar = window.location.urlLike()
        val goToIndex = storedStack.value.indexOfLast { UrlLikePath.fromUrlString(it) == urlBar }
        println("Finding $urlBar in ${storedStack.value}, index $goToIndex")
        if (goToIndex == -1) {
            println("Could not find, pushing")
            val newScreen = (routes.parse(urlBar) ?: routes.fallback)
            this.stack.value = storedStack.value.mapNotNull { routes.parse(UrlLikePath.fromUrlString(it)) } + newScreen
            routes.render(newScreen)?.let { storedStack.value += it.urlLikePath.render() }
        } else {
            println("Found, popping backwards")
            storedStack.value = storedStack.value.subList(0, goToIndex + 1)
            this.stack.value = storedStack.value.mapNotNull { routes.parse(UrlLikePath.fromUrlString(it)) }
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
                    println("popstate pops back to index $it")
                    stack.value = stack.value.subList(0, it + 1)
                } ?: run {
                println("popstate is going to match the best it can")
                guessAndImplementFromUrlBar()
            }
        } finally {
            suppressNav = false
        }
    })
    with(CalculationContext.NeverEnds) {
        reactiveScope {
            // After boot, we want to make storage ALWAYS match the actual stack.
            storedStack.value = stack().mapNotNull { routes.render(it)?.urlLikePath?.render() }
        }
        var lastStack = stack.value
        reactiveScope {
            // Whenever the stack changes, we need to update the history as best we can.
            val s = stack()
            if (!suppressNav) {
                var indexWhereChangesStart = s.zip(lastStack).indexOfFirst { it.first != it.second }
                if (indexWhereChangesStart == -1) indexWhereChangesStart = min(s.size, lastStack.size)
                val removed = lastStack.subList(indexWhereChangesStart, lastStack.size)
                val added = s.subList(indexWhereChangesStart, s.size)
                println("Nav changed!  Removed $removed, added $added")
                if (removed.isNotEmpty()) {
                    // Pop the states
                    try {
                        suppressNav = true
                        for (item in removed) {
                            if (routes.render(item) == null) continue
                            println("Popping...")
                            window.history.back()
                        }
                    } finally {
                        suppressNav = false
                    }
                }
                if (added.isNotEmpty()) {
                    for ((index, new) in added.withIndex()) {
                        routes.render(new)?.urlLikePath?.render()?.let {
                            println("Pushing $it as index ${index + indexWhereChangesStart}...")
                            window.history.pushState(index + indexWhereChangesStart, "", basePath + it)
                        }
                    }
                }
            }
            lastStack = s
        }
        reactiveScope {
            // Whenever the stack's top changes, we want to update the URL bar.
            val s = stack()
            s.lastOrNull()?.let { routes.render(it) }?.let {
                it.listenables.forEach { rerunOn(it) }
                println("Replacing  state as index ${s.lastIndex}")
                window.history.replaceState(
                    s.lastIndex, "", basePath + it.urlLikePath.render()
                )
            }
        }
    }
}

// From URL Bar
// From Stack / Last Update

external interface BaseUrlScript {
    val baseUrl: String
}

var basePath = (document.getElementById("baseUrlLocation") as? HTMLScriptElement)?.innerText?.let {
    JSON.parse<BaseUrlScript>(it).baseUrl
} ?: "/"

private fun Location.urlLike() = UrlLikePath(
    segments = pathname.removePrefix(basePath).split('/').filter { it.isNotBlank() },
    parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
        .associate { it.substringBefore('=') to decodeURIComponent(it.substringAfter('=')) }
)