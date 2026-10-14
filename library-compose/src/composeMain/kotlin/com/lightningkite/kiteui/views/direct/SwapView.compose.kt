package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.CoroutineScope

public actual class SwapView actual constructor(context: ElementContext) : NativeContainerElement(context) {
    actual override val underlyingNativeElement: SwapView get() = this

    // TODO: transitions; animations are out of scope for now, so swaps are instant.
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
        writer.createNewView()
        children.firstOrNull()?.let { removeChild(it) }
        newView?.let { addChild(it) }
        hasContent = newView != null
    }

    // Separate from shown, which belongs to shownWhen.
    private var hasContent by mutableStateOf(false)
    override val takesSpace: Boolean get() = shown && hasContent

    @Composable
    override fun Content(modifier: Modifier) {
        Layout(content = { ShownChildren() }, modifier = modifier, measurePolicy = FrameMeasurePolicy)
    }
}
