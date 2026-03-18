package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Alignment
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.reactive.context.StatusListener
import kotlinx.coroutines.CoroutineScope

interface Element : CoroutineScopeHelpers2, StatusListener {
    val context: ElementContext
    val underlyingNativeElement: NativeElement  // in the end all elements defer to some kind of native element, otherwise they don't really exist. This interface is typically either used directly by native elements, or by delegation to a native element (like with a wrapper)

    val parent: ContainerElement?

    var opacity: Double
    var shown: Boolean
    var visible: Boolean
    var gap: Dimension?

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

    @InternalKiteUi
    fun shutdown()

    companion object;

    object Debugger {
        var removeBeforeShutdown = false
        var leakDetect = false
        var debugTarget: Element? = null
    }
}

interface ContainerElement : Element, ViewWriter2 {
    val children: List<Element>

    fun addChild(index: Int, element: Element)
    fun removeChild(index: Int)

    override fun addChild(element: Element) = addChild(children.size, element)

    fun removeChild(element: Element) {
        val i = children.indexOf(element)
        if (i != -1) removeChild(i)
        else {
            throw IllegalArgumentException("$element is not a child of $this!")
        }
    }

    fun clearChildren() { for (i in children.indices) removeChild(i) }

    var childDefaultAlignment: Alignment?
}



fun Element.withoutLoadingAnimations(): CoroutineScope = CoroutineScope(coroutineContext.minusKey(StatusListener.Key))

val Element.theme: Theme get() = themeAndBack.theme

var Element.padding: Dimension?
    get() = paddingByEdge?.left
    set(value) { paddingByEdge = value?.let(::Edges) }




private fun Element.defaultDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("scrollIntoView") {
        scrollIntoView(Align.Center, Align.Center, animate = false)
        "OK"
    }
    if (dragData != null) put("getDragData") {
        val data = dragData ?: throw DriverActionException("no dragData on this view")
        val serialized = buildString {
            append(data.label)
            for ((mime, value) in data.typeToData) {
                append('\u0000'); append(mime); append('\u0000'); append(value)
            }
        }
        kotlin.io.encoding.Base64.encode(serialized.encodeToByteArray())
    }
    if (dropTargetDelegate != null) put("drop") { args ->
        val encoded = args.firstOrNull() ?: throw DriverActionException("drop requires base64 drag data argument")
        val decoded = kotlin.io.encoding.Base64.decode(encoded).decodeToString()
        val parts = decoded.split('\u0000')
        if(parts.size % 2 == 0) throw DriverActionException("invalid drag data format: expected label followed by mime/value pairs")
        val label = parts[0]
        val typeToData = (1 until parts.size step 2).associate { parts[it] to parts[it + 1] }
        val dragData = DragData(label, typeToData)
        val delegate = dropTargetDelegate ?: throw DriverActionException("no drop target found on this view or ancestors")
        val event = DragEvent(dragData, 0.0, 0.0)
        delegate.enter(event)
        val result = delegate.drop(event)
        delegate.end(event)
        if (result) "OK" else throw DriverActionException("drop was rejected by the target")
    }
}