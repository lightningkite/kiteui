@file:OptIn(InternalComposeUiApi::class)

package com.lightningkite.kiteui.views

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/** The mouse pointer over elements, as Compose asks the window to show it. */
class CursorTest {
    @Test
    fun clickableElementsShowAHand() = runBlocking(Dispatchers.Main) {
        // Claude: the test rule's scene doesn't report cursors, so this one has a platform that records them.
        // Compose only sets one on a change, so never set means the default.
        var icon: PointerIcon = PointerIcon.Default
        val context = object : PlatformContext by PlatformContext.Empty() {
            override fun setPointerIcon(pointerIcon: PointerIcon) {
                icon = pointerIcon
            }
        }
        // The same app id as showKiteUi's, since a process stores settings for only one app.
        val root = KiteUiRoot("kiteui-test", { testTheme }) {
            col {
                sizeConstraints(height = 50.px).text("Text")
                sizeConstraints(height = 50.px).button { text("Button"); onClick { } }
                sizeConstraints(height = 50.px).button { text("Disabled"); enabled = false }
            }
        }
        val recomposer = FrameRecomposer(coroutineContext) {}
        val scene = CanvasLayersComposeScene(recomposer, Density(1f), size = IntSize(400, 300), platformContext = context)
        try {
            scene.setContent(recomposer.compositionContext) { root.Content() }
            suspend fun pointAt(y: Float): PointerIcon {
                repeat(5) { recomposer.performFrame(System.nanoTime()); scene.measureAndLayout(); delay(20) }
                scene.sendPointerEvent(PointerEventType.Move, Offset(20f, y))
                repeat(5) { recomposer.performFrame(System.nanoTime()); scene.measureAndLayout(); delay(20) }
                return icon
            }
            assertEquals(PointerIcon.Default, pointAt(25f), "over text")
            assertEquals(PointerIcon.Hand, pointAt(75f), "over a button")
            // As on the web, where .clickable keeps its pointer while disabled.
            assertEquals(PointerIcon.Hand, pointAt(125f), "over a disabled button")
            assertEquals(PointerIcon.Default, pointAt(25f), "back over text")
        } finally {
            scene.close()
            recomposer.close()
            root.close()
        }
    }
}
