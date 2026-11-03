package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VisualMediaSource
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

/**
 * Compares [MediaView.opaqueTransitions] side by side. Each pair of cards gets the same picture
 * changes: the left card dissolves between them (opaqueTransitions = false) and the right card cuts
 * (opaqueTransitions = true).
 *
 * Use "Next" and "Previous" to change pictures. The first section swaps between different pictures;
 * the second shows a small thumbnail first and then the full-size version of the same picture, the
 * way the image viewer does.
 */
@Routable("media-view-transition-test")
object MediaViewTransitionTestPage : Page {

    private val seeds = listOf("transition-a", "transition-b", "transition-c", "transition-d")

    private fun fullSize(seed: String) = ImageRemote("https://picsum.photos/seed/$seed/1200/1200")
    private fun thumbnail(seed: String) = ImageRemote("https://picsum.photos/seed/$seed/200/200")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            val index = Signal(0)

            h2("MediaView transitions")
            text(
                "Left: opaqueTransitions = false, the new image dissolves in over the old one. " +
                        "Right: opaqueTransitions = true, the new image appears instantly."
            )
            row {
                expanding.button {
                    text("Previous")
                    onClick { if (index.value > 0) index.value-- }
                }
                expanding.button {
                    text("Next")
                    onClick { if (index.value < seeds.lastIndex) index.value++ }
                }
            }
            subtext { ::content { "Picture ${index() + 1} of ${seeds.size}" } }

            h3("Different pictures")
            text("Each change swaps to a different full-size picture.")
            row {
                transitionCard(index, opaque = false, label = "dissolve") { i -> listOf(fullSize(seeds[i])) }
                transitionCard(index, opaque = true, label = "cut") { i -> listOf(fullSize(seeds[i])) }
            }

            h3("Thumbnail, then full size")
            text("Each picture shows a small thumbnail first, then the full-size version of the same picture.")
            row {
                transitionCard(index, opaque = false, label = "dissolve") { i ->
                    listOf(thumbnail(seeds[i]), fullSize(seeds[i]))
                }
                transitionCard(index, opaque = true, label = "cut") { i ->
                    listOf(thumbnail(seeds[i]), fullSize(seeds[i]))
                }
            }
        }
    }

    private fun ElementWriter.transitionCard(
        index: Signal<Int>,
        opaque: Boolean,
        label: String,
        sources: (Int) -> List<VisualMediaSource>,
    ) = col {
        card.col {
            subtext(if (opaque) "opaqueTransitions = true ($label)" else "opaqueTransitions = false ($label)")
            sizeConstraints(width = 16.rem, height = 16.rem).media {
                opaqueTransitions = opaque
                ::info {
                    MediaView.Info(
                        sources = sources(index()),
                        scaleType = ImageScaleType.Fit,
                        description = "transition test",
                    )
                }
            }
        }
    }
}
