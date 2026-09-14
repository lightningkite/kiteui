package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.applySafeInsets
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

// Manual + semi-automated verification for a batch of iOS-only fixes that don't fit a unit test
// (real UILongPressGestureRecognizer wiring, real UIScrollView axis behavior) or that a unit test
// can only partially cover (the bottom-sheet theme-poll loop's lifetime - HintPopoverTest and
// CoordinatorFrameThemePollTest cover the logic; this page is for eyeballing the visible result and
// watching the loop count on a real device/simulator over repeated open/close cycles).
//
// Each section says what to do and what correct looks like.
@Routable("ios-misc-fixes")
object IosMiscFixesPage : Page {
    override val title: Reactive<String> get() = Constant("iOS Misc Fix Verification")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("iOS Misc Fix Verification")
            text(
                "Manual/semi-automated checks for hintPopover, bidirectional scrolling, and the " +
                        "bottom-sheet theme-poll loop's cleanup. See docs/MANUAL_VERIFICATION.md for " +
                        "the full cross-page checklist."
            )

            hintPopoverSection()
            bidirectionalScrollSection()
        }
    }

    private fun ElementWriter.CanAddTheme.hintPopoverSection() = card.col {
        h2("hintPopover")
        text(
            "Long-press (long-click) the box below. Expect: a popover appears anchored below-right " +
                    "of it, themed like other KiteUI popovers, and dismisses when you tap outside it. " +
                    "Failure: nothing happens (the pre-fix bug - the long-press handler was an empty " +
                    "stub), or it opens more than one popover per press."
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

    /**
     * See ScrollView.ios.kt's comment on the `horizontal` init for why: iOS's ScrollLayout measures
     * the child unbound only along the scrolling axis and deliberately zeroes UIScrollView's
     * contentSize on the cross axis, so the cross axis never scrolls - it's clipped to the
     * viewport. Both boxes below hold content that overflows in BOTH directions at once; each one
     * only scrolls the axis it was built for.
     */
    private fun ElementWriter.CanAddTheme.bidirectionalScrollSection() = card.col {
        h2("Bidirectional scrolling (investigated, not changed)")
        text(
            "Each box below holds a grid of numbered cells wider AND taller than the box itself. " +
                    "Expected (matches the investigation - NOT a bug): the first box scrolls " +
                    "vertically only, clipping the overflow off the right edge; the second scrolls " +
                    "horizontally only, clipping the overflow off the bottom edge. iOS cannot scroll " +
                    "both axes of one KiteUI `scrolling`/`scrollingHorizontally` container at once - " +
                    "see the comment on ScrollView.ios.kt's `horizontal` init for the underlying " +
                    "reason this isn't a simple fix."
        )
        row {
            col {
                subtext("scrolling (vertical) - try to scroll right too")
                sizeConstraints(width = 12.rem, height = 10.rem).card.scrolling.col {
                    overflowGrid()
                }
            }
            col {
                subtext("scrollingHorizontally - try to scroll down too")
                sizeConstraints(width = 12.rem, height = 10.rem).card.scrollingHorizontally.row {
                    overflowGrid()
                }
            }
        }
    }

    private fun ElementWriter.CanAddSizing.overflowGrid() = sizeConstraints(width = 24.rem, height = 20.rem).col {
        // Loop variables are named r/c rather than row/column so they don't shadow the `row {}`
        // container builder below.
        for (r in 0 until 8) {
            row {
                for (c in 0 until 8) {
                    sizeConstraints(width = 3.rem, height = 2.5.rem).card.frame {
                        centered.text("$r,$c")
                    }
                }
            }
        }
    }
}
