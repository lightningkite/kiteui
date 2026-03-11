package com.lightningkite.kiteui.components

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.testing.*
import com.lightningkite.kiteui.views.direct.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Android tests for container default child alignment feature.
 * Tests the newChildHorizontalAlign and newChildVerticalAlign properties.
 */
@RunWith(RobolectricTestRunner::class)
class AlignmentTest {

    @Test
    fun testDefaultAlignmentInCol() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "parent-col"
                newChildHorizontalAlign = Align.Center

                text("Centered by default").apply { debugName = "centered-text" }
                text("Also centered").apply { debugName = "also-centered" }
            }
        }

        val centeredText = root.findByDebugName("centered-text")
        assertNotNull(centeredText, "First text should be found")
        assertEquals(Align.Center, centeredText.horizontalAlign, "First text should be horizontally centered")

        val alsoCentered = root.findByDebugName("also-centered")
        assertNotNull(alsoCentered, "Second text should be found")
        assertEquals(Align.Center, alsoCentered.horizontalAlign, "Second text should be horizontally centered")
    }

    @Test
    fun testDefaultAlignmentInRow() = withTestHarness { harness ->
        val root = harness.render {
            row {
                debugName = "parent-row"
                newChildVerticalAlign = Align.End

                text("Bottom aligned").apply { debugName = "bottom-text" }
                text("Also bottom").apply { debugName = "also-bottom" }
            }
        }

        val bottomText = root.findByDebugName("bottom-text")
        assertNotNull(bottomText, "First text should be found")
        assertEquals(Align.End, bottomText.verticalAlign, "First text should be vertically aligned to end")

        val alsoBottom = root.findByDebugName("also-bottom")
        assertNotNull(alsoBottom, "Second text should be found")
        assertEquals(Align.End, alsoBottom.verticalAlign, "Second text should be vertically aligned to end")
    }

    @Test
    fun testDefaultAlignmentInFrame() = withTestHarness { harness ->
        val root = harness.render {
            frame {
                debugName = "parent-frame"
                newChildHorizontalAlign = Align.End
                newChildVerticalAlign = Align.Start

                text("Top-Right").apply { debugName = "top-right-text" }
            }
        }

        val topRightText = root.findByDebugName("top-right-text")
        assertNotNull(topRightText, "Text should be found")
        assertEquals(Align.End, topRightText.horizontalAlign, "Text should be horizontally aligned to end")
        assertEquals(Align.Start, topRightText.verticalAlign, "Text should be vertically aligned to start")
    }

    @Test
    fun testExplicitAlignmentOverridesDefault() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "parent-col"
                newChildHorizontalAlign = Align.Center

                text("Uses default").apply { debugName = "default-text" }
                align(Align.Start, Align.Stretch).text("Explicitly left").apply { debugName = "explicit-text" }
            }
        }

        val defaultText = root.findByDebugName("default-text")
        assertNotNull(defaultText, "Default text should be found")
        assertEquals(Align.Center, defaultText.horizontalAlign, "Default text should use parent's default (Center)")

        val explicitText = root.findByDebugName("explicit-text")
        assertNotNull(explicitText, "Explicit text should be found")
        assertEquals(Align.Start, explicitText.horizontalAlign, "Explicit text should be Start, not Center")
    }

    @Test
    fun testNestedContainersWithDifferentDefaults() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "outer-col"
                newChildHorizontalAlign = Align.Start

                text("Outer left").apply { debugName = "outer-text" }

                col {
                    debugName = "inner-col"
                    newChildHorizontalAlign = Align.End

                    text("Inner right").apply { debugName = "inner-text" }
                }
            }
        }

        val outerText = root.findByDebugName("outer-text")
        assertNotNull(outerText, "Outer text should be found")
        assertEquals(Align.Start, outerText.horizontalAlign, "Outer text should use outer default (Start)")

        val innerText = root.findByDebugName("inner-text")
        assertNotNull(innerText, "Inner text should be found")
        assertEquals(Align.End, innerText.horizontalAlign, "Inner text should use inner default (End)")
    }

    @Test
    fun testNoDefaultAlignment() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "parent-col"
                // No newChildHorizontalAlign set

                text("Default stretch").apply { debugName = "stretch-text" }
            }
        }

        val stretchText = root.findByDebugName("stretch-text")
        assertNotNull(stretchText, "Text should be found")
        // Without explicit alignment or parent default, Stretch is mapped to Center in the implementation
        assertEquals(Align.Center, stretchText.horizontalAlign, "Text should have Center alignment (Stretch maps to Center)")
    }

    @Test
    fun testBothHorizontalAndVerticalDefaults() = withTestHarness { harness ->
        val root = harness.render {
            frame {
                debugName = "parent-frame"
                newChildHorizontalAlign = Align.Center
                newChildVerticalAlign = Align.Center

                text("Fully centered").apply { debugName = "centered-text" }
            }
        }

        val centeredText = root.findByDebugName("centered-text")
        assertNotNull(centeredText, "Text should be found")
        assertEquals(Align.Center, centeredText.horizontalAlign, "Text should be horizontally centered")
        assertEquals(Align.Center, centeredText.verticalAlign, "Text should be vertically centered")
    }

    @Test
    fun testDefaultAlignmentWithButton() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "parent-col"
                newChildHorizontalAlign = Align.Center

                button {
                    debugName = "centered-button"
                    text("Button")
                }
            }
        }

        val button = root.findByDebugName("centered-button")
        assertNotNull(button, "Button should be found")
        assertEquals(Align.Center, button.horizontalAlign, "Button should be horizontally centered")
    }
}
