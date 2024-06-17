package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlin.time.Duration


actual class SwapView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-stack")
    }
    var previousLast: RView? = null
    actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
        nativeSwap(transition, createNewView)
    }
}

expect fun SwapView.nativeSwap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit
//{
//    val keyframeName = KiteUiCss.transition(transition)
//
//    val myStyle = window.getComputedStyle(native)
//    val transitionTime = myStyle.transitionDuration.takeUnless { it.isBlank() } ?: "0.15"
//    val transitionMs = Duration.parseOrNull(transitionTime)?.inWholeMilliseconds ?: 150L
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