package com.lightningkite.kiteui.views

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

// Endless animations run only while they can be seen, so a still screen draws nothing.
class IdleTest {
    private val fading = ThemeDerivation { it.copy(id = "fading", background = FadingColor(Color.red, Color.blue)).withBack }

    // Shows [app] and, once it has settled, counts the frames out of 30 that had anything to do.
    private fun busyFrames(app: ViewWriter.() -> Unit): Int = runBlocking(Dispatchers.Main) {
        val root = KiteUiRoot("kiteui-test", { testTheme }, app)
        val scene = ImageComposeScene(400, 300) { root.Content() }
        var time = 0L
        suspend fun frames(n: Int): Int = (1..n).count {
            Snapshot.sendApplyNotifications()
            val busy = scene.hasInvalidations()
            time += 16_000_000
            scene.render(time)
            repeat(3) { yield() }
            busy
        }
        frames(60)
        frames(30).also {
            scene.close()
            root.close()
        }
    }

    @Test
    fun spinnersTurnOnlyWhileSeen() {
        assertEquals(30, busyFrames { activityIndicator() }, "a spinner on screen turns")
        assertEquals(0, busyFrames { activityIndicator { opacity = 0.0 } }, "a transparent spinner (an image view's, once loaded)")
        assertEquals(0, busyFrames { frame { visible = false; activityIndicator() } }, "a spinner in an invisible frame")
        assertEquals(0, busyFrames {
            scrolling.col {
                sizeConstraints(height = 1000.px).space()
                activityIndicator()
            }
        }, "a spinner scrolled out of the window")
    }

    @Test
    fun fadingColorsFadeOnlyWhileSeen() {
        assertEquals(30, busyFrames { col { themed(fading).frame { text("Loading") } } }, "a fading background on screen fades")
        assertEquals(0, busyFrames { col { themed(fading).frame { opacity = 0.0; text("Loading") } } }, "a transparent one")
        assertEquals(0, busyFrames {
            scrolling.col {
                sizeConstraints(height = 1000.px).space()
                themed(fading).frame { text("Loading") }
            }
        }, "one scrolled out of the window")
    }
}
