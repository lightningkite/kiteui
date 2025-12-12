package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.reflect.KClass

class Routes(
    val parsers: List<(UrlLikePath) -> Page?>,
    val renderers: Map<KClass<out Page>, (Page) -> RouteRendered?>,
    val fallback: Page = object: Page {
        override val title = Constant("Not Found")
        override fun ViewWriter.render(): Unit = run {
            frame {
                centered.col {
                    h1("Not Found")
                    text("Sorry, we couldn't find the page you're looking for.")
                }
            }
        }
    }
) {
    fun render(screen: Page) = renderers.get(screen::class)?.invoke(screen)
    fun parse(path: UrlLikePath): Page? = parsers.asSequence().mapNotNull { it(path) }.firstOrNull()
    fun parseOrFallback(path: UrlLikePath) =
        try {
            parse(path) ?: fallback
        } catch(e: Exception) {
            LogRoot.warn("Encountered exception when parsing route: $e")
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

fun Page.render(writer: ViewWriter): Unit = with(writer) { render() }

