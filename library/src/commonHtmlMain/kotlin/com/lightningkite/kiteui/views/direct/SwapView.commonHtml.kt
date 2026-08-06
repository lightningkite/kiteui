package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*


public actual class SwapView actual constructor(context: ElementContext) : NativeContainerElement(context) {
    actual override val underlyingNativeElement: SwapView get() = this

    init {
        native.tag = "div"
        native.classes.add("kiteui-stack")
        native.style.overflowX = "hidden"
        native.style.overflowY = "hidden"
    }

    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    internal var previousLast: Element? = null
    public actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit) {
        nativeSwap(transition, createNewView)
    }
}

public expect fun SwapView.nativeSwap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit)
//{
//    val keyframeName = KiteUiCss.transition(transition)
//
//    val myStyle = window.getComputedStyle(native)
//    val transitionTime = myStyle.transitionDuration.takeUnless { it.isBlank() } ?: "0.15"
//    val transitionMs = Duration.parse(transitionTime)?.inWholeMilliseconds ?: 150L
//    native.children.let { (0 until it.length).map { i -> it.get(i) } }.filterIsInstance<HTMLElement>()
//        .forEach { view ->
//            if (view.asDynamic().__ROCK__removing) return@forEach
//            view.asDynamic().__ROCK__removing = true
//            view.shutdown()
//            view.style.animation = "${keyframeName}-exit $transitionTime forwards"
//            val parent = view.parentElement
//            afterTimeout(transitionMs) {
//                if (view.parentElement == parent) {
//                    native.removeChild(view)
//                }
//            }
//        }
//    native.withoutAnimation {
//        createNewView(vw)
//    }
//    (native.lastElementChild as? HTMLElement).takeUnless { it == previousLast }?.let { newView ->
//        if (native.hidden) {
//            native.hidden = false
//            native.style.opacity = "1"
//        }
//        exists = true
//        newView.style.animation = "${keyframeName}-enter $transitionTime forwards"
//    } ?: run {
//        if(!native.hidden) {
//            native.style.opacity = "0"
//            afterTimeout(transitionMs) {
//                native.hidden = true
//            }
//        }
//    }
//}