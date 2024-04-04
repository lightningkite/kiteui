package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Cancellable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.reactive.CalculationContext
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.get

actual class NContext { companion object { val shared = NContext() }}
actual val NView.nContext: NContext get() = NContext.shared
actual fun NView.removeNView(child: NView) {
    this.removeChild(child)
    child.shutdown()
}

actual fun NView.listNViews(): List<NView> = children.let {
    (0..<it.length).mapNotNull { index -> it.get(index) as? HTMLElement }.toList()
}

actual fun NView.scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
    val d: dynamic = js("{}")
    d.behavior = if(animate) "smooth" else "instant"
    d.inline = when(horizontal) {
        Align.Start -> "start"
        Align.Center -> "center"
        Align.End -> "end"
        else -> "nearest"
    }
    d.block = when(vertical) {
        Align.Start -> "start"
        Align.Center -> "center"
        Align.End -> "end"
        else -> "nearest"
    }
    this.scrollIntoView(d)
}

var animationsEnabled: Boolean = true
actual inline fun NView.withoutAnimation(action: () -> Unit) {
    if(!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        clientWidth
        classList.add("notransition")
        clientWidth
        action()
    } finally {
        offsetHeight  // force layout calculation
        window.setTimeout({
            classList.remove("notransition")
        }, 100)
        animationsEnabled = true
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NView = HTMLElement

actual var NView.exists: Boolean
    get() = throw NotImplementedError()
    set(value) {
        hidden = !value
    }

actual var NView.visible: Boolean
    get() = throw NotImplementedError()
    set(value) {
        style.visibility = if (value) "visible" else "hidden"
    }

actual var NView.spacing: Dimension
    get() {
        return Dimension(style.getPropertyValue("--spacing").takeUnless { it.isBlank() } ?: "0px")
    }
    set(value) {
        style.setProperty("--spacing", value.value)
        val cn = "spacingOf${value.value.replace(".", "_").filter { it.isLetterOrDigit() || it == '_' }}"
        DynamicCSS.styleIfMissing(".$cn.$cn.$cn.$cn.$cn.$cn.$cn > *, .$cn.$cn.$cn.$cn.$cn.$cn.$cn > .hidingContainer > *", mapOf(
            "--parentSpacing" to value.value
        ))
        className = className.split(' ').filter { !it.startsWith("spacingOf") }.plus(cn).joinToString(" ")
    }

actual var NView.ignoreInteraction: Boolean
    get() = this.classList.contains("noInteraction")
    set(value) { if(value) this.classList.add("noInteraction") else this.classList.remove("noInteraction") }

actual var NView.opacity: Double
    get() = throw NotImplementedError()
    set(value) {
        style.opacity = value.toString()
    }

actual var NView.nativeRotation: Angle
    get() = throw NotImplementedError()
    set(value) {
        style.transform = "rotate(${value.turns}turn)"
    }

actual fun NView.clearNViews() {
    val c = childNodes
    (0 ..< c.length).forEach { (c.get(it) as? HTMLElement)?.shutdown() }
    innerHTML = ""
}
actual fun NView.addNView(child: NView) {
    // Cursed as fuck
    if(this.classList.contains("kiteui-stack")){
        child.style.zIndex = this.childElementCount.toString()
    }
    appendChild(child)
}

fun NView.shutdown() {
    calculationContextMaybe?.cancel()
    val c = childNodes
    (0 ..< c.length).forEach { (c.get(it) as? HTMLElement)?.shutdown() }
}

data class NViewCalculationContext(val native: NView): CalculationContext.WithLoadTracking(), Cancellable {
    val removeListeners = ArrayList<()->Unit>()
    override fun cancel() {
        removeListeners.removeAll { it(); true }
    }

    override fun onRemove(action: () -> Unit) {
        removeListeners.add(action)
    }

    override fun showLoad() {
        native.classList.add("loading")
    }

    override fun hideLoad() {
        native.classList.remove("loading")
    }
}
private val CalculationContextSymbol = js("Symbol('CalculationContextSymbol')")
val NView.calculationContextMaybe: NViewCalculationContext?
    get() = this.asDynamic()[CalculationContextSymbol] as? NViewCalculationContext
actual val NView.calculationContext: CalculationContext
    get() {
        return this.asDynamic()[CalculationContextSymbol] as? NViewCalculationContext ?: run {
            val new = NViewCalculationContext(this)
            this.asDynamic()[CalculationContextSymbol] = new
            return new
        }
    }

actual fun NView.consumeInputEvents() {
    onclick = { it.stopImmediatePropagation() }
}

/**
 *  There's no explicit "light" mode detection in web.  Bummer.  So it's `true` or `null`.
 */
actual val NContext.darkMode: Boolean? get() = window.matchMedia("(prefers-color-scheme: dark)").matches.takeIf { it }