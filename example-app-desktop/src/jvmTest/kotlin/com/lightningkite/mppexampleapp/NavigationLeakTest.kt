package com.lightningkite.mppexampleapp

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementWithChildren
import com.lightningkite.kiteui.views.KiteUiRoot
import com.lightningkite.reactive.context.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.lang.ref.WeakReference
import java.util.IdentityHashMap
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalComposeUiApi::class)
class NavigationLeakTest {
    private val routes = listOf(
        "/", "/docs", "/controls", "/docs/layout", "/docs/theming", "/docs/available-views", "/internal",
        "/settings", "/sample/login", "/docs/dialogs", "/counter", "/reactivity", "/docs/text",
    )
    private val rounds = 3

    // Not inline, so that no strong reference is left behind in the test's frame.
    private fun removed(seen: Map<Element, WeakReference<Element>>) = seen.filterKeys { it.underlyingNativeElement.isShutdown }.values.toList()
    private fun survivors(removed: List<WeakReference<Element>>) = removed.count { it.get() != null }

    @Test
    fun navigatingAwayDoesNotAccumulateElements() = runBlocking(Dispatchers.Main) {
        val navigator = PageNavigator { AutoRoutes }
        val root = KiteUiRoot("kiteui-example-test", { appTheme() }) { app(navigator) }
        val scene = ImageComposeScene(1280, 800) { root.Content() }
        var time = 0L
        suspend fun frames(n: Int) = repeat(n) {
            time += 16_000_000
            scene.render(time)
            repeat(3) { yield() }
        }
        val seen = IdentityHashMap<Element, WeakReference<Element>>()
        fun record(element: Element) {
            seen.getOrPut(element) { WeakReference(element) }
            (element as? ElementWithChildren)?.children?.forEach { record(it) }
        }
        frames(30)
        repeat(rounds) { round ->
            for (route in routes) {
                record(root.root!!)
                navigator.navigateUrlLikePath(route)
                frames(40)
                // Hovering changes elements' modifiers, as a real pointer would.
                scene.sendPointerEvent(PointerEventType.Move, Offset(600f + round, 380f))
                frames(5)
            }
        }
        navigator.navigateUrlLikePath("/")
        frames(60)
        val removed = removed(seen)
        seen.clear()
        // System.gc() only starts a concurrent collection in these tests (see the build file), which may not have
        // finished, so this gives it a few tries.
        var survivors = survivors(removed)
        var collections = 0
        while (survivors > 0 && collections++ < 20) {
            System.gc()
            frames(2)
            Thread.sleep(100)
            survivors = survivors(removed)
        }
        println("Navigation leak test: $survivors of ${removed.size} removed elements survive")
        assertEquals(0, survivors, "$survivors of ${removed.size} removed elements survive $rounds rounds of navigation")
        scene.close()
        root.close()
    }
}
