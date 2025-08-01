package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.dom.MouseEvent
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

public actual class Button actual constructor(context: RContext): RViewWithSecondaryAction(context) {
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

    private var downPress: Pair<Double, Double> = Pair(0.0, 0.0)
    private var longPressDetect: Job? = null
    init {
        val beginLongPressCountdown = { e: Event ->
            val me = e as? MouseEvent
            downPress = Pair(me?.pageX ?: 0.0, me?.pageY ?: 0.0)
            longPressDetect = longPressDetect ?: launch {
                delay(500)
                secondaryAction?.let {
                    longPressDetect = null
                    if (enabled) {
                        it.startAction(this)
                    }
                }
            }
        }
        val cancelOrClick = { e: Event ->
            val me = e as? MouseEvent
            longPressDetect?.cancel()
            if (longPressDetect != null) {
                longPressDetect = null
                if (me != null) {
                    val dx = me.pageX - downPress.first
                    val dy = me.pageY - downPress.second
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist <= 5) {
                        action?.startAction(this)
                    }
                }
            }
            Unit
        }
        val cancel = { event: Event ->
            longPressDetect?.cancel()
            longPressDetect = null
        }

        native.addEventListener("mousedown", beginLongPressCountdown)
        native.addEventListener("touchstart", beginLongPressCountdown)

        native.addEventListener("mouseup", cancelOrClick)
        native.addEventListener("mouseleave", cancel)
        native.addEventListener("touchend", cancelOrClick)
        native.addEventListener("touchcancel", cancel)
    }

    public actual inline var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
        }

}
