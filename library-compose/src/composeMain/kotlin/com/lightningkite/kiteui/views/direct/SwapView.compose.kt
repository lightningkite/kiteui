package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.CoroutineScope

public actual class SwapView actual constructor(context: ElementContext) : NativeContainerElement(context) {
    actual override val underlyingNativeElement: SwapView get() = this

    private var current: Element? = null

    // The old view animates out over the new one animating in, then goes, as on the web.
    public actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit) {
        var newView: Element? = null
        val writer = object : ViewWriter, CoroutineScope by this {
            override val context: ElementContext get() = this@SwapView.context

            @OverrideOnly
            override fun willAddChild(element: Element) {
                element.underlyingNativeElement.parent = this@SwapView
            }

            @OverrideOnly
            override fun addChild(element: Element) {
                newView = element
            }
        }
        withoutAnimation { writer.createNewView() }
        val old = current
        current = newView
        old?.animateOut(transition) {
            removeChild(old)
            if (current == null) hasContent = false
        }
        newView?.let {
            addChild(it)
            it.animateIn(transition)
            hasContent = true
        }
    }

    // Separate from shown, which belongs to shownWhen.
    private var hasContent by mutableStateOf(false)
    override val takesSpace: Boolean get() = shown && hasContent

    @Composable
    override fun Content(modifier: Modifier) {
        // Clipped, like the web's, so views sliding in and out don't show beyond it.
        Layout(content = { ShownChildren() }, modifier = modifier.clipToBounds(), measurePolicy = FrameMeasurePolicy)
    }
}
