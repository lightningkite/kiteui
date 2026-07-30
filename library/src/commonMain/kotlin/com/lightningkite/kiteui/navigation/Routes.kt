package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kotlinx.serialization.uri.decodeURIComponent
import com.lightningkite.kotlinx.serialization.uri.encodeURIComponent
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
    public fun render(screen: Page): RouteRendered? = renderers.get(screen::class)?.invoke(screen)
    public fun parse(path: UrlLikePath): Page? = parsers.asSequence().mapNotNull { it(path) }.firstOrNull()

    /**
     * Like [parse], but returns null instead of throwing when the path cannot be parsed.
     *
     * Generated route parsers decode path segments into typed parameters, so malformed input
     * raises rather than simply failing to match. Any path that came from outside the
     * application - a deep link, a restored navigation stack, a link inside user content -
     * must go through this or [parseOrFallback]; calling [parse] directly on such input
     * crashes the app on input an attacker or a stale saved state controls.
     */
    public fun parseOrNull(path: UrlLikePath): Page? =
        try {
            parse(path)
        } catch (e: Exception) {
            LogRoot.warn("Encountered exception when parsing route: $e")
            null
        }

    public fun parseOrFallback(path: UrlLikePath): Page =
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
        public val EMPTY: UrlLikePath = UrlLikePath(listOf(), mapOf())

        public fun fromParts(pathname: String, search: String): UrlLikePath = UrlLikePath(
            segments = pathname.split('/').filter { it.isNotBlank() }.map { decodeURIComponent(it) },
            parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
                .associate { decodeURIComponent(it.substringBefore('=')) to decodeURIComponent(it.substringAfter('=')) }
        )

        public fun fromUrlString(url: String): UrlLikePath {
            // Only the first '?' separates path from query; a literal '?' is legal inside the
            // query per RFC 3986. Splitting on every occurrence silently discarded everything
            // after the second one, so "/p?redirect=/other?a=b" lost "a=b" with no error.
            return fromParts(url.substringBefore('?'), url.substringAfter('?', ""))
        }
    }

    // by Claude - removed debug println that fired on every route render
    public fun render(): String = segments.joinToString("/") { encodeURIComponent(it) } + (parameters.takeUnless { it.isEmpty() }?.entries?.joinToString(
        "&",
        "?"
    ) { "${encodeURIComponent(it.key)}=${encodeURIComponent(it.value)}" } ?: "")

}


