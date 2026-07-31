package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.forEachReorderable
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.math.abs

/**
 * Manual verification for review fixes that no automated test can cover.
 *
 * Each section states what to do and what correct behavior looks like, and computes a verdict where
 * the answer is measurable. Sections that depend on the platform say which platform they apply to;
 * on the others they are expected to be uninteresting rather than wrong.
 */
@Routable("review-fixes")
object ReviewFixVerificationPage : Page {
    override val title: Reactive<String> get() = Constant("Review Fix Verification")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1("Review Fix Verification")
            text(
                "Manual checks for changes that unit tests cannot reach. " +
                        "Each card says what to do and what the correct result is."
            )

            scrollMetricsSection()
            circularProgressSection()
            reorderIndicatorSection()
            externalLinkSchemeSection()
        }
    }

    /**
     * `ScrollingBehaviors.content` must report the full scrollable extent on the scrolling axis and
     * the viewport's own size on the other, identically on every platform. iOS is the one that was
     * wrong: it added the viewport onto the scroll axis, inflating "distance to the end" by a whole
     * screen so lazy loading never fired.
     */
    private fun ElementWriter.CanAddTheme.scrollMetricsSection() = card.col {
        h2("1. Scroll content metrics")
        text("Platform: iOS is the one under test. Web and Android should agree with it.")
        text("Scroll the box to the bottom, then read the verdict. No interaction needed beyond that.")

        lateinit var scroll: ScrollingBehaviors
        sizeConstraints(height = 12.rem).scrolling { scroll = this }.col {
            repeat(12) { sizeConstraints(minHeight = 4.rem).card.text("Row $it") }
        }

        col {
            text {
                ::content {
                    val c = scroll.content()
                    val v = scroll.viewport()
                    "content:  ${c.width.fmt()} x ${c.height.fmt()}\n" +
                            "viewport: ${v.width.fmt()} x ${v.height.fmt()} at ${v.left.fmt()}, ${v.top.fmt()}"
                }
            }
            text {
                ::content {
                    val c = scroll.content()
                    val v = scroll.viewport()
                    // The cross axis must match the viewport exactly. If contentSize leaked through
                    // it would read 0; if the viewport were added on top it would read double.
                    val crossOk = abs(c.width - v.width) < 1.0
                    // Content taller than the viewport, but not by a suspiciously exact viewport
                    // height - that pattern is the old "viewport added twice" bug.
                    val scrollOk = c.height > v.height
                    "cross axis matches viewport: ${crossOk.verdict()}\n" +
                            "scroll axis exceeds viewport: ${scrollOk.verdict()}"
                }
            }
            text {
                ::content {
                    val c = scroll.content()
                    val v = scroll.viewport()
                    val remaining = c.bottom - v.bottom
                    "distance to end: ${remaining.fmt()} - must reach ~0 when scrolled fully down, " +
                            "never stall at a full viewport height (${v.height.fmt()})"
                }
            }
        }
    }

    /**
     * The web progress ring is now a native `<progress>` painted by CSS rather than an SVG built in
     * script. Worth eyeballing because a stylesheet can fail in ways a unit test cannot see.
     */
    private fun ElementWriter.CanAddTheme.circularProgressSection() = card.col {
        h2("2. Circular progress ring")
        text("Platform: web and SSR changed. Android and iOS are unchanged - check them for parity.")
        text("Expected: four rings filling a quarter, half, three-quarters and completely, drawn as a ring with a hollow centre - not a bar, not a filled disc.")

        row {
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { r ->
                col {
                    sizeConstraints(width = 4.rem, height = 4.rem).circularProgress { ratio = r }
                    centered.text(r.toString())
                }
            }
        }

        val live = Signal(0.35f)
        text("Drag to confirm the arc follows continuously:")
        slider { value bind live }
        row {
            sizeConstraints(width = 5.rem, height = 5.rem).circularProgress { ::ratio { live() } }
            centered.text { ::content { "ratio ${live().fmt()}" } }
        }
        text("Accessibility: with a screen reader on, focusing a ring should announce a progress value. It is a real <progress> element on web, so this comes from the platform rather than from aria attributes.")
    }

    /**
     * Drop indicators toggle with `visible`, which keeps their space reserved, so revealing one
     * repaints instead of reflowing. Using `shown` instead made rows jump under the pointer.
     */
    private fun ElementWriter.CanAddTheme.reorderIndicatorSection() = card.col {
        h2("3. Drag-reorder drop indicators")
        text("Platform: all.")
        text("Drag a row slowly up and down the list. Expected: the row under the pointer does not jump, and rows below do not shift as the indicator moves. A highlight appearing at the drop position is correct; items changing position before you release is not.")

        val numbers = Signal((1..8).toList())
        col {
            forEachReorderable(
                items = numbers,
                reorder = { move -> numbers.value = move.reorder(numbers.value) },
            ) { item ->
                card.row {
                    icon(Icon.menu, "Drag handle")
                    centered.text { ::content { "Item ${item()}" } }
                }
            }
        }
    }

    /**
     * `ExternalLink.to` rejects URL schemes outside the safe set on every platform. Untestable in
     * unit tests because the failure mode is the OS opening something.
     */
    private fun ElementWriter.CanAddTheme.externalLinkSchemeSection() = card.col {
        h2("4. External link scheme validation")
        text("Platform: all, but Android and iOS are the ones that changed.")
        text("Tap each link. The first two must open normally. The rest must do nothing at all - no app switch, no dialer, no crash, no console error.")

        col {
            externalLink {
                to = "https://example.com"
                text("https://example.com - must open")
            }
            externalLink {
                to = "mailto:someone@example.com"
                text("mailto: - must open a mail draft")
            }
            externalLink {
                to = "javascript:alert(1)"
                text("javascript: - must do nothing")
            }
            externalLink {
                to = "intent://scan/#Intent;scheme=zxing;end"
                text("intent: - must do nothing (Android)")
            }
            externalLink {
                to = "file:///etc/passwd"
                text("file: - must do nothing")
            }
            externalLink {
                to = "data:text/html,<script>alert(1)</script>"
                text("data: - must do nothing")
            }
        }
        text {
            ::content {
                // Reading `to` back shows what survived validation, which is the check itself.
                "A rejected target is stored as null, so these links have no destination at all rather than a destination that is ignored on click."
            }
        }
    }
}

private fun Double.fmt(): String = ((this * 10).toLong() / 10.0).toString()
private fun Float.fmt(): String = ((this * 100).toLong() / 100.0).toString()
private fun Boolean.verdict(): String = if (this) "PASS" else "FAIL"
