package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.viewUnits
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.setClipboardText
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.RecyclerViewPlacerVerticalGrid
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.toReactive
import com.lightningkite.readable.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Routable("docs/material-icons")
object MaterialIconLibraryPage : Page {
    override val title: Reactive<String> = Constant("Material Icons")

    // Pinned so the file listing and the served SVGs stay in lockstep; bump when refreshing.
    private const val PACKAGE_VERSION = "0.44.6"
    private const val PKG = "@material-symbols/svg-400"

    enum class IconStyle(val folder: String, val suffix: String, val label: String) {
        Outlined("outlined", "", "Outlined"),
        Rounded("rounded", "", "Rounded"),
        Sharp("sharp", "", "Sharp"),
        Filled("outlined", "-fill", "Filled"),
    }

    @Serializable
    private data class JsdFile(
        val type: String,
        val name: String,
        val files: List<JsdFile> = emptyList(),
    )

    @Serializable
    private data class JsdPackage(val files: List<JsdFile>)

    private val iconCache = HashMap<Pair<String, IconStyle>, Icon>()
    private val json = Json { ignoreUnknownKeys = true }

    private val searchText = Signal("")
    private val style = Signal(IconStyle.Outlined)

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val allIcons: Reactive<List<String>> = AppScope.async() {
            val resp = fetch("https://data.jsdelivr.com/v1/packages/npm/$PKG@$PACKAGE_VERSION")
            val pkg = json.decodeFromString<JsdPackage>(resp.text())
            // Every style directory holds the same icon set; the outlined dir is enough.
            // Each icon ships as `<name>.svg` (default) plus `<name>-fill.svg` (filled).
            // We only want the base names, sorted.
            pkg.files
                .firstOrNull { it.type == "directory" && it.name == "outlined" }
                ?.files.orEmpty()
                .asSequence()
                .filter { it.type == "file" && it.name.endsWith(".svg") && !it.name.endsWith("-fill.svg") }
                .map { it.name.removeSuffix(".svg") }
                .sorted()
                .toList()
        }.toReactive()

        val filtered: Reactive<List<String>> = shared {
            val q = searchText().trim().lowercase()
            if (q.isEmpty()) allIcons() else allIcons().filter { it.contains(q) }
        }

        col {
            card.col {
                h1 { content = "Material Icons" }
                text {
                    ::content {
                        val total = allIcons().size
                        val visible = filtered().size
                        if (visible == total) "$total icons — click to copy as Kotlin Icon(...)"
                        else "$visible / $total icons — click to copy as Kotlin Icon(...)"
                    }
                }
                fieldTheme.row {
                    expanding.textInput {
                        hint = "Search icons…"
                        content bind searchText
                    }
                    select {
                        bind(
                            edits = style,
                            data = Constant(IconStyle.entries),
                            render = { it.label },
                        )
                    }
                }
            }

            expanding.recyclerView {
                ::placer {
                    RecyclerViewPlacerVerticalGrid((AppState.windowInfo().width.viewUnits / 100.0).toInt().coerceAtLeast(1), sizeDoesNotChange = true)
                }
                children(filtered, id = { it }) { name ->
                    card.button {
                        val iconForCell: Reactive<Icon> = rememberSuspending {
                            fetchMaterialIcon(name(), style())
                        }
                        col {
                            centered.icon {
                                source = Icon.dot
                                ::source { iconForCell() }
                                ::description { name() }
                            }
                            centered.subtext {
                                ::content { name() }
                            }
                        }
                        onClick {
                            val ic = iconForCell.await()
                            val snippet = iconToKotlinSource(name(), ic)
                            context.setClipboardText(snippet)
                            context.toast("Copied Icon: ${name.await()}")
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchMaterialIcon(name: String, style: IconStyle): Icon {
        val key = name to style
        iconCache[key]?.let { return it }
        val url =
            "https://cdn.jsdelivr.net/npm/$PKG@$PACKAGE_VERSION/${style.folder}/$name${style.suffix}.svg"
        val svg = fetch(url).text()
        // All variants use viewBox `0 -960 960 960`; we collect every <path d="…"/>.
        val paths = Regex("""d="([^"]*)"""").findAll(svg).map { it.groupValues[1] }.toList()
        val icon = Icon(
            width = 1.5.rem,
            height = 1.5.rem,
            viewBoxMinX = 0,
            viewBoxMinY = -960,
            viewBoxWidth = 960,
            viewBoxHeight = 960,
            pathDatas = paths,
        )
        iconCache[key] = icon
        return icon
    }

    private fun iconToKotlinSource(name: String, ic: Icon): String {
        val paths = ic.pathDatas.joinToString(", ") { "\"$it\"" }
        return "val $name = Icon(1.5.rem, 1.5.rem, 0, -960, 960, 960, listOf($paths))"
    }
}
