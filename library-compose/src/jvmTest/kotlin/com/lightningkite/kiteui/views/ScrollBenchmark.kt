package com.lightningkite.kiteui.views

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test

/**
 * Frame times while wheel-scrolling a long page of cards (backgrounds, rounded corners, a third elevated) under a still
 * pointer.  Prints statistics rather than asserting.  Left out of the default test run; see the plan doc for how to run it.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScrollBenchmark {
    @Test
    fun scrollCards() = runBlocking(Dispatchers.Main) {
        val root = KiteUiRoot("kiteui-benchmark", { testTheme }) {
            scrolling.col {
                repeat(200) { index ->
                    (if (index % 3 == 0) themed(Elevated) else card).row {
                        text("Card $index")
                        expanding.space()
                        if (index % 2 == 0) important.button { text("Act"); onClick { } } else button { text("Act"); onClick { } }
                    }
                }
            }
        }
        val scene = ImageComposeScene(1280, 800) { root.Content() }
        var time = 0L
        // One frame, including the work KiteUI queues on the main thread in response.
        suspend fun frame(): Long {
            val start = System.nanoTime()
            time += 16_000_000
            scene.render(time)
            repeat(3) { yield() }
            return System.nanoTime() - start
        }
        repeat(30) { frame() }

        val pointer = Offset(640f, 400f)
        scene.sendPointerEvent(PointerEventType.Move, pointer)
        repeat(10) { frame() }

        // Warm up, then measure: a wheel notch every frame, down the page and back up.
        val nanos = (0 until 2000).map { i ->
            val direction = if ((i / 50) % 2 == 0) 1f else -1f
            scene.sendPointerEvent(PointerEventType.Scroll, pointer, scrollDelta = Offset(0f, direction))
            frame()
        }.drop(100).sorted()
        fun ms(n: Long) = "%.2f".format(n / 1_000_000.0)
        println(
            "Scroll benchmark: median ${ms(nanos[nanos.size / 2])}ms, p90 ${ms(nanos[nanos.size * 9 / 10])}ms, " +
                "p99 ${ms(nanos[nanos.size * 99 / 100])}ms, max ${ms(nanos.last())}ms over ${nanos.size} frames"
        )
        scene.close()
        root.close()
    }
}

private object Elevated : Semantic("benchmark-elevated") {
    override fun default(theme: Theme): ThemeAndBack = theme.alter(elevation = 4.px).withBack
}
