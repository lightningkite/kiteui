package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.shared
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.*
import kotlin.test.assertEquals

class LayoutsTestScreen : Screen {
    val checks = ArrayList<() -> Unit>()
    override fun ViewWriter.render() {
        fun RView.parentRectangle() = parent?.let { rectangleRelativeTo(it) }
        card - col {
            checks += { println(parentRectangle()) }
            lateinit var above: RView
            card - stack {
                above = this
                checks += {
                    assertEquals(theme.spacing.px, parentRectangle()?.top ?: 0.0, 1.0)
                }
            }
            h2("Sample").apply {
                checks += {
                    assertEquals(true, parent?.themeAndBack?.useBackground)
                    assertEquals(
                        theme.spacing.px,
                        screenRectangle()?.top?.minus(above?.screenRectangle()?.bottom ?: 0.0) ?: 0.0,
                        1.0
                    )
                }
            }
            row {
                expanding - text("Left").apply {
                    checks += {
                        assertEquals(
                            (this@row.parentRectangle()?.width?.div(2) ?: 0.0) - theme.spacing.px / 2,
                            (parentRectangle()?.right ?: 0.0),
                            1.0
                        )
                    }
                }
                expanding - text("Right").apply {
                    checks += {
                        assertEquals(
                            (this@row.parentRectangle()?.width?.div(2) ?: 0.0) + theme.spacing.px / 2,
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
                                customSpacing.px,
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
