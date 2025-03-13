package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.shared
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.*
import kotlin.test.assertEquals

class LayoutsTestPage : Page {
    val checks = ArrayList<() -> Unit>()
    override fun ViewWriter.render(): ViewModifiable {
        fun RView.parentRectangle() = parent!!.let { rectangleRelativeTo(it) }!!
        return card - col {
            checks += { println(parentRectangle()) }
            val start = text("Start")
            lateinit var above: RView
            card - frame {
                above = this
                checks += {
                    assertEquals(start.parentRectangle().bottom + theme.spacing.canvasUnits, parentRectangle().top, 1.0)
                }
            }
            h2("Sample").apply {
                checks += {
                    assertEquals(true, parent?.themeAndBack?.drawBackground)
                    assertEquals(true, parent?.themeAndBack?.padding)
                    assertEquals(
                        theme.spacing.canvasUnits,
                        screenRectangle()?.top?.minus(above?.screenRectangle()?.bottom ?: 0.0) ?: 0.0,
                        1.0
                    )
                }
            }
            row {
                expanding - text("Left").apply {
                    checks += {
                        assertEquals(
                            (this@row.parentRectangle()?.width?.div(2) ?: 0.0) - theme.spacing.canvasUnits / 2,
                            (parentRectangle()?.right ?: 0.0),
                            1.0
                        )
                    }
                }
                expanding - text("Right").apply {
                    checks += {
                        assertEquals(
                            (this@row.parentRectangle()?.width?.div(2) ?: 0.0) + theme.spacing.canvasUnits / 2,
                            (parentRectangle()?.left ?: 0.0),
                            1.0
                        )
                    }
                }
            }


            // Verify that spacing is set between each item produced by the forEach.
            col {
                val customSpacing = 25.px
                spacing = customSpacing
                val textList = shared { listOf("Text 1", "Text 2", "Text 3").withIndex().toList() }
                val textViews = mutableListOf<TextView>()
                forEach(textList) { (index, it) ->
                    textViews.add(text(it).apply {
                        checks += check@{
                            val below = textViews.getOrNull(index + 1) ?: return@check
                            assertEquals(
                                customSpacing.canvasUnits,
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
