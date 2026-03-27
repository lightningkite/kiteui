package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.reactive.context.StatusListener

/**
 * Base interface for all UI elements in KiteUI.
 *
 * All elements are backed by platform-native views and support reactivity, theming, and lifecycle management.
 * Elements can be either standalone views or containers that hold child elements.
 */
@ViewTreeBuilder
interface Element : KiteUiCoroutineScopeHelpers, StatusListener {
    val context: ElementContext
    val underlyingNativeElement: NativeElement
    val parent: ContainerElement?

    @OverrideOnly
    fun onStartup()

    @OverrideOnly
    fun onShutdown()

    var opacity: Double
    var shown: Boolean
    var visible: Boolean
    var ignoreInteraction: Boolean
    var paddingByEdge: Edges?
    var safeAreaPadding: Edges?

    var themeChoice: ThemeDerivation
    val themeAndBack: ThemeAndBack

    var dragData: DragData?
    var dropTargetDelegate: DropTargetDelegate?

    fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    fun requestFocus()

    var debugName: String?
    var showOnPrint: Boolean

    val driverValue: String? get() = null
    val driverActions: Map<String, suspend (List<String>) -> String> get() = AiDriver.Defaults.defaultDriverActions(this)
    fun driverDisplay(options: DriverSnapshotOptions): String = AiDriver.Defaults.defaultDriverDisplay(this, options)

    companion object;

    data class DriverSnapshotOptions(
        val includeHidden: Boolean = false,
        val interactiveOnly: Boolean = false,
        val includeThemes: Boolean = false,
    )

    object Debugger {
        var removeBeforeShutdown = false
        var leakDetect = false
        var debugTarget: Element? = null
    }
}

/**
 * An element that can contain child elements.
 *
 * Container elements manage child lifecycle, layout, and theming propagation.
 */
interface ContainerElement : Element, ViewWriter {
    override val underlyingNativeElement: NativeContainerElement

    /** List of child elements in this container */
    val children: List<Element>

    /** Adds a child element at the specified index */
    fun addChild(index: Int, element: Element)

    /** Removes the child element at the specified index */
    fun removeChild(index: Int)

    /** Adds a child element at the end */
    @OptIn(OverrideOnly::class)
    override fun addChild(element: Element) = addChild(children.size, element)

    /** Removes the specified child element */
    fun removeChild(element: Element) {
        val i = children.indexOf(element)
        if (i != -1) removeChild(i)
        else throw IllegalArgumentException("$element is not a child of $this!")
    }

    /** Removes all child elements */
    fun clearChildren() { for (i in children.indices) removeChild(i) }



    /** Spacing used for child [CornerRadii.RatioOfSpacing] and [CornerRadii.AdaptiveToSpacing] calculations. */
    @Deprecated("Will probably be removed in the future.")
    val spacingForChildCornerRadii: Dimension get() {
        val pad = padding ?: themeAndBack.theme.padding.top
        val gap = themeAndBack.theme.gap
        return minOf(pad, gap)
    }
}