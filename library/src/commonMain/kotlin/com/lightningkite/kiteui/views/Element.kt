package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.reactive.context.StatusListener

interface Element : CoroutineScopeHelpers2, StatusListener {
    val context: ElementContext
    val underlyingNativeElement: NativeElement  // in the end all elements defer to some kind of native element, otherwise they don't really exist. This interface is typically either used directly by native elements, or by delegation to a native element (like with a wrapper)

    val parent: ContainerElement?

    var opacity: Double
    var shown: Boolean
    var visible: Boolean

    var ignoreInteraction: Boolean

    var paddingByEdge: Edges?
    var safeAreaPadding: Edges?

    var dragData: DragData?
    var dropTargetDelegate: DropTargetDelegate?

    fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    fun requestFocus()

    var themeChoice: ThemeDerivation
    val themeAndBack: ThemeAndBack

    val driverValue: String? get() = null
    val driverActions: Map<String, suspend (List<String>) -> String> get() = defaultDriverActions()

    var debugName: String?

    companion object;

    object Debugger {
        var removeBeforeShutdown = false
        var leakDetect = false
        var debugTarget: Element? = null
    }
}

interface ContainerElement : Element, ViewWriter2 {
    override val underlyingNativeElement: NativeContainerElement

    val children: List<Element>

    fun addChild(index: Int, element: Element)
    fun removeChild(index: Int)

    override fun addChild(element: Element) = addChild(children.size, element)

    fun removeChild(element: Element) {
        val i = children.indexOf(element)
        if (i != -1) removeChild(i)
        else throw IllegalArgumentException("$element is not a child of $this!")
    }

    fun clearChildren() { for (i in children.indices) removeChild(i) }

    var childDefaultAlignment: Alignment?

    var gap: Dimension?

    val mySpacingForChildren: Dimension get() {
        val pad = padding ?: themeAndBack.theme.padding.top
        val gap = gap ?: themeAndBack.theme.gap
        return minOf(pad, gap)
    }
}