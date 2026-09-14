package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.activityIndicator
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.navigatorView
import com.lightningkite.reactive.context.await
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.launch
import kotlin.test.Test

/**
 * Regression tests for a "redirect page" - a page that, from a coroutine launched in its own
 * render() body, immediately swaps itself out via PageNavigator (the standard pattern for an
 * auth gate: show a spinner, then replace() once the login check resolves). If the launched
 * coroutine doesn't actually suspend (e.g. the check resolves synchronously), the navigation call
 * lands *while the redirecting page's own write() is still in progress*, re-entering reactive
 * machinery that is still mid-calculation. That must still result in the target page being shown;
 * it must never be silently dropped.
 *
 * There are two independent places this can go wrong, exercised separately below:
 * - [SwapView]'s own `swapping()` reactive block, when the very first page shown is already
 *   present on `navigator.stack` before `navigatorView()` is even built ([replaceFromRenderTimeLaunchIsNotDropped],
 *   [navigateFromRenderTimeLaunchIsNotDropped]).
 * - [PageNavigator.currentPage]'s `remember { }`, when the *first* page is instead pushed via a
 *   separate `navigate()`/`reset()` call after `navigatorView()` is already mounted
 *   ([replaceFromRenderTimeLaunchAfterMountIsNotDropped], [replaceAfterAwaitingAnAlreadyReadySignalFromRenderTimeLaunchIsNotDropped]).
 *   In that shape, the re-entrant `stack` mutation from inside render() lands while
 *   `currentPage`'s own calculation is still unwinding the listener cascade from the *first*
 *   mutation, which is what actually reproduces the originally-reported freeze - the mount-before-navigate
 *   order matters far more than whether the coroutine suspends.
 */
class NavigateDuringRenderTest {

    private object TargetPage : Page {
        override fun ElementWriter.CanAddTheme.render() {
            text("target page content")
        }
    }

    private object RedirectPage : Page {
        override fun ElementWriter.CanAddTheme.render() {
            frame { centered.activityIndicator() }
            launch {
                // No actual suspension: this runs to completion synchronously on the Unconfined
                // test dispatcher, re-entering the navigator's reactive machinery while the
                // RedirectPage's own write() (which is what launched this coroutine) is still on
                // the call stack.
                context.pageNavigator.replace(TargetPage)
            }
        }
    }

    private object PushRedirectPage : Page {
        override fun ElementWriter.CanAddTheme.render() {
            frame { centered.activityIndicator() }
            launch { context.pageNavigator.navigate(TargetPage) }
        }
    }

    private class AwaitThenRedirectPage(val session: Signal<Int>) : Page {
        override fun ElementWriter.CanAddTheme.render() {
            frame { centered.activityIndicator() }
            launch {
                // Goes through the suspend await() machinery (com.lightningkite.reactive.context.await)
                // rather than a bare function call, even though the session is already Ready and this
                // returns without actually suspending - matching a loggedInOrNull() that resolves
                // synchronously because the session is already known. This turned out not to be the
                // deciding factor (see replaceFromRenderTimeLaunchAfterMountIsNotDropped, which
                // reproduces the same freeze with a bare call), but it's kept as the closest match to
                // the originally-reported shape.
                val me = session.await()
                context.pageNavigator.replace(TargetPage)
            }
        }
    }

    @Test
    fun replaceFromRenderTimeLaunchIsNotDropped() {
        lateinit var navigator: PageNavigator
        uiTest(content = {
            navigator = PageNavigator { Routes(parsers = emptyList(), renderers = emptyMap()) }
            navigator.stack.value = listOf(RedirectPage)
            navigatorView(navigator)
        }) {
            assertTextVisible("target page content")
        }
        // The navigator's own state must agree with what's on screen - it must not be stuck
        // pointing at the redirect page while the reactive machinery silently gave up.
        kotlin.test.assertEquals(TargetPage, navigator.stack.value.lastOrNull())
    }

    @Test
    fun navigateFromRenderTimeLaunchIsNotDropped() {
        // Same re-entrant scenario as above, but via navigate() (push) rather than replace().
        lateinit var navigator: PageNavigator
        uiTest(content = {
            navigator = PageNavigator { Routes(parsers = emptyList(), renderers = emptyMap()) }
            navigator.stack.value = listOf(PushRedirectPage)
            navigatorView(navigator)
        }) {
            assertTextVisible("target page content")
        }
        kotlin.test.assertEquals(TargetPage, navigator.stack.value.lastOrNull())
    }

    @Test
    fun replaceFromRenderTimeLaunchAfterMountIsNotDropped() {
        // navigatorView is mounted with an *empty* stack, and the first page is pushed afterwards
        // via navigate() - rather than pre-seeding navigator.stack before mounting navigatorView,
        // as in the two tests above. That ordering means the navigate() call below is what first
        // activates PageNavigator.currentPage's remember{} (it has no listener until navigatorView
        // reads it), so the re-entrant replace() from inside RedirectPage.render() lands while that
        // remember{} calculation is still unwinding its own listener cascade - a second, independent
        // place a redirect-during-render can be dropped, distinct from SwapView's own swapping().
        lateinit var navigator: PageNavigator
        uiTest(content = {
            navigator = PageNavigator { Routes(parsers = emptyList(), renderers = emptyMap()) }
            navigatorView(navigator)
            navigator.navigate(RedirectPage)
        }) {
            assertTextVisible("target page content")
        }
        kotlin.test.assertEquals(TargetPage, navigator.stack.value.lastOrNull())
    }

    @Test
    fun replaceAfterAwaitingAnAlreadyReadySignalFromRenderTimeLaunchIsNotDropped() {
        // The exact shape originally reported downstream: mount-then-navigate (see
        // replaceFromRenderTimeLaunchAfterMountIsNotDropped above) combined with awaiting an
        // already-ready Reactive before navigating, matching a loggedInOrNull() that resolves
        // synchronously because the session is already known.
        lateinit var navigator: PageNavigator
        uiTest(content = {
            navigator = PageNavigator { Routes(parsers = emptyList(), renderers = emptyMap()) }
            navigatorView(navigator)
            navigator.navigate(AwaitThenRedirectPage(Signal(42)))
        }) {
            assertTextVisible("target page content")
        }
        kotlin.test.assertEquals(TargetPage, navigator.stack.value.lastOrNull())
    }
}
