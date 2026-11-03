package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.*
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*


public actual class Canvas actual constructor(context: ElementContext): NativeElement(context) {
    public actual var delegate: CanvasDelegate? = null
        set(value) {
            field = value
            onDelegateSet(value)
            value?.theme = themeAndBack.theme
            delegate?.invalidate?.invoke()
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        delegate?.theme = theme.theme
        delegate?.invalidate?.invoke()
    }
    init {
        native.tag = "canvas"
        native.attributes.tabIndex = 1
        native.addEventListener("keydown") { event ->
            event as KeyboardEvent
            if(delegate?.onKeyDown(event.code) == true)
                event.preventDefault()
        }
        native.addEventListener("keyup") { event ->
            event as KeyboardEvent
            if(delegate?.onKeyUp(event.code) == true)
                event.preventDefault()
        }
        native.addEventListener("wheel") { event ->
            event as WheelEvent
            if(delegate?.onWheel(event.deltaX, event.deltaY, event.deltaZ) == true)
                event.preventDefault()
        }

        native.addEventListener("pointerdown") { event ->
            val dpr = AppState.windowInfo.value.density
            event as PointerEvent
            val b = (event.target as DOMElement).getBoundingClientRect()
            if(delegate?.onPointerDown(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointermove") { event ->
            val dpr = AppState.windowInfo.value.density
            event as PointerEvent
            val b = (event.target as DOMElement).getBoundingClientRect()
            if(delegate?.onPointerMove(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointerup") { event ->
            val dpr = AppState.windowInfo.value.density
            event as PointerEvent
            val b = (event.target as DOMElement).getBoundingClientRect()
            if(delegate?.onPointerUp(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointercancel") { event ->
            val dpr = AppState.windowInfo.value.density
            event as PointerEvent
            val b = (event.target as DOMElement).getBoundingClientRect()
            if(delegate?.onPointerCancel(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        native.addEventListener("pointerleave") { event ->
            val dpr = AppState.windowInfo.value.density
            event as PointerEvent
            val b = (event.target as DOMElement).getBoundingClientRect()
            if(delegate?.onPointerCancel(
                    event.pointerId,
                    (event.pageX - b.x) * dpr,
                    (event.pageY - b.y) * dpr,
                    (b.width) * dpr,
                    (b.height) * dpr
            ) == true) event.preventDefault()
        }
        setupResizeListener()
    }
}

public expect fun Canvas.onDelegateSet(delegate: CanvasDelegate?)

public expect fun Canvas.setupResizeListener()

//actual var Canvas.delegate: CanvasDelegate?
//    get() = this.native.asDynamic().__ROCK_delegate__ as? CanvasDelegate
//    set(value) {
//        this.native.asDynamic().__ROCK_delegate__ = value
//        value?.let { value ->
//            value.invalidate = {
//                native.getContext("2d").apply {
//                    this as DrawingContext2D
//                    this.lineCap = CanvasLineCap.ROUND
//                    this.lineJoin = CanvasLineJoin.ROUND
//                    value.draw(this)
//                }
//            }
//            value.invalidate()
//        }
//    }
