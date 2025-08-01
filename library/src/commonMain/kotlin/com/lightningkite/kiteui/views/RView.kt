package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope

expect abstract class RView constructor(context: RContext) : RViewHelper {
    override var showOnPrint: Boolean
    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)
    override fun requestFocus()
    override fun screenRectangle(): Rect?
    override fun applyTheme(theme: ThemeAndBack)
    override fun internalAddChild(index: Int, view: RView)
    override fun internalRemoveChild(index: Int)
    override fun internalClearChildren()
}

abstract class RViewWrapper(context: RContext) : RView(context) {
    override var gap: Dimension? = null
        get() = field ?: parent?.gap
}

abstract class RViewWithAction(context: RContext) : RView(context) {
    private var actionStatusRemove: (() -> Unit)? = null
    init { onRemove { actionStatusRemove?.invoke(); actionStatusRemove = null } }
    var action: Action? = null
        set(value) {
            field = value
            actionSet(value)
        }

    open fun actionSet(value: Action?) {
        actionStatusRemove?.invoke()
        actionStatusRemove = value?.let { listenForWorking(it) }
    }
}

abstract class RViewWithSecondaryAction(context: RContext) : RViewWithAction(context) {
    private var secondaryActionStatusRemove: (() -> Unit)? = null
    init { onRemove { secondaryActionStatusRemove?.invoke(); secondaryActionStatusRemove = null } }
    var secondaryAction: Action? = null
        set(value) {
            field = value
            secondaryActionSet(value)
        }

    open fun secondaryActionSet(value: Action?) {
        secondaryActionStatusRemove?.invoke()
        secondaryActionStatusRemove = value?.let { listenForWorking(it) }
    }
}

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

expect val RView.areAnimationsEnabled: Boolean
expect inline fun RView.withoutAnimation(action: () -> Unit)

//expect abstract class RView2 expect constructor(context: RContext): CoroutineContext {
//    val context: RContext
//
//    open var cannotBeCovered: Boolean
//    open var showOnPrint: Boolean
//    open var opacity: Double
//    open var shown: Boolean
//    open var visible: Boolean
//    open var gap: Dimension?
//    open var ignoreInteraction: Boolean
//    open var paddingByEdge: Edges?
//    open var transitionId: String?
//
//    // drag 'n drop
//    open var dragData: DragData?
//    open var dropTargetDelegate: DropTargetDelegate?
//
//    abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
//    abstract fun requestFocus()
//
//    var interceptor: ExceptionInterceptor?
//
//    // Theming
//    val workAndLoadTracker: WorkAndLoadTracker
//    val themeCalculator: ThemeCalculator
//    abstract fun applyTheme(theme: ThemeAndBack)
//    open val mySpacingForChildren: Dimension
//
//    // Hierarchy
//    val parent: RView2Parent?
//    open fun startup(parent: RView2Parent)
//    open fun shutdown()
//
//    // Debugging and testing
//    var debugName: String?
//    abstract fun screenRectangle(): Rect?
//
//}
//
//expect abstract class RView2Parent expect constructor(context: RContext) : RView2 {
//    val children: List<RView>
//
//    fun willAddChild(view: RView)
//    fun addChild(index: Int, view: RView)
//    fun addChild(view: RView)
//    fun removeChild(index: Int)
//    fun clearChildren()
//}
//
//@Deprecated("Renamed to 'shown'", ReplaceWith("shown"))
//var RView2.exists: Boolean
//    get() = shown
//    set(value) { shown = value }
//
//@Deprecated("Renamed to 'gap'", ReplaceWith("gap"))
//var RView2.spacing: Dimension?
//    get() = gap
//    set(value) { gap = value }
//
//var RView2.padding: Dimension?
//    get() = paddingByEdge?.left
//    set(value) { paddingByEdge = value?.let(::Edges) }
//val RView2.theme: Theme get() = themeCalculator.themeAndBack.theme
//val RView2.themeAndBack get() = themeCalculator.themeAndBack
//var RView2.themeChoice: ThemeDerivation
//    get() = themeCalculator.themeChoice
//    set(value) { themeCalculator.themeChoice = value }
//
//@Deprecated("Not needed anymore", ReplaceWith("this"))
//val RView2.calculationContext get() = this as CoroutineScope
//
//fun RView2.handleException(throwable: Exception, working: Boolean, source: RView2 = this): (() -> Unit)? {
//    return generateSequence(this) { it.parent }
//        .firstNotNullOfOrNull { it.interceptor?.handle(source, working, throwable) }
//        ?: ExceptionHandlers.root.handle(this, working, throwable)
//}
//
//abstract class RViewWrapper(context: RContext) : RView(context) {
//    override var gap: Dimension? = null
//        get() = field ?: parent?.gap
//}
//
//interface ExceptionInterceptor {
//    fun handle(view: RView, working: Boolean, throwable: Throwable): (() -> Unit)?
//    fun toMessage(view: RView, working: Boolean, throwable: Throwable): ExceptionMessage?
//}
//
