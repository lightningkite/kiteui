package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*

actual class Stack actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }
    companion object {
        internal fun internalAddChildStack(on: RView, index: Int, view: RView) {
            if(index >= 1) {
                if(on.native.style.display != "grid") {
                    on.native.style.display = "grid"
                    on.native.setStyleProperty("grid-template-columns", "100%")
                    on.native.setStyleProperty("grid-template-rows", "100%")
                }
            }
            on.native.children.forEachIndexed { index, futureElement ->
                view.native.style.zIndex = index.toString()
            }

            view.native.setStyleProperty("grid-area", "1 / 1 / 1 / 1")
            when(view.native.desiredVerticalGravity) {
                Align.Start -> {
                    view.native.style.verticalAlign = "top"
                    view.native.style.alignSelf = "start"
                }
                Align.Center -> {
                    if(on.native.style.display != "grid") {
                        on.native.style.display = "grid"
                        on.native.setStyleProperty("grid-template-columns", "100%")
                        on.native.setStyleProperty("grid-template-rows", "100%")
                    }
                    view.native.style.alignSelf = "center"
                }
                Align.End -> {
                    view.native.style.verticalAlign = "bottom"
                    view.native.style.alignSelf = "end"
                }
                else -> {
                    view.native.style.verticalAlign = "top"
                    if(view.native.style.height.isNullOrEmpty()) view.native.style.height = "100%"
                    view.native.style.alignSelf = "stretch"
                }
            }
            when(view.native.desiredHorizontalGravity) {
                Align.Start -> {
                    view.native.style.marginLeft = "unset"
                    view.native.style.marginRight = "auto"
                    view.native.style.justifySelf = "start"
                }
                Align.Center -> {
                    view.native.style.marginLeft = "auto"
                    view.native.style.marginRight = "auto"
                    view.native.style.justifySelf = "center"
                }
                Align.End -> {
                    view.native.style.marginLeft = "auto"
                    view.native.style.marginRight = "unset"
                    view.native.style.justifySelf = "end"
                }
                else -> {
                    if(view.native.style.width.isNullOrEmpty()) view.native.style.width = "100%"
                    view.native.style.justifySelf = "stretch"
                }
            }
        }
    }
}

actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.flexDirection = "column"
    }
    private var complex = false
    actual var vertical: Boolean = true
        set(value) {
            field = value
            native.style.flexDirection = if(value) "column" else "row"
        }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        if (!vertical || view.native.style.flexGrow?.toDoubleOrNull()?.let { it > 0.0 } == true) enterFlexMode()
        val align = if (vertical) view.native.desiredHorizontalGravity else view.native.desiredVerticalGravity
        when(align) {
            Align.Start -> {
                view.native.style.alignSelf = "start"
            }
            Align.Center -> {
                view.native.style.alignSelf = "center"
            }
            Align.End -> {
                view.native.style.alignSelf = "end"
            }
            else -> {
                view.native.style.alignSelf = "stretch"
            }
        }
        if (!complex) {
            // Optimized mode requires weird stuff
            view.native.classes.add("optColChild")
            when(align) {
                Align.Start -> {
                    view.native.style.marginLeft = "unset"
                    view.native.style.marginRight = "auto"
                    if(view.native.style.width.let { it == null || it == "" })
                        view.native.style.width = "fit-content"
                }
                Align.Center -> {
                    view.native.style.marginLeft = "auto"
                    view.native.style.marginRight = "auto"
                    if(view.native.style.width.let { it == null || it == "" })
                        view.native.style.width = "fit-content"
                }
                Align.End -> {
                    view.native.style.marginLeft = "auto"
                    view.native.style.marginRight = "unset"
                    if(view.native.style.width.let { it == null || it == "" })
                        view.native.style.width = "fit-content"
                }
                else -> {
                    if(view.native.style.width.isNullOrEmpty()) view.native.style.width = "100%"
                }
            }
        }
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        if (!complex) {
            val amnt = spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing
            for (child in children) {
                child.native.style.marginBottom = amnt.value
            }
        }
    }
    private fun enterFlexMode() {
        if (!complex) {
            complex = true
            native.style.display = "flex"
            for (child in children) {
                native.classes.remove("optColChild")
                child.native.style.marginBottom = "0px"
            }
        }
    }
}

actual class RowCollapsingToColumn actual constructor(context: RContext, breakpoints: List<Dimension>) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add(context.kiteUiCss.rowCollapsingToColumn(breakpoints))
        native.classes.add("rowCollapsing")
    }
}
