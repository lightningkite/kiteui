package com.lightningkite.kiteui.views

import android.os.Bundle
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.reactive.context.ReactiveContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for the reentrancy fix to `close()` in
 * [com.lightningkite.kiteui.views.overlay] (ViewContextExtensions.android.kt), fixed in
 * 94f1ebe6b. The pre-fix `close` only nulled `willRemove` inside `animateOut`'s completion
 * callback, so a second invocation of `close()` before that callback ran would re-enter
 * `willRemove?.let { ... }`, call `animateOut`/`removeChild` a second time on the same element,
 * and throw `IllegalArgumentException` from `NativeContainerElement.removeChild` since the
 * element was no longer a child.
 *
 * Animations are disabled for the whole test so `animateIn`/`animateOut` (animations.android.kt)
 * invoke their `done` callback synchronously - this makes `close()`'s `removeChild` call happen
 * immediately instead of after a real ViewPropertyAnimator, so the double-invocation race is
 * exercised deterministically rather than depending on animation timing.
 */
@RunWith(RobolectricTestRunner::class)
class OverlayCloseReentrancyTest {
    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_Mppexample)
            with(viewWriter) {
                frame {
                    // Use the frame itself as the overlay container, same as appBase() does in
                    // AppNavV2.kt (`coordinatorFrame { context.overlayFrame = this }`).
                    context.overlayFrame = this
                }
            }
        }
    }

    @Test
    fun secondCloseCallAfterFirstIsANoOpInsteadOfThrowing() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val context = controller.get().viewWriter.context
            val overlayFrame = context.overlayFrame!!

            var close: (() -> Unit)? = null
            // Animations stay disabled across close() too, not just the overlay() call: close() is
            // what invokes animateOut, and it is that completion callback which must run inline for
            // removeChild to have happened by the time we assert.
            animationsEnabled = false
            try {
                context.overlay(modal = true, navClosable = false, transition = ScreenTransitions.Fade) { remove ->
                    close = remove
                    text("dialog content")
                }

                assertEquals(1, overlayFrame.children.size, "overlay body should have been added as a single child")

                close!!.invoke()
                assertEquals(0, overlayFrame.children.size, "first close() should remove the overlay's child")

                // Pre-fix, this line throws IllegalArgumentException("... is not a child of ...")
                // because close() re-entered animateOut/removeChild on an already-removed element.
                close!!.invoke()
                assertEquals(0, overlayFrame.children.size, "second close() must be a no-op, not remove/throw again")
            } finally {
                animationsEnabled = true
            }
        }
    }

    @Test
    fun navClosableOverlayIsUnregisteredFromTheDismissStackExactlyOnce() {
        // navClosable=true registers close() via pushDismissableDialog. A dangling second
        // registration (from the old code re-running the willRemove?.let block) would let a
        // stray back-press find and invoke an already-closed dialog's dismiss lambda.
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val context = controller.get().viewWriter.context

            var close: (() -> Unit)? = null
            animationsEnabled = false
            try {
                context.overlay(modal = true, navClosable = true, transition = ScreenTransitions.Fade) { remove ->
                    close = remove
                    text("dialog content")
                }

                close!!.invoke()
                close!!.invoke()

                // The dialog was already dismissed by close(); nothing should remain on the stack for
                // a system back-press to find.
                assertEquals(false, context.dismissTopDialog())
            } finally {
                animationsEnabled = true
            }
        }
    }
}
