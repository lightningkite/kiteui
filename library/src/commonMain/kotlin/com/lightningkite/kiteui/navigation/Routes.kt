package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.stack
import com.lightningkite.kiteui.views.direct.text
import kotlin.reflect.KClass

class Routes(
    val parsers: List<(UrlLikePath) -> Screen?>,
    val renderers: Map<KClass<out Screen>, (Screen) -> RouteRendered?>,
    val fallback: Screen = object: Screen {
        override val title = Constant("Not Found")
        override fun ViewWriter.render() {
            stack {
                centered - col {
                    h1("Not Found")
                    text("Sorry, we couldn't find the page you're looking for.")
                }
            }
        }
    }
) {
    fun render(screen: Screen) = renderers.get(screen::class)?.invoke(screen)
    fun parse(path: UrlLikePath) = parsers.asSequence().mapNotNull { it(path) }.firstOrNull()
    fun parseOrFallback(path: UrlLikePath) = try { parse(path) } catch(e: Exception) {
        fallback
    }
}

data class RouteRendered(
    val urlLikePath: UrlLikePath,
    val listenables: List<Listenable>
)

data class UrlLikePath(
    val segments: List<String>,
    val parameters: Map<String, String>
) {
    companion object {
        val EMPTY = UrlLikePath(listOf(), mapOf())

        fun fromParts(pathname: String, search: String) = UrlLikePath(
            segments = pathname.split('/').filter { it.isNotBlank() },
            parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }
                .associate { it.substringBefore('=') to decodeURIComponent(it.substringAfter('=')) }
        )

        fun fromUrlString(url: String): UrlLikePath {
            val parts = url.split("?")
            return fromParts(parts.getOrNull(0) ?: "", parts.getOrNull(1) ?: "")
        }
    }

    fun render() = segments.joinToString("/") + (parameters.takeUnless { it.isEmpty() }?.entries?.joinToString(
        "&",
        "?"
    ) { "${it.key}=${encodeURIComponent(it.value)}" } ?: "")

}

fun Screen.render(writer: ViewWriter): Unit = with(writer) { render() }

