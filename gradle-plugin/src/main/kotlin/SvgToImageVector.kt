// by Claude
package com.lightningkite.kiteui

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Converts an SVG file to Kotlin source code that constructs an `ImageVector`.
 *
 * Handles CSS `<style>` class resolution, `<linearGradient>` with `xlink:href` inheritance,
 * shape elements (`path`, `line`, `polyline`, `polygon`, `circle`, `ellipse`, `rect`),
 * `<g>` group traversal, and stroke attributes.
 *
 * Gradients are sampled to solid colors (weighted average of stops).
 */
fun svgToImageVectorCode(svgFile: File): String {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }
    val doc = factory.newDocumentBuilder().parse(svgFile.inputStream())
    val svg = doc.documentElement

    val width = svg.getAttribute("width")
    val height = svg.getAttribute("height")
    val viewBox = svg.getAttribute("viewBox")
    val vbParts = viewBox.split(Regex("[\\s,]+")).map { it.toFloatOrNull() ?: 0f }
    val minX = vbParts.getOrElse(0) { 0f }.toInt()
    val minY = vbParts.getOrElse(1) { 0f }.toInt()
    val vbWidth = vbParts.getOrElse(2) { 24f }.toInt()
    val vbHeight = vbParts.getOrElse(3) { 24f }.toInt()

    val cssStyles = parseCssStyles(svg)
    val gradients = parseGradients(svg)

    val paths = mutableListOf<String>()
    collectPaths(svg, cssStyles, gradients, paths)

    return buildString {
        appendLine("ImageVector(")
        appendLine("        width = ${width.withDimensionExtension()},")
        appendLine("        height = ${height.withDimensionExtension()},")
        appendLine("        viewBoxMinX = $minX,")
        appendLine("        viewBoxMinY = $minY,")
        appendLine("        viewBoxWidth = $vbWidth,")
        appendLine("        viewBoxHeight = $vbHeight,")
        appendLine("        paths = listOf(")
        for (p in paths) {
            appendLine(p)
        }
        appendLine("        )")
        appendLine("    )")
    }
}

// --- CSS Parsing ---

/**
 * Parses `<style>` blocks inside `<defs>` and returns a map of class name to CSS properties.
 * Handles multi-selector rules like `.cls-1, .cls-2 { fill: none; }`.
 */
internal fun parseCssStyles(root: Element): Map<String, Map<String, String>> {
    val result = mutableMapOf<String, MutableMap<String, String>>()
    val styleElements = root.getElementsByTagName("style")
    for (i in 0 until styleElements.length) {
        val styleText = styleElements.item(i).textContent ?: continue
        parseCssText(styleText, result)
    }
    return result
}

internal fun parseCssText(css: String, result: MutableMap<String, MutableMap<String, String>>) {
    // Match rule blocks: selectors { declarations }
    val rulePattern = Regex("""([^{}]+)\{([^}]*)\}""")
    for (match in rulePattern.findAll(css)) {
        val selectorPart = match.groupValues[1].trim()
        val declarationPart = match.groupValues[2].trim()

        val properties = mutableMapOf<String, String>()
        for (decl in declarationPart.split(";")) {
            val trimmed = decl.trim()
            if (trimmed.isEmpty()) continue
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex < 0) continue
            val prop = trimmed.substring(0, colonIndex).trim()
            val value = trimmed.substring(colonIndex + 1).trim()
            properties[prop] = value
        }
        if (properties.isEmpty()) continue

        // Split selectors by comma, handle `.cls-1, .cls-2`
        for (selector in selectorPart.split(",")) {
            val trimmedSelector = selector.trim()
            if (trimmedSelector.startsWith(".")) {
                val className = trimmedSelector.removePrefix(".")
                result.getOrPut(className) { mutableMapOf() }.putAll(properties)
            }
        }
    }
}

// --- Gradient Parsing ---

internal data class GradientDef(
    val id: String,
    val stops: List<GradientStopDef>,
    val href: String? = null, // xlink:href reference (without #)
    val x1: Double? = null,
    val y1: Double? = null,
    val x2: Double? = null,
    val y2: Double? = null,
    val gradientUnits: String? = null, // "userSpaceOnUse" or "objectBoundingBox"
)

internal data class GradientStopDef(
    val offset: Float,
    val color: String, // raw color string
    val opacity: Float = 1f,
)

