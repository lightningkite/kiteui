package com.lightningkite.kiteui.views

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
interface Element : CoroutineScopeHelpers2, StatusListener {
    /** Context providing platform-specific resources and configuration */
    val context: ElementContext

    /** The underlying native element that this element ultimately delegates to for rendering */
    val underlyingNativeElement: NativeElement

    /** Parent container element, null if this is a root element */
    val parent: ContainerElement?

    /** Opacity level from 0.0 (transparent) to 1.0 (opaque) */
    var opacity: Double

    /** Whether the element is shown (does not take up space when false) */
    var shown: Boolean

    /** Whether the element is visible (takes up space but may be visually hidden) */
    var visible: Boolean

    /** Whether the element should ignore all user interaction */
    var ignoreInteraction: Boolean

    /** Custom padding for each edge, overrides theme padding */
    var paddingByEdge: Edges?

    /** Additional padding for safe areas (notches, system bars, etc.) */
    var safeAreaPadding: Edges?

    /** Data to be provided when this element is dragged */
    var dragData: DragData?

    /** Delegate for handling drop events when items are dropped on this element */
    var dropTargetDelegate: DropTargetDelegate?

    /** Scrolls this element into view within its scrollable parent */
    fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)

    /** Requests keyboard focus for this element */
    fun requestFocus()

    /** Theme derivation to apply to this element (semantic theme changes like 'important', 'card', etc.) */
    var themeChoice: ThemeDerivation

    /** Current resolved theme with background information */
    val themeAndBack: ThemeAndBack

    /** Value exposed to UI driver/testing frameworks */
    val driverValue: String? get() = null

    /** Actions exposed to UI driver/testing frameworks */
    val driverActions: Map<String, suspend (List<String>) -> String> get() = defaultDriverActions()

    /** Debug name for logging and debugging purposes */
    var debugName: String?

    companion object;

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

    /** Default alignment for children if not specified individually */
    var childDefaultAlignment: Alignment?

    /** Adds a child element at the specified index */
    fun addChild(index: Int, element: Element)

    /** Removes the child element at the specified index */
    fun removeChild(index: Int)

    /** Adds a child element at the end */
    override fun addChild(element: Element) = addChild(children.size, element)

    /** Removes the specified child element */
    fun removeChild(element: Element) {
        val i = children.indexOf(element)
        if (i != -1) removeChild(i)
        else throw IllegalArgumentException("$element is not a child of $this!")
    }

    /** Removes all child elements */
    fun clearChildren() { for (i in children.indices) removeChild(i) }
}