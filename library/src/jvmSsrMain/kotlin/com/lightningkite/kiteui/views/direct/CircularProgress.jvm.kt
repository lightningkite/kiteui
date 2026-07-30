package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.NativeElement
import kotlin.math.PI

public actual class CircularProgress actual constructor(context: ElementContext) :
    NativeElement(context) {

    private val circumference: Double = 2 * PI * 16
    private val circle: FutureElement

    init {
        native.tag = "div"
        native.setStyleProperty("background-color", "transparent")
        native.setAttribute("role", "progressbar")
        native.setAttribute("aria-valuemin", "0")
        native.setAttribute("aria-valuemax", "100")
        native.setAttribute("aria-valuenow", "0")

        circle = FutureElement().apply {
            tag = "circle"
            xmlns = "http://www.w3.org/2000/svg"
            classes.add("circle-progress")
            setAttribute("cx", "18")
            setAttribute("cy", "18")
            setAttribute("r", "16")
            setAttribute("fill", "none")
            setAttribute("stroke-linecap", "round")
            setAttribute("transform", "rotate(-90 18 18)")
            setAttribute("stroke-dasharray", circumference.toString())
            setAttribute("stroke-dashoffset", circumference.toString())
        }

        native.appendChild(FutureElement().apply {
            tag = "svg"
            xmlns = "http://www.w3.org/2000/svg"
            setAttribute("viewBox", "0 0 36 36")
            setAttribute("width", "100%")
            setAttribute("height", "100%")

            appendChild(FutureElement().apply {
                tag = "circle"
                xmlns = "http://www.w3.org/2000/svg"
                classes.add("circle-progress-background")
                setAttribute("cx", "18")
                setAttribute("cy", "18")
                setAttribute("r", "16")
                setAttribute("stroke-width", "3")
                setAttribute("fill", "none")
            })
            appendChild(circle)
        })
    }

    private var ratioBacking: Float = 0f

    public actual var ratio: Float
        get() = ratioBacking
        set(value) {
            val p = value.coerceIn(0f, 1f)
            ratioBacking = p
            val offset = circumference * (1 - p)
            circle.setAttribute("stroke-dashoffset", offset.toString())
            native.setAttribute("aria-valuenow", (p * 100).toInt().toString())
        }
}
