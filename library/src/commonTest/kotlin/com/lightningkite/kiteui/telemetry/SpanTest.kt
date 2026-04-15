package com.lightningkite.kiteui.telemetry

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class SpanTest {

    private val recordedSpans = mutableListOf<OtlpSpan>()

    @BeforeTest
    fun setup() {
        recordedSpans.clear()
        spanRecorder = { recordedSpans.add(it) }
    }

    @AfterTest
    fun teardown() {
        spanRecorder = null
    }

    private fun sampledContext(
        traceId: String = "aabb1122ccdd3344eeff5566aabb7788",
        spanId: String = "1122334455667788",
    ) = TelemetryContext(traceId = traceId, spanId = spanId, sampled = true)

    @Test
    fun spanRecordsOnCompletion() = runTest {
        withContext(sampledContext()) {
            span("my-work") { /* do nothing */ }
        }

        assertEquals(1, recordedSpans.size)
        assertEquals("my-work", recordedSpans[0].name)
    }

    @Test
    fun spanUsesParentTraceAndSpanIds() = runTest {
        withContext(sampledContext(traceId = "aaaa", spanId = "bbbb")) {
            span("child") {}
        }

        val s = recordedSpans.single()
        assertEquals("aaaa", s.traceId)
        assertEquals("bbbb", s.parentSpanId)
        assertTrue(s.spanId.isNotEmpty())
        assertNotEquals("bbbb", s.spanId, "Should get its own span ID")
    }

    @Test
    fun spanReturnsBlockResult() = runTest {
        val result = withContext(sampledContext()) {
            span("compute") { 42 }
        }
        assertEquals(42, result)
    }

    @Test
    fun spanPropagatesErrorStatus() = runTest {
        assertFailsWith<IllegalStateException> {
            withContext(sampledContext()) {
                span("failing") { error("boom") }
            }
        }

        val s = recordedSpans.single()
        assertNotNull(s.status)
        assertEquals(2, s.status!!.code)
        assertEquals("boom", s.status!!.message)
    }

    @Test
    fun spanRethrowsException() = runTest {
        val ex = assertFailsWith<IllegalArgumentException> {
            withContext(sampledContext()) {
                span("bad") { throw IllegalArgumentException("nope") }
            }
        }
        assertEquals("nope", ex.message)
    }

    @Test
    fun nestedSpansFormHierarchy() = runTest {
        withContext(sampledContext(spanId = "parent")) {
            span("outer") {
                span("inner") {}
            }
        }

        assertEquals(2, recordedSpans.size)
        val inner = recordedSpans[0] // inner completes first
        val outer = recordedSpans[1]

        assertEquals("inner", inner.name)
        assertEquals("outer", outer.name)
        assertEquals(outer.spanId, inner.parentSpanId, "Inner should be child of outer")
        assertEquals("parent", outer.parentSpanId, "Outer should be child of original context")
    }

    @Test
    fun httpCallsInsideSpanBecomeChildren() = runTest {
        withContext(sampledContext(spanId = "action-span")) {
            span("my-span") {
                // Verify the coroutine context has the span's ID
                val ctx = kotlin.coroutines.coroutineContext[TelemetryContext]!!
                assertNotEquals("action-span", ctx.spanId, "Should have new span ID")
                assertEquals("aabb1122ccdd3344eeff5566aabb7788", ctx.traceId)
            }
        }
    }

    @Test
    fun spanNoOpWhenNoRecorder() = runTest {
        spanRecorder = null

        val result = withContext(sampledContext()) {
            span("ignored") { 99 }
        }

        assertEquals(99, result)
        assertTrue(recordedSpans.isEmpty())
    }

    @Test
    fun spanNoOpWhenNoContext() = runTest {
        val result = span("no-context") { 77 }

        assertEquals(77, result)
        assertTrue(recordedSpans.isEmpty())
    }

    @Test
    fun spanNoOpWhenNotSampled() = runTest {
        val unsampledCtx = TelemetryContext(
            traceId = "aaaa",
            spanId = "bbbb",
            sampled = false,
        )
        withContext(unsampledCtx) {
            span("unsampled") {}
        }

        assertTrue(recordedSpans.isEmpty(), "Unsampled spans should not be recorded")
    }

    @Test
    fun spanIncludesAttributes() = runTest {
        withContext(sampledContext()) {
            span(
                "tagged",
                attributes = listOf(
                    OtlpKeyValue("custom.key", OtlpAnyValue(stringValue = "custom-value"))
                )
            ) {}
        }

        val s = recordedSpans.single()
        val attr = s.attributes.find { it.key == "custom.key" }
        assertNotNull(attr)
        assertEquals("custom-value", attr.value.stringValue)
    }
}
