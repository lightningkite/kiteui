package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

actual class Button actual constructor(context: RContext): RViewWithAction(context) {
    init {
        themeChoice += ClickableSemantic
        native.tag = "button"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
    }

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    private var longPressDetect: Job? = null

    init {
        val beginLongPressCountdown = { _: Event ->
            longPressDetect = longPressDetect ?: launch {
                delay(500)
                onLongClick?.let {
                    longPressDetect = null
                    it()
                }
            }
        }
        val cancelOrClick = { event: Event ->
            longPressDetect?.cancel()
            if (longPressDetect != null) {
                action?.startAction(this)
            }
            Unit
        }
        val cancel = { event: Event ->
            longPressDetect?.cancel()
            Unit
        }

        native.addEventListener("mousedown", beginLongPressCountdown)
        native.addEventListener("touchstart", beginLongPressCountdown)

        native.addEventListener("mouseup", cancelOrClick)
        native.addEventListener("mouseleave", cancel)
        native.addEventListener("touchend", cancelOrClick)
        native.addEventListener("touchcancel", cancel)
    }

    actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }

    private var onLongClick: (suspend () -> Unit)? = null
    actual fun onLongClick(action: (suspend () -> Unit)?) {
        onLongClick = action
    }
}
