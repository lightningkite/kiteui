package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.test.assertEquals

class LayoutsTestPage : Page {
    val checks = ArrayList<() -> Unit>()
    override fun ViewWriter.render(): ViewModifiable {
        fun RView.parentRectangle() = parent!!.let { rectangleRelativeTo(it) }!!
        return card.col {
            checks += { println(parentRectangle()) }
            val start = text("Start")
            lateinit var above: RView
            card.frame {
                above = this
                checks += {
                    assertEquals(start.parentRectangle().bottom + theme.gap.viewUnits, parentRectangle().top, 1.0)
                }
            }
            h2("Sample").apply {
                checks += {
                    assertEquals(true, parent?.themeAndBack?.drawBackground)
                    assertEquals(true, parent?.themeAndBack?.padding)
                    assertEquals(
                        theme.gap.viewUnits,
                        screenRectangle()?.top?.minus(above?.screenRectangle()?.bottom ?: 0.0) ?: 0.0,
                        1.0
                    )
                }
            }
            row {
                expanding.text("Left").apply {
                    checks += {
                        assertEquals(
                            (this@row.parentRectangle()?.width?.div(2) ?: 0.0) - theme.gap.viewUnits / 2,
                            (parentRectangle()?.right ?: 0.0),
                            1.0
                        )
                    }
                }
                expanding.text("Right").apply {
                    checks += {
                        assertEquals(
                            (this@row.parentRectangle()?.width?.div(2) ?: 0.0) + theme.gap.viewUnits / 2,
                            (parentRectangle()?.left ?: 0.0),
                            1.0
                        )
                    }
                }
            }


            // Verify that gap is set between each item produced by the forEach.
            col {
                val customSpacing = 25.px
                gap = customSpacing
                val textList = remember { listOf("Text 1", "Text 2", "Text 3").withIndex().toList() }
                val textViews = mutableListOf<TextView>()
                forEach(textList) { (index, it) ->
                    textViews.add(text(it).apply {
                        checks += check@{
                            val below = textViews.getOrNull(index + 1) ?: return@check
                            assertEquals(
                                customSpacing.viewUnits,
                                below.screenRectangle()?.top?.minus(screenRectangle()?.bottom ?: 0.0) ?: 0.0,
                                1.0
                            )
                        }
                    })
                }
            }

        }
    }
}



/**
 * Calculates the rectangle of this view relative to another view's coordinate space.
 *
 * This is useful for positioning elements relative to each other, such as tooltips or popovers.
 *
 * @param other The view whose coordinate space should be used as the reference.
 * @return The rectangle of this view in the other view's coordinate space, or null if either
 *         view doesn't have a screen rectangle (e.g., not visible or not yet laid out).
 */
fun RView.rectangleRelativeTo(other: RView): Rect? {
    val myRect = screenRectangle() ?: return null
    val otherRect = other.screenRectangle() ?: return null
    return Rect(
        left = myRect.left - otherRect.left,
        top = myRect.top - otherRect.top,
        right = myRect.right - otherRect.left,
        bottom = myRect.bottom - otherRect.top,
    )
}