package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.models.px
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.INSTANT
import org.w3c.dom.SMOOTH
import org.w3c.dom.ScrollBehavior
import org.w3c.dom.ScrollIntoViewOptions
import kotlin.math.roundToInt

actual fun NativeElement.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
    native.element?.scrollIntoView(
        ScrollIntoViewOptions(
            block = vertical.logicalPosition(),
            inline = horizontal.logicalPosition(),
            behavior = if (animate) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
        )
    )
}

actual fun NativeElement.nativeSetDragData(data: DragData?) {
    native.onElement { element ->
        if (data != null) {
            (element as HTMLElement).ondragstart = { event ->
                event.stopPropagation()
                for ((type, value) in data.typeToData) {
                    event.dataTransfer!!.setData(type, value)
                    data.dragShadow?.let { shadow ->
                        shadow.view.native.onElement {
                            event.dataTransfer!!.setDragImage(
                                it,
                                x = when (shadow.xAlign) {
                                    Align.Start -> 0
                                    Align.Center, Align.Stretch -> (it.getBoundingClientRect().width / 2).roundToInt()
                                    Align.End -> it.getBoundingClientRect().width.roundToInt()
                                } + (shadow.xOffset?.px?.roundToInt() ?: 0),
                                y = when (shadow.yAlign) {
                                    Align.Start -> 0
                                    Align.Center, Align.Stretch -> (it.getBoundingClientRect().height / 2).roundToInt()
                                    Align.End -> it.getBoundingClientRect().height.roundToInt()
                                } + (shadow.yOffset?.px?.roundToInt() ?: 0)
                            )
                        }
                    }
                }
            }
        } else {
            (element as HTMLElement).ondragstart = null
            element.ondragend = null
        }
    }
}

actual fun NativeElement.nativeOnDrop(listener: DropTargetDelegate?) {
    fun DragEvent.toDragEvent() = com.lightningkite.kiteui.models.DragEvent(
        data = DragData("", typeToData = dataTransfer!!.types.associate { it to dataTransfer!!.getData(it) }),
        xInView = x,
        yInView = y
    )

    native.onElement {
        if (listener != null) {
            it as HTMLElement
            it.ondragover = { e ->
                if (listener.over(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondragenter = { e ->
                if (listener.enter(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondragleave = { e ->
                if (listener.exit(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondragend = { e ->
                if (listener.end(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondrop = { e ->
                if (listener.drop(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
        } else {
            (it as HTMLElement).ondragover = null
            it.ondragenter = null
            it.ondragleave = null
            it.ondragexit = null
            it.ondrop = null
        }
    }
}