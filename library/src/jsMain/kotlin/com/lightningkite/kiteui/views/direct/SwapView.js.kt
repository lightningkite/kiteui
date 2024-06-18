package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwapView = HTMLDivElement

@ViewDsl
actual inline fun ViewWriter.swapViewActual(crossinline setup: SwapView.() -> Unit): Unit = themedElement<NSwapView>("div") {
    classList.add("kiteui-stack")
    this.asDynamic().__ROCK_ViewWriter__ = split()
    setup(SwapView(this))
}

@ViewDsl
actual inline fun ViewWriter.swapViewDialogActual(crossinline setup: SwapView.() -> Unit): Unit = themedElement<NSwapView>("div") {
    classList.add("kiteui-stack")
    classList.add("dialog")
    this.asDynamic().__ROCK_ViewWriter__ = split()
    hidden = true
    setup(SwapView(this))
}

actual fun SwapView.swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit {
    val vw = native.asDynamic().__ROCK_ViewWriter__ as ViewWriter
    val keyframeName = DynamicCSS.transition(transition)
    val previousLast = native.lastElementChild
    val myStyle = window.getComputedStyle(native)
    val transitionTime = myStyle.transitionDuration.takeUnless { it.isBlank() } ?: "0.15"
    val transitionMs = Duration.parseOrNull(transitionTime)?.inWholeMilliseconds ?: 150L
    native.children.let { (0 until it.length).map { i -> it.get(i) } }.filterIsInstance<HTMLElement>()
        .forEach { view ->
            if (view.asDynamic().__ROCK__removing) return@forEach
            view.asDynamic().__ROCK__removing = true
            view.shutdown()
            view.style.animation = "${keyframeName}-exit $transitionTime forwards"
            val parent = view.parentElement
            afterTimeout(transitionMs) {
                if (view.parentElement == parent) {
                    native.removeChild(view)
                }
            }
        }
    native.withoutAnimation {
        createNewView(vw)
    }
    (native.lastElementChild as? HTMLElement).takeUnless { it == previousLast }?.let { newView ->
        if (native.hidden) {
            native.hidden = false
            native.style.opacity = "1"
        }
        exists = true
        newView.style.animation = "${keyframeName}-enter $transitionTime forwards"
    } ?: run {
        if(!native.hidden) {
            native.style.opacity = "0"
            afterTimeout(transitionMs) {
                native.hidden = true
            }
        }
    }
}