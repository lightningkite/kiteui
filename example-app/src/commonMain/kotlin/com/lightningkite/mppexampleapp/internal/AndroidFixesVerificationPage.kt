package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

// Manual verification for a batch of Android-only fixes that a JVM/Robolectric unit test can't
// reach: real popover window placement and dismissal, real CoordinatorLayout touch dispatch
// between overlapping siblings, and a real device's resolved RTL layout direction. Companion to
// PlatformFixVerificationPage - see docs/MANUAL_VERIFICATION.md for the combined checklist.
//
// Each section says what to do, what correct looks like, and what a failure looks like.
@Routable("android-fixes")
object AndroidFixesVerificationPage : Page {
    override val title: Reactive<String> get() = Constant("Android Fix Verification")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("Android Fix Verification")
            text(
                "Manual checks for Android-only fixes - hintPopover, CoordinatorFrame's " +
                        "blockBehind, and RTL canvas text alignment. See docs/MANUAL_VERIFICATION.md " +
                        "for the full cross-page checklist."
            )

            hintPopoverSection()
            blocksBehindSection()
            rtlTextAlignSection()
        }
    }

    private fun ElementWriter.CanAddTheme.hintPopoverSection() = card.col {
        h2("hintPopover")
        text(
            "Long-press (long-click) the box below. Expect: a popover appears anchored below-right " +
                    "of it, themed like other KiteUI popovers (see PopoverTestingPage), and dismisses " +
                    "when you tap outside it. Failure: nothing happens, or the box's normal long-press " +
                    "(e.g. text selection) fires instead."
        )
        card.hintPopover(PopoverPreferredDirection.belowRight) {
            card.col {
                text("Popover content!")
                subtext("Tap outside to dismiss.")
            }
        }.col {
            centered.text("Long-press me")
        }
    }

    private fun ElementWriter.CanAddTheme.blocksBehindSection(): Unit = run {
        val counter = Signal(0)
        card.col {
            h2("CoordinatorFrame blockBehind")
            text(
                "The counter button is positioned at the bottom-left, under the sliding panel's " +
                        "own empty area (below the panel's \"Close\" button, not on top of it). Open a " +
                        "panel below, then tap that same bottom-left spot through the panel."
            )
            text(
                "blockBehind = true: the tap must NOT reach the counter - it should be swallowed by " +
                        "the panel/scrim. blockBehind = false: the tap SHOULD reach the counter and " +
                        "increment it. The counter incrementing while blockBehind = true is the failure."
            )
            row {
                card.button {
                    text("Open panel (blockBehind = true)")
                    onClick {
                        context.coordinatorFrame?.leftSlidingPanel(ratio = 0.6f, blockBehind = true) { control ->
                            slidingPanelContent(control)
                        }
                    }
                }
                card.button {
                    text("Open panel (blockBehind = false)")
                    onClick {
                        context.coordinatorFrame?.leftSlidingPanel(ratio = 0.6f, blockBehind = false) { control ->
                            slidingPanelContent(control)
                        }
                    }
                }
            }
            sizeConstraints(height = 12.rem).frame {
                atBottomStart.important.button {
                    text { ::content { "Counter: ${counter()}" } }
                    onClick { counter.value++ }
                }
            }
        }
    }

    private fun ElementWriter.CanAddShownWhen.slidingPanelContent(control: SlidingPanelControl) = col {
        card.col {
            text("Panel")
            subtext("Its empty area below extends the full height - that's what's under test.")
            button {
                text("Close")
                onClick { control.close() }
            }
        }
    }

    private fun ElementWriter.CanAddTheme.rtlTextAlignSection() = card.col {
        h2("RTL text alignment (Canvas start/end)")
        text(
            "start/end are direction-relative; left/right are not. To see start/end swap sides, " +
                    "enable Developer Options > \"Force RTL layout direction\" (or switch the device " +
                    "language to Arabic/Hebrew) and come back to this screen. Expect: only the " +
                    "start and end rows move; left, right and center never do."
        )
        text(
            "Read each row as \"which side of the line did 'sample' land on\". Note that the sample " +
                    "sits on the opposite side from the alignment's name: left-aligned text starts at " +
                    "the line and runs rightwards, so the left row's sample is to the RIGHT of the " +
                    "line. That is the anchor doing its job, in both directions - it is not a swap."
        )
        sizeConstraints(height = 10.rem).canvas {
            delegate = TextAlignSampleDelegate()
        }
    }
}

/**
 * Draws one row per [TextAlign], each anchored to the same vertical line.
 *
 * The row's name is drawn in its own left-hand column rather than being the sample text itself.
 * That matters: an earlier version drew the word "left" *as* the left-aligned sample, and since
 * left-aligned text extends to the **right** of its anchor, the word "left" appeared on the right
 * of the line - which reads as though left and right had been swapped. They had not. Keeping the
 * label and the sample separate makes the anchor the only thing the sample is telling you about.
 */
private class TextAlignSampleDelegate : CanvasDelegate() {
    override fun draw(context: DrawingContext2D) {
        with(context) {
            clear()
            strokePaint = Color.gray
            lineWidth = 1.0
            font(16.0, FontAndStyle())

            val labelX = 8.0
            val markerX = 150.0
            val rows = listOf(
                TextAlign.start to "start",
                TextAlign.end to "end",
                TextAlign.left to "left",
                TextAlign.right to "right",
                TextAlign.center to "center",
            )
            rows.forEachIndexed { i, (align, label) ->
                val y = 20.0 + i * 24.0

                // Reference line: where this row's sample is anchored.
                beginPath()
                moveTo(markerX, y - 12.0)
                lineTo(markerX, y + 6.0)
                stroke()

                // Row name, always in the same place, so it cannot be mistaken for the sample.
                fillPaint = Color.gray
                textAlign(TextAlign.left)
                drawText(label, labelX, y)

                // The sample. Which side of the line it lands on is the whole result.
                fillPaint = Color.black
                textAlign(align)
                drawText("sample", markerX, y)
            }
        }
    }
}
