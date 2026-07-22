package com.lightningkite.kiteui

import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.ssr.ssrResource
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.reactive.core.LateInitSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the deterministic settle point in [SsrContext.awaitAllResources] (a
 * QuiescenceTracker await, replacing the old `delay(1)` guess). These tests use real
 * asynchronous loads and chained async bindings, so they would flake or fail if the
 * settle point raced the reactive propagation. - by Claude
 */
class SsrQuiescenceTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    @Test
    fun resourceLoadAndChainedAsyncSettleBeforeSerialize() = runBlocking {
        val context = SsrContext("/")
        context.render {
            col {
                val user = ssrResource("user") {
                    delay(50) // genuinely asynchronous load on the loading scope
                    "Jane Doe"
                }
                text {
                    ::content {
                        user().handle(
                            success = { "Name: $it" },
                            notReady = { "Loading..." },
                            exception = { "Error" }
                        )
                    }
                }
                // A binding that starts MORE async work when the resource lands - the propagation
                // tail the old delay(1) raced against. The 10ms delay here is longer than the old
                // 1ms grace period, so this test fails without real quiescence tracking.
                text {
                    ::content {
                        user().handle(
                            success = { name -> async(name) { delay(10); "UPPER:" + name.uppercase() } },
                            notReady = { "Pending" },
                            exception = { "Error" }
                        )
                    }
                }
            }
        }

        context.awaitAllResources()
        val html = context.serialize().html
        assertTrue(html.contains("Name: Jane Doe"), "resource value must be serialized, got: $html")
        assertTrue(html.contains("UPPER:JANE DOE"), "chained async result must be serialized, got: $html")
        assertFalse(html.contains("Loading..."), "no loading placeholders may remain, got: $html")
        context.cancel()
    }

    @Test
    fun neverReadySourceDoesNotHangSsr() = runBlocking {
        // Quiescence is work-based, not readiness-based: a binding stuck on a source that never
        // becomes ready has no running work, so SSR settles immediately and serializes the
        // binding's loading state rather than hanging the request.
        val context = SsrContext("/")
        val never = LateInitSignal<String>()
        context.render {
            col {
                text {
                    ::content { "value: " + never() }
                }
                text("static content")
            }
        }

        withTimeout(5_000) { context.awaitAllResources() }
        val html = context.serialize().html
        assertTrue(html.contains("static content"))
        assertFalse(html.contains("value:"), "never-ready binding must not have produced content")
        context.cancel()
    }
}
