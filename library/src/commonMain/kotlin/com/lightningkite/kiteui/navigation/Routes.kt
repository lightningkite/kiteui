package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.reflect.KClass

class Routes(
    val parsers: List<(UrlLikePath) -> Screen?>,
    val renderers: Map<KClass<out Screen>, (Screen) -> RouteRendered?>,
    val fallback: Screen
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

fun Screen.render(viewWriter: ViewWriter) = with(viewWriter) { render() }

