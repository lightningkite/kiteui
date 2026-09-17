package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.MediaQuery
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.viewUnits
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*

/**
 * Manual verification for `shownForQuery` on the native targets.
 *
 * On web this is a CSS media query and the browser re-evaluates it for free. On Android and iOS it
 * is a reactive condition over `AppState.windowInfo`, so the thing worth checking by hand is that
 * it actually *re-runs* when the window changes - rotate the device, or resize a multi-window or
 * iPad split view. The evaluator itself has unit tests; the reactivity does not.
 *
 * Everything here is driven by the live window size printed at the top, so a row whose visibility
 * disagrees with that number is the bug.
 */
@Routable("media-query")
object MediaQueryPage : Page {
    override val title: Reactive<String> get() = Constant("Media Queries")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("Media queries")
            text(
                "Rotate the device, or resize the window in split view. Each row below states the " +
                        "query it is gated on. A row must appear exactly when the live size above it " +
                        "satisfies that query, and must update as you rotate - not only on next launch."
            )

            card.col {
                h2("Live window")
                text {
                    ::content {
                        val w = AppState.windowInfo()
                        val width = w.width.viewUnits
                        val height = w.height.viewUnits
                        val ratio = if (height > 0.0) width / height else 0.0
                        "width  ${width.trim()}\n" +
                                "height ${height.trim()}\n" +
                                "aspect ${ratio.trim()}  (width over height)"
                    }
                }
            }

            widthSection()
            aspectSection()
            deviceSection()
        }
    }

    /** Width breakpoints, the overwhelmingly common use. */
    private fun ElementWriter.CanAddTheme.widthSection() = card.col {
        h2("1. Width breakpoints")
        text("Rotate a phone and the two rows must swap. Both showing, or neither, is the bug.")

        shownForQuery(MediaQuery.MinWidth(30.rem)).card.text("Shown when width is at least 30rem")
        shownForQuery(MediaQuery.MaxWidth(30.rem)).card.text("Shown when width is at most 30rem")

        text(
            "Note both are inclusive, so at exactly 30rem both are visible on purpose - that is what " +
                    "CSS does too."
        )
    }

    /** Aspect ratio, which is the one whose direction is easy to get backwards. */
    private fun ElementWriter.CanAddTheme.aspectSection() = card.col {
        h2("2. Aspect ratio")
        text("Aspect is width over height, so landscape is the larger number. Rotate to swap these.")

        shownForQuery(MediaQuery.MinAspectRatio(1.0)).card.text("Landscape or square (aspect at least 1.0)")
        shownForQuery(MediaQuery.MaxAspectRatio(1.0)).card.text("Portrait or square (aspect at most 1.0)")
    }

    /**
     * Device features, which are constants on the native targets and must not flicker when the
     * window changes.
     */
    private fun ElementWriter.CanAddTheme.deviceSection() = card.col {
        h2("3. Device features")
        text(
            "These describe the hardware, not the window, so they must stay put while you rotate. " +
                    "On a phone or tablet the first two show and the last two do not - matching what a " +
                    "mobile browser reports on the same device."
        )

        shownForQuery(MediaQuery.Pointer(MediaQuery.Pointer.Option.Coarse)).card.text("Coarse pointer - expected on Android and iOS")
        shownForQuery(MediaQuery.Hover(MediaQuery.Hover.Option.None)).card.text("No hover - expected on Android and iOS")
        shownForQuery(MediaQuery.Pointer(MediaQuery.Pointer.Option.Fine)).card.text("Fine pointer - must NOT appear on a touch device")
        shownForQuery(MediaQuery.Hover(MediaQuery.Hover.Option.Hover)).card.text("Hover available - must NOT appear on a touch device")

        text(
            "Attaching a mouse or trackpad does not change these, by design: the query asks about " +
                    "the primary pointer, exactly as CSS does."
        )
    }
}

/** One decimal place, so the readout does not jitter with sub-pixel window sizes. */
private fun Double.trim(): String = ((this * 10).toLong() / 10.0).toString()
