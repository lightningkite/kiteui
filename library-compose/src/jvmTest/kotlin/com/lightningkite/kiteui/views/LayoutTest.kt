package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LayoutTest {
    // CSS break-all breaks inside words that would fit on the next line, so it needs fewer lines.
    @Test
    fun breakAllBreaksInsideWords() = runKiteUiTest {
        val content = "abcdefghijklmn ".repeat(8).trim()
        lateinit var normal: TextView
        lateinit var breakAll: TextView
        showKiteUi {
            col {
                sizeConstraints(width = 150.px).col {
                    normal = text(content)
                    breakAll = text(content).apply { wordBreak = WordBreak.BreakAll }
                }
            }
        }
        val normalHeight = runOnUiThread { normal.screenRectangle()!!.let { it.bottom - it.top } }
        val breakAllHeight = runOnUiThread { breakAll.screenRectangle()!!.let { it.bottom - it.top } }
        assertTrue(breakAllHeight < normalHeight, "break-all should fill its lines, taking $breakAllHeight against $normalHeight")
    }

    // CSS gives a line of text its whole line-height, half the leading above and half below, even when it's the only line.
    @Test
    fun aLineOfTextTakesItsWholeLineHeight() = runKiteUiTest {
        lateinit var label: TextView
        lateinit var field: TextInput
        lateinit var area: TextArea
        showKiteUi {
            col {
                label = text("One line")
                field = unpadded.textInput { content.value = "One line" }
                area = unpadded.textArea { content.value = "One line" }
            }
        }
        val lineHeight = testTheme.font.size.value * testTheme.font.lineSpacingMultiplier
        fun height(view: NativeElement) = runOnUiThread { view.screenRectangle()!!.let { it.bottom - it.top } }
        assertEquals(lineHeight, height(label), 0.5, "text")
        assertEquals(lineHeight, height(field), 0.5, "text input")
        // The web's textarea is sized by a hidden copy of its text with 2px padding all round, while the textarea's own
        // padding is reset to none, so it's 4px taller than its lines.
        assertEquals(lineHeight + 4, height(area), 0.5, "text area")
    }

    // Compose caps a single-line field at its line's height as Skia reports it, which for some fonts and sizes (the
    // default font at 14px and 1.5, Inter at 16px and 1.5) is a pixel short of the line-height the text is laid out in.
    @Test
    fun aSingleLineFieldIsAsTallAsItsLineHeight() = runKiteUiTest {
        lateinit var field: TextInput
        showKiteUi {
            col {
                themed(ThemeDerivation { it.copy(id = "lines", font = it.font.copy(size = 14.px, lineSpacingMultiplier = 1.5)).withoutBack }).col {
                    field = unpadded.textInput { content.value = "One line" }
                }
            }
        }
        val height = runOnUiThread { field.screenRectangle()!!.let { it.bottom - it.top } }
        assertEquals(21.0, height, 0.5)
    }

    // The web centers at a half pixel and paints there rounded to the nearest, half up.
    @Test
    fun centeringLeavesTheOddPixelAbove() = runKiteUiTest {
        lateinit var frame: Frame
        lateinit var box: Frame
        lateinit var rowBox: Frame
        showKiteUi {
            col {
                frame = sizeConstraints(width = 100.px, height = 33.px).frame {
                    box = centered.sizeConstraints(width = 20.px, height = 20.px).frame { }
                }
                sizeConstraints(height = 33.px).row {
                    rowBox = centered.sizeConstraints(width = 20.px, height = 20.px).frame { }
                }
            }
        }
        val frameTop = runOnUiThread { frame.screenRectangle()!!.top }
        assertEquals(7.0, runOnUiThread { box.screenRectangle()!!.top } - frameTop, "in a frame")
        assertEquals(40.0, runOnUiThread { box.screenRectangle()!!.left } - runOnUiThread { frame.screenRectangle()!!.left }, "across a frame")
        assertEquals(33.0 + 7.0, runOnUiThread { rowBox.screenRectangle()!!.top } - frameTop - testTheme.gap.value, "in a row")
    }

    @Test
    fun textWrapsInsideAWrappingRow() = runKiteUiTest {
        val long = "word ".repeat(80)
        showKiteUi {
            col {
                text("One line")
                row {
                    text("Label")
                    expanding.text(long)
                }
            }
        }
        val oneLine = onNodeWithText("One line").getUnclippedBoundsInRoot().height
        val wrapped = onNodeWithText(long.trim(), substring = true).getUnclippedBoundsInRoot().height
        assertTrue(wrapped > oneLine * 2, "Expected the long text to wrap onto several lines, got $wrapped vs one line $oneLine")
    }

    // Like the web's scrollIntoView: an alignment says where the element ends up, null scrolls as little as
    // possible, and animate decides whether it jumps.
    @Test
    fun scrollIntoViewHonoursAlignmentAndAnimate() = runKiteUiTest {
        mainClock.autoAdvance = false
        val items = ArrayList<TextView>()
        showKiteUi {
            col {
                sizeConstraints(height = 300.px).scrolling.col {
                    gap = 0.px
                    for (i in 0 until 50) items += sizeConstraints(height = 40.px).text("Item $i")
                }
            }
        }
        mainClock.advanceTimeBy(400)
        val viewport = onNodeWithText("Item 0").getUnclippedBoundsInRoot().top
        fun bounds(i: Int) = onNodeWithText("Item $i").getUnclippedBoundsInRoot()

        runOnUiThread { items[20].scrollIntoView(null, Align.Center, animate = false) }
        mainClock.advanceTimeBy(32)
        assertEquals((viewport + 150.dp).value, ((bounds(20).top + bounds(20).bottom) / 2).value, 1f, "centered in the 300dp viewport")

        // Already in view, so null doesn't move it.
        val before = bounds(21).top
        runOnUiThread { items[21].scrollIntoView(null, null, animate = false) }
        mainClock.advanceTimeBy(32)
        assertEquals(before, bounds(21).top)

        // Further down, null brings it just into view, at the bottom.
        runOnUiThread { items[30].scrollIntoView(null, null, animate = false) }
        mainClock.advanceTimeBy(32)
        assertEquals((viewport + 300.dp).value, bounds(30).bottom.value, 1f)

        runOnUiThread { items[10].scrollIntoView(null, Align.Start, animate = true) }
        mainClock.advanceTimeBy(48)
        val moving = bounds(10).top
        assertTrue(moving < viewport, "an animated scroll should still be on its way, at $moving")
        mainClock.advanceTimeBy(1000)
        assertEquals(viewport.value, bounds(10).top.value, 1f)
    }

    // As with flex (and Android's fillViewport), scrolled content is at least as wide as the viewport,
    // so expanding spaces around narrow content center it.
    @Test
    fun weightsFillAScrollViewport() = runKiteUiTest {
        showKiteUi {
            col {
                scrollingHorizontally.row {
                    expanding.space()
                    text("Middle")
                    expanding.space()
                }
            }
        }
        val root = onRoot().getUnclippedBoundsInRoot()
        val middle = onNodeWithText("Middle").getUnclippedBoundsInRoot()
        val center = (middle.left + middle.right) / 2
        assertEquals((root.width / 2).value, center.value, 2f, "Expected the text centered in the viewport")
    }

    @Test
    fun weightsShareTheRemainingSpace() = runKiteUiTest {
        showKiteUi {
            row {
                sizeConstraints(width = 100.px).text("Fixed")
                weight(1f).text("One")
                weight(3f).text("Three")
            }
        }
        val one = onNodeWithText("One").getUnclippedBoundsInRoot().width
        val three = onNodeWithText("Three").getUnclippedBoundsInRoot().width
        assertTrue(one > 0.dp, "weighted child got no space")
        assertEquals(3.0, (three / one).toDouble(), 0.05)
    }

    // CSS flex-grow factors that add up to less than 1 take only that fraction of the free space.
    @Test
    fun weightsUnderOneLeaveSpaceUnused() = runKiteUiTest {
        showKiteUi {
            col {
                sizeConstraints(width = 400.px).row {
                    gap = 0.px
                    weight(0.25f).text("Quarter")
                    weight(0.5f).text("Half")
                }
            }
        }
        assertEquals(100.dp, onNodeWithText("Quarter").getUnclippedBoundsInRoot().width)
        assertEquals(200.dp, onNodeWithText("Half").getUnclippedBoundsInRoot().width)
    }

    // The web's space has a min-width of the theme's gap, and CSS lets a min-width beat a smaller width.
    @Test
    fun aSpaceIsNeverNarrowerThanTheGap() = runKiteUiTest {
        showKiteUi {
            row {
                gap = 0.px
                text("Before")
                sizeConstraints(width = 2.px).space()
                text("After")
            }
        }
        val before = onNodeWithText("Before").getUnclippedBoundsInRoot()
        val after = onNodeWithText("After").getUnclippedBoundsInRoot()
        assertEquals(testTheme.gap.toDp(), after.left - before.right)
    }

    // In CSS a min size beats a max size, which beats an explicit size.
    @Test
    fun minAndMaxSizesBeatAnExplicitSize() = runKiteUiTest {
        showKiteUi {
            col {
                sizeConstraints(width = 50.px, minWidth = 80.px).text("Min")
                sizeConstraints(width = 300.px, maxWidth = 120.px).text("Max")
                sizeConstraints(width = 300.px, minWidth = 90.px, maxWidth = 60.px).text("Both")
                sizeConstraints(minWidth = 90.px, maxWidth = 60.px).text("NoWidth")
                sizeConstraints(height = 10.px, minHeight = 40.px).text("MinHeight")
                sizeConstraints(height = 300.px, maxHeight = 30.px).text("MaxHeight")
            }
        }
        fun bounds(text: String) = onNodeWithText(text).getUnclippedBoundsInRoot()
        assertEquals(80.dp, bounds("Min").width)
        assertEquals(120.dp, bounds("Max").width)
        assertEquals(90.dp, bounds("Both").width)
        assertEquals(90.dp, bounds("NoWidth").width)
        assertEquals(40.dp, bounds("MinHeight").height)
        assertEquals(30.dp, bounds("MaxHeight").height)
    }

    @Test
    fun aspectRatioFollowsAHeightStretchedByARow() = runKiteUiTest {
        var box: Frame? = null
        showKiteUi {
            col {
                sizeConstraints(height = 100.px).row {
                    sizeConstraints(aspectRatio = 2 to 1).frame { box = this }
                    expanding.space()
                }
            }
        }
        val bounds = box!!.screenRectangle()!!
        assertEquals(100.0, bounds.height, 1.0)
        assertEquals(200.0, bounds.width, 1.0)
    }

    // As on the web, every view a swap writes is shown, and goes at the next swap.
    @Test
    fun swapViewShowsAndRemovesEveryViewWritten() = runKiteUiTest {
        lateinit var swapView: SwapView
        lateinit var first: TextView
        lateinit var second: TextView
        showKiteUi {
            col {
                swapView = swapView {
                    swap {
                        first = text("First")
                        second = text("Second")
                    }
                }
            }
        }
        onNodeWithText("First", useUnmergedTree = true).assertExists()
        onNodeWithText("Second", useUnmergedTree = true).assertExists()
        runOnUiThread { swapView.swap { text("Replacement") } }
        waitForIdle()
        onNodeWithText("First", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Second", useUnmergedTree = true).assertDoesNotExist()
        assertTrue(first.isShutdown && second.isShutdown, "the replaced views should be shut down")
    }

    @Test
    fun swapViewKeepsShownWhenInCharge() = runKiteUiTest {
        val visible = Signal(false)
        showKiteUi {
            col {
                shownWhen { visible() }.swapView { swap { text("Swapped in") } }
            }
        }
        onNodeWithText("Swapped in", useUnmergedTree = true).assertDoesNotExist()
        visible.value = true
        // shownWhen reacts on the Swing thread, which waitForIdle doesn't track.
        waitUntil { onAllNodesWithText("Swapped in", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    // On the web, a scroller with no size limit grows to its content; Compose throws on an unbounded scroller.
    @Test
    fun nestedScrollersGrowToTheirContent() = runKiteUiTest {
        lateinit var innerCol: RowOrCol
        lateinit var innerRow: RowOrCol
        showKiteUi {
            col {
                sizeConstraints(height = 300.px).scrolling.col {
                    // Sized on the other axis only because a bare `scrolling` here is NativeElement's internal member.
                    innerCol = sizeConstraints(width = 200.px).scrolling.col {
                        gap = 0.px
                        padding = 0.px
                        for (i in 0 until 20) sizeConstraints(height = 40.px).text("Row $i")
                    }
                }
                sizeConstraints(width = 300.px).scrollingHorizontally.row {
                    innerRow = sizeConstraints(height = 40.px).scrollingHorizontally.row {
                        gap = 0.px
                        padding = 0.px
                        for (i in 0 until 20) sizeConstraints(width = 40.px).text("Column $i")
                    }
                }
            }
        }
        val colHeight = runOnUiThread { innerCol.screenRectangle()!!.let { it.bottom - it.top } }
        assertEquals(800.0, colHeight, 1.0)
        val rowWidth = runOnUiThread { innerRow.screenRectangle()!!.let { it.right - it.left } }
        assertEquals(800.0, rowWidth, 1.0)
    }

    // Browsers ignore negative padding; Compose throws on it, which took down the whole window.
    @Test
    fun negativePaddingCountsAsNone() = runKiteUiTest {
        showKiteUi {
            col {
                frame {
                    paddingByEdge = Edges(left = 4.px, top = 0.px, bottom = 0.px, right = (-8).px)
                    text("Inside")
                }
            }
        }
        assertEquals(4.dp, onNodeWithText("Inside").getUnclippedBoundsInRoot().left)
    }

    // As on the web, hovering recolors a button but doesn't pad one whose theme leaves it unpadded.
    @Test
    fun hoverDoesNotAddPadding() = runKiteUiTest {
        showKiteUi {
            col {
                themed(UnpaddedClickables).col {
                    button { text("Item"); onClick { } }
                }
            }
        }
        val before = onNodeWithText("Item").getUnclippedBoundsInRoot()
        onNodeWithText("Item").performMouseInput { moveTo(center) }
        waitForIdle()
        assertEquals(before, onNodeWithText("Item").getUnclippedBoundsInRoot())
    }
}

private object UnpaddedClickables : Semantic("unpadded-clickables") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        semanticOverrides = SemanticOverrides(ClickableSemantic.override { it.withoutBack }),
    )
}
