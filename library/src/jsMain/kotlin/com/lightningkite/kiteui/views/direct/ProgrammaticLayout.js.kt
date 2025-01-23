package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.views.*
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.style.position = "relative"
    }

    actual var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull
        set(value) {
            field = value; invalidateLayout()
        }
    var log: Console? = null// ConsoleRoot.tag("ProgrammaticLayout")

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


    override fun applyPadding(dimension: Dimension?) {
        super.applyPadding(dimension)
        paddingPx = dimension?.px ?: 0.0
    }
    override fun applyForeground(theme: Theme) {
        spacingPx = spacing?.px ?: theme.spacing.px
    }
    override fun spacingSet(value: Dimension?) {
        spacingPx = spacing?.px ?: theme.spacing.px
    }

    override fun internalClearChildren() {
        super.internalClearChildren()
        log?.log("children clear calls invalidateLayout()")
        invalidateLayout()
    }
    private var spacingPx: Double = 0.0
    private var paddingPx: Double = 0.0

    private val inProgress = object : ProgrammingLayoutInProgress {
        override val spacing: Double get() = spacingPx
        override val padding: Double get() = paddingPx
        override fun measure(child: RView, sizeConstraint: Size): Size {
            println("Measuring child...")
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
    actual fun invalidateLayout() {
        log?.log("invalidateLayout()")
        if (timeoutSet) return
        window.setTimeout({
            log?.log("invalidateLayout timeout")
            val element = native.element as? HTMLElement ?: return@setTimeout
            val parentElement = element.parentElement as? HTMLElement ?: return@setTimeout

            // run measure
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
            delegate.layout(this, inProgress, Size(
                width = if(lastFillWidth) lastConstraintSize.width else natSize.width,
                height = if(lastFillHeight) lastConstraintSize.height else natSize.height
            ))

            window.setTimeout({
                timeoutSet = false
            }, 1)
        }, 1)
        timeoutSet = true
    }
}