/** Fully resolved gradient with stops and coordinates inherited from xlink:href chain. */
internal data class ResolvedGradient(
    val stops: List<GradientStopDef>,
    val x1: Double?,
    val y1: Double?,
    val x2: Double?,
    val y2: Double?,
    val gradientUnits: String?,
)

/**
 * Parses all `<linearGradient>` elements, resolves `xlink:href` inheritance chains,
 * and returns a map of gradient ID to fully resolved gradient data.
 */
internal fun parseGradients(root: Element): Map<String, ResolvedGradient> {
    val rawGradients = mutableMapOf<String, GradientDef>()

    val gradientElements = root.getElementsByTagName("linearGradient")
    for (i in 0 until gradientElements.length) {
        val el = gradientElements.item(i) as? Element ?: continue
        val id = el.getAttribute("id").takeIf { it.isNotEmpty() } ?: continue

        // xlink:href attribute (namespace-aware)
        val href = el.getAttributeNS("http://www.w3.org/1999/xlink", "href")
            .takeIf { it.isNotEmpty() }
            ?.removePrefix("#")

        val stops = mutableListOf<GradientStopDef>()
        val stopElements = el.getElementsByTagName("stop")
        for (j in 0 until stopElements.length) {
            val stopEl = stopElements.item(j) as? Element ?: continue
            val offset = stopEl.getAttribute("offset").toFloatOrNull() ?: 0f
            val stopColor = stopEl.getAttribute("stop-color").takeIf { it.isNotEmpty() } ?: "#000000"
            val stopOpacity = stopEl.getAttribute("stop-opacity").toFloatOrNull() ?: 1f
            stops.add(GradientStopDef(offset, stopColor, stopOpacity))
        }

        rawGradients[id] = GradientDef(
            id = id,
            stops = stops,
            href = href,
            x1 = el.getAttribute("x1").toDoubleOrNull(),
            y1 = el.getAttribute("y1").toDoubleOrNull(),
            x2 = el.getAttribute("x2").toDoubleOrNull(),
            y2 = el.getAttribute("y2").toDoubleOrNull(),
            gradientUnits = el.getAttribute("gradientUnits").takeIf { it.isNotEmpty() },
        )
    }

    // Resolve xlink:href chains: inherit stops, coordinates, and units from referenced gradient
    fun resolve(id: String, visited: MutableSet<String> = mutableSetOf()): ResolvedGradient? {
        if (id in visited) return null // prevent cycles
        visited.add(id)
        val grad = rawGradients[id] ?: return null
        val parent = grad.href?.let { resolve(it, visited) }
        return ResolvedGradient(
            stops = grad.stops.ifEmpty { parent?.stops ?: emptyList() },
            x1 = grad.x1 ?: parent?.x1,
            y1 = grad.y1 ?: parent?.y1,
            x2 = grad.x2 ?: parent?.x2,
            y2 = grad.y2 ?: parent?.y2,
            gradientUnits = grad.gradientUnits ?: parent?.gradientUnits,
        )
    }

    val resolved = mutableMapOf<String, ResolvedGradient>()
    for ((id, _) in rawGradients) {
        resolve(id)?.let { resolved[id] = it }
    }
    return resolved
}

/**
 * Samples gradient stops to a single hex color using weighted averaging.
 */
internal fun sampleGradientToHex(stops: List<GradientStopDef>): String {
    if (stops.isEmpty()) return "#000000"
    if (stops.size == 1) return normalizeHexColor(stops[0].color)

    // Weighted average across consecutive stop pairs
    var r = 0f; var g = 0f; var b = 0f
    for (i in 0 until stops.size - 1) {
        val s0 = stops[i]
        val s1 = stops[i + 1]
        val weight = s1.offset - s0.offset
        val c0 = parseHexToRgb(normalizeHexColor(s0.color))
        val c1 = parseHexToRgb(normalizeHexColor(s1.color))
        r += weight * (c0.first + c1.first) / 2f
        g += weight * (c0.second + c1.second) / 2f
        b += weight * (c0.third + c1.third) / 2f
    }

    fun clamp(v: Float) = v.coerceIn(0f, 255f).toInt()
    return "#${clamp(r).toString(16).padStart(2, '0')}${clamp(g).toString(16).padStart(2, '0')}${clamp(b).toString(16).padStart(2, '0')}"
}

