@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.rawImageUnsized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every [ImageScaleType] has to render, on every html target.
 *
 * [ImageScaleType.Stretch] used to be `TODO("Not supported yet")` in the `background-size` mapping,
 * so constructing a sizeless raw image with a perfectly ordinary enum value threw. The enum is
 * closed and public, so there was no way for a caller to know that one of its four members was a
 * trap - which is why this iterates the whole enum rather than testing Stretch alone.
 */
class RawImageScaleTypeSsrTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun renderHtml(build: ViewWriter.() -> Unit): String {
        val writer = Frame(ElementContext("/"))
        with(writer) { col { build() } }
        return buildString { writer.children[0].native.render(this) }
    }

    private fun renderScaleType(scaleType: ImageScaleType): String = renderHtml {
        rawImageUnsized(ImageRemote("https://example.com/a.png"), "an image", scaleType)
    }

    @Test
    fun everyScaleTypeRenders() {
        for (scaleType in ImageScaleType.entries) {
            val html = renderScaleType(scaleType)
            assertTrue(
                "background-size" in html,
                "$scaleType produced no background-size at all: $html",
            )
        }
    }

    @Test
    fun stretchFillsBothAxesIndependently() {
        val html = renderScaleType(ImageScaleType.Stretch)
        // Stretch is the one scale type that deliberately abandons the aspect ratio, so it cannot
        // reuse contain/cover - it has to name both axes.
        assertTrue(
            "100% 100%" in html,
            "Stretch must fill both axes independently: $html",
        )
    }

    @Test
    fun theOtherScaleTypesKeepTheirAspectRatioBehaviour() {
        // Guards the mapping as a whole: a careless edit to the `when` could satisfy the Stretch
        // assertion above by making every scale type stretch.
        assertTrue("contain" in renderScaleType(ImageScaleType.Fit), "Fit should map to contain")
        assertTrue("cover" in renderScaleType(ImageScaleType.Crop), "Crop should map to cover")
        assertTrue("auto" in renderScaleType(ImageScaleType.NoScale), "NoScale should map to auto")
    }
}
