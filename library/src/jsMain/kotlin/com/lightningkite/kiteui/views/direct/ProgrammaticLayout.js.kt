package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.math.roundToInt
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.position = "relative"
    }

    public actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value; invalidateLayout()
        }
    var log: Log? = null// ConsoleRoot.tag("ProgrammaticLayout")

    override fun postSetup() {
        super.postSetup()
        onRemove(parent!!.native.resizeObserver().addListener {
            log?.log("resizeObserver calls invalidateLayout()")
            remeasureConstrainedSize()
            invalidateLayout()
        })
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        view.native.onElement { it.asDynamic().__existingMeasure = null }
        view.native.style.position = "absolute"
        view.onRemove(view.native.mutationObserver(true).addListener {
            view.native.onElement { it.asDynamic().__existingMeasure = null }
            if (timeoutSet) return@addListener
            log?.log("child mutation calls invalidateLayout()")
            invalidateLayout()
        })
        log?.log("child insert calls invalidateLayout()")
        invalidateLayout()
    }

    override fun internalRemoveChild(index: Int) {
        super.internalRemoveChild(index)
        log?.log("child remove calls invalidateLayout()")
        invalidateLayout()
    }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            spacingCurrentPx = gap?.px ?: theme.gap.px
        }

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        spacingCurrentPx = gap?.px ?: theme.gap.px
    }

    override fun refreshPadding() {
        super.refreshPadding()
        val value = appliedPadding
        paddingTopCurrentPx = value.top.viewUnits
        paddingLeftCurrentPx = value.left.viewUnits
        paddingRightCurrentPx = value.right.viewUnits
        paddingBottomCurrentPx = value.bottom.viewUnits
    }

    override fun internalClearChildren() {
        super.internalClearChildren()
        log?.log("children clear calls invalidateLayout()")
        invalidateLayout()
    }
    private var spacingCurrentPx: Double = 0.0
    private var paddingTopCurrentPx: Double = 0.0
    private var paddingLeftCurrentPx: Double = 0.0
    private var paddingRightCurrentPx: Double = 0.0
    private var paddingBottomCurrentPx: Double = 0.0

    private val inProgress = object : ProgrammingLayoutInProgress {
        override val within: Size
            get() = currentSize
        override val gap: Double get() = spacingCurrentPx
        override val padding: Double get() = paddingLeftCurrentPx
        override val paddingTop: Double get() = paddingTopCurrentPx
        override val paddingLeft: Double get() = paddingLeftCurrentPx
        override val paddingRight: Double get() = paddingRightCurrentPx
        override val paddingBottom: Double get() = paddingBottomCurrentPx
        override fun measure(child: RView, sizeConstraint: Size): Size {
            val e = child.native.element as? HTMLElement ?: return Size(0.0, 0.0)
            val existing = e.asDynamic().__existingMeasure as? Size
            val existingConstraint = e.asDynamic().__existingMeasureConstraint as? Size
            if (existing != null && existingConstraint == sizeConstraint) return existing
            val m = e.measureByDuplicate(sizeConstraint)
            e.asDynamic().__existingMeasure = m
            e.asDynamic().__existingMeasureConstraint = sizeConstraint
            return m
        }

        override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
            if (
                child.asDynamic().__last_left == left &&
                child.asDynamic().__last_top == top &&
                child.asDynamic().__last_right == right &&
                child.asDynamic().__last_bottom == bottom
            ) {
                // avoid adjusting style because it's expensive
                return
            }
            child.native.suppressMutationObserverForStyle {
                child.native.style.position = "absolute"
                child.native.style.left = left.toString() + "px"
                child.native.style.top = top.toString() + "px"
                child.native.style.width = (right - left).toString() + "px"
                child.native.style.height = (bottom - top).toString() + "px"
            }
            child.asDynamic().__last_left = left
            child.asDynamic().__last_top = top
            child.asDynamic().__last_right = right
            child.asDynamic().__last_bottom = bottom
        }

        override fun existingPosition(child: RView): Rect = Rect.fromSize(
            left = child.native.element?.scrollLeft ?: child.native.style.left?.removeSuffix("px")?.toDoubleOrNull()
            ?: 0.0,
            top = child.native.element?.scrollTop ?: child.native.style.top?.removeSuffix("px")?.toDoubleOrNull()
            ?: 0.0,
            width = child.native.element?.scrollWidth?.toDouble() ?: child.native.style.width?.removeSuffix("px")
                ?.toDoubleOrNull() ?: 0.0,
            height = child.native.element?.scrollHeight?.toDouble() ?: child.native.style.height?.removeSuffix("px")
                ?.toDoubleOrNull() ?: 0.0,
        )
    }

    private fun remeasureConstrainedSize() {
        val element = native.element as? HTMLElement ?: return
        val parentElement = element.parentElement as? HTMLElement ?: return
        log?.log("Remeasuring the parent element constraint")
        lastConstraintParentWidth = parentElement.clientWidth
        lastConstraintParentHeight = parentElement.clientHeight

        // Get current constraint style
        definedFlexGrow = native.style.flexGrow?.takeUnless { it.isBlank() }
        definedWidth = native.style.width?.takeUnless { it == enforcedWidth || it.isBlank() }
        definedHeight = native.style.height?.takeUnless { it == enforcedHeight || it.isBlank() }
        val parentIsFlex = parentElement?.style?.display?.contains("flex") == true
        val parentIsVertical = parentElement?.style?.flexDirection?.contains("col") == true
        val elementHasGrow = element.style.flexGrow.isNotBlank()
        val elementIsStretch = element.style.alignSelf == "stretch" || element.style.alignSelf.isBlank()
        lastFillWidth =
            (parentIsFlex && (parentIsVertical && elementHasGrow) || (!parentIsVertical && elementIsStretch)) || definedWidth?.contains(
                "100%"
            ) == true
        lastFillHeight =
            (parentIsFlex && (!parentIsVertical && elementHasGrow) || (parentIsVertical && elementIsStretch)) || definedHeight?.contains(
                "100%"
            ) == true

        // Remove width/height constraints if we've enforced them, Maximize the size
        if (native.style.width == enforcedWidth) {
            if (parentIsFlex && !parentIsVertical) {
                native.style.flexGrow = "999"
                native.style.width = "unset"
            } else native.style.width = "100%"
        }
        if (native.style.height == enforcedHeight) {
            if (parentIsFlex && parentIsVertical) {
                native.style.flexGrow = "999"
                native.style.height = "unset"
            } else native.style.height = "100%"
        }

        // get clientWidth and clientHeight
        lastConstraintSize = Size(element.clientWidth.toDouble(), element.clientHeight.toDouble())

        // Revert our edits
        native.style.width = definedWidth ?: "unset"
        native.style.height = definedHeight ?: "unset"
        native.style.flexGrow = definedFlexGrow ?: "unset"
    }

    private var definedFlexGrow: String? = null
    private var definedWidth: String? = null
    private var definedHeight: String? = null
    private var enforcedWidth: String? = null
    private var enforcedHeight: String? = null
    private var lastConstraintParentWidth: Int = 0
    private var lastConstraintParentHeight: Int = 0
    private var lastConstraintSize: Size = Size.Zero
    private var renderSize: Size = Size.Zero
    private var lastFillWidth: Boolean = true
    private var lastFillHeight: Boolean = true
    private var timeoutSet = false
    private var currentSize: Size = Size.Zero
    actual fun invalidateLayout() {
        log?.log("invalidateLayout()")
        if (timeoutSet) return
        window.setTimeout({
            log?.log("invalidateLayout timeout")
            val element = native.element as? HTMLElement ?: return@setTimeout
            val parentElement = element.parentElement as? HTMLElement ?: return@setTimeout

            // run measure
            currentSize = lastConstraintSize
            val natSize = delegate.measure(this, inProgress, lastConstraintSize)

            // set width and height to result IF layout rules say minimum, revert otherwise to continue taking space
            if (!lastFillWidth) {
                enforcedWidth = natSize.width.roundToInt().toString() + "px"
                element.style.width = enforcedWidth!!
            }
            if (!lastFillHeight) {
                enforcedHeight = natSize.height.roundToInt().toString() + "px"
                element.style.height = enforcedHeight!!
            }

            // run layout
            val s = Size(
                width = if(lastFillWidth) lastConstraintSize.width else natSize.width,
                height = if(lastFillHeight) lastConstraintSize.height else natSize.height
            )
            currentSize = s
            delegate.layout(this, inProgress, s)

            window.setTimeout({
                timeoutSet = false
            }, 1)
        }, 1)
        timeoutSet = true
    }
}