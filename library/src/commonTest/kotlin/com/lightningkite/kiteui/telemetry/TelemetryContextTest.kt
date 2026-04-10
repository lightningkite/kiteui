package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.*

class TelemetryContextTest {

    @Test
    fun traceIdFromContextElement() = runTest {
        val ctx = TelemetryContext(traceId = "aaaa1111bbbb2222cccc3333dddd4444", spanId = "1234567890abcdef")
        withContext(ctx) {
            assertEquals("aaaa1111bbbb2222cccc3333dddd4444", coroutineContext.traceId())
        }
    }

    @Test
    fun spanIdFromContextElement() = runTest {
        val ctx = TelemetryContext(traceId = "aaaa1111bbbb2222cccc3333dddd4444", spanId = "1234567890abcdef")
        withContext(ctx) {
            assertEquals("1234567890abcdef", coroutineContext.spanId())
        }
    }

    @Test
    fun emptyWhenNoContext() = runTest {
        assertEquals("", coroutineContext.traceId())
        assertEquals("", coroutineContext.spanId())
    }

    @Test
    fun contextElementInstallsAndRetrievesCorrectly() = runTest {
        val ctx = TelemetryContext(traceId = "aabb", spanId = "ccdd")
        withContext(ctx) {
            val retrieved = coroutineContext[TelemetryContext]
            assertNotNull(retrieved)
            assertEquals("aabb", retrieved.traceId)
            assertEquals("ccdd", retrieved.spanId)
        }
    }

    @Test
    fun emptyContextHasNoElement() {
        val element = EmptyCoroutineContext[TelemetryContext]
        assertNull(element)
    }

    @Test
    fun childCoroutineInheritsContext() = runTest {
        val ctx = TelemetryContext(traceId = "parent_trace_id_00000000000000", spanId = "parent_span_1234")
        withContext(ctx) {
            // Launch a child — it should inherit the TelemetryContext
            withContext(EmptyCoroutineContext) {
                // EmptyCoroutineContext is merged with parent, so TelemetryContext survives
                assertEquals("parent_trace_id_00000000000000", coroutineContext.traceId())
                assertEquals("parent_span_1234", coroutineContext.spanId())
            }
        }
    }

    @Test
    fun viewPathFromProvider() = runTest {
        val provider = ViewPathProvider { "AppNav/content[0]/button[2]" }
        val ctx = TelemetryContext(viewPathProvider = provider)
        withContext(ctx) {
            assertEquals("AppNav/content[0]/button[2]", coroutineContext.viewPath())
        }
    }

    @Test
    fun viewPathEmptyWithoutProvider() = runTest {
        val ctx = TelemetryContext(traceId = "aa", spanId = "bb")
        withContext(ctx) {
            assertEquals("", coroutineContext.viewPath())
        }
    }
}