internal fun normalizeHexColor(color: String): String {
    val hex = color.removePrefix("#")
    return if (hex.length == 3) {
        "#${hex[0]}${hex[0]}${hex[1]}${hex[1]}${hex[2]}${hex[2]}"
    } else {
        "#$hex"
    }
}

internal fun parseHexToRgb(hex: String): Triple<Float, Float, Float> {
    val h = hex.removePrefix("#")
    val value = h.toIntOrNull(16) ?: return Triple(0f, 0f, 0f)
    return Triple(
        ((value shr 16) and 0xFF).toFloat(),
        ((value shr 8) and 0xFF).toFloat(),
        (value and 0xFF).toFloat(),
    )
}

// --- Style Resolution ---

/**
 * Merges CSS class styles and inline attributes for an element.
 * Inline attributes take precedence.
 */
internal fun resolveElementStyle(element: Element, cssStyles: Map<String, Map<String, String>>): Map<String, String> {
    val merged = mutableMapOf<String, String>()

    // Apply CSS class styles
    val classAttr = element.getAttribute("class")
    if (classAttr.isNotEmpty()) {
        for (cls in classAttr.split(Regex("\\s+"))) {
            cssStyles[cls]?.let { merged.putAll(it) }
        }
    }

    // Inline style attribute overrides
    val styleAttr = element.getAttribute("style")
    if (styleAttr.isNotEmpty()) {
        for (decl in styleAttr.split(";")) {
            val trimmed = decl.trim()
            if (trimmed.isEmpty()) continue
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex < 0) continue
            merged[trimmed.substring(0, colonIndex).trim()] = trimmed.substring(colonIndex + 1).trim()
        }
    }

    // Direct XML attributes (fill, stroke, etc.) also override
    for (attr in listOf("fill", "stroke", "stroke-width", "stroke-linecap")) {
        val value = element.getAttribute(attr)
        if (value.isNotEmpty()) {
            merged[attr] = value
        }
    }

    return merged
}

// --- Shape to Path Conversion ---

/** Formats a Double to a clean string, removing trailing zeros and unnecessary decimal points. */
private fun Double.fmt(): String {
    // Round to 2 decimal places to avoid floating-point noise like 237.64000000000001
    val rounded = kotlin.math.round(this * 100.0) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
    else rounded.toBigDecimal().stripTrailingZeros().toPlainString()
}

internal fun lineToPath(element: Element): String {
    val x1 = element.getAttribute("x1")
    val y1 = element.getAttribute("y1")
    val x2 = element.getAttribute("x2")
    val y2 = element.getAttribute("y2")
    return "M $x1,$y1 L $x2,$y2"
}

internal fun parsePoints(pointsAttr: String): List<Pair<String, String>> {
    // Handle both "x1,y1 x2,y2" and "x1 y1 x2 y2" formats
    val tokens = pointsAttr.trim().split(Regex("[\\s,]+"))
    val points = mutableListOf<Pair<String, String>>()
    var i = 0
    while (i + 1 < tokens.size) {
        points.add(tokens[i] to tokens[i + 1])
        i += 2
    }
    return points
}

internal fun polylineToPath(element: Element): String {
    val points = parsePoints(element.getAttribute("points"))
    if (points.isEmpty()) return ""
    return buildString {
        append("M ${points[0].first},${points[0].second}")
        for (i in 1 until points.size) {
            append(" L ${points[i].first},${points[i].second}")
        }
    }
}

internal fun polygonToPath(element: Element): String {
    val path = polylineToPath(element)
    return if (path.isNotEmpty()) "$path Z" else ""
}

internal fun circleToPath(element: Element): String {
    val cx = element.getAttribute("cx").toDoubleOrNull() ?: 0.0
    val cy = element.getAttribute("cy").toDoubleOrNull() ?: 0.0
    val r = element.getAttribute("r").toDoubleOrNull() ?: 0.0
    return "M ${(cx - r).fmt()},${cy.fmt()} A ${r.fmt()},${r.fmt()} 0 1,0 ${(cx + r).fmt()},${cy.fmt()} A ${r.fmt()},${r.fmt()} 0 1,0 ${(cx - r).fmt()},${cy.fmt()} Z"
}

