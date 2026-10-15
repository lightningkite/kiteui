package com.lightningkite.mppexampleapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.KiteUiRoot
import com.lightningkite.reactive.context.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test

/**
 * Frame times while wheel-scrolling the tests index (a long list of links) under a still pointer.
 * Prints statistics rather than asserting: run it to compare changes, e.g.
 * `./gradlew :example-app-desktop:jvmTest --tests '*ScrollBenchmark*' -i | grep 'Scroll benchmark'`.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScrollBenchmark {
    @Test
    fun scrollTestsIndex() = runBlocking(Dispatchers.Main) {
        val navigator = PageNavigator { AutoRoutes }
        val root = KiteUiRoot("kiteui-example-test", { appTheme() }) { app(navigator) }
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
        navigator.navigateUrlLikePath("/internal")
        repeat(30) { frame() }

        val pointer = Offset(640f, 400f)
        scene.sendPointerEvent(PointerEventType.Move, pointer)
        repeat(10) { frame() }

        // Warm up, then measure: a wheel notch every frame, down the list and back up.
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
