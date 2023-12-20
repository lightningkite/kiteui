@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.rock.views.direct

import com.lightningkite.rock.views.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName

fun ViewWriter.todo(name: String) = element(UIView())  {}

class Ref<T>(var target: T?)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
inline fun UIControl.onEvent(events: UIControlEvents, crossinline action: ()->Unit): ()->Unit {
    val actionHolder = object: NSObject() {
        @ObjCAction
        fun eventHandler() = action()
    }
    val sel = sel_registerName("eventHandler")
    addTarget(actionHolder, sel, events)
    val ref = Ref(actionHolder)
    calculationContext.onRemove {
        // Retain the sleeve until disposed
        ref.target?.let { removeTarget(it, sel, events) }
        ref.target = null
    }
    return {
        ref.target?.let { removeTarget(it, sel, events) }
        ref.target = null
    }
}

fun UIControl.findNextFocus(): UIView? {
    return superview?.let {
        it.findNextParentFocus(startingAtIndex = subviews.indexOf(this) + 1)
    }
}

private fun UIView.findNextParentFocus(startingAtIndex: Int): UIView? {
    findNextChildFocus(startingAtIndex = startingAtIndex)?.let { return it }
    return superview?.let {
        it.findNextParentFocus(startingAtIndex = subviews.indexOf(this) + 1)
    }
}

private fun UIView.findNextChildFocus(startingAtIndex: Int): UIView? {
    var index = startingAtIndex
    while(index < subviews.size - 1) {
        val sub = subviews[index] as UIView
        if (sub.canBecomeFirstResponder) {
            return sub
        } else {
            sub.findNextChildFocus(0)?.let { return it }
        }
        index++
    }
    return null
}

object NextFocusDelegate: NSObject(), UITextFieldDelegateProtocol {
    override fun textFieldShouldReturn(textField: UITextField): Boolean {
        textField.findNextFocus()?.let {
            it.becomeFirstResponder()
        } ?: textField.resignFirstResponder()
        return true
    }
}

//private val UIViewExtensionGravity = ExtensionProperty<UIView, Pair<Align, Align>>()
//val UIView.extensionGravity by UIViewExtensionGravity
//class FrameView: UIView(CGRectZero.readValue()) {
//    override fun addSubview(view: UIView) {
//        super.addSubview(view)
//    }
//}

//@ViewDsl
//internal fun ViewWriter.textElement(elementBase: String, setup: TextView.() -> Unit): Unit =
//    themedElement<HTMLDivElement>(elementBase) {
//        setup(TextView(this))
//        style.whiteSpace = "pre-wrap"
//    }
//
//@ViewDsl
//internal fun ViewWriter.headerElement(elementBase: String, setup: TextView.() -> Unit): Unit =
//    themedElement<HTMLDivElement>(elementBase) {
//        setup(TextView(this))
//        style.whiteSpace = "pre-wrap"
//        classList.add("title")
//    }
//
//fun UIView.__resetContentToOptionList(options: List<WidgetOption>, selected: String) {
//    innerHTML = ""
//    for (item in options) appendChild((document.createElement("option") as HTMLOptionElement).apply {
//        this.value = item.key
//        this.innerText = item.display
//        this.selected = item.key == selected
//    })
//}
//
//internal fun Canvas.pointerListenerHandler(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): (Event) -> Unit =
//    {
//        val event = it as PointerEvent
//        val b = native.getBoundingClientRect()
//        action(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height)
//    }
//
//
//external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
//    fun disconnect()
//    fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
//    fun unobserve(target: Element)
//}
//external interface ResizeObserverOptions {
//    val box: String
//}
//external interface ResizeObserverEntry {
//    val target: Element
//    val contentRect: DOMRectReadOnly
//    val contentBoxSize: ResizeObserverEntryBoxSize
//    val borderBoxSize: ResizeObserverEntryBoxSize
//}
//external interface ResizeObserverEntryBoxSize {
//    val blockSize: Double
//    val inlineSize: Double
//}
//
//class SizeReader(val native: UIView, val key: String): Readable<Double> {
//    override suspend fun awaitRaw(): Double = native.asDynamic()[key].unsafeCast<Int>().toDouble()
//    override fun addListener(listener: () -> Unit): () -> Unit {
//        val o = ResizeObserver { _, _ ->
//            listener()
//        }
//        o.observe(native)
//        return { o.disconnect() }
//    }
//}