internal fun ellipseToPath(element: Element): String {
    val cx = element.getAttribute("cx").toDoubleOrNull() ?: 0.0
    val cy = element.getAttribute("cy").toDoubleOrNull() ?: 0.0
    val rx = element.getAttribute("rx").toDoubleOrNull() ?: 0.0
    val ry = element.getAttribute("ry").toDoubleOrNull() ?: 0.0
    return "M ${(cx - rx).fmt()},${cy.fmt()} A ${rx.fmt()},${ry.fmt()} 0 1,0 ${(cx + rx).fmt()},${cy.fmt()} A ${rx.fmt()},${ry.fmt()} 0 1,0 ${(cx - rx).fmt()},${cy.fmt()} Z"
}

internal fun rectToPath(element: Element): String {
    val x = element.getAttribute("x").toDoubleOrNull() ?: 0.0
    val y = element.getAttribute("y").toDoubleOrNull() ?: 0.0
    val w = element.getAttribute("width").toDoubleOrNull() ?: 0.0
    val h = element.getAttribute("height").toDoubleOrNull() ?: 0.0
    val rx = element.getAttribute("rx").toDoubleOrNull() ?: 0.0
    val ry = element.getAttribute("ry").toDoubleOrNull()?.takeIf { it > 0.0 } ?: rx

    return if (rx > 0.0 || ry > 0.0) {
        // Rounded rectangle
        val arcRx = rx.coerceAtMost(w / 2)
        val arcRy = ry.coerceAtMost(h / 2)
        buildString {
            append("M ${(x + arcRx).fmt()},${y.fmt()}")
            append(" H ${(x + w - arcRx).fmt()}")
            append(" A ${arcRx.fmt()},${arcRy.fmt()} 0 0,1 ${(x + w).fmt()},${(y + arcRy).fmt()}")
            append(" V ${(y + h - arcRy).fmt()}")
            append(" A ${arcRx.fmt()},${arcRy.fmt()} 0 0,1 ${(x + w - arcRx).fmt()},${(y + h).fmt()}")
            append(" H ${(x + arcRx).fmt()}")
            append(" A ${arcRx.fmt()},${arcRy.fmt()} 0 0,1 ${x.fmt()},${(y + h - arcRy).fmt()}")
            append(" V ${(y + arcRy).fmt()}")
            append(" A ${arcRx.fmt()},${arcRy.fmt()} 0 0,1 ${(x + arcRx).fmt()},${y.fmt()}")
            append(" Z")
        }
    } else {
        "M ${x.fmt()},${y.fmt()} H ${(x + w).fmt()} V ${(y + h).fmt()} H ${x.fmt()} Z"
    }
}

// --- Color Resolution ---

private val urlPattern = Regex("""url\(#([^)]+)\)""")

private val NAMED_COLORS = setOf(
    "white", "gray", "black", "red", "orange", "yellow", "green", "teal", "blue", "purple", "transparent"
)

/** Resolves a color string to a Kotlin `Color` expression (no gradients). */
internal fun resolveColorExpression(value: String): String {
    val trimmed = value.trim()
    return when {
        trimmed.equals("none", ignoreCase = true) -> "Color.transparent"
        trimmed.startsWith("#") -> {
            val normalized = normalizeHexColor(trimmed)
            "Color.fromHexString(\"$normalized\")"
        }
        trimmed.startsWith("rgb") -> "Color.fromRgbString(\"$trimmed\")"
        trimmed in NAMED_COLORS -> "Color.$trimmed"
        else -> "Color.black"
    }
}

/**
 * Resolves a CSS fill value to a Kotlin `Paint` expression.
 * Gradient `url(#id)` references emit `LinearGradient(...)` with full stop/coordinate data.
 */
internal fun resolveFillExpression(value: String, gradients: Map<String, ResolvedGradient>): String {
    val trimmed = value.trim()
    if (!trimmed.startsWith("url(")) return resolveColorExpression(trimmed)

    val id = urlPattern.find(trimmed)?.groupValues?.get(1) ?: return "Color.black"
    val grad = gradients[id] ?: return "Color.black"
    return gradientToExpression(grad)
}

/**
 * Resolves a CSS stroke value to a Kotlin `Color` expression.
 * Gradient references are sampled to solid color since `strokeColor` is `Color?`.
 */
