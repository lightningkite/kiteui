package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.svg.SVGCircleElement
import org.w3c.dom.svg.SVGSVGElement
import kotlin.math.PI

actual class CircularProgress actual constructor(context: ElementContext) : NativeElement(context) {

    private val circle: SVGCircleElement
    private val circumference: Double

    init {
        native.tag = "div"
        native.setStyleProperty("background-color", "transparent")


        val svg = document.createElementNS("http://www.w3.org/2000/svg", "svg") as SVGSVGElement
        svg.setAttribute("viewBox", "0 0 36 36")
        svg.setAttribute("width", "100%")
        svg.setAttribute("height", "100%")

        val circleBg = document.createElementNS("http://www.w3.org/2000/svg", "circle") as SVGCircleElement
        circleBg.setAttribute("cx", "18")
        circleBg.setAttribute("cy", "18")
        circleBg.setAttribute("r", "16")
        circleBg.classList.add("circle-progress-background")
        circleBg.setAttribute("stroke-width", "3")
        circleBg.setAttribute("fill", "none")

        circle = document.createElementNS("http://www.w3.org/2000/svg", "circle") as SVGCircleElement
        circle.classList.add("circle-progress")
        circle.setAttribute("cx", "18")
        circle.setAttribute("cy", "18")
        circle.setAttribute("r", "16")
        circle.setAttribute("fill", "none")
        circle.setAttribute("stroke-linecap", "round")
        circle.setAttribute("transform", "rotate(-90 18 18)")

        circumference = 2 * PI * 16
        circle.setAttribute("stroke-dasharray", circumference.toString())
        circle.setAttribute("stroke-dashoffset", circumference.toString())

        svg.appendChild(circleBg)
        svg.appendChild(circle)
        native.onElement {
            it.appendChild(svg)
        }
    }

    actual var ratio: Float
        get() {
            val offset = circle.getAttribute("stroke-dashoffset")?.toDoubleOrNull() ?: circumference
            return ((circumference - offset) / circumference).toFloat()
        }
        set(value) {
            val p = value.coerceIn(0f, 1f)
            val offset = circumference * (1 - p)
            circle.setAttribute("stroke-dashoffset", offset.toString())
        }
}