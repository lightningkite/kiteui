package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertEquals

class AlignmentTest {

    @Test
    fun defaultAlignmentInCol() = uiTest(
        content = {
            col {
                debugName = "parent"
                newChildHorizontalAlign = Align.Center
                text("A").apply { debugName = "a" }
                text("B").apply { debugName = "b" }
            }
        }
    ) {
        assertEquals("horizontal=Center vertical=Stretch", getAlignment("a"))
        assertEquals("horizontal=Center vertical=Stretch", getAlignment("b"))
    }

    @Test
    fun defaultAlignmentInRow() = uiTest(
        content = {
            row {
                debugName = "parent"
                newChildVerticalAlign = Align.End
                text("A").apply { debugName = "a" }
                text("B").apply { debugName = "b" }
            }
        }
    ) {
        assertEquals("horizontal=Stretch vertical=End", getAlignment("a"))
        assertEquals("horizontal=Stretch vertical=End", getAlignment("b"))
    }

    @Test
    fun defaultAlignmentInFrame() = uiTest(
        content = {
            frame {
                debugName = "parent"
                newChildHorizontalAlign = Align.End
                newChildVerticalAlign = Align.Start
                text("X").apply { debugName = "x" }
            }
        }
    ) {
        assertEquals("horizontal=End vertical=Start", getAlignment("x"))
    }

    @Test
    fun explicitOverridesDefault() = uiTest(
        content = {
            col {
                newChildHorizontalAlign = Align.Center
                text("Default").apply { debugName = "default" }
                align(Align.Start, Align.Stretch).text("Explicit").apply { debugName = "explicit" }
            }
        }
    ) {
        assertEquals("horizontal=Center vertical=Stretch", getAlignment("default"))
        assertEquals("horizontal=Start vertical=Stretch", getAlignment("explicit"))
    }

    @Test
    fun nestedContainersDifferentDefaults() = uiTest(
        content = {
            col {
                newChildHorizontalAlign = Align.Start
                text("Outer").apply { debugName = "outer" }
                col {
                    newChildHorizontalAlign = Align.End
                    text("Inner").apply { debugName = "inner" }
                }
            }
        }
    ) {
        assertEquals("horizontal=Start vertical=Stretch", getAlignment("outer"))
        assertEquals("horizontal=End vertical=Stretch", getAlignment("inner"))
    }

    @Test
    fun bothAxes() = uiTest(
        content = {
            frame {
                newChildHorizontalAlign = Align.Center
                newChildVerticalAlign = Align.Center
                text("Centered").apply { debugName = "centered" }
            }
        }
    ) {
        assertEquals("horizontal=Center vertical=Center", getAlignment("centered"))
    }
}