internal fun resolveStrokeExpression(value: String, gradients: Map<String, ResolvedGradient>): String {
    val trimmed = value.trim()
    if (!trimmed.startsWith("url(")) return resolveColorExpression(trimmed)

    val id = urlPattern.find(trimmed)?.groupValues?.get(1) ?: return "Color.black"
    val grad = gradients[id] ?: return "Color.black"
    val hex = sampleGradientToHex(grad.stops)
    return "Color.fromHexString(\"$hex\")"
}

/** Generates a Kotlin `LinearGradient(...)` expression from resolved gradient data. */
internal fun gradientToExpression(grad: ResolvedGradient): String {
    if (grad.stops.isEmpty()) return "Color.black"

    return buildString {
        append("LinearGradient(stops = listOf(")
        append(grad.stops.joinToString(", ") { stop ->
            val colorExpr = resolveColorExpression(stop.color)
            val alphaExpr = if (stop.opacity < 1f) ".applyAlpha(${stop.opacity}f)" else ""
            "GradientStop(${stop.offset}f, $colorExpr$alphaExpr)"
        })
        append(")")
        // Emit absolute coordinates if available (userSpaceOnUse)
        if (grad.x1 != null && grad.y1 != null && grad.x2 != null && grad.y2 != null) {
            append(", x0 = ${grad.x1}, y0 = ${grad.y1}, x1 = ${grad.x2}, y1 = ${grad.y2}")
        }
        append(")")
    }
}

// --- Element Processing ---

private val SKIP_ELEMENTS = setOf("defs", "title", "desc", "metadata")
private val SHAPE_ELEMENTS = setOf("path", "line", "polyline", "polygon", "circle", "ellipse", "rect")

internal fun collectPaths(
    parent: Element,
    cssStyles: Map<String, Map<String, String>>,
    gradients: Map<String, ResolvedGradient>,
    out: MutableList<String>,
) {
    val children = parent.childNodes
    for (i in 0 until children.length) {
        val node = children.item(i) as? Element ?: continue
        val tagName = node.localName ?: node.tagName

        if (tagName in SKIP_ELEMENTS) continue

        if (tagName == "g") {
            collectPaths(node, cssStyles, gradients, out)
            continue
        }

        if (tagName !in SHAPE_ELEMENTS) continue

        val pathData = when (tagName) {
            "path" -> node.getAttribute("d")
            "line" -> lineToPath(node)
            "polyline" -> polylineToPath(node)
            "polygon" -> polygonToPath(node)
            "circle" -> circleToPath(node)
            "ellipse" -> ellipseToPath(node)
            "rect" -> rectToPath(node)
            else -> continue
        }

        if (pathData.isBlank()) continue

        val style = resolveElementStyle(node, cssStyles)

        val fill = style["fill"]
        val stroke = style["stroke"]
        val strokeWidth = style["stroke-width"]?.removeSuffix("px")
        val strokeLinecap = style["stroke-linecap"]

        out.add(buildString {
            append("            ImageVector.Path(")
            val parts = mutableListOf<String>()

            // Fill color (supports LinearGradient for url() references)
            if (fill != null) {
                parts.add("fillColor = ${resolveFillExpression(fill, gradients)}")
            }

            // Stroke color (sampled to Color since strokeColor is Color?)
            if (stroke != null && !stroke.equals("none", ignoreCase = true)) {
                parts.add("strokeColor = ${resolveStrokeExpression(stroke, gradients)}")
            }

            // Stroke width
            if (strokeWidth != null) {
                val sw = strokeWidth.toDoubleOrNull()
                if (sw != null && sw > 0.0) {
                    parts.add("strokeWidth = $sw")
                }
            }

            // Stroke line cap
            if (strokeLinecap != null) {
                val capExpr = when (strokeLinecap.lowercase()) {
                    "round" -> "Icon.StrokeLineCap.Round"
                    "square" -> "Icon.StrokeLineCap.Square"
                    "butt" -> "Icon.StrokeLineCap.Butt"
                    else -> null
                }
                if (capExpr != null) {
                    parts.add("strokeCap = $capExpr")
                }
            }

            // Path data (always last)
            parts.add("path = \"${pathData.escapeForKotlinString()}\"")

            append(parts.joinToString(", "))
            append("),")
        })
    }
}

private fun String.escapeForKotlinString(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "")

private fun String.withDimensionExtension(): String {
    return if (this.isBlank()) "24.dp"
    else this.replace("px", ".dp").replace("rem", ".rem").let {
        if (it.contains(".")) it
        else it.plus(".dp")
    }
}
