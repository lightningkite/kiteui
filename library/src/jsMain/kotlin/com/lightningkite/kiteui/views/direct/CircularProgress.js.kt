package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.FieldSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.svg.SVGCircleElement
import org.w3c.dom.svg.SVGSVGElement
import kotlin.math.PI

actual class CircularProgress actual constructor(context: RContext) : RView(context) {

    private val circle: SVGCircleElement
    private val circleBg: SVGCircleElement
    private val svg: SVGSVGElement
    private val circumference: Double
    private val defs: Element
    private var filterId: String? = null

    init {
        native.tag = "div"
        native.setStyleProperty("background-color", "transparent")

        svg = document.createElementNS("http://www.w3.org/2000/svg", "svg") as SVGSVGElement
        svg.setAttribute("viewBox", "0 0 36 36")
        svg.setAttribute("width", "100%")
        svg.setAttribute("height", "100%")
        svg.setAttribute("overflow", "visible")

        defs = document.createElementNS("http://www.w3.org/2000/svg", "defs")
        svg.appendChild(defs)

        circleBg = document.createElementNS("http://www.w3.org/2000/svg", "circle") as SVGCircleElement
        circleBg.setAttribute("cx", "18")
        circleBg.setAttribute("cy", "18")
        circleBg.setAttribute("r", "15.5")
        circleBg.setAttribute("stroke-width", "3")
        circleBg.setAttribute("fill", "none")
        circleBg.classList.add("circle-progress-background")

        circle = document.createElementNS("http://www.w3.org/2000/svg", "circle") as SVGCircleElement
        circle.classList.add("circle-progress")
        circle.setAttribute("cx", "18")
        circle.setAttribute("cy", "18")
        circle.setAttribute("r", "15.5")
        circle.setAttribute("fill", "none")
        circle.setAttribute("stroke-linecap", "round")
        circle.setAttribute("transform", "rotate(-90 18 18)")

        circumference = 2 * PI * 15.5
        circle.setAttribute("stroke-dasharray", circumference.toString())
        circle.setAttribute("stroke-dashoffset", circumference.toString())

        svg.appendChild(circleBg)
        svg.appendChild(circle)
        native.onElement {
            it.appendChild(svg)
        }
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        val t = theme.theme
        val fieldTheme = theme[FieldSemantic].theme
        val shadows = fieldTheme.shadows

        // Apply track color from field theme background
        circleBg.setAttribute("stroke", fieldTheme.background.closestColor().toWeb())

        // Apply progress color from foreground
        circle.setAttribute("stroke", t.foreground.closestColor().toWeb())

        // Apply inset shadow filter for neumorphic groove effect
        if (shadows != null && shadows.isNotEmpty()) {
            val insetShadows = shadows.filter { it.inset }
            if (insetShadows.isNotEmpty()) {
                applyInsetShadowFilter(insetShadows, fieldTheme.background.closestColor().toWeb())
            }
        } else {
            // Remove filter if no shadows
            filterId?.let {
                circleBg.removeAttribute("filter")
                filterId = null
            }
        }
    }

    private fun applyInsetShadowFilter(
        shadows: List<com.lightningkite.kiteui.models.Shadow>,
        bgColor: String
    ) {
        // Clear previous filter
        defs.innerHTML = ""

        val id = "circ-prog-shadow-${hashCode()}"
        filterId = id

        // Build an SVG filter that creates an inset shadow on the stroke
        // Technique: use the shape as a mask, blur an inverted version, composite inside
        val filter = document.createElementNS("http://www.w3.org/2000/svg", "filter")
        filter.setAttribute("id", id)
        filter.setAttribute("x", "-50%")
        filter.setAttribute("y", "-50%")
        filter.setAttribute("width", "200%")
        filter.setAttribute("height", "200%")

        var filterContent = ""

        // Start with the source graphic
        filterContent += """<feFlood flood-color="$bgColor" result="bg"/>"""
        filterContent += """<feComposite in="bg" in2="SourceGraphic" operator="in" result="base"/>"""

        // For each inset shadow, create the shadow effect
        // SVG viewBox is 36x36, so scale pixel values to SVG coordinate space
        val scaleFactor = 36.0 / 100.0  // approximate: assume ~100px rendered size
        shadows.forEachIndexed { index, shadow ->
            val dx = shadow.offsetX.value.roughPx * scaleFactor
            val dy = shadow.offsetY.value.roughPx * scaleFactor
            val blur = shadow.blurRadius.value.roughPx * scaleFactor / 2.0
            val color = shadow.color.toWeb()

            // Inset shadow technique:
            // 1. Take source alpha, invert it
            // 2. Offset and blur
            // 3. Clip to original shape
            filterContent += """
                <feComponentTransfer in="SourceAlpha" result="invert$index">
                    <feFuncA type="table" tableValues="1 0"/>
                </feComponentTransfer>
                <feOffset in="invert$index" dx="$dx" dy="$dy" result="offset$index"/>
                <feGaussianBlur in="offset$index" stdDeviation="$blur" result="blur$index"/>
                <feFlood flood-color="$color" result="color$index"/>
                <feComposite in="color$index" in2="blur$index" operator="in" result="shadow$index"/>
                <feComposite in="shadow$index" in2="SourceGraphic" operator="in" result="clipped$index"/>
            """.trimIndent()
        }

        // Merge all layers: base + all shadows
        filterContent += """<feMerge>"""
        filterContent += """<feMergeNode in="base"/>"""
        shadows.indices.forEach { index ->
            filterContent += """<feMergeNode in="clipped$index"/>"""
        }
        filterContent += """</feMerge>"""

        filter.innerHTML = filterContent
        defs.appendChild(filter)
        circleBg.setAttribute("filter", "url(#$id)")
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
