package com.lightningkite.mppexampleapp

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWithChildren
import com.lightningkite.kiteui.views.KiteUiRoot
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.core.BaseListenable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

/** A still page draws nothing, and navigating leaves no coroutines or listeners behind. */
@OptIn(ExperimentalComposeUiApi::class)
class IdleWorkTest {
    // Not /controls, whose progress bars move every 100ms.
    private val routes = listOf(
        "/", "/docs", "/docs/layout", "/docs/theming", "/docs/available-views", "/internal",
        "/settings", "/sample/login", "/docs/dialogs", "/counter", "/reactivity", "/docs/text",
    )

    @Test
    fun stillPagesDoNoWork() = runBlocking(Dispatchers.Main) {
        val navigator = PageNavigator { AutoRoutes }
        val root = KiteUiRoot("kiteui-example-test", { appTheme() }) { app(navigator) }
        val scene = ImageComposeScene(1280, 800) { root.Content() }
        var time = 0L
        // How many of the frames had anything to do.
        suspend fun frames(n: Int): Int = (1..n).count {
            Snapshot.sendApplyNotifications()
            val busy = scene.hasInvalidations()
            time += 16_000_000
            scene.render(time)
            repeat(3) { yield() }
            busy
        }
        fun coroutines(): Int {
            var n = 0
            fun count(job: Job): Unit = job.children.forEach { n++; count(it) }
            fun walk(e: Element) {
                count(e.underlyingNativeElement.coroutineContext[Job]!!)
                (e as? ElementWithChildren)?.children?.forEach { walk(it) }
            }
            walk(root.root!!)
            return n
        }
        fun listeners() = listOf(AppState.animationFrame, AppState.windowInfo, appTheme, navigator.currentPage, navVariant)
            .sumOf { (it as BaseListenable).listenerCount }

        val firstVisit = HashMap<String, Pair<Int, Int>>()
        repeat(2) {
            for (route in routes) {
                navigator.navigateUrlLikePath(route)
                frames(60)
                assertEquals(0, frames(30), "frames with work on $route once it has settled")
                val counts = coroutines() to listeners()
                assertEquals(firstVisit.getOrPut(route) { counts }, counts, "(coroutines, listeners) on $route, visited again")
            }
        }
        scene.close()
        root.close()
    }
}
