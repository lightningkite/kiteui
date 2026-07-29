package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import kotlin.reflect.KClass

public class Routes(
    public val parsers: List<(UrlLikePath) -> Page?>,
    public val renderers: Map<KClass<out Page>, (Page) -> RouteRendered?>,
    public val fallback: Page = Page.Direct("Not Found") {
        frame {
            centered.col {
                h1("Not Found")
                text("Sorry, we couldn't find the page you're looking for.")
            }
        }
    }
) {
    public fun render(screen: Page) = renderers.get(screen::class)?.invoke(screen)
    public fun parse(path: UrlLikePath): Page? = parsers.asSequence().mapNotNull { it(path) }.firstOrNull()
    public fun parseOrFallback(path: UrlLikePath) =
        try {
            parse(path) ?: fallback
        } catch(e: Exception) {
            LogRoot.warn("Encountered exception when parsing route: $e")
            fallback
        }
}

public data class RouteRendered(
    val urlLikePath: UrlLikePath,
    val listenables: List<Listenable>
)

public data class UrlLikePath(
    val segments: List<String>,
    val parameters: Map<String, String>
) {
    public companion object {
        public val EMPTY = UrlLikePath(listOf(), mapOf())

        public fun fromParts(pathname: String, search: String) = UrlLikePath(
            segments = pathname.split('/').filter { it.isNotBlank() },
            parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
                .associate { it.substringBefore('=') to decodeURIComponent(it.substringAfter('=')) }
        )

        public fun fromUrlString(url: String): UrlLikePath {
            val parts = url.split("?")
            return fromParts(parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "")
        }
    }

    // by Claude - removed debug println that fired on every route render
    public fun render() = segments.joinToString("/") + (parameters.takeUnless { it.isEmpty() }?.entries?.joinToString(
        "&",
        "?"
    ) { "${it.key}=${encodeURIComponent(it.value)}" } ?: "")

}


