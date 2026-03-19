@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.alignSelf
import com.lightningkite.kiteui.views.display
import com.lightningkite.kiteui.views.flexDirection
import com.lightningkite.kiteui.views.flexGrow
import com.lightningkite.kiteui.views.hidden
import com.lightningkite.kiteui.views.marginBottom
import com.lightningkite.kiteui.views.marginLeft
import com.lightningkite.kiteui.views.marginRight
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.theme
import com.lightningkite.kiteui.views.width

actual class RowOrCol actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init {
        native.tag = "div"
        native.style.flexDirection = "column"
        native.classes += "kiteui-flex"
        native.classes += "kiteui-col"
        native.classes.add("optimized")
    }

    private var complex = false
        set(value) {
            field = value
            if (!value) native.classes.add("optimized")
            else native.classes.remove("optimized")
        }

    actual var vertical: Boolean = true
        set(value) {
            field = value
            native.style.flexDirection = if (value) "column" else "row"
            if (value) {
                native.classes -= "kiteui-row"
                native.classes += "kiteui-col"
            } else {
                native.classes -= "kiteui-col"
                native.classes += "kiteui-row"
            }
        }

    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)

        val native = element.underlyingNativeElement.native

        if (!vertical || native.style.flexGrow?.toDoubleOrNull()?.let { it > 0.0 } == true) enterFlexMode()
        val align = if (vertical) native.desiredHorizontalGravity else native.desiredVerticalGravity
        when (align) {
            Align.Start -> {
                native.style.alignSelf = "start"
            }
            Align.Center -> {
                native.style.alignSelf = "center"
            }
            Align.End -> {
                native.style.alignSelf = "end"
            }
            else -> {
                native.style.alignSelf = "stretch"
            }
        }
        if (!complex) {
            // Optimized mode requires weird stuff
            when (align) {
                Align.Start -> {
                    native.style.marginLeft = "unset"
                    native.style.marginRight = "auto"
                    if (native.style.width.let { it == null || it == "" })
                        native.style.width = "fit-content"
                }
                Align.Center -> {
                    native.style.marginLeft = "auto"
                    native.style.marginRight = "auto"
                    if (native.style.width.let { it == null || it == "" })
                        native.style.width = "fit-content"
                }
                Align.End -> {
                    native.style.marginLeft = "auto"
                    native.style.marginRight = "unset"
                    if (native.style.width.let { it == null || it == "" })
                        native.style.width = "fit-content"
                }
                else -> {
                    if (native.style.width.isNullOrEmpty()) native.style.width = "100%"
                }
            }
        }
        rerunOptimizedBottomMarginCalc()
    }

    override fun nativeClearChildren() {
        super.nativeClearChildren()
        rerunOptimizedBottomMarginCalc()
    }

    override fun nativeRemoveChild(index: Int) {
        super.nativeRemoveChild(index)
        rerunOptimizedBottomMarginCalc()
    }

    override fun startup() {
        super.startup()
        rerunOptimizedBottomMarginCalc()
    }

    fun rerunOptimizedBottomMarginCalc() {
        if (complex) return

        val newLastShownElement = children.lastOrNull { it.native.attributes.hidden != true }
        val amnt = gap ?: theme.gap
        for (child in children) child.native.style.marginBottom = amnt.value.toString()
        debug { "last shown index: ${children.indexOf(newLastShownElement)}" }
        newLastShownElement?.native?.style?.marginBottom = "0"
    }

    actual fun spacingOverrideBeforeNext(amount: Dimension) {}

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        rerunOptimizedBottomMarginCalc()
    }

    private fun enterFlexMode() {
        if (!complex) {
            complex = true
            native.style.display = "flex"
            for (child in children) {
                child.native.style.marginBottom = "0px"
            }
        }
    }
}

actual class RowWrapping actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init {
        native.tag = "div"
        native.setStyleProperty("display", "flex")
        native.setStyleProperty("flex-direction", "row")
        native.setStyleProperty("flex-wrap", "wrap")
        native.classes += "kiteui-flex"
        native.classes += "kiteui-row-wrap"
    }

    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)

        // Handle alignment
        val horizontalAlign = element.native.desiredHorizontalGravity
        val verticalAlign = element.native.desiredVerticalGravity

        // Set horizontal alignment
        when (horizontalAlign) {
            Align.Start -> {
                element.native.setStyleProperty("justify-self", "start")
            }

            Align.Center -> {
                element.native.setStyleProperty("justify-self", "center")
            }

            Align.End -> {
                element.native.setStyleProperty("justify-self", "end")
            }

            else -> {
                element.native.setStyleProperty("justify-self", "stretch")
            }
        }

        // Set vertical alignment
        when (verticalAlign) {
            Align.Start -> {
                element.native.setStyleProperty("align-self", "start")
            }

            Align.Center -> {
                element.native.setStyleProperty("align-self", "center")
            }

            Align.End -> {
                element.native.setStyleProperty("align-self", "end")
            }

            else -> {
                element.native.setStyleProperty("align-self", "stretch")
            }
        }
    }
}

actual class RowCollapsingToColumn actual constructor(context: ElementContext, breakpoints: List<Dimension>) : NativeContainerElement(context) {
    init {
        native.tag = "div"
        native.classes.add(context.kiteUiCss.rowCollapsingToColumn(breakpoints))
        native.classes.add("rowCollapsing")
    }
}