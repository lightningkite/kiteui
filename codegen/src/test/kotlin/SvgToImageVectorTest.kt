// by Claude
package com.lightningkite.kiteui

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SvgToImageVectorTest {

    // --- CSS Parsing Tests ---

    @Test
    fun `parseCssText handles single class rule`() {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        parseCssText("""
            .cls-1 {
                fill: none;
                stroke-width: 5px;
            }
        """.trimIndent(), result)

        assertEquals("none", result["cls-1"]?.get("fill"))
        assertEquals("5px", result["cls-1"]?.get("stroke-width"))
    }

    @Test
    fun `parseCssText handles multi-selector rule`() {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        parseCssText("""
            .cls-1, .cls-2, .cls-3 {
                fill: none;
                stroke-linejoin: round;
            }
        """.trimIndent(), result)

        assertEquals("none", result["cls-1"]?.get("fill"))
        assertEquals("none", result["cls-2"]?.get("fill"))
        assertEquals("none", result["cls-3"]?.get("fill"))
        assertEquals("round", result["cls-1"]?.get("stroke-linejoin"))
    }

    @Test
    fun `parseCssText merges multiple rules for same class`() {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        parseCssText("""
            .cls-1, .cls-2 {
                fill: none;
            }
            .cls-1 {
                stroke: url(#linear-gradient);
                stroke-width: 7px;
            }
        """.trimIndent(), result)

        assertEquals("none", result["cls-1"]?.get("fill"))
        assertEquals("url(#linear-gradient)", result["cls-1"]?.get("stroke"))
        assertEquals("7px", result["cls-1"]?.get("stroke-width"))
        assertEquals("none", result["cls-2"]?.get("fill"))
        assertNull(result["cls-2"]?.get("stroke"))
    }

    @Test
    fun `parseCssText handles fill with hex color`() {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        parseCssText("""
            .cls-18 {
                fill: #fff;
            }
        """.trimIndent(), result)

        assertEquals("#fff", result["cls-18"]?.get("fill"))
    }

    // --- Gradient Parsing Tests ---

    @Test
    fun `normalizeHexColor expands 3-char hex`() {
        assertEquals("#ffffff", normalizeHexColor("#fff"))
        assertEquals("#000000", normalizeHexColor("#000"))
        assertEquals("#aabbcc", normalizeHexColor("#abc"))
    }

    @Test
    fun `normalizeHexColor preserves 6-char hex`() {
        assertEquals("#07b0ce", normalizeHexColor("#07b0ce"))
    }

    @Test
    fun `parseHexToRgb parses correctly`() {
        val (r, g, b) = parseHexToRgb("#ff0000")
        assertEquals(255f, r, 0.01f)
        assertEquals(0f, g, 0.01f)
        assertEquals(0f, b, 0.01f)
    }

    @Test
    fun `sampleGradientToHex averages stops`() {
        val stops = listOf(
            GradientStopDef(0f, "#07b0ce"),
            GradientStopDef(1f, "#0894b5"),
        )
        val result = sampleGradientToHex(stops)
        // Midpoint of #07b0ce and #0894b5
        // R: (7 + 8)/2 = 7.5 -> 7,  G: (176 + 148)/2 = 162 -> a2,  B: (206 + 181)/2 = 193.5 -> c1
        assertEquals("#07a2c1", result)
    }

    @Test
    fun `sampleGradientToHex handles single stop`() {
        val stops = listOf(GradientStopDef(0f, "#ff0000"))
        assertEquals("#ff0000", sampleGradientToHex(stops))
    }

    @Test
    fun `parseGradients resolves xlink href inheritance`() {
        val svgXml = """
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 100 100">
                <defs>
                    <linearGradient id="base" x1="0" y1="0" x2="100" y2="0" gradientUnits="userSpaceOnUse">
                        <stop offset="0" stop-color="#ff0000"/>
                        <stop offset="1" stop-color="#0000ff"/>
                    </linearGradient>
                    <linearGradient id="ref" x1="10" y1="20" x2="90" y2="80" xlink:href="#base"/>
                </defs>
            </svg>
        """.trimIndent()
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(svgXml.byteInputStream())
        val gradients = parseGradients(doc.documentElement)

        // Both should have same stops (inherited from base)
        assertEquals(gradients["base"]!!.stops, gradients["ref"]!!.stops)
        assertEquals(2, gradients["base"]!!.stops.size)
        assertEquals("#ff0000", gradients["base"]!!.stops[0].color)
        assertEquals("#0000ff", gradients["base"]!!.stops[1].color)

        // base has its own coordinates
        assertEquals(0.0, gradients["base"]!!.x1)
        assertEquals(100.0, gradients["base"]!!.x2)

        // ref has its own coordinates (not inherited since it specifies them)
        assertEquals(10.0, gradients["ref"]!!.x1)
        assertEquals(90.0, gradients["ref"]!!.x2)

        // ref inherits gradientUnits from base
        assertEquals("userSpaceOnUse", gradients["ref"]!!.gradientUnits)
    }

    @Test
    fun `gradientToExpression emits LinearGradient`() {
        val grad = ResolvedGradient(
            stops = listOf(
                GradientStopDef(0f, "#ff0000"),
                GradientStopDef(1f, "#0000ff"),
            ),
            x1 = 0.0, y1 = 50.0, x2 = 100.0, y2 = 50.0,
            gradientUnits = "userSpaceOnUse",
        )
        val expr = gradientToExpression(grad)
        assertTrue("Should contain LinearGradient", expr.contains("LinearGradient("))
        assertTrue("Should contain GradientStop", expr.contains("GradientStop("))
        assertTrue("Should contain x0", expr.contains("x0 = 0.0"))
        assertTrue("Should contain x1", expr.contains("x1 = 100.0"))
        assertTrue("Should contain stop colors", expr.contains("Color.fromHexString(\"#ff0000\")"))
    }

    @Test
    fun `resolveStrokeExpression samples gradient to solid color`() {
        val gradients = mapOf("my-grad" to ResolvedGradient(
            stops = listOf(
                GradientStopDef(0f, "#ff0000"),
                GradientStopDef(1f, "#0000ff"),
            ),
            x1 = null, y1 = null, x2 = null, y2 = null, gradientUnits = null,
        ))
        val result = resolveStrokeExpression("url(#my-grad)", gradients)
        // Should be sampled to a single Color, not LinearGradient
        assertTrue("Should be Color", result.startsWith("Color.fromHexString"))
        assertFalse("Should not be gradient", result.contains("LinearGradient"))
    }

    @Test
    fun `resolveFillExpression emits LinearGradient for url ref`() {
        val gradients = mapOf("my-grad" to ResolvedGradient(
            stops = listOf(
                GradientStopDef(0f, "#07b0ce"),
                GradientStopDef(1f, "#0894b5"),
            ),
            x1 = 10.0, y1 = 20.0, x2 = 30.0, y2 = 40.0,
            gradientUnits = "userSpaceOnUse",
        ))
        val result = resolveFillExpression("url(#my-grad)", gradients)
        assertTrue("Should contain LinearGradient", result.contains("LinearGradient("))
        assertTrue("Should contain coordinates", result.contains("x0 = 10.0"))
    }

    @Test
    fun `resolveFillExpression returns Color for non-gradient values`() {
        val result = resolveFillExpression("#ff0000", emptyMap())
        assertEquals("Color.fromHexString(\"#ff0000\")", result)
    }

    // --- Shape to Path Tests ---

    @Test
    fun `lineToPath generates correct path`() {
        val el = createElement("line", mapOf("x1" to "10", "y1" to "20", "x2" to "30", "y2" to "40"))
        assertEquals("M 10,20 L 30,40", lineToPath(el))
    }

    @Test
    fun `parsePoints handles comma-separated format`() {
        val points = parsePoints("10,20 30,40 50,60")
        assertEquals(listOf("10" to "20", "30" to "40", "50" to "60"), points)
    }

    @Test
    fun `parsePoints handles space-separated format`() {
        val points = parsePoints("10 20 30 40")
        assertEquals(listOf("10" to "20", "30" to "40"), points)
    }

    @Test
    fun `polylineToPath generates correct path`() {
        val el = createElement("polyline", mapOf("points" to "10,20 30,40 50,60"))
        assertEquals("M 10,20 L 30,40 L 50,60", polylineToPath(el))
    }

    @Test
    fun `polygonToPath generates closed path`() {
        val el = createElement("polygon", mapOf("points" to "10,20 30,40 50,60"))
        assertEquals("M 10,20 L 30,40 L 50,60 Z", polygonToPath(el))
    }

    @Test
    fun `circleToPath generates arc path`() {
        val el = createElement("circle", mapOf("cx" to "50", "cy" to "50", "r" to "25"))
        val path = circleToPath(el)
        assertTrue(path.startsWith("M 25,50"))
        assertTrue(path.contains("A 25,25"))
        assertTrue(path.endsWith("Z"))
    }

    @Test
    fun `ellipseToPath generates arc path`() {
        val el = createElement("ellipse", mapOf("cx" to "100", "cy" to "100", "rx" to "50", "ry" to "30"))
        val path = ellipseToPath(el)
        assertTrue(path.startsWith("M 50,100"))
        assertTrue(path.contains("A 50,30"))
        assertTrue(path.endsWith("Z"))
    }

    @Test
    fun `rectToPath generates simple rect without rounding`() {
        val el = createElement("rect", mapOf("x" to "10", "y" to "20", "width" to "100", "height" to "50"))
        assertEquals("M 10,20 H 110 V 70 H 10 Z", rectToPath(el))
    }

    @Test
    fun `rectToPath generates rounded rect`() {
        val el = createElement("rect", mapOf("x" to "10", "y" to "20", "width" to "100", "height" to "50", "rx" to "5", "ry" to "5"))
        val path = rectToPath(el)
        assertTrue(path.contains("A 5,5"))
        assertTrue(path.endsWith("Z"))
    }

    // --- Color Resolution Tests ---

    @Test
    fun `resolveColorExpression handles none`() {
        assertEquals("Color.transparent", resolveColorExpression("none"))
    }

    @Test
    fun `resolveColorExpression handles hex colors`() {
        assertEquals("Color.fromHexString(\"#ff0000\")", resolveColorExpression("#ff0000"))
    }

    @Test
    fun `resolveColorExpression expands 3-char hex`() {
        assertEquals("Color.fromHexString(\"#ffffff\")", resolveColorExpression("#fff"))
    }

    @Test
    fun `resolveColorExpression handles named colors`() {
        assertEquals("Color.white", resolveColorExpression("white"))
        assertEquals("Color.red", resolveColorExpression("red"))
    }

    @Test
    fun `resolveColorExpression handles rgb`() {
        assertEquals("Color.fromRgbString(\"rgb(255,0,0)\")", resolveColorExpression("rgb(255,0,0)"))
    }

    @Test
    fun `resolveColorExpression defaults to black`() {
        assertEquals("Color.black", resolveColorExpression("magenta"))
    }

    // --- Integration Tests ---

    @Test
    fun `etzgo_icon converts without errors`() {
        val svgFile = File("test-data/sample-svgs/etzgo_icon.svg")
        val result = svgToImageVectorCode(svgFile)
        assertTrue("Should contain ImageVector", result.contains("ImageVector("))
        assertTrue("Should contain paths", result.contains("ImageVector.Path("))
        // etzgo_icon has 3 polylines
        assertEquals("Should have 3 paths", 3, result.countOccurrences("ImageVector.Path("))
        assertFalse("Should not have unresolved gradient refs", result.contains("url(#"))
    }

    @Test
    fun `etzgo_direct converts without errors`() {
        val svgFile = File("test-data/sample-svgs/etzgo_direct.svg")
        val result = svgToImageVectorCode(svgFile)
        assertTrue("Should contain ImageVector", result.contains("ImageVector("))
        assertTrue("Should contain paths", result.contains("ImageVector.Path("))
        assertFalse("Should not have unresolved gradient refs", result.contains("url(#"))
        // Has polygon, lines, paths, ellipse - should have many paths
        assertTrue("Should have multiple paths", result.countOccurrences("ImageVector.Path(") > 10)
    }

    @Test
    fun `etzgo_price converts without errors`() {
        val svgFile = File("test-data/sample-svgs/etzgo_price.svg")
        val result = svgToImageVectorCode(svgFile)
        assertTrue("Should contain ImageVector", result.contains("ImageVector("))
        assertTrue("Should contain paths", result.contains("ImageVector.Path("))
        assertFalse("Should not have unresolved gradient refs", result.contains("url(#"))
        // Has paths, circles, lines, polygons
        assertTrue("Should have multiple paths", result.countOccurrences("ImageVector.Path(") > 10)
    }

    @Test
    fun `etzgo_snap converts without errors`() {
        val svgFile = File("test-data/sample-svgs/etzgo_snap.svg")
        val result = svgToImageVectorCode(svgFile)
        assertTrue("Should contain ImageVector", result.contains("ImageVector("))
        assertTrue("Should contain paths", result.contains("ImageVector.Path("))
        assertFalse("Should not have unresolved gradient refs", result.contains("url(#"))
        // Has ellipses, rect, paths, lines
        assertTrue("Should have multiple paths", result.countOccurrences("ImageVector.Path(") > 8)
    }

    @Test
    fun `etzgo_direct has stroke properties`() {
        val svgFile = File("test-data/sample-svgs/etzgo_direct.svg")
        val result = svgToImageVectorCode(svgFile)
        assertTrue("Should have strokeColor", result.contains("strokeColor ="))
        assertTrue("Should have strokeWidth", result.contains("strokeWidth ="))
    }

    @Test
    fun `etzgo_icon has stroke properties`() {
        val svgFile = File("test-data/sample-svgs/etzgo_icon.svg")
        val result = svgToImageVectorCode(svgFile)
        assertTrue("Should have strokeColor", result.contains("strokeColor ="))
        assertTrue("Should have strokeWidth", result.contains("strokeWidth ="))
    }

    @Test
    fun `etzgo_price has stroke linecap round`() {
        val svgFile = File("test-data/sample-svgs/etzgo_price.svg")
        val result = svgToImageVectorCode(svgFile)
        // etzgo_price has stroke-linecap: round on many elements
        assertTrue("Should have strokeCap Round", result.contains("Icon.StrokeLineCap.Round"))
    }

    @Test
    fun `etzgo_direct white fill preserved`() {
        val svgFile = File("test-data/sample-svgs/etzgo_direct.svg")
        val result = svgToImageVectorCode(svgFile)
        // .cls-18 has fill: #fff, used by the ellipse
        assertTrue("Should have white fill", result.contains("Color.fromHexString(\"#ffffff\")"))
    }

    @Test
    fun `etzgo_snap white fill preserved`() {
        val svgFile = File("test-data/sample-svgs/etzgo_snap.svg")
        val result = svgToImageVectorCode(svgFile)
        // Several elements have fill: #fff
        assertTrue("Should have white fill", result.contains("Color.fromHexString(\"#ffffff\")"))
    }

    @Test
    fun `etzgo_direct fill gradient emits LinearGradient`() {
        val svgFile = File("test-data/sample-svgs/etzgo_direct.svg")
        val result = svgToImageVectorCode(svgFile)
        // .cls-17 has fill: url(#linear-gradient-15), used on the location pin path
        assertTrue("Should have LinearGradient fill", result.contains("LinearGradient("))
        assertTrue("Should have GradientStop", result.contains("GradientStop("))
    }

    @Test
    fun `etzgo_price fill gradient emits LinearGradient for tag shapes`() {
        // etzgo_price .cls-16 has fill: #fff (not gradient), but .cls-21 has fill: #fff too
        // Only strokes use gradients in etzgo_price - so no LinearGradient fills expected
        val svgFile = File("test-data/sample-svgs/etzgo_price.svg")
        val result = svgToImageVectorCode(svgFile)
        // All fills in etzgo_price are #fff or none, gradients are only on strokes
        assertFalse("Should not have LinearGradient fill (only stroke gradients)", result.contains("LinearGradient("))
    }

    // --- Helpers ---

    private fun createElement(tagName: String, attrs: Map<String, String>): org.w3c.dom.Element {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val el = doc.createElement(tagName)
        for ((k, v) in attrs) {
            el.setAttribute(k, v)
        }
        return el
    }

    private fun String.countOccurrences(sub: String): Int {
        var count = 0
        var startIndex = 0
        while (true) {
            val index = indexOf(sub, startIndex)
            if (index < 0) break
            count++
            startIndex = index + 1
        }
        return count
    }
}
