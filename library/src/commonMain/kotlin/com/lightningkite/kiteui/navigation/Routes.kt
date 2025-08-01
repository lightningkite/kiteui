package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.signal.Constant
import com.lightningkite.signal.Listenable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import kotlin.reflect.KClass

public class Routes(
    public val parsers: List<(UrlLikePath) -> Page?>,
    public val renderers: Map<KClass<out Page>, (Page) -> RouteRendered?>,
    public val fallback: Page = object: Page {
        override val title = Constant("Not Found")
        override fun ViewWriter.render(): ViewModifiable = run {
            return frame {
                centered - col {
                    h1("Not Found")
                    text("Sorry, we couldn't find the page you're looking for.")
                }
            }
        }
    }
) {
    public fun render(screen: Page): RouteRendered? = renderers.get(screen::class)?.invoke(screen)
    public fun parse(path: UrlLikePath): Page? = parsers.asSequence().mapNotNull { it(path) }.firstOrNull()
    public fun parseOrFallback(path: UrlLikePath): Page? = try { parse(path) } catch(e: Exception) {
        fallback
    }
}

public data class RouteRendered(
    public val urlLikePath: UrlLikePath,
    public val listenables: List<Listenable>
)

public data class UrlLikePath(
    public val segments: List<String>,
    public val parameters: Map<String, String>
) {
    public companion object {
        public val EMPTY: UrlLikePath = UrlLikePath(listOf(), mapOf())

        public fun fromParts(pathname: String, search: String): UrlLikePath = UrlLikePath(
            segments = pathname.split('/').filter { it.isNotBlank() },
            parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
                .associate { it.substringBefore('=') to decodeURIComponent(it.substringAfter('=')) }
        )

        public fun fromUrlString(url: String): UrlLikePath {
            val parts = url.split("?")
            return fromParts(parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "")
        }
    }

    public fun render(): String = segments.joinToString("/") + (parameters.takeUnless { it.isEmpty() }?.entries?.joinToString(
        "&",
        "?"
    ) { "${it.key}=${encodeURIComponent(it.value)}" } ?: "")

}

public fun Page.render(writer: ViewWriter): ViewModifiable = with(writer) { render() }

