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

    // Usually one view, but as on the web, every view a swap writes is shown, and goes at the next.
    private var current: List<Element> = emptyList()

    // The old views animate out over the new ones animating in, then go, as on the web.
    public actual fun swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit) {
        val newViews = ArrayList<Element>()
        val writer = object : ViewWriter, CoroutineScope by this {
            override val context: ElementContext get() = this@SwapView.context

            @OverrideOnly
            override fun willAddChild(element: Element) {
                element.underlyingNativeElement.parent = this@SwapView
            }

            @OverrideOnly
            override fun addChild(element: Element) {
                newViews += element
            }
        }
        withoutAnimation { writer.createNewView() }
        for (old in current) old.animateOut(transition) {
            removeChild(old)
            if (current.isEmpty()) hasContent = false
        }
        current = newViews
        for (view in newViews) {
            addChild(view)
            view.animateIn(transition)
        }
        if (newViews.isNotEmpty()) hasContent = true
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
