package com.lightningkite.kiteui.paint

import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.HSLColor
import com.lightningkite.kiteui.models.HSPColor
import com.lightningkite.kiteui.models.HSVColor
import kotlin.math.abs
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import kotlin.math.sqrt

class ColorConversionTest {

    private fun assertColorClose(
        expected: Color,
        actual: Color,
        tolerance: Float = 0.01f
    ) {
        assertTrue(abs(expected.red - actual.red) <= tolerance, "Red differs: $expected vs $actual")
        assertTrue(abs(expected.green - actual.green) <= tolerance)
        assertTrue(abs(expected.blue - actual.blue) <= tolerance)
        assertTrue(abs(expected.alpha - actual.alpha) <= tolerance)
    }

    // ---------- RGB & HSV ----------

    @Test
    fun rgb_toHSV_toRGB_roundTrip_red() = runTest {
        val original = Color.red
        val hsv = original.toHSV()
        val result = hsv.toRGB()
        assertColorClose(original, result)
    }

    @Test
    fun rgb_toHSV_toRGB_roundTrip_random() = runTest {
        val original = Color(1f, 0.23f, 0.67f, 0.11f)
        val result = original.toHSV().toRGB()
        assertColorClose(original, result)
    }


    // ---------- RGB & HSL ----------

    @Test
    fun rgb_toHSL_toRGB_roundTrip_red() = runTest {
        val original = Color.red
        val result = HSLColor.fromRGB(original).toRGB()
        assertColorClose(original, result)
    }

    @Test
    fun rgb_toHSL_toRGB_roundTrip_random() = runTest {
        val original = Color(1f, 0.35f, 0.62f, 0.91f)
        val result = HSLColor.fromRGB(original).toRGB()
        assertColorClose(original, result)
    }


    // ---------- RGB & HSP ----------

    @Test
    fun rgb_toHSP_toRGB_roundTrip_red() = runTest {
        val original = Color.red
        val result = original.toHSP().toRGB()
        assertColorClose(original, result)
    }

    @Test
    fun rgb_toHSP_toRGB_roundTrip_random() = runTest {
        val original = Color(1f, 0.81f, 0.22f, 0.44f)
        val result = original.toHSP().toRGB()
        assertColorClose(original, result)
    }


    // ---------- Edge Cases ----------

    @Test
    fun black_roundTrip_allModels() = runTest {
        val original = Color.black
        assertColorClose(original, original.toHSV().toRGB())
        assertColorClose(original, HSLColor.fromRGB(original).toRGB())
        assertColorClose(original, original.toHSP().toRGB())
    }


    @Test
    fun white_roundTrip_allModels() = runTest {
        val original = Color.white
        assertColorClose(original, original.toHSV().toRGB())
        assertColorClose(original, HSLColor.fromRGB(original).toRGB())
        assertColorClose(original, original.toHSP().toRGB())
    }


    @Test
    fun gray_roundTrip_allModels() = runTest {
        val original = Color.gray
        assertColorClose(original, original.toHSV().toRGB())
        assertColorClose(original, HSLColor.fromRGB(original).toRGB())
        assertColorClose(original, original.toHSP().toRGB())
    }


    // ---------- Known Value Tests ----------

    @Test
    fun hsv_knownValues() = runTest {
        val hsv = HSVColor(
            alpha = 1f,
            hue = Angle(0f),
            saturation = 1f,
            value = 1f
        )
        val rgb = hsv.toRGB()
        assertColorClose(Color.red, rgb)
    }


    @Test
    fun hsl_knownValues() = runTest {
        val hsl = HSLColor(
            alpha = 1f,
            hue = Angle(0f),
            saturation = 1f,
            lightness = 0.5f
        )
        val rgb = hsl.toRGB()
        assertColorClose(Color.red, rgb)
    }


    @Test
    fun hsp_knownValues() = runTest {
        val hsp = HSPColor(
            alpha = 1f,
            hue = Angle(0f),
            saturation = 1f,
            brightness = sqrt(HSPColor.redBrightness)
        )
        val rgb = hsp.toRGB()
        assertTrue(rgb.red > rgb.green)
        assertTrue(rgb.red > rgb.blue)
    }


    // ---------- Alpha Preservation ----------

    @Test
    fun alpha_preserved_HSV() = runTest {
        val original = Color(0.42f, 0.1f, 0.2f, 0.3f)
        val result = original.toHSV().toRGB()
        assertEquals(original.alpha, result.alpha, 0.01f)
    }


    @Test
    fun alpha_preserved_HSL() = runTest {
        val original = Color(0.42f, 0.1f, 0.2f, 0.3f)
        val result = HSLColor.fromRGB(original).toRGB()
        assertEquals(original.alpha, result.alpha, 0.01f)
    }


    @Test
    fun alpha_preserved_HSP() = runTest {
        val original = Color(0.42f, 0.1f, 0.2f, 0.3f)
        val result = original.toHSP().toRGB()
        assertEquals(original.alpha, result.alpha, 0.01f)
    }